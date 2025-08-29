package com.example.mjg.services.migration.internal.migration_runner;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.FailedRecordGroup;
import com.example.mjg.services.migration.internal.reflective.RMatchWith;
import com.example.mjg.services.migration.internal.reflective.RMigrationUtils;
import com.example.mjg.storage.DataStoreRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@AllArgsConstructor
@Slf4j
public class MatchAndReduceRunner {
    private final MigrationRunner migrationRunner;

    public List<RecordProcessingContext>
    run(AtomicBoolean anyFailed, List<MigratableEntity> inputRecords) {
        List<RecordProcessingContext> inputContexts = startReduction(anyFailed, inputRecords);

        List<RMatchWith> rMatchWiths = migrationRunner.getRMatchWiths();
        // TODO: Probably should not use parallelStream() here,
        // TODO: since several simultaneous matching operations
        // TODO: could eat up too much memory.
        // TODO: Second, ordering is important!
        for (RMatchWith rMatchWith : rMatchWiths) {
            matchAndReduceRecordsPerMatching(anyFailed, rMatchWith, inputContexts, inputRecords);
        }

        return inputContexts;
    }

    private List<RecordProcessingContext> startReduction(
        AtomicBoolean anyFailed,
        List<MigratableEntity> inputRecords
    ) {
        RetryLogic retryLogic = RetryLogic
            .maxRetries(migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError().retryTimes())
            .retryDelayInSeconds(migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                anyFailed.set(true);
                migrationRunner.getMigrationErrorInvestigator()
                    .reportFailedRecords(
                        new FailedRecordGroup(
                            inputRecords,
                            migrationRunner,
                            migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError(),
                            exception
                        )
                    );
            })
            .debugContext("While starting reduction");

        var callStartReduction = retryLogic
            .withCallback(arg -> {
                RecordProcessingContext ctx = (RecordProcessingContext) arg;
                migrationRunner.getRMigrationUtils()
                    .callStartReductionMethod(ctx.getRecord(), ctx.getAggregates());

                return null;
            });

        List<RecordProcessingContext> inputContexts = inputRecords
            .stream()
            .map(record -> {
                RecordProcessingContext ctx = new RecordProcessingContext(record);
                try {
                    callStartReduction.apply(ctx);
                } catch (RetriesExhaustedException e) {
                    return null;
                }
                return ctx;
            })
            .filter(Objects::nonNull)
            .toList();

        return inputContexts;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void matchAndReduceRecordsPerMatching(
        AtomicBoolean anyFailed,
        RMatchWith rMatchWith,
        List<RecordProcessingContext> recordContexts,
        List<MigratableEntity> inputRecords
    ) {
        DataStoreRegistry storeRegistry = migrationRunner.getStoreRegistry();
        RMigrationUtils rMigrationUtils = migrationRunner.getRMigrationUtils();
        String migrationFQCN = migrationRunner.getMigrationFQCN();

        DataStore<? extends MigratableEntity, ?, ?> store = storeRegistry
            .get(rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName());
        final int BATCH_SIZE = rMatchWith.getMatchWith().batchSize();

        final Cardinality cardinality = rMatchWith.getMatchWith().cardinality();

        var retryLogic = RetryLogic
            .maxRetries(rMatchWith.getMatchWith().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rMatchWith.getMatchWith().inCaseOfError().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                anyFailed.set(true);

                List<MigratableEntity> problematicRecords;
                if (arg instanceof RecordProcessingContext ctx) {
                    problematicRecords = List.of(ctx.getRecord());
                } else if (arg != null && arg.getClass().isArray()) {
                    RecordProcessingContext ctx1 = (RecordProcessingContext) (
                        ((Object[]) arg)[0]
                    );
                    problematicRecords = List.of(ctx1.getRecord());
                } else {
                    problematicRecords = inputRecords;
                }
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    problematicRecords,
                    migrationRunner,
                    rMatchWith.getMatchWith().inCaseOfError(),
                    exception
                );
                migrationRunner.getMigrationErrorInvestigator()
                    .reportFailedRecords(failedRecordGroup);
            })
            .debugContext(
                "While matching with store: " + rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName()
            );

        var getFirstPageOfRecordsWithFilters = retryLogic
            .withCallback((Map<Object, Object> filters) -> store.getFirstPageOfRecordsWithFilter((Map) filters, BATCH_SIZE));

        var getNextPageOfRecordsAfter = retryLogic
            .withCallback(((DataStore<MigratableEntity, Object, Object>) store)::getNextPageOfRecordsAfter);
        
        var callMatchingMethod = retryLogic
            .withCallback((RecordProcessingContext ctx) -> {
                return rMigrationUtils.callMatchingMethod(rMatchWith, ctx.getRecord(), ctx.getAggregates());
            });

        var callReduceMethod = retryLogic
            .withCallback((Object[] args) -> {
                RecordProcessingContext ctx = (RecordProcessingContext) args[0];
                List<MigratableEntity> moreMatchingRecords = (List<MigratableEntity>) args[1];
                rMigrationUtils.callReduceMethod(
                    rMatchWith,
                    ctx.getAggregates(),
                    moreMatchingRecords
                );
                return null;
            });

        // Group records by matching filter sets
        Map<Map<Object, Object>, List<RecordProcessingContext>> recordContextsByFiltersMap = new ConcurrentHashMap<>();
        // TODO: parallelStream here could improve speed.
        // TODO: But aggregates are being mutated...
        recordContexts.forEach(recordContext -> {
            try {
                Map<Object, Object> filterSet = callMatchingMethod.apply(recordContext);
                if (filterSet == null) return;

                List<RecordProcessingContext> contextsOfSameFilterSet = recordContextsByFiltersMap.computeIfAbsent(
                    filterSet,
                    k -> new ArrayList<>()
                );

                contextsOfSameFilterSet.add(recordContext);
            } catch (RetriesExhaustedException ignored) {}
        });

        // For each filter set, query the datastore, get matching records,
        // and reduce each input record on those matching records.
        for (var entry : recordContextsByFiltersMap.entrySet()) {
            final Map<Object, Object> filters = entry.getKey();
            final List<RecordProcessingContext> inputRecordContexts = entry.getValue();

            try {
                retryLogic.run(() -> {
                    DataPage<? extends MigratableEntity, ?, ?> matchingPage = getFirstPageOfRecordsWithFilters
                        .apply(filters);

                    long numMatchingRecords = 0;
                    while (matchingPage.getSize() > 0) {
                        List<MigratableEntity> moreMatchingRecords = (List<MigratableEntity>) matchingPage.getRecords();
                        numMatchingRecords += moreMatchingRecords.size();

                        CardinalityCheck.checkConformantInProgress(
                            migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality,
                            numMatchingRecords, moreMatchingRecords
                        );

                        // TODO: parallelStream() here might improve speed,
                        // TODO: but ctx.getAggregates() is being mutated, so...
                        for (RecordProcessingContext ctx : inputRecordContexts) {
                            callReduceMethod.apply(
                                new Object[]{ctx, moreMatchingRecords}
                            );
                            // rMigrationUtils.callReduceMethod(rMatchWith, ctx.getAggregates(), moreMatchingRecords);
                        }

                        matchingPage = getNextPageOfRecordsAfter.apply((DataPage) matchingPage);
                    }

                    CardinalityCheck.checkConformant(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);
                });
            } catch (RetriesExhaustedException ignored) {}
        }
    }

}

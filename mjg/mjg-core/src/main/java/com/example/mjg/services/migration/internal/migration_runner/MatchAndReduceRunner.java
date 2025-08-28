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
import com.example.mjg.services.migration.internal.fault_tolerance.MigrationProgressManager;
import com.example.mjg.services.migration.internal.reflective.RMatchWith;
import com.example.mjg.services.migration.internal.reflective.RMigrationUtils;
import com.example.mjg.storage.DataStoreRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class MatchAndReduceRunner {
    private final MigrationRunner migrationRunner;

    public List<RecordProcessingContext>
    run(List<MigratableEntity> inputRecords)
    throws RetriesExhaustedException {
        List<RecordProcessingContext> inputContexts = inputRecords
            .stream()
            .map(RecordProcessingContext::new)
            .toList();

        List<RMatchWith> rMatchWiths = migrationRunner.getRMatchWiths();
        // TODO: Probably should not use parallelStream() here,
        // TODO: since several matching in parallel could eat
        // TODO: up too much memory.
        for (RMatchWith rMatchWith : rMatchWiths) {
            matchAndReduceRecordsPerMatching(rMatchWith, inputContexts, inputRecords);
        }

        return inputContexts;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void matchAndReduceRecordsPerMatching(
        RMatchWith rMatchWith,
        List<RecordProcessingContext> recordContexts,
        List<MigratableEntity> inputRecords
    ) throws RetriesExhaustedException {
        DataStoreRegistry storeRegistry = migrationRunner.getStoreRegistry();
        MigrationProgressManager migrationProgressManager = migrationRunner.getMigrationProgressManager();
        RMigrationUtils rMigrationUtils = migrationRunner.getRMigrationUtils();
        String migrationFQCN = migrationRunner.getMigrationFQCN();

        DataStore<? extends MigratableEntity, ?, ?> store = storeRegistry
            .get(rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName());
        final int BATCH_SIZE = rMatchWith.getMatchWith().batchSize();

        final Cardinality cardinality = rMatchWith.getMatchWith().cardinality();

        var retryLogic = RetryLogic
            .maxRetries(rMatchWith.getMatchWith().retries())
            .retryDelayInSeconds(rMatchWith.getMatchWith().retryDelayInSeconds())
            .exceptionReporter((exception, argIgnored) -> {
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    inputRecords,
                    migrationRunner,
                    rMatchWith.getMatchWith().inCaseOfError(),
                    exception
                );
                migrationProgressManager.reportFailedRecords(failedRecordGroup);
            })
            .debugContext(
                "While matching with store: " + rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName()
            );

        var getFirstPageOfRecordsWithFilters = retryLogic
            .withCallback((Map<Object, Object> filters) -> store.getFirstPageOfRecordsWithFilter((Map) filters, BATCH_SIZE));

        var getNextPageOfRecordsAfter = retryLogic
            .withCallback(store::getNextPageOfRecordsAfter);

        // Group records by matching filter sets
        Map<Map<Object, Object>, List<RecordProcessingContext>> recordContextsByFiltersMap = recordContexts.stream()
            .collect(
                Collectors.groupingBy(recordContext -> rMigrationUtils.callMatchingMethod(rMatchWith, recordContext.getRecord()))
            );

        // For each filter set, query the datastore, get matching records,
        // and reduce each input record on those matching records.
        for (var entry : recordContextsByFiltersMap.entrySet()) {
            final Map<Object, Object> filters = entry.getKey();
            final List<RecordProcessingContext> inputRecordContexts = entry.getValue();

            retryLogic.run(() -> {
                try {
                    DataPage<? extends MigratableEntity, ?, ?> matchingPage = getFirstPageOfRecordsWithFilters
                        .apply(filters);

                    long numMatchingRecords = 0;
                    final AtomicBoolean isReductionInitialized = new AtomicBoolean(false);
                    while (matchingPage.getSize() > 0) {
                        List<MigratableEntity> moreMatchingRecords = (List<MigratableEntity>) matchingPage.getRecords();
                        numMatchingRecords += moreMatchingRecords.size();

                        CardinalityCheck.checkConformantInProgress(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);

                        // TODO: Parallel streaming here might improve speed,
                        // TODO: but ctx.getAggregates() is being mutated, so...
                        inputRecordContexts.forEach(ctx -> {
                            try {
                                if (!isReductionInitialized.get()) {
                                    rMigrationUtils.callStartReductionMethod(ctx.getAggregates());
                                }
                                rMigrationUtils.callReduceMethod(rMatchWith, ctx.getAggregates(), moreMatchingRecords);
                            } catch (Exception e) {
                                // report
                            }
                        });

                        matchingPage = getNextPageOfRecordsAfter.apply((DataPage) matchingPage);

                        isReductionInitialized.set(true);
                    }

                    CardinalityCheck.checkConformant(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);
                } catch (RetriesExhaustedException ignored) {}
            });
        }
    }

}

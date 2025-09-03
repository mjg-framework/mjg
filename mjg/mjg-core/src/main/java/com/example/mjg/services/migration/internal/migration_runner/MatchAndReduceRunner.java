package com.example.mjg.services.migration.internal.migration_runner;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.CardinalityViolationException;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.FailedRecordGroup;
import com.example.mjg.services.migration.internal.reflective.RMatchWith;
import com.example.mjg.services.migration.internal.reflective.RMigrationUtils;
import com.example.mjg.storage.DataStoreRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
@AllArgsConstructor
@Slf4j
public class MatchAndReduceRunner {
    private final MigrationRunner migrationRunner;

    public List<RecordProcessingContext> run(List<MigratableEntity> inputRecords) {
        List<RecordProcessingContext> inputContexts = startReduction(inputRecords);

        List<RMatchWith> rMatchWiths = migrationRunner.getRMatchWiths();
        // TODO: Probably should not use parallelStream() here,
        // TODO: since several simultaneous matching operations
        // TODO: could eat up too much memory.
        // TODO: Second, ordering is important!
        for (RMatchWith rMatchWith : rMatchWiths) {
            inputContexts = matchAndReduceRecordsPerMatching(rMatchWith, inputContexts, inputRecords);
            if (inputContexts.isEmpty()) break;
        }

        return inputContexts;
    }

    private List<RecordProcessingContext> startReduction(
            List<MigratableEntity> inputRecords) {
        RetryLogic retryLogic = RetryLogic
                .maxRetries(migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError().retryTimes())
                .retryDelayInSeconds(migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError()
                        .retryDelayInSeconds())
                .exceptionReporter((exception, arg) -> {
                    migrationRunner.getMigrationErrorInvestigator()
                            .reportFailedRecords(
                                    new FailedRecordGroup(
                                            inputRecords,
                                            migrationRunner,
                                            migrationRunner.getRForEachRecordFrom().getForEachRecordFrom()
                                                    .inCaseOfError(),
                                            exception));
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
    private List<RecordProcessingContext> matchAndReduceRecordsPerMatching(
        RMatchWith rMatchWith,
        List<RecordProcessingContext> recordContexts,
        List<MigratableEntity> inputRecords
    ) {
        DataStoreRegistry storeRegistry = migrationRunner.getStoreRegistry();
        RMigrationUtils rMigrationUtils = migrationRunner.getRMigrationUtils();
        String migrationFQCN = migrationRunner.getMigrationFQCN();

        DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet> store = storeRegistry
            .get(rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName());
        final int BATCH_SIZE = rMatchWith.getMatchWith().batchSize();

        final Cardinality cardinality = rMatchWith.getMatchWith().cardinality();

        var retryLogic = RetryLogic
            .maxRetries(rMatchWith.getMatchWith().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rMatchWith.getMatchWith().inCaseOfError().retryDelayInSeconds());

        BiConsumer<Exception, List<MigratableEntity>> reportProblematicRecords = (exception, problematicRecords) -> {
            FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                problematicRecords,
                migrationRunner,
                rMatchWith.getMatchWith().inCaseOfError(),
                exception
            );
            migrationRunner.getMigrationErrorInvestigator()
                .reportFailedRecords(failedRecordGroup);
        };

        Function<String, String> buildDebugContext = (String methodName) -> {
            return "While matching with store: " + rMatchWith.getDataStoreReflection().getStoreClass().getCanonicalName() + "\nand calling method: "
                + methodName + "()";
        };

        var getFirstPageOfRecords = retryLogic
            .exceptionReporter((exception, arg) -> reportProblematicRecords.accept(exception, inputRecords))
            .debugContext(buildDebugContext.apply("getFirstPageOfRecords"))
            .withCallback((DataFilterSet filterSet) -> ((DataStore<MigratableEntity, Serializable, DataFilterSet>) store).getFirstPageOfRecords(filterSet, BATCH_SIZE));

        var getNextPageOfRecords = retryLogic
            .exceptionReporter((exception, arg) -> reportProblematicRecords.accept(exception, inputRecords))
            .debugContext(buildDebugContext.apply("getNextPageOfRecords"))
            .withCallback(((DataStore<MigratableEntity, Serializable, DataFilterSet>) store)::getNextPageOfRecords);

        var callMatchingMethod = retryLogic
            .exceptionReporter((exception, arg) -> {
                if (arg instanceof RecordProcessingContext ctx) {
                    reportProblematicRecords.accept(exception, List.of(ctx.getRecord()));
                } else {
                    var internalError = new Exception("Internal error (please report to mjg's author): instead of a RecordProcessingContext, got " + arg);
                    reportProblematicRecords.accept(internalError, inputRecords);
                }
            })
            .debugContext(buildDebugContext.apply("callMatchingMethod"))
            .withCallback((RecordProcessingContext ctx) -> {
                return rMigrationUtils.callMatchingMethod(rMatchWith, ctx.getRecord(), ctx.getAggregates());
            });

        var callReduceMethod = retryLogic
            .exceptionReporter((exception, arg) -> {
                if (
                    arg != null && arg.getClass().isArray()
                    && ((Object[]) arg).length > 0
                    && ((Object[]) arg)[0] instanceof RecordProcessingContext ctx
                ) {
                    reportProblematicRecords.accept(exception, List.of(ctx.getRecord()));
                } else {
                    var internalError = new Exception("Internal error (please report to mjg's author): instead of a RecordProcessingContext at args[0], got args = " + arg);
                    reportProblematicRecords.accept(internalError, inputRecords);
                }
            })
            .debugContext(buildDebugContext.apply("callReduceMethod"))
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
        Map<DataFilterSet, List<RecordProcessingContext>> recordContextsByFiltersMap = new ConcurrentHashMap<>();
        // TODO: parallelStream here could improve speed.
        // TODO: But aggregates are being mutated...

        List<RecordProcessingContext> legitRecordContexts = new ArrayList<>();
        recordContexts = recordContexts.stream()
            .filter(Objects::nonNull)
            .map(recordContext -> {
                try {
                    DataFilterSet filterSet = callMatchingMethod.apply(recordContext);
                    if (filterSet == null) {
                        // Unmatched, not reduced, but still legit record context
                        legitRecordContexts.add(recordContext);
                    } else {
                        List<RecordProcessingContext> contextsOfSameFilterSet = recordContextsByFiltersMap.computeIfAbsent(
                            filterSet,
                            k -> new ArrayList<>()
                        );

                        contextsOfSameFilterSet.add(recordContext);
                    }
                    return recordContext;
                } catch (RetriesExhaustedException ignored) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();

        // For each filter set, query the datastore, get matching records,
        // and reduce each input record on those matching records.
        // Cardinality requirement is guaranteed per filter set.
        for (var entry : recordContextsByFiltersMap.entrySet()) {
            final DataFilterSet filterSet = entry.getKey();
            List<RecordProcessingContext> inputRecordContexts = entry.getValue();
            if (inputRecordContexts.isEmpty()) continue;

            try {
                DataPage<MigratableEntity, Serializable, DataFilterSet> matchingPage = getFirstPageOfRecords
                    .apply(filterSet);

                long numMatchingRecords = 0;
                while (matchingPage.getSize() > 0) {
                    List<MigratableEntity> moreMatchingRecords = matchingPage.getRecords();
                    numMatchingRecords += moreMatchingRecords.size();

                    CardinalityCheck.checkConformantInProgress(
                        migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality,
                        numMatchingRecords, moreMatchingRecords
                    );

                    // TODO: parallelStream() here might improve speed,
                    // TODO: but ctx.getAggregates() is being mutated, so...
                    inputRecordContexts = inputRecordContexts.stream()
                        .map(ctx -> {
                            try {
                                callReduceMethod.apply(
                                    new Object[]{ctx, moreMatchingRecords}
                                );
                                return ctx;
                            } catch (RetriesExhaustedException ignored) {
                                return null;
                            }
                        })
                        .toList();
                    
                    entry.setValue(inputRecordContexts);
                    if (inputRecordContexts.isEmpty()) break;
                    matchingPage = getNextPageOfRecords.apply((DataPage) matchingPage);
                }

                CardinalityCheck.checkConformant(migrationFQCN, rMatchWith.getMatchWith().toString(), cardinality, numMatchingRecords);
            } catch (CardinalityViolationException exception) {
                // Cardinality violation is no excuse, so not retried!
                final List<MigratableEntity> currentInputRecords = inputRecordContexts.stream()
                    .map(RecordProcessingContext::getRecord)
                    .toList();

                reportProblematicRecords.accept(exception, currentInputRecords);

                entry.setValue(List.of());
            } catch (RetriesExhaustedException ignored) {
                entry.setValue(List.of());
            }
        }

        legitRecordContexts.addAll(
            recordContextsByFiltersMap.values()
            .stream()
            .reduce(new ArrayList<>(), (result, element) -> {
                result.addAll(element);
                return result;
            })
        );

        legitRecordContexts.removeIf(Objects::isNull);

        return legitRecordContexts;
    }
}

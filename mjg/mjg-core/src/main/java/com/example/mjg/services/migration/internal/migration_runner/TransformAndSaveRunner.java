package com.example.mjg.services.migration.internal.migration_runner;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.CardinalityViolationException;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.FailedRecordGroup;
import com.example.mjg.services.migration.internal.fault_tolerance.SuccessfulRecordGroup;
import com.example.mjg.services.migration.internal.reflective.RTransformAndSaveTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Slf4j
class TransformAndSaveRunner {
    private final MigrationRunner migrationRunner;

    public TransformAndSaveRunner(MigrationRunner migrationRunner) {
        this.migrationRunner = migrationRunner;
    }

    public void run(
        List<RecordProcessingContext> inputContexts
    ) {
        Stream<RecordOutputContext> transformed = transform(inputContexts);
        saveAndResolveDuplicatesIfAny(transformed);
    }

    @Getter
    @AllArgsConstructor
    private static class RecordOutputContext {
        private MigratableEntity inputRecord;
        @Setter
        private List<MigratableEntity> outputRecords;
    }

    private Stream<RecordOutputContext> transform(
        List<RecordProcessingContext> inputContexts
    ) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        Cardinality transformCardinality = rTransformAndSaveTo.getTransformAndSaveTo().cardinality();

        var retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryDelayInSeconds());

        var handleRecordContext = retryLogic
            .exceptionReporter((exception, arg) -> {
                reportProblematicRecords(
                    exception,
                    List.of(((RecordProcessingContext) arg).getRecord())
                );
            })
            .debugContext("While transforming records")
            .withCallback(
            (RecordProcessingContext ctx) -> {
                try {
                    List<MigratableEntity> outputRecords = migrationRunner
                        .getRMigrationUtils()
                        .callTransformMethod(ctx.getAggregates(), ctx.getRecord());

                    CardinalityCheck.checkConformant(
                        migrationRunner.getMigrationFQCN(),
                        rTransformAndSaveTo.getTransformAndSaveTo().toString(),
                        transformCardinality,
                        outputRecords.size()
                    );

                    return new RecordOutputContext(
                        ctx.getRecord(), outputRecords
                    );
                } catch (CardinalityViolationException exception) {
                    // Cardinality violation is no excuse, so not retried!
                    reportProblematicRecords(exception, List.of(ctx.getRecord()));
                    return null;
                }
            });

        // TODO: Could use parallelStream here?
        return inputContexts
            .stream()
            .map(ctx -> {
                try {
                    return handleRecordContext.apply(ctx);
                } catch (RetriesExhaustedException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

    private void saveAndResolveDuplicatesIfAny(
        Stream<RecordOutputContext> outputContextsStream
    ) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        var retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryDelayInSeconds());

        var fromOutputContext_SaveAndResolveDuplicatesIfAny = retryLogic
            .exceptionReporter((exception, arg) -> {
                reportProblematicRecords(exception, List.of(((RecordOutputContext) arg).getInputRecord()));
            })
            .debugContext("While saving records")
            .withCallback(
            (RecordOutputContext ctx) -> {
                try {
                    migrationRunner.getOutputStore()
                        .saveAll(ctx.getOutputRecords());
                } catch (DuplicateDataException e) {
                    List<MigratableEntity> newOutputRecords = migrationRunner
                        .getRMigrationUtils()
                        .callHandleDuplicateMethod(
                            e,
                            ctx.getInputRecord(),
                            ctx.getOutputRecords()
                        );
                    
                    if (newOutputRecords == null) {
                        throw e;
                    }
                    
                    ctx.setOutputRecords(newOutputRecords);

                    // further exception...
                    migrationRunner.getOutputStore()
                        .saveAll(ctx.getOutputRecords());
                }   // ...or any other exception
                // is handled by retryLogic
                return null;
            }
        );

        final int BATCH_SIZE = rTransformAndSaveTo.getTransformAndSaveTo().batchSize();
        Set<Serializable> successfullyMigratedInputRecordIds = new HashSet<>();

        try {
            // Split into chunks/batches of output records that are transformed from one *same input record*.
            // Reason? For error reporting.
            // If one chunk is too large, split it further.

            // NOTE: A parallelStream here won't do any good I guess
            outputContextsStream.forEach(outputContext -> {
                List<MigratableEntity> outputRecords = outputContext.getOutputRecords();

                int beginPos = 0;
                int endPos = Math.min(BATCH_SIZE, outputRecords.size());
                boolean failed = false;

                while (endPos <= outputRecords.size()) {
                    if (beginPos >= endPos) break;
                    final List<MigratableEntity> outputRecordsBatch = outputRecords.subList(beginPos, endPos);
                    final RecordOutputContext subCtx = new RecordOutputContext(
                        outputContext.getInputRecord(),
                        outputRecordsBatch
                    );
                    try {
                        fromOutputContext_SaveAndResolveDuplicatesIfAny.apply(subCtx);
                    } catch (RetriesExhaustedException ignored) {
                        failed = true;
                    }
                    beginPos = endPos;
                    endPos = Math.min(endPos + BATCH_SIZE, outputRecords.size());
                }

                if (!failed) {
                    successfullyMigratedInputRecordIds.add(
                        outputContext.getInputRecord().getMigratableId()
                    );
                }
            });
        } finally {
            migrationRunner.getMigrationErrorInvestigator()
                .reportSuccessfulRecords(
                    new SuccessfulRecordGroup(
                        successfullyMigratedInputRecordIds,
                        migrationRunner
                    )
                );
        }
    }

    private void reportProblematicRecords(Exception exception, List<MigratableEntity> problematicRecords) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
            problematicRecords,
            migrationRunner,
            rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError(),
            exception
        );
        migrationRunner.getMigrationErrorInvestigator()
            .reportFailedRecords(failedRecordGroup);
    }
}

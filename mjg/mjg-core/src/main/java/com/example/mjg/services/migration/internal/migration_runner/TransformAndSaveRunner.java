package com.example.mjg.services.migration.internal.migration_runner;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.FailedRecordGroup;
import com.example.mjg.services.migration.internal.fault_tolerance.SuccessfulRecordGroup;
import com.example.mjg.services.migration.internal.reflective.RTransformAndSaveTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Getter
@Slf4j
class TransformAndSaveRunner {
    private final MigrationRunner migrationRunner;

    public TransformAndSaveRunner(MigrationRunner migrationRunner) {
        this.migrationRunner = migrationRunner;
    }

    public void run(
        AtomicBoolean anyFailed,
        List<RecordProcessingContext> inputContexts
    ) {
        Stream<RecordOutputContext> transformed = transform(anyFailed, inputContexts);
        saveAndResolveDuplicatesIfAny(anyFailed, transformed);
    }

    @Getter
    @AllArgsConstructor
    private static class RecordOutputContext {
        private MigratableEntity inputRecord;
        private List<MigratableEntity> outputRecords;
    }

    private Stream<RecordOutputContext> transform(
        AtomicBoolean anyFailed,
        List<RecordProcessingContext> inputContexts
    ) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        Cardinality transformCardinality = rTransformAndSaveTo.getTransformAndSaveTo().cardinality();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                anyFailed.set(true);
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    List.of(((RecordProcessingContext) arg).getRecord()),
                    migrationRunner,
                    rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError(),
                    exception
                );
                migrationRunner.getMigrationErrorInvestigator()
                    .reportFailedRecords(failedRecordGroup);
            })
            .debugContext("While transforming records");

        var handleRecordContext = retryLogic.withCallback(
            (RecordProcessingContext ctx) -> {
                List<MigratableEntity> outputRecords = migrationRunner
                    .getRMigrationUtils()
                    .callTransformMethod(ctx.getAggregates(), ctx.getRecord());

                CardinalityCheck.checkConformant(
                    migrationRunner.getMigrationFQCN(),
                    rTransformAndSaveTo.getTransformAndSaveTo().toString(),
                    transformCardinality,
                    outputRecords.size()
                );
                return outputRecords;
            }
        );

        // TODO: Could use parallelStream here?
        return inputContexts
            .stream()
            .map(ctx -> {
                List<MigratableEntity> outputRecords;
                try {
                    outputRecords = handleRecordContext.apply(ctx);
                } catch (RetriesExhaustedException e) {
                    return null;
                }
                return new RecordOutputContext(
                    ctx.getRecord(),
                    outputRecords
                );
            })
            .filter(Objects::nonNull);
    }

    private void saveAndResolveDuplicatesIfAny(
        AtomicBoolean anyFailed,
        Stream<RecordOutputContext> outputContextsStream
    ) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryTimes())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                anyFailed.set(true);
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    List.of(((RecordOutputContext) arg).getInputRecord()),
                    migrationRunner,
                    rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError(),
                    exception
                );
                migrationRunner.getMigrationErrorInvestigator()
                    .reportFailedRecords(failedRecordGroup);
            })
            .debugContext("While saving records");

        var fromOutputContext_SaveAndResolveDuplicatesIfAny = retryLogic.withCallback(
            (RecordOutputContext ctx) -> {
                try {
                    migrationRunner.getOutputStore()
                        .saveMultiple(ctx.getOutputRecords());
                } catch (DuplicateDataException e) {
                    List<MigratableEntity> newOutputRecords = migrationRunner
                        .getRMigrationUtils()
                        .callHandleDuplicateMethod(
                            ctx.getInputRecord(),
                            ctx.getOutputRecords()
                        );
                    
                    if (newOutputRecords == null) {
                        throw e;
                    }
                    
                    // TODO: This trick will only work if we run everything
                    // TODO: in the same thread (which we currently do, but
                    // TODO: not so sure in the future!)
                    ctx.getOutputRecords().clear();
                    ctx.getOutputRecords().addAll(newOutputRecords);

                    // further exception
                    migrationRunner.getOutputStore()
                        .saveMultiple(ctx.getOutputRecords());
                }   // or any other exception
                // is handled by retryLogic
                return null;
            }
        );

        final int BATCH_SIZE = rTransformAndSaveTo.getTransformAndSaveTo().batchSize();

        List<MigratableEntity> successfulOutputRecords = new ArrayList<>();

        try {
            // Split into chunks/batches of output records that are transformed from one *same input record*.
            // Reason? For error reporting.
            // If one chunk is too large, split it further.
            // TODO: A parallelStream here won't do any good I guess
            outputContextsStream.forEach(outputContext -> {
                List<MigratableEntity> outputRecords = outputContext.getOutputRecords();

                int beginPos = 0;
                int endPos = Math.min(BATCH_SIZE, outputRecords.size());

                while (endPos <= outputRecords.size()) {
                    if (beginPos >= endPos) break;
                    final List<MigratableEntity> outputRecordsBatch = outputRecords.subList(beginPos, endPos);
                    final RecordOutputContext subCtx = new RecordOutputContext(
                        outputContext.getInputRecord(),
                        outputRecordsBatch
                    );
                    try {
                        fromOutputContext_SaveAndResolveDuplicatesIfAny.apply(subCtx);
                        successfulOutputRecords.add(subCtx.getInputRecord());
                    } catch (RetriesExhaustedException ignored) {
                    }
                    beginPos = endPos;
                    endPos = Math.min(endPos + BATCH_SIZE, outputRecords.size());
                }
            });
        } finally {
            if (!successfulOutputRecords.isEmpty()) {
                migrationRunner.getMigrationErrorInvestigator()
                    .reportSuccessfulRecords(
                        new SuccessfulRecordGroup(
                            successfulOutputRecords,
                            migrationRunner
                        )
                    );
            }
        }
    }
}

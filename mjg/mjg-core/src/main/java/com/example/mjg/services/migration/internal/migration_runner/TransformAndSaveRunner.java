package com.example.mjg.services.migration.internal.migration_runner;

import com.example.mjg.algorithms.cardinality_check.CardinalityCheck;
import com.example.mjg.algorithms.retrying.RetryLogic;
import com.example.mjg.config.Cardinality;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.services.migration.internal.RecordProcessingContext;
import com.example.mjg.services.migration.internal.fault_tolerance.FailedRecordGroup;
import com.example.mjg.services.migration.internal.reflective.RTransformAndSaveTo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Getter
class TransformAndSaveRunner {
    private final MigrationRunner migrationRunner;

    private final AtomicInteger fails;

    public TransformAndSaveRunner(MigrationRunner migrationRunner) {
        this.migrationRunner = migrationRunner;
        this.fails = new AtomicInteger(0);
    }

    public void run(List<RecordProcessingContext> inputContexts)
    throws RetriesExhaustedException {
        Stream<RecordOutputContext> transformed = transform(inputContexts);
        saveAndResolveDuplicatesIfAny(transformed);

        if (fails.get() > 0) {
            throw new RetriesExhaustedException("Number of failed transform-and-save operations:" + fails.get());
        }
    }

    @Getter
    @AllArgsConstructor
    private static class RecordOutputContext {
        private MigratableEntity inputRecord;
        private List<MigratableEntity> outputRecords;
    }

    private Stream<RecordOutputContext> transform(List<RecordProcessingContext> inputContexts) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        Cardinality transformCardinality = rTransformAndSaveTo.getTransformAndSaveTo().cardinality();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().retries())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    List.of(((RecordProcessingContext) arg).getRecord()),
                    migrationRunner,
                    rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError(),
                    exception
                );
                migrationRunner.getMigrationProgressManager()
                    .reportFailedRecords(failedRecordGroup);
                fails.incrementAndGet();
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

    private void saveAndResolveDuplicatesIfAny(Stream<RecordOutputContext> outputContextsStream) {
        final RTransformAndSaveTo rTransformAndSaveTo = migrationRunner.getRTransformAndSaveTo();

        RetryLogic retryLogic = RetryLogic
            .maxRetries(rTransformAndSaveTo.getTransformAndSaveTo().retries())
            .retryDelayInSeconds(rTransformAndSaveTo.getTransformAndSaveTo().retryDelayInSeconds())
            .exceptionReporter((exception, arg) -> {
                FailedRecordGroup failedRecordGroup = new FailedRecordGroup(
                    List.of(((RecordOutputContext) arg).getInputRecord()),
                    migrationRunner,
                    rTransformAndSaveTo.getTransformAndSaveTo().inCaseOfError(),
                    exception
                );
                migrationRunner.getMigrationProgressManager()
                    .reportFailedRecords(failedRecordGroup);
                fails.incrementAndGet();
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

        // Split into chunks/batches of output records that are transformed from one *same input record*.
        // Reason? For error reporting.
        // If one chunk is too large, split it further.
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
                } catch (RetriesExhaustedException ignored) {}
                beginPos = endPos;
                endPos = Math.min(endPos + BATCH_SIZE, outputRecords.size());
            }
        });
        // TODO: A parallel stream here won't do any good I guess
    }
}

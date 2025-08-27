package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.MigrationRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * While the retry logic is already implemented in
 * MigrationRunner, when all retries exhausted, the
 * faulty records are sent to an instance of this
 * class for investigation, and optionally log into
 * the MigrationProgressManager to be included in
 * the end JSON report.
 * 
 * Why send to this class in the first place? Because
 * there are errors while migrating a batch of several
 * records. So which one(s) of them are faulty? An
 * instance of this class handles that. It will narrow
 * down the search scope by iteratively split a
 * faulty record list by half, retry each, and go
 * on till some halves contain only 1 record, at
 * which point faulty records are IDENTIFIED and will
 * be reported to MigrationProgressManager, while all
 * the normal records in the batch got migrated
 * successfully.
 */
public class MigrationErrorInvestigator {
    private final LinkedBlockingQueue<FailedRecordGroup>
        failedRecordGroupQueue = new LinkedBlockingQueue<>();
    
    private static final FailedRecordGroup
        FailedRecordGroup_POISON = new FailedRecordGroup(
            null, null, null, null
        );
    
    private final MigrationProgressManager migrationProgressManager;

    private final MigrationRunner migrationRunner;

    private final int numThreads;
    private final ExecutorService pool;

    public MigrationErrorInvestigator(
        MigrationProgressManager migrationProgressManager,
        MigrationRunner migrationRunner
    ) {
        this.migrationProgressManager = migrationProgressManager;
        this.migrationRunner = migrationRunner;
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.pool = Executors.newFixedThreadPool(numThreads);
    }

    public void startInBackground() {
        Runnable workerRunnable = this::backgroundProcessingThreadRunnable;

        for (int i = 0; i < numThreads; ++i) {
            pool.submit(workerRunnable);
        }

        // tell the pool: "no more new tasks will be submitted"
        pool.shutdown();
    }
    
    public void handleException(
        List<MigratableEntity> records,
        ErrorResolution errorResolution,
        Exception exception
    ) {
        try {
            failedRecordGroupQueue.put(
                new FailedRecordGroup(
                    records,
                    migrationRunner,
                    errorResolution,
                    exception
                )
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * When the main thread is done running
     * this particular @Migration, call this
     * to finish all leftover error handling
     */
    public void migrationFinishing() {
        try {
            for (int i = 0; i < numThreads; i++) {
                failedRecordGroupQueue.put(FailedRecordGroup_POISON); // one poison per worker
            }
            pool.shutdown();
            // wait until all tasks are done
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // if this thread is interrupted, handle appropriately
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void backgroundProcessingThreadRunnable() {
        FailedRecordGroup failedRecordGroup;
        while (true) {
            try {
                failedRecordGroup = failedRecordGroupQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (failedRecordGroup.getRecords() == null /* poison */) {
                break;
            }

            List<MigratableEntity> records = failedRecordGroup.getRecords();
            int N = records.size();
            if (N > 1) {
                // Identify the culprit of failure among several records:
                // Divide the list by two, retry each.
                // For each one that fails, we could narrow down the search scope
                // by half!
                // (It is possible that both halves fail, or just one.)
                int mid = N / 2;
                List<MigratableEntity> firstHalfRecords = records.subList(0, mid);
                List<MigratableEntity> secondHalfRecords = records.subList(mid, N);
                migrationRunner.runWithRecordIdIn(firstHalfRecords.stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet()));
                migrationRunner.runWithRecordIdIn(secondHalfRecords.stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet()));
            } else if (N == 0) {
                continue;
            } else if (N == 1) {
                // This record group got sent here because records
                // in it failed to migrate. Here the group consists
                // of just ONE record.
                // So we IDENTIFIED the culprit - report it.

                this.migrationProgressManager.reportFailedRecords(failedRecordGroup);
            } else {
                throw new RuntimeException("unreachable: N = " + N);
            }
        }
    }
}

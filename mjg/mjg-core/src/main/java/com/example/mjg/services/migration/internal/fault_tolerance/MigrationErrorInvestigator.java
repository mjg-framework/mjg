package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
@Slf4j
public class MigrationErrorInvestigator {
    private final LinkedBlockingQueue<FailedRecordGroup>
        failedRecordGroupQueue;
    
    private static final FailedRecordGroup
        FailedRecordGroup_POISON = new FailedRecordGroup(
            null, null, null, null
        );
    
    private final MigrationProgressManager migrationProgressManager;

    private final MigrationRunner migrationRunner;

    private final int numThreads;
    private final ExecutorService pool;

    private final AtomicBoolean stopped;

    private final AtomicInteger numFailures;

    private final Set<Object> ignoredRecordIds;

    public int getNumFailures() {
        return numFailures.get();
    }

    public MigrationErrorInvestigator(
        MigrationProgressManager migrationProgressManager,
        MigrationRunner migrationRunner
    ) {
        this.failedRecordGroupQueue = new LinkedBlockingQueue<>();
        this.migrationProgressManager = migrationProgressManager;
        this.migrationRunner = migrationRunner;
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.pool = Executors.newFixedThreadPool(numThreads);
        this.stopped = new AtomicBoolean(false);
        this.numFailures = new AtomicInteger(0);

        this.ignoredRecordIds = migrationProgressManager.getIgnoredRecordIds(
            migrationRunner.getMigrationFQCN()
        );
    }

    public void startInBackground() {
        Runnable workerRunnable = this::backgroundProcessingThreadRunnable;

        for (int i = 0; i < numThreads; ++i) {
            pool.submit(workerRunnable);
        }

        // tell the pool: "no more new tasks will be submitted"
        pool.shutdown();
    }

    /**
     * When the main thread is done running
     * this particular @Migration, call this
     * to finish all leftover error handling
     */
    public void join() {
        try {
            for (int i = 0; i < numThreads; i++) {
                failedRecordGroupQueue.put(FailedRecordGroup_POISON); // one poison per worker
            }
            // wait until all tasks are done
            @SuppressWarnings("unused")
            boolean ignored = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // if this thread is interrupted, handle appropriately
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop with best effort
     */
    public void stop() {
        try {
            this.stopped.set(true);
            for (int i = 0; i < numThreads; i++) {
                failedRecordGroupQueue.put(FailedRecordGroup_POISON); // one poison per worker
            }
            pool.shutdownNow();
            @SuppressWarnings("unused")
            boolean ignored = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void backgroundProcessingThreadRunnable() {
        FailedRecordGroup failedRecordGroup;
        while (true) {
            if (stopped.get()) break;
            try {
                failedRecordGroup = failedRecordGroupQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (stopped.get()) break;

            if (failedRecordGroup.getRecords() == null /* poison */) {
                break;
            }

            List<MigratableEntity> records = failedRecordGroup.getRecords();
            int N = records.size();

            numFailures.decrementAndGet();

            if (N > 1) {
                // Identify the culprit of failure among several records:
                // Divide the list by two, retry each.
                // For each one that fails, we could narrow down the search scope
                // by half!
                // (It is possible that both halves fail, or just one.)
                int mid = N / 2;
                List<MigratableEntity> firstHalfRecords = records.subList(0, mid);
                List<MigratableEntity> secondHalfRecords = records.subList(mid, N);
                // TODO: Submit to a pool
                migrationRunner.runWithRecordIdIn(firstHalfRecords.stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet()));
                if (stopped.get()) break;
                // TODO: Submit to a pool
                migrationRunner.runWithRecordIdIn(secondHalfRecords.stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet()));
            } else if (N == 0) {
                continue;
            } else {
                // N == 1
                // This record group got sent here because records
                // in it failed to migrate. Here the group consists
                // of just ONE record.
                // So we IDENTIFIED the culprit - report it.

                this.migrationProgressManager.reportFailedRecords(failedRecordGroup);
                numFailures.incrementAndGet();
            }
        }
    }
    
    /**
     * Load from migrationProgressManager
     */
    public void retryPreviouslyFailedRecords() {
        List<MigratableEntity> failedRecords = migrationProgressManager.popFailedRecords(
            migrationRunner.getMigrationFQCN()
        );
        if (failedRecords.isEmpty()) {
            log.info("No previously failed records to retry from migration: " + migrationRunner.getMigrationFQCN());
        } else {
            log.info("Retrying " + failedRecords.size() + " previously failed records from migration: " + migrationRunner.getMigrationFQCN());

            // TODO: Submit to a pool
            migrationRunner.runWithRecordIdIn(
                failedRecords.stream()
                    .map(MigratableEntity::getMigratableId)
                    .collect(Collectors.toSet())
            );

            // reportFailedRecords(
            //     new FailedRecordGroup(
            //         failedRecords,
            //         migrationRunner,
            //         migrationRunner.getRForEachRecordFrom().getForEachRecordFrom().inCaseOfError(),
            //         new Exception("previously failed records (from restored migration progress)")
            //     )
            // );
        }
    }

    //////////////////////////////////////////////////
    /// PROXY METHODS FOR migrationProgressManager ///
    //////////////////////////////////////////////////

    public void reportSuccessfulRecords(
        SuccessfulRecordGroup successfulRecordGroup
    ) {
        migrationProgressManager.reportSuccessfulRecords(successfulRecordGroup);
    }

    public void excludeSuccessfullyMigratedRecordIds(
        Set<Object> inputRecordIds
    ) {
        migrationProgressManager.excludeSuccessfullyMigratedRecordIds(
            migrationRunner.getMigrationFQCN(),
            inputRecordIds
        );
    }

    public void excludeIgnoredRecordIds(
        Set<Object> inputRecordIds
    ) {
        inputRecordIds.removeAll(ignoredRecordIds);
    }

    public void restorePreviousProgress(MigrationProgress previousProgress) {
        migrationProgressManager.restorePreviousProgress(previousProgress);
    }

    public void reportFailedRecords(FailedRecordGroup failedRecordGroup) {
        if (failedRecordGroup.getRecords().size() == 0) {
            log.warn("WILL NOT PROCESS: FailedRecordGroup.getRecords().size() == 0 ; " + failedRecordGroup.getMigrationRunner().getMigrationFQCN() + " ; " + failedRecordGroup.getException() + " ; " + failedRecordGroup.getErrorResolution());
            return;
        }
        numFailures.incrementAndGet();
        try {
            failedRecordGroupQueue.put(failedRecordGroup);
        } catch (InterruptedException e) {
            migrationProgressManager.reportFailedRecords(failedRecordGroup);
            Thread.currentThread().interrupt();
        }
    }

    public void reportFatalError(Exception e) {
        numFailures.incrementAndGet();
        migrationProgressManager.reportFatalError(e);
        stop();
    }



    // public void handleException(
    //     List<MigratableEntity> records,
    //     ErrorResolution errorResolution,
    //     Exception exception
    // ) {
    //     numFailures.incrementAndGet();
    //     try {
    //         failedRecordGroupQueue.put(
    //             new FailedRecordGroup(
    //                 records,
    //                 migrationRunner,
    //                 errorResolution,
    //                 exception
    //             )
    //         );
    //     } catch (InterruptedException e) {
    //         Thread.currentThread().interrupt();
    //     }
    // }
}

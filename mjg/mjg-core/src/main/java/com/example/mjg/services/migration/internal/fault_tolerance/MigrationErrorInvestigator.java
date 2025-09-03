package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
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

    private final MigrationProgressManager migrationProgressManager;

    private final MigrationRunner migrationRunner;

    private final int numThreads;
    private final ExecutorService pool;

    private final AtomicBoolean stopped;

    private final AtomicBoolean noMoreExternalTasks;

    private final AtomicBoolean hasFatalError;

    /**
     * Failures that, even when retried, still did not resolve,
     * and have been submitted to migrationProgressManager
     */
    private final AtomicInteger unresolvableFailures;

    private final Set<Serializable> ignoredRecordIds;

    /**
     * Only calls when join() is done.
     */
    public int getNumFailures() {
        return failedRecordGroupQueue.size() + unresolvableFailures.get() + (hasFatalError.get() ? 1 : 0);
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
        this.unresolvableFailures = new AtomicInteger(0);
        this.noMoreExternalTasks = new AtomicBoolean(false);
        this.hasFatalError = new AtomicBoolean(false);

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
        this.noMoreExternalTasks.set(true);

        try {
            // wait until all tasks are done
            @SuppressWarnings("unused")
            boolean ignored = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // if this thread is interrupted, handle appropriately
            Thread.currentThread().interrupt();
        } finally {
            this.stopped.set(true);
            pool.shutdownNow();
        }
    }

    /**
     * Stop with best effort
     */
    public void stop() {
        this.noMoreExternalTasks.set(true);
        this.stopped.set(true);
        pool.shutdownNow();

        try {
            @SuppressWarnings("unused")
            boolean ignored = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
    }
    
    private void backgroundProcessingThreadRunnable() {
        FailedRecordGroup failedRecordGroup;
        while (true) {
            if (stopped.get()) break;
            try {
                failedRecordGroup = failedRecordGroupQueue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (failedRecordGroup == null) {
                if (noMoreExternalTasks.get() && failedRecordGroupQueue.isEmpty()) {
                    break;
                } else {
                    continue;
                }
            }
            if (stopped.get()) break;

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
                // So we IDENTIFIED the culprit.
                this.unresolvableFailures.incrementAndGet();
                // (We already reported it to migrationProgressManager.
                // Along the way, all records that successfully migrated
                // on retry also got reported, so no need to:
                //      migrationProgressManager.reportFailedRecords...
                // here.)
            }
        }
    }
    
    /**
     * Load from migrationProgressManager
     */
    public void retryPreviouslyFailedRecords() {
        Set<Serializable> failedRecordIds = migrationProgressManager.getFailedRecordIds(
            migrationRunner.getMigrationFQCN()
        );
        if (!failedRecordIds.isEmpty()) {
            log.info("Retrying " + failedRecordIds.size() + " previously failed records from migration: " + migrationRunner.getMigrationFQCN());

            // TODO: Submit to a pool
            migrationRunner.runWithRecordIdIn(failedRecordIds);
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
        Set<Serializable> inputRecordIds
    ) {
        migrationProgressManager.excludeSuccessfullyMigratedRecordIds(
            migrationRunner.getMigrationFQCN(),
            inputRecordIds
        );
    }

    public void excludeIgnoredRecordIds(
        Set<Serializable> inputRecordIds
    ) {
        inputRecordIds.removeAll(ignoredRecordIds);
    }

    public void reportFailedRecords(FailedRecordGroup failedRecordGroup) {
        if (failedRecordGroup.getRecords().isEmpty()) {
            log.warn("WILL NOT PROCESS: FailedRecordGroup.getRecords() is empty ; " + failedRecordGroup.getMigrationRunner().getMigrationFQCN() + " ; " + failedRecordGroup.getException() + " ; " + failedRecordGroup.getErrorResolution());
            return;
        }
        migrationProgressManager.reportFailedRecords(failedRecordGroup);
        try {
            failedRecordGroupQueue.put(failedRecordGroup);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void reportFatalError(Exception e) {
        hasFatalError.set(true);
        migrationProgressManager.reportFatalError(e);
        stop();
    }
}

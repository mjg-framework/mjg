package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgressPerMigrationClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class MigrationProgressManager {
    private final ConcurrentList<Consumer<MigrationProgress>> onProgressPersistenceCallbacks = new ArrayList<>();

    private static class MigrationProgressSafeAccess {
        private MigrationProgress migrationProgress = null;
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        public void read(Consumer<MigrationProgress> callback) {
            rwLock.readLock().lock();
            try {
                callback.accept(migrationProgress);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void readAndMutate(Function<MigrationProgress, MigrationProgress> callback) {
            rwLock.writeLock().lock();
            try {
                this.migrationProgress = callback.apply(migrationProgress);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    MigrationProgressSafeAccess migrationProgressSafeAccess = new MigrationProgressSafeAccess();

    public MigrationProgressManager() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                log.info("SIGTERM received, stopping gracefully. Please do not kill this process!");
                migrationProgressSafeAccess.read((migrationProgress) -> {
                    onProgressPersistenceCallbacks.forEach(callback -> {
                        callback.accept(migrationProgress);
                    });
                });
                log.info("OK");
            })
        );
    }

    public void onProgressPersistence(Consumer<MigrationProgress> onProgressPersistenceCallback) {
        onProgressPersistenceCallbacks.add(onProgressPersistenceCallback);
    }

    public void initialize(MigrationProgress migrationProgress) {
        this.migrationProgressSafeAccess.set(migrationProgress);
    }

    public void reportSuccessfulRecords(
        SuccessfulRecordGroup successfulRecordGroup
    ) {
        assertProgressInitialized();

        MigrationProgressPerMigrationClass x = migrationProgressSafeAccess.get().getMigrationProgress().computeIfAbsent(
            successfulRecordGroup.getMigrationRunner().getMigrationFQCN(),
            MigrationProgressPerMigrationClass::new
        );
    }

    public void reportFailedRecords(
        FailedRecordGroup failedRecordGroup
    ) {
        assertProgressInitialized();

        migrationProgressSafeAccess.getMigrationProgress().computeIfAbsent(
            failedRecordGroup.getMigrationRunner().getMigrationFQCN(),
            MigrationProgressPerMigrationClass::new
        );
    }

    private void assertProgressInitialized() {
        if (this.migrationProgressSafeAccess == null) {
            throw new IllegalStateException("MigrationProgress was not initialized!");
        }
    }
}

package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecord;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecordAction;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgressPerMigrationClass;
import com.example.mjg.utils.ConcurrentContainer;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
public class MigrationProgressManager {
    ConcurrentContainer<
        MigrationProgress
    > migrationProgressContainer = new ConcurrentContainer<>(null);

    ConcurrentContainer<
        List<Consumer<MigrationProgress>>
    > onProgressPersistenceCallbacksContainer = new ConcurrentContainer<>(
        new ArrayList<>()
    );

    public MigrationProgressManager() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                log.info("SIGTERM received, stopping gracefully. Please do not kill this process!");

                try {
                    // Wait for stuff to finalize
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}

                migrationProgressContainer.read(migrationProgress -> {
                    if (migrationProgress == null) {
                        log.warn("Migration progress is null, ignoring persistence callbacks");
                    }
                    onProgressPersistenceCallbacksContainer.read(callbacks -> {
                        callbacks.forEach(callback -> {
                            callback.accept(migrationProgress);
                        });
                    });
                });
                log.info("OK");
            })
        );
    }

    public void onProgressPersistence(Consumer<MigrationProgress> onProgressPersistenceCallback) {
        onProgressPersistenceCallbacksContainer.update(callbacks -> {
            callbacks.add(onProgressPersistenceCallback);
            return callbacks;
        });
    }

    public void initialize(MigrationProgress migrationProgress) {
        migrationProgressContainer.update(_oldMigrationProgress -> migrationProgress);
    }

    public void reportSuccessfulRecords(
        SuccessfulRecordGroup successfulRecordGroup
    ) {
        assertProgressInitialized();

        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                .getMigrationProgress()
                .computeIfAbsent(
                    successfulRecordGroup.getMigrationRunner().getMigrationFQCN(),
                    MigrationProgressPerMigrationClass::new
                );
            
            progressInThisMigrationClass.getMigratedRecordIds().addAll(
                successfulRecordGroup.getRecords().stream()
                    .map(MigratableEntity::getMigratableId)
                    .toList()
            );

            return migrationProgress;
        });
    }

    public void reportFailedRecords(
        FailedRecordGroup failedRecordGroup
    ) {
        assertProgressInitialized();

        String cause = ExceptionUtils.getRootCauseMessage(failedRecordGroup.getException());
        ErrorResolution.Strategy errorResolutionStrategy = failedRecordGroup
            .getErrorResolution().strategy();
        String effect = errorResolutionStrategy.toString();

        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                .getMigrationProgress()
                .computeIfAbsent(
                    failedRecordGroup.getMigrationRunner().getMigrationFQCN(),
                    MigrationProgressPerMigrationClass::new
                );
            
            progressInThisMigrationClass.getFailedRecords().addAll(
                failedRecordGroup.getRecords().stream()
                    .map(failedRecord -> {
                        return new FailedRecord(
                            failedRecord.getMigratableId(),
                            failedRecord.getMigratableDescription(),
                            cause,
                            effect,
                            (
                                errorResolutionStrategy == ErrorResolution.Strategy.REPORT_AND_PROCEED
                                ? new FailedRecordAction(FailedRecordAction.Type.IGNORE)
                                : new FailedRecordAction(FailedRecordAction.Type.RETRY)
                            ),
                            LocalDateTime.now()
                        );
                    })
                    .toList()
            );

            return migrationProgress;
        });
    }

    public void excludeSuccessfullyMigratedRecordIds(
        String migrationFQCN,
        Set<Object> recordIds
    ) {
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                .getMigrationProgress()
                .computeIfAbsent(
                    migrationFQCN,
                    MigrationProgressPerMigrationClass::new
                );
            
            Set<Object> migratedRecordIds = progressInThisMigrationClass
                .getMigratedRecordIds();
            
            recordIds.removeAll(migratedRecordIds);
        });
    }

    private void assertProgressInitialized() {
        AtomicBoolean isMigrationProgressInitialized = new AtomicBoolean(false);

        migrationProgressContainer.read(migrationProgress -> {
            if (migrationProgress != null) {
                isMigrationProgressInitialized.set(true);
            }
        });

        if (!isMigrationProgressInitialized.get()) {
            throw new IllegalStateException("MigrationProgress was not initialized!");
        }
    }
}

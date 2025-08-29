package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecord;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecordAction;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgressPerMigrationClass;
import com.example.mjg.utils.ConcurrentContainer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
public class MigrationProgressManager {
    ConcurrentContainer<MigrationProgress> migrationProgressContainer;

    ConcurrentContainer<List<Consumer<MigrationProgress>>> onProgressPersistenceCallbacksContainer;

    public MigrationProgressManager() {
        this.migrationProgressContainer = new ConcurrentContainer<>(new MigrationProgress());
        this.onProgressPersistenceCallbacksContainer = new ConcurrentContainer<>(
                new ArrayList<>());
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                log.info("Stopping gracefully. Please do not kill this process!");
                try {
                    // Wait for stuff to finalize
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    this.flush();
                }

                this.flush();

                log.info("Gracefully stopped");
            }));
    }

    /**
     * Get and remove.
     */
    public List<MigratableEntity> popFailedRecords(String migrationFQCN) {
        List<MigratableEntity> failedRecords = new ArrayList<>();
        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass migrationProgressOfThisMigrationClass = migrationProgress
                .getMigrationProgress().computeIfAbsent(
                    migrationFQCN,
                    k -> new MigrationProgressPerMigrationClass(migrationFQCN)
                );
            
            List<FailedRecord> failedRecordsInMigrationProgress = migrationProgressOfThisMigrationClass.getFailedRecords();
            List<MigratableEntity> _failedRecords = failedRecordsInMigrationProgress
                .stream()
                .filter(failedRecord -> FailedRecordAction.Type.RETRY.equals(failedRecord.getAction().getActionType()))
                .map(failedRecord -> (MigratableEntity) new FailedMigratableEntity(
                    failedRecord.getId(),
                    failedRecord.getDescription()
                ))
                .toList();
            
            failedRecords.addAll(_failedRecords);

            failedRecordsInMigrationProgress.clear();
            return migrationProgress;
        });

        return failedRecords;
    }

    public Set<Object> getIgnoredRecordIds(String migrationFQCN) {
        Set<Object> ignoredRecordIds = new HashSet<>();
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass migrationProgressOfThisMigrationClass = migrationProgress
                .getMigrationProgress().getOrDefault(
                    migrationFQCN,
                    new MigrationProgressPerMigrationClass(migrationFQCN)
                );
            
            Set<Object> ignoredRecordIds1 = migrationProgressOfThisMigrationClass.getFailedRecords()
                .stream()
                .filter(failedRecord -> failedRecord.getAction().getActionType() == FailedRecordAction.Type.IGNORE)
                .map(FailedRecord::getId)
                .collect(Collectors.toSet());
            
            ignoredRecordIds.addAll(ignoredRecordIds1);
        });

        return ignoredRecordIds;
    }

    @Getter
    @AllArgsConstructor
    public static class FailedMigratableEntity implements MigratableEntity {
        private Object migratableId;

        private String migratableDescription;
    }

    /**
     * Finalizes the migration progress data
     * and call persistence callbacks
     */
    public void flush() {
        migrationProgressContainer.read(migrationProgress -> {
            if (migrationProgress == null) {
                log.warn("Migration progress is null, ignoring persistence callbacks");
            }
            onProgressPersistenceCallbacksContainer.read(callbacks -> {
                if (callbacks.isEmpty()) {
                    log.warn("No callback to save migration progress. You are at risk of losing progress.");
                    // TODO: Write directly into a temp file here for backup in case of catastrophic failure
                }
                callbacks.forEach(callback -> {
                    try {
                        callback.accept(migrationProgress);
                    } catch (Exception e) {
                        log.error(
                            "Error while running progress persistence callback, but will be ignored",
                            e
                        );
                    }
                });
            });
        });
    }

    public void addProgressPersistenceCallback(Consumer<MigrationProgress> onProgressPersistenceCallback) {
        onProgressPersistenceCallbacksContainer.update(callbacks -> {
            callbacks.add(onProgressPersistenceCallback);
            return callbacks;
        });
    }

    public void removeAllProgressPersistenceCallbacks() {
        onProgressPersistenceCallbacksContainer.update(callbacks -> {
            callbacks.clear();
            return callbacks;
        });
    }

    public void restorePreviousProgress(MigrationProgress previousProgress) {
        migrationProgressContainer.update(_oldMigrationProgress -> previousProgress);
    }

    public void reportSuccessfulRecords(
            SuccessfulRecordGroup successfulRecordGroup) {
        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                    .getMigrationProgress()
                    .computeIfAbsent(
                            successfulRecordGroup.getMigrationRunner().getMigrationFQCN(),
                            MigrationProgressPerMigrationClass::new);

            progressInThisMigrationClass.getMigratedRecordIds().addAll(
                    successfulRecordGroup.getRecords().stream()
                            .map(MigratableEntity::getMigratableId)
                            .toList());

            return migrationProgress;
        });
    }

    public void startMigration(String migrationFQCN) {
        migrationProgressContainer.update(migrationProgress -> {
            migrationProgress.getMetadata().getCompletedMigrationFQCNs().remove(migrationFQCN);
            migrationProgress.getMetadata().getInProgressMigrationFQCNs().add(migrationFQCN);
            return migrationProgress;
        });
    }

    public void finishMigration(String migrationFQCN) {
        migrationProgressContainer.update(migrationProgress -> {
            migrationProgress.getMetadata().getInProgressMigrationFQCNs().remove(migrationFQCN);
            migrationProgress.getMetadata().getCompletedMigrationFQCNs().add(migrationFQCN);
            return migrationProgress;
        });
    }

    public void reportFailedRecords(
        FailedRecordGroup failedRecordGroup
    ) {
        String cause = ExceptionUtils.getStackTrace(failedRecordGroup.getException());

        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                    .getMigrationProgress()
                    .computeIfAbsent(
                            failedRecordGroup.getMigrationRunner().getMigrationFQCN(),
                            MigrationProgressPerMigrationClass::new);
            
            List<FailedRecord> reportedFailedRecords = progressInThisMigrationClass.getFailedRecords();

            // Overwriting records with same IDs
            Set<Object> oldIds = reportedFailedRecords.stream().map(FailedRecord::getId).collect(Collectors.toSet());
            Set<Object> newIds = failedRecordGroup.getRecords().stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet());
            Set<Object> idsToRemove = new HashSet<>(oldIds);
            idsToRemove.retainAll(newIds);
            reportedFailedRecords.removeIf(failedRecord -> idsToRemove.contains(failedRecord.getId()));

            reportedFailedRecords.addAll(
                failedRecordGroup.getRecords().stream()
                    .map(failedRecord -> {
                        return new FailedRecord(
                            failedRecord.getMigratableId(),
                            failedRecord.getMigratableDescription(),
                            cause,
                            new FailedRecordAction(FailedRecordAction.Type.RETRY),
                            LocalDateTime.now());
                        })
                    .toList()
            );

            return migrationProgress;
        });

        log.error("Some records failed to migrate: " + failedRecordGroup.getException());
    }

    public void excludeSuccessfullyMigratedRecordIds(
            String migrationFQCN,
            Set<Object> recordIds) {
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                    .getMigrationProgress()
                    .computeIfAbsent(
                            migrationFQCN,
                            MigrationProgressPerMigrationClass::new);

            Set<Object> migratedRecordIds = progressInThisMigrationClass
                    .getMigratedRecordIds();

            recordIds.removeAll(migratedRecordIds);
        });
    }

    public void reportFatalError(Exception exception) {
        migrationProgressContainer.update(migrationProgress -> {
            migrationProgress.getFatalErrors().add(
                ExceptionUtils.getStackTrace(exception)
            );
            return migrationProgress;
        });
    }
}

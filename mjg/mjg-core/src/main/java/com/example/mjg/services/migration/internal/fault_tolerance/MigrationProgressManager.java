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

import java.io.Serializable;
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
    public Set<Serializable> getFailedRecordIds(String migrationFQCN) {
        Set<Serializable> failedRecordIds = new HashSet<>();
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass migrationProgressOfThisMigrationClass = migrationProgress
                .getMigrationProgress().get(
                    migrationFQCN
                );

            if (migrationProgressOfThisMigrationClass != null) {
                List<FailedRecord> failedRecordsInMigrationProgress = migrationProgressOfThisMigrationClass.getFailedRecords();
                Set<Serializable> failedRecordIds1 = failedRecordsInMigrationProgress
                    .stream()
                    .filter(failedRecord -> FailedRecordAction.Type.RETRY.equals(failedRecord.getAction().getActionType()))
                    .map(FailedRecord::getId)
                    .collect(Collectors.toSet());

                failedRecordIds.addAll(failedRecordIds1);
            }
        });

        return failedRecordIds;
    }

    public Set<Serializable> getIgnoredRecordIds(String migrationFQCN) {
        Set<Serializable> ignoredRecordIds = new HashSet<>();
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass migrationProgressOfThisMigrationClass = migrationProgress
                .getMigrationProgress().getOrDefault(
                    migrationFQCN,
                    new MigrationProgressPerMigrationClass(migrationFQCN)
                );
            
            Set<Serializable> ignoredRecordIds1 = migrationProgressOfThisMigrationClass.getFailedRecords()
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
        private Serializable migratableId;

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
                return;
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
            SuccessfulRecordGroup successfulRecordGroup
    ) {
        migrationProgressContainer.update(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                    .getMigrationProgress()
                    .computeIfAbsent(
                            successfulRecordGroup.getMigrationRunner().getMigrationFQCN(),
                            MigrationProgressPerMigrationClass::new
                    );

            progressInThisMigrationClass.getSucceededRecordIds().addAll(
                    successfulRecordGroup.getRecordIds()
            );

            progressInThisMigrationClass.getFailedRecords()
                .removeIf(failedRecord -> {
                    return progressInThisMigrationClass.getSucceededRecordIds().contains(
                        failedRecord.getId()
                    );
                });

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
            Set<Serializable> oldIds = reportedFailedRecords.stream().map(FailedRecord::getId).collect(Collectors.toSet());
            Set<Serializable> newIds = failedRecordGroup.getRecords().stream().map(MigratableEntity::getMigratableId).collect(Collectors.toSet());
            Set<Serializable> idsToRemove = new HashSet<>(oldIds);
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

            // Remove failed records from successful record set
            progressInThisMigrationClass.getSucceededRecordIds()
                .removeAll(newIds);

            return migrationProgress;
        });

        log.error("Some records failed to migrate: " + failedRecordGroup.getException());
    }

    public void excludeSuccessfullyMigratedRecordIds(
            String migrationFQCN,
            Set<Serializable> recordIds
    ) {
        migrationProgressContainer.read(migrationProgress -> {
            MigrationProgressPerMigrationClass progressInThisMigrationClass = migrationProgress
                    .getMigrationProgress()
                    .get(migrationFQCN);

            if (progressInThisMigrationClass != null) {
                Set<Serializable> migratedRecordIds = progressInThisMigrationClass
                    .getSucceededRecordIds();

                recordIds.removeAll(migratedRecordIds);
            }
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

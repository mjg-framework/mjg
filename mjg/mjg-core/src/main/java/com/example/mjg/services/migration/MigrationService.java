package com.example.mjg.services.migration;

import com.example.mjg.exceptions.RetriesExhaustedException;
import com.example.mjg.processors.Migration.RuntimeMigration;
import com.example.mjg.processors.Migration.RuntimeMigrationDataLocation;
import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;
import com.example.mjg.services.migration.internal.fault_tolerance.MigrationProgressManager;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.storage.MigrationRegistry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MigrationService {
    private static class InstanceHolder {
        private static final MigrationService instance = new MigrationService();
    };

    public static MigrationService _getInstForTesting() {
        return InstanceHolder.instance;
    }

    @Getter
    private DataStoreRegistry dataStoreRegistry = new DataStoreRegistry();

    @Getter
    private final MigrationRegistry migrationRegistry = new MigrationRegistry();

    @Getter
    private final MigrationProgressManager migrationProgressManager = new MigrationProgressManager();

    public MigrationService(DataStoreRegistry dataStoreRegistry) {
        this.dataStoreRegistry = dataStoreRegistry;
    }

    public void run(MigrationProgress previousProgress) {
        migrationProgressManager.restorePreviousProgress(previousProgress);
        _run();
    }

    public void runWithPreviousProgress() {
        _run();
    }
    
    public void runWithoutPreviousProgress() {
        migrationProgressManager.restorePreviousProgress(new MigrationProgress());
        _run();
    }

    private void _run() {
        List<RuntimeMigration> sortedMigrations = getSortedMigrations();

        System.out.println("Loading compiled migration ordering: " + sortedMigrations);

        List<MigrationRunner> migrationRunners = sortedMigrations
            .stream()
            .map(runtimeMigration -> new MigrationRunner(
                dataStoreRegistry,
                migrationRegistry,
                migrationProgressManager,
                runtimeMigration.getMigrationFQCN()
            ))
            .toList();

        try {
            for (MigrationRunner runner : migrationRunners) {
                log.info("Starting migration: " + runner.getMigrationFQCN());
                migrationProgressManager.startMigration(runner.getMigrationFQCN());

                try {
                    runner.run();
                } catch (RetriesExhaustedException propagatedException) {
                    log.info("Error while running migration: " + runner.getMigrationFQCN() + " (number of failures: "
                        + runner.getMigrationErrorInvestigator().getNumFailures()
                        + ") ; propagated exception = " + propagatedException
                    );
                    log.info("Stopping migration process altogether");
                    break;
                }

                migrationProgressManager.finishMigration(runner.getMigrationFQCN());
                log.info("Finished migration: " + runner.getMigrationFQCN());
            }
        } finally {
            migrationProgressManager.flush();
        }
    }
    
    public void addProgressPersistenceCallback(
        Consumer<MigrationProgress> callback
    ) {
        migrationProgressManager.addProgressPersistenceCallback(callback);
    }

    public void removeAllProgressPersistenceCallbacks() {
        migrationProgressManager.removeAllProgressPersistenceCallbacks();
    }

    @SuppressWarnings("unchecked")
    private List<RuntimeMigration> getSortedMigrations() {
        try {
            Class<?> clazz = Class.forName(RuntimeMigrationDataLocation.FQCN);
            Field field = clazz.getField("sortedMigrations");
            return (List<RuntimeMigration>) field.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("mjg: cannot get compiled migration data. Try: mvn clean compile test-compile", e);
        }
    }
}

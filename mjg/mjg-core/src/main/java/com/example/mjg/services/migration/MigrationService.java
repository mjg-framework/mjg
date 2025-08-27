package com.example.mjg.services.migration;

import com.example.mjg.processors.Migration.RuntimeMigration;
import com.example.mjg.processors.Migration.RuntimeMigrationDataLocation;
import com.example.mjg.services.migration.internal.MigrationRunner;
import com.example.mjg.services.migration.internal.fault_tolerance.MigrationProgressManager;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mjg.storage.MigrationRegistry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MigrationService {
    private static class InstanceHolder {
        private static final MigrationService instance = new MigrationService();
    };

    public static MigrationService getInst() {
        return InstanceHolder.instance;
    }

    @Getter
    private final DataStoreRegistry dataStoreRegistry = new DataStoreRegistry();

    @Getter
    private final MigrationRegistry migrationRegistry = new MigrationRegistry();

    @Getter
    private final MigrationProgressManager migrationProgressManager = new MigrationProgressManager();

    public void run() {
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

        for (MigrationRunner runner : migrationRunners) {
            log.info("Starting migration: " + runner.getMigrationFQCN());
            runner.run();
            log.info("Finished migration: " + runner.getMigrationFQCN());
        }
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

package com.example.mongo_migrate_multids.services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.example.mjg.storage.DataStoreRegistry;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestAreaStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestStationIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestStationStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcAreaStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcStationIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcStationStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.mjg.services.migration.MigrationService;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.utils.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class MigrateToTWService {
    public static final String MIGRATION_PROGRESS_FILE_PATH = "migrate-tw-progress.json";

    private final MigrationService migrationService;
    private final DataStoreRegistry dataStoreRegistry;

    public MigrateToTWService(
        SrcAreaStore srcAreaStore,                              DestAreaStore destAreaStore,
        SrcIndicatorStore srcIndicatorStore,                    DestIndicatorStore destIndicatorStore,
        SrcStationIndicatorStore srcStationIndicatorStore,      DestStationIndicatorStore destStationIndicatorStore,
    SrcStationStore srcStationStore,                            DestStationStore destStationStore
    ) {
        dataStoreRegistry = new DataStoreRegistry();

        dataStoreRegistry.set(SrcAreaStore.class, srcAreaStore);
        dataStoreRegistry.set(DestAreaStore.class, destAreaStore);
        dataStoreRegistry.set(SrcIndicatorStore.class, srcIndicatorStore);
        dataStoreRegistry.set(DestIndicatorStore.class, destIndicatorStore);
        dataStoreRegistry.set(SrcStationIndicatorStore.class, srcStationIndicatorStore);
        dataStoreRegistry.set(DestStationIndicatorStore.class, destStationIndicatorStore);
        dataStoreRegistry.set(SrcStationStore.class, srcStationStore);
        dataStoreRegistry.set(DestStationStore.class, destStationStore);

        migrationService = new MigrationService(dataStoreRegistry);

        migrationService.removeAllProgressPersistenceCallbacks();

        migrationService.addProgressPersistenceCallback(MigrateToTWService::saveMigrationProgress);
    }

    public void run() {
        MigrationProgress migrationProgress = loadMigrationProgress();

        migrationService.run(migrationProgress);
    }

    private static MigrationProgress loadMigrationProgress() {
        ObjectMapper objectMapper = ObjectMapperFactory.get();
        MigrationProgress migrationProgress;
        boolean fileExists;
        try {
            migrationProgress = objectMapper.readValue(new File(MIGRATION_PROGRESS_FILE_PATH), MigrationProgress.class);
            fileExists = true;
        } catch (FileNotFoundException e) {
            fileExists = false;
            log.warn("No previous migration progress saved in " + MIGRATION_PROGRESS_FILE_PATH);
            log.warn("Starting fresh.");
            migrationProgress = new MigrationProgress();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load migration progress from file: " + MIGRATION_PROGRESS_FILE_PATH, e);
        }

        if (fileExists) {
            // Copy to backup file
            try {
                Files.copy(
                    Paths.get(MIGRATION_PROGRESS_FILE_PATH),

                    Paths.get(
                        MIGRATION_PROGRESS_FILE_PATH
                            + "."
                            + migrationProgress.getMetadata().getTimestamp().toEpochSecond(
                            ZoneOffset.of("Z")
                        )
                            + ".bak.json"
                    ),

                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                log.warn("Could not backup previous migration progress file.");
            }
        }

        migrationProgress.getMetadata().setTimestamp(
            LocalDateTime.now()
        );

        return migrationProgress;
    }

    private static void saveMigrationProgress(MigrationProgress migrationProgress) {
        System.out.println("============= SAVING PROGRESS ================");
        BufferedWriter writer = null;
        try {
            FileWriter fileWriter = new FileWriter(MIGRATION_PROGRESS_FILE_PATH, false);
            writer = new BufferedWriter(fileWriter);

            ObjectMapper objectMapper = ObjectMapperFactory.get();
            String jsonString = objectMapper.writeValueAsString(migrationProgress);
            writer.write(jsonString);

            System.out.println("Migration progress successfully saved to: " + MIGRATION_PROGRESS_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Error saving migration progress to file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing BufferedWriter: " + e.getMessage());
                }
            }
        }
    }
}

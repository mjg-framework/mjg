package com.example.mjg.migration_testing.suite1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.mjg.migration_testing.suite1.utils.MigrationServiceSingleton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2;
import com.example.mjg.migration_testing.suite1.data.mocking.common.MockDataLoader;
import com.example.mjg.migration_testing.suite1.data.stores.IndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.MeasurementResultStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore2;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore2;
import com.example.mjg.migration_testing.suite1.migrations.M1_PopulatePivotTable_StationIndicators;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.FailedRecordAction;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgress;
import com.example.mjg.services.migration.internal.fault_tolerance.schemas.MigrationProgressPerMigrationClass;
import com.example.mjg.utils.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Test4_HandleIgnoredRecordsInRestoredProgress {
    private static final List<IndicatorEntity> INITIAL_INDICATORS = List.of(
        new IndicatorEntity(1, "INDICATOR_1", "pH"),
        new IndicatorEntity(2, "INDICATOR_2", "TSS"),
        new IndicatorEntity(3, "INDICATOR_3", "COD"),
        new IndicatorEntity(4, "INDICATOR_4", "Hg"),
        new IndicatorEntity(5, "INDICATOR_5", "NO2")
    );

    private static final List<StationEntity> INITIAL_STATIONS = List.of(
        new StationEntity(1, "STATION_1", "Binh Duong - Ace Cook (NT)"),
        new StationEntity(2, "STATION_2", "Ha Noi - 556 Nguyen Van Cu"),
        new StationEntity(3, "STATION_3", "Nam Dinh - Cua xa day 1")
    );

    private static final List<MeasurementResultEntity> INITIAL_MEASUREMENT_RESULTS = List.of(
        new MeasurementResultEntity(1, "STATION_1,INDICATOR_1", 14),
        new MeasurementResultEntity(2, "STATION_1,INDICATOR_2", 255),
        new MeasurementResultEntity(3, "STATION_1,INDICATOR_3", 411),
        new MeasurementResultEntity(4, "STATION_1,INDICATOR_4", -52.25),

        new MeasurementResultEntity(4, "STATION_2,INDICATOR_4", 12.03),
        new MeasurementResultEntity(4, "STATION_2,INDICATOR_4", -40.01),
        new MeasurementResultEntity(4, "STATION_2,INDICATOR_4", 50774),
        new MeasurementResultEntity(4, "STATION_2,INDICATOR_4", 0)
    );

    private static final Set<StationIndicatorEntity> FINAL_M1_STATION_INDICATORS = Set.of(
        new StationIndicatorEntity("STATION_1,INDICATOR_1", "STATION_1", 1, "INDICATOR_1", 1),
        new StationIndicatorEntity("STATION_1,INDICATOR_2", "STATION_1", 1, "INDICATOR_2", 2),
        new StationIndicatorEntity("STATION_1,INDICATOR_3", "STATION_1", 1, "INDICATOR_3", 3),
        new StationIndicatorEntity("STATION_1,INDICATOR_4", "STATION_1", 1, "INDICATOR_4", 4),
        new StationIndicatorEntity("STATION_1,INDICATOR_5", "STATION_1", 1, "INDICATOR_5", 5),

        new StationIndicatorEntity("STATION_2,INDICATOR_1", "STATION_2", 2, "INDICATOR_1", 1),
        new StationIndicatorEntity("STATION_2,INDICATOR_2", "STATION_2", 2, "INDICATOR_2", 2),
        new StationIndicatorEntity("STATION_2,INDICATOR_3", "STATION_2", 2, "INDICATOR_3", 3),
        new StationIndicatorEntity("STATION_2,INDICATOR_4", "STATION_2", 2, "INDICATOR_4", 4),
        new StationIndicatorEntity("STATION_2,INDICATOR_5", "STATION_2", 2, "INDICATOR_5", 5),

        new StationIndicatorEntity("STATION_3,INDICATOR_1", "STATION_3", 3, "INDICATOR_1", 1),
        new StationIndicatorEntity("STATION_3,INDICATOR_2", "STATION_3", 3, "INDICATOR_2", 2),
        new StationIndicatorEntity("STATION_3,INDICATOR_3", "STATION_3", 3, "INDICATOR_3", 3),
        new StationIndicatorEntity("STATION_3,INDICATOR_4", "STATION_3", 3, "INDICATOR_4", 4),
        new StationIndicatorEntity("STATION_3,INDICATOR_5", "STATION_3", 3, "INDICATOR_5", 5)
    );

    @BeforeAll
    public static void setup() {
        MockDataLoader.load(
            IndicatorStore.class,
            INITIAL_INDICATORS
        );

        MockDataLoader.load(
            StationStore.class,
            INITIAL_STATIONS
        );

        MockDataLoader.load(
            MeasurementResultStore.class,
            INITIAL_MEASUREMENT_RESULTS
        );

        MockDataLoader.reset(StationIndicatorStore.class);
        MockDataLoader.reset(StationIndicatorStore2.class);
        MockDataLoader.reset(StationStore2.class);

        runTillAllMigrated();
    }

    ///////////////////////////////////
    /////// COPIED FROM Test3 /////////
    /////// WITH MODIFICATIONS ////////
    ///////////////////////////////////

    private static void saveMigrationProgressToFileAndIgnoreSome(
        MigrationProgress migrationProgress,
        String filePath
    ) {
        System.out.println("============= PERSISTING PROGRESS ================");

        {
            // Ignore some records once seen "Fake error while transform"
            final String migration1Class = M1_PopulatePivotTable_StationIndicators.class.getCanonicalName();
            MigrationProgressPerMigrationClass migration1Progress = migrationProgress.getMigrationProgress().computeIfAbsent(
                migration1Class,
                k -> new MigrationProgressPerMigrationClass(migration1Class)
            );
            migration1Progress.getFailedRecords().forEach(failedRecord -> {
                if (
                    failedRecord.getCause().contains("Fake error while transforming")
                    && failedRecord.getDescription().contains("IndicatorEntity")
                    && failedRecord.getId().equals(1)
                ) {
                    // Ignore INDICATOR_1
                    failedRecord.setAction(new FailedRecordAction(FailedRecordAction.Type.IGNORE));
                }
            });
        }

        BufferedWriter writer = null;
        try {
            FileWriter fileWriter = new FileWriter(filePath, false);
            writer = new BufferedWriter(fileWriter);

            ObjectMapper objectMapper = ObjectMapperFactory.get();
            String jsonString = objectMapper.writeValueAsString(migrationProgress);
            writer.write(jsonString);

            System.out.println("JSON string successfully saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving JSON string to file: " + e.getMessage());
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

    private static MigrationProgress loadMigrationProgressFromFile(
        String filePath
    ) {
        System.out.println("_______________ LOADING PROGRESS _______________");
        ObjectMapper objectMapper = ObjectMapperFactory.get();

        MigrationProgress migrationProgress;
        try {
            migrationProgress = objectMapper.readValue(new File(filePath), MigrationProgress.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        migrationProgress.getMetadata().setTimestamp(
            LocalDateTime.now()
        );

        return migrationProgress;
    }

    private static AtomicInteger runIndex = new AtomicInteger(0);
    private static void runTillAllMigrated() {
        final List<String> methods = List.of(
            "matchWithIndicatorStore", "startReduction",
            "reduceFromIndicatorStore", "transform", "handleDuplicate"
        );

        try {
            MigrationServiceSingleton.getInstance().addProgressPersistenceCallback(
                migrationProgress -> {
                    Test4_HandleIgnoredRecordsInRestoredProgress.saveMigrationProgressToFileAndIgnoreSome(
                        migrationProgress,
                        "migration-progress-test-4-run-" + runIndex.get() + ".json"
                    );
                }
            );

            /**
             * Migrate till M1 migration migrates fully successfully
             */
            MigrationProgress progress = new MigrationProgress();
            while (true) {
                int i = runIndex.get();
                System.out.println("@@@@@@ MIGRATION ATTEMPT " + i);

                if (i < methods.size()) {
                    M1_PopulatePivotTable_StationIndicators.setFailRandomly(methods.get(i));
                } else {
                    M1_PopulatePivotTable_StationIndicators.disableFailRandomly();
                }

                MigrationServiceSingleton.getInstance().run(progress);

                progress = Test4_HandleIgnoredRecordsInRestoredProgress.loadMigrationProgressFromFile("migration-progress-test-4-run-" + i + ".json");

                runIndex.incrementAndGet();
                if (
                    progress.getMigrationProgress().get(M1_PopulatePivotTable_StationIndicators.class.getCanonicalName()).getFailedRecords()
                        .stream()
                        .toList()
                        .size() == 0
                ) {
                    break;
                }
            }
        } finally {
            M1_PopulatePivotTable_StationIndicators.disableFailRandomly();
        }
    }

    @Test
    public void testIgnored() {
        assertEquals(
            5, // 4 failed attempts (respectively fail at matchWithIndicatorStore, startReduction, reduceFromIndicatorStore, and transform) + 1 last one (successful)
            runIndex.get()
        );
    }

    @Test
    public void testDataMigrated_M1() {
        final var actualFinalM1StationIndicators = new HashSet<>(MockDataLoader.getStore(StationIndicatorStore.class).getRecords());
        assertEquals(
            FINAL_M1_STATION_INDICATORS,
            actualFinalM1StationIndicators
        );
    }

    @Test
    public void testDataMigrated_M2() {
        assertEquals(
            INITIAL_STATIONS.size(),
            MockDataLoader.getStore(StationStore2.class).getRecords().size()
        );
    }

    @Test
    public void testDataMigrated_M3() {
        List<MigratableEntity> rawMigratedRecords = MockDataLoader.getStore(StationIndicatorStore2.class).getRecords();
        List<StationIndicatorEntity2> migratedRecords = rawMigratedRecords
            .stream()
            .map(StationIndicatorEntity2.class::cast)
            .toList();

        assertEquals(FINAL_M1_STATION_INDICATORS.size(), migratedRecords.size());
        
        AtomicReference<String> twoRecordsWithSameIdExist = new AtomicReference<>(null);

        Map<String, StationIndicatorEntity2> codeToRecordMap = migratedRecords
            .stream()
            .collect(
                Collectors.toMap(
                    record -> record.getStationCode() + "," + record.getIndicatorCode(),
                    Function.identity(),
                    (existing, replacement) -> {
                        twoRecordsWithSameIdExist.set(existing.getId());
                        return existing;
                    },
                    HashMap::new
                )
            );
        
        assertTrue(twoRecordsWithSameIdExist.get() == null);

        // System.out.println("MAP: " + codeToRecordMap);
        
        assertEquals(14, codeToRecordMap.get("new code STATION_1,INDICATOR_1").getAverageValue());
        assertEquals(255, codeToRecordMap.get("new code STATION_1,INDICATOR_2").getAverageValue());
        assertEquals(411, codeToRecordMap.get("new code STATION_1,INDICATOR_3").getAverageValue());
        assertEquals(-52.25, codeToRecordMap.get("new code STATION_1,INDICATOR_4").getAverageValue());
        assertEquals(12686.505, codeToRecordMap.get("new code STATION_2,INDICATOR_4").getAverageValue());

        assertEquals("new code STATION_1", codeToRecordMap.get("new code STATION_1,INDICATOR_1").getStationCode());
        assertEquals("new code STATION_1", codeToRecordMap.get("new code STATION_1,INDICATOR_2").getStationCode());
        assertEquals("new code STATION_1", codeToRecordMap.get("new code STATION_1,INDICATOR_3").getStationCode());
        assertEquals("new code STATION_1", codeToRecordMap.get("new code STATION_1,INDICATOR_4").getStationCode());
        assertEquals("new code STATION_2", codeToRecordMap.get("new code STATION_2,INDICATOR_4").getStationCode());
    }
}

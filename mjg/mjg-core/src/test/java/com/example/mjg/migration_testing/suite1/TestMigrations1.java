package com.example.mjg.migration_testing.suite1;

import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.mocking.common.MockDataLoader;
import com.example.mjg.migration_testing.suite1.data.stores.*;
import com.example.mjg.services.migration.MigrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMigrations1 {
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

        new MeasurementResultEntity(4, "STATION_2,INDICATOR_4", 12.03)
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
    static void setup() {
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

        MigrationService.getInst().run();
    }

    @Test
    public void testSourceDataStoresAreIntactAfterMigration() {
        assertEquals(
            INITIAL_INDICATORS,
            MockDataLoader.getStore(IndicatorStore.class).getRecords()
        );

        assertEquals(
            INITIAL_STATIONS,
            MockDataLoader.getStore(StationStore.class).getRecords()
        );

        assertEquals(
            INITIAL_MEASUREMENT_RESULTS,
            MockDataLoader.getStore(MeasurementResultStore.class).getRecords()
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
}

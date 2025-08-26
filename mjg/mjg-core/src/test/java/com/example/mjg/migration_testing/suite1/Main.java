package com.example.mjg.migration_testing.suite1;

import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.mocking.common.MockDataLoader;
import com.example.mjg.migration_testing.suite1.data.stores.IndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore;
import com.example.mjg.services.migration.MigrationService;

import java.util.List;

public class Main {
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

    public static void main(String[] args) {
        MockDataLoader.load(
            IndicatorStore.class,
            INITIAL_INDICATORS
        );

        MockDataLoader.load(
            StationStore.class,
            INITIAL_STATIONS
        );

        System.out.println("BEFORE MIGRATION: StationIndicatorStore content: " + MockDataLoader.getStore(StationIndicatorStore.class).getRecords());

        MigrationService.getInst().run();

        System.out.println("AFTER MIGRATION: StationIndicatorStore content: " + MockDataLoader.getStore(StationIndicatorStore.class).getRecords());
    }
}

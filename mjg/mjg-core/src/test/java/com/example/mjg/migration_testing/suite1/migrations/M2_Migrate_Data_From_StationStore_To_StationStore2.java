package com.example.mjg.migration_testing.suite1.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore2;

import java.util.List;
import java.util.Map;

@Migration
@ForEachRecordFrom(StationStore.class)
@TransformAndSaveTo(
    value = StationStore2.class,
    cardinality = Cardinality.EXACTLY_ONE
)
public class M2_Migrate_Data_From_StationStore_To_StationStore2 {
    public void startReduction(Map<String, Object> aggregates) {}

    public List<StationEntity> transform(
        Map<String, Object> aggregates,
        StationEntity station
    ) {
        StationEntity newStation = new StationEntity(
            null,
            "new code " + station.getStationCode(),
            station.getStationName()
        );
        return List.of(newStation);
    }
}

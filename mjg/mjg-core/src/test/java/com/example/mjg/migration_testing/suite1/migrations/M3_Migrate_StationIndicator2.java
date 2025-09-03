package com.example.mjg.migration_testing.suite1.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2;
import com.example.mjg.migration_testing.suite1.data.filtering.MeasurementResultsFilterSet;
import com.example.mjg.migration_testing.suite1.data.filtering.StationsFilterSet;
import com.example.mjg.migration_testing.suite1.data.stores.MeasurementResultStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore2;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore2;
import com.example.mjg.storage.DataStoreRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Migration
@ForEachRecordFrom(StationIndicatorStore.class)
@MatchWith(
    value = StationStore2.class,
    cardinality = Cardinality.EXACTLY_ONE
)
@MatchWith(
    value = MeasurementResultStore.class,
    cardinality = Cardinality.ZERO_OR_MORE
)
@TransformAndSaveTo(
    value = StationIndicatorStore2.class,
    cardinality = Cardinality.EXACTLY_ONE
)
public class M3_Migrate_StationIndicator2 {
    public StationsFilterSet matchWithStationStore2(
        StationIndicatorEntity record,
        Map<String, Object> aggregates,
        StationStore2 stationStore2
    ) {
        return StationsFilterSet.filterByStationCodeIn(
            Set.of("new code " + record.getStationCode())
        );
    }

    public MeasurementResultsFilterSet matchWithMeasurementResultStore(
        StationIndicatorEntity record,
        Map<String, Object> aggregates,
        MeasurementResultStore measurementResultStore
    ) {
        return MeasurementResultsFilterSet.filterByStationIndicatorIdIn(
            Set.of(record.getId())
        );
    }

    public void startReduction(
        StationIndicatorEntity inputRecord,
        Map<String, Object> aggregates
    ) {
        aggregates.put("station", null);
        aggregates.put("sumValues", 0.0);
        aggregates.put("countValues", 0);
    }

    public void reduceFromStationStore2(
        Map<String, Object> aggregates,
        List<StationEntity> moreMatchingStations
    ) {
        if (!moreMatchingStations.isEmpty()) {
            aggregates.put("station", moreMatchingStations.get(0));
        }
    }

    public void reduceFromMeasurementResultStore(
        Map<String, Object> aggregates,
        List<MeasurementResultEntity> moreMatchingMeasurementResults
    ) {
        if (!moreMatchingMeasurementResults.isEmpty()) {
            double sumValues = moreMatchingMeasurementResults.stream()
                .map(MeasurementResultEntity::getValue)
                .reduce(0.0, Double::sum);

            int countValues = moreMatchingMeasurementResults.size();

            aggregates.put("sumValues", (double) aggregates.get("sumValues") + sumValues);
            aggregates.put("countValues", (int) aggregates.get("countValues") + countValues);
        }
    }

    public List<StationIndicatorEntity2> transform(
            Map<String, Object> aggregates,
            StationIndicatorEntity oldRecord
    ) {
        StationEntity matchingStation = (StationEntity) aggregates.get("station");

        double average = ((double) aggregates.get("sumValues")) / ((int) aggregates.get("countValues"));

        return List.of(
                new StationIndicatorEntity2(
                    null,
                    // station info changed in accordance with StationStore2
                    matchingStation.getStationCode(),
                    matchingStation.getId(),
                    // indicator info isn't changed since we are still referencing IndicatorStore
                    oldRecord.getIndicatorCode(),
                    oldRecord.getIndicatorId(),
                    // average value
                    average
                )
        );
    }

    public List<StationIndicatorEntity2> handleDuplicate(
        StationIndicatorEntity inputRecord,
        List<StationIndicatorEntity2> outputRecords,
        StationIndicatorStore stationIndicatorStore,
        StationIndicatorStore2 saStationIndicatorStore2,
        DataStoreRegistry dataStoreRegistry
    ) {
        return null;
    }
}

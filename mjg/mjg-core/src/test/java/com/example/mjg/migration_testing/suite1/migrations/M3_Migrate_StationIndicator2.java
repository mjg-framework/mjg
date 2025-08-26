package com.example.mjg.migration_testing.suite1.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.migration_testing.suite1.data.entities.MeasurementResultEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity2;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterMeasurementResultsBy;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterStationsBy2;
import com.example.mjg.migration_testing.suite1.data.stores.MeasurementResultStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore2;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore2;

import java.util.List;
import java.util.Map;

@Migration
@ForEachRecordFrom(StationIndicatorStore.class)
@MatchWith(
    value = StationStore2.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(strategy = ErrorResolution.Strategy.FINISH_THE_MIGRATION_THEN_STOP_AND_REPORT)
)
@MatchWith(
    value = MeasurementResultStore.class,
    cardinality = Cardinality.ZERO_OR_MORE,
    inCaseOfError = @ErrorResolution(strategy = ErrorResolution.Strategy.REPORT_AND_PROCEED)
)
@TransformAndSaveTo(
    value = StationIndicatorStore2.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(strategy = ErrorResolution.Strategy.REPORT_AND_PROCEED)
)
public class M3_Migrate_StationIndicator2 {
    public Map<FilterStationsBy2, Object> matchWithStationStore2(
            StationIndicatorEntity record,
            StationStore2 stationStore2
    ) {
        return Map.of(FilterStationsBy2.STATION_CODE, "new code " + record.getStationCode());
    }

    public Map<FilterMeasurementResultsBy, Object> matchWithMeasurementResultStore(
            StationIndicatorEntity record,
            MeasurementResultStore measurementResultStore
    ) {
        return Map.of(FilterMeasurementResultsBy.STATION_INDICATOR_ID, record.getId());
    }

    public void startReduction(Map<String, Object> aggregates) {
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
}

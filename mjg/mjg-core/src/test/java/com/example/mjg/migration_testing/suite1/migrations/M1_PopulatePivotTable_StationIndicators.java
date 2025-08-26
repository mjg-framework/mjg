package com.example.mjg.migration_testing.suite1.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.DuplicateResolution;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterIndicatorsBy;
import com.example.mjg.migration_testing.suite1.data.stores.IndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Migration
@ForEachRecordFrom(StationStore.class)
@MatchWith(
        value = IndicatorStore.class,
        cardinality = Cardinality.ZERO_OR_MORE,
        inCaseOfError = @ErrorResolution(strategy = ErrorResolution.Strategy.STOP_IMMEDIATELY_AND_REPORT)
)
@TransformAndSaveTo(
    value = StationIndicatorStore.class,
    cardinality = Cardinality.ZERO_OR_MORE,
    inCaseOfError = @ErrorResolution(strategy = ErrorResolution.Strategy.STOP_IMMEDIATELY_AND_REPORT),
    inCaseOfDuplicate = @DuplicateResolution(strategy = DuplicateResolution.Strategy.REPORT_AND_PROCEED)
)
public class M1_PopulatePivotTable_StationIndicators {
    public Map<FilterIndicatorsBy, Object> matchWithIndicatorStore(
        StationEntity record,
        IndicatorStore indicatorStore
    ) {
        return Map.of(); // get all, no filter
    }

    public void startReduction(Map<String, Object> aggregates) {
        aggregates.put("indicators", new ArrayList<IndicatorEntity>());
    }

    public void reduceFromIndicatorStore(
        Map<String, Object> aggregates,
        List<IndicatorEntity> moreIndicators
    ) {
        @SuppressWarnings("unchecked")
        List<IndicatorEntity> indicators = (List<IndicatorEntity>) aggregates.get("indicators");
        
        indicators.addAll(moreIndicators);
    }

    public List<StationIndicatorEntity> transform(
        Map<String, Object> aggregates,
        StationEntity station
    ) {
        @SuppressWarnings("unchecked")
        List<IndicatorEntity> indicators = (List<IndicatorEntity>) aggregates.get("indicators");
        
        return indicators.stream()
                .map(indicator -> new StationIndicatorEntity(
                        station.getStationCode() + "," + indicator.getIndicatorCode(),
                        station.getStationCode(),
                        station.getId(),
                        indicator.getIndicatorCode(),
                        indicator.getId()
                ))
                .toList();
    }
}

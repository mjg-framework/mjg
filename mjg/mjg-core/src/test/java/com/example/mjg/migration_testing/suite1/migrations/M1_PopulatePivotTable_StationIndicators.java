package com.example.mjg.migration_testing.suite1.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.migration_testing.suite1.data.entities.IndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationEntity;
import com.example.mjg.migration_testing.suite1.data.entities.StationIndicatorEntity;
import com.example.mjg.migration_testing.suite1.data.filtering.FilterIndicatorsBy;
import com.example.mjg.migration_testing.suite1.data.stores.IndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationIndicatorStore;
import com.example.mjg.migration_testing.suite1.data.stores.StationStore;
import com.example.mjg.storage.DataStoreRegistry;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Migration
@ForEachRecordFrom(
    value = StationStore.class,
    batchSize = 1,
    inCaseOfError = @ErrorResolution(retryTimes = 0, retryDelayInSeconds = 0)
)
@MatchWith(
    value = IndicatorStore.class,
    batchSize = 1,
    cardinality = Cardinality.ZERO_OR_MORE,
    inCaseOfError = @ErrorResolution(retryTimes = 0, retryDelayInSeconds = 0)
)
@TransformAndSaveTo(
    value = StationIndicatorStore.class,
    cardinality = Cardinality.ZERO_OR_MORE,
    inCaseOfError = @ErrorResolution(retryTimes = 0, retryDelayInSeconds = 0)
)
public class M1_PopulatePivotTable_StationIndicators {
    private static AtomicReference<String> METHOD_THAT_RANDOMLY_FAILS = new AtomicReference<>(null);

    public static void setFailRandomly(String methodThatRandomlyFails) {
        METHOD_THAT_RANDOMLY_FAILS.set(methodThatRandomlyFails);
    }

    public static void disableFailRandomly() {
        METHOD_THAT_RANDOMLY_FAILS.set(null);
    }

    private static boolean toss(String method) {
        if (Objects.equals(method, METHOD_THAT_RANDOMLY_FAILS.get())) {
            return false;
        }
        return true;
    }

    public Map<FilterIndicatorsBy, Object> matchWithIndicatorStore(
        StationEntity record,
        IndicatorStore indicatorStore
    ) {
        if (!toss("matchWithIndicatorStore")) {
            throw new RuntimeException("Fake error while matching with indicator store :)))");
        }
        return Map.of(); // get all, no filter
    }

    public void startReduction(Map<String, Object> aggregates) {
        if (!toss("startReduction")) {
            throw new RuntimeException("Fake error while starting reduction :)))");
        }
        aggregates.put("indicators", new ArrayList<IndicatorEntity>());
    }

    public void reduceFromIndicatorStore(
        Map<String, Object> aggregates,
        List<IndicatorEntity> moreIndicators
    ) {
        if (!toss("reduceFromIndicatorStore")) {
            throw new RuntimeException("Fake error while reducing from indicator store :)))");
        }

        @SuppressWarnings("unchecked")
        List<IndicatorEntity> indicators = (List<IndicatorEntity>) aggregates.get("indicators");
        
        indicators.addAll(moreIndicators);
    }

    public List<StationIndicatorEntity> transform(
        Map<String, Object> aggregates,
        StationEntity station
    ) {
        if (!toss("transform")) {
            throw new RuntimeException("Fake error while transforming :)))");
        }

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

    @Getter
    private AtomicInteger numTimesHandleDuplicateCalled = new AtomicInteger(0);
    public List<StationIndicatorEntity> handleDuplicate(
        StationEntity inputRecord,
        List<StationIndicatorEntity> outputRecords,
        StationStore stationStore,
        StationIndicatorStore stationIndicatorStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        numTimesHandleDuplicateCalled.incrementAndGet();
        if (!toss("handleDuplicate")) {
            throw new RuntimeException("Fake error while handling duplicates :)))");
        }
        return null;
    }
}

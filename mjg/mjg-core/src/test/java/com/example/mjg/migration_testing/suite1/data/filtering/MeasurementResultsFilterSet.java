package com.example.mjg.migration_testing.suite1.data.filtering;

import com.example.mjg.data.DataFilterSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MeasurementResultsFilterSet implements DataFilterSet {
    public static enum FilterBy {
        NONE,
        ID_IN,
        STATION_INDICATOR_ID_IN
    }

    public static MeasurementResultsFilterSet takeAll() {
        return new MeasurementResultsFilterSet(FilterBy.NONE, null);
    }

    public static MeasurementResultsFilterSet filterByIdIn(Set<Integer> ids) {
        return new MeasurementResultsFilterSet(FilterBy.ID_IN, ids);
    }

    public static MeasurementResultsFilterSet filterByStationIndicatorIdIn(Set<String> stationIndicatorIds) {
        return new MeasurementResultsFilterSet(FilterBy.STATION_INDICATOR_ID_IN, stationIndicatorIds);
    }

    public boolean isTakeAll() {
        return filterBy == FilterBy.NONE;
    }

    @SuppressWarnings("unchecked")
    public Set<Integer> getFilterByIdIn() {
        if (filterBy == FilterBy.ID_IN) {
            return (Set<Integer>) data;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getStationIndicatorIdsIn() {
        if (filterBy == FilterBy.STATION_INDICATOR_ID_IN) {
            return (Set<String>) data;
        } else {
            return null;
        }
    }

    private final FilterBy filterBy;
    private final Object data;
}

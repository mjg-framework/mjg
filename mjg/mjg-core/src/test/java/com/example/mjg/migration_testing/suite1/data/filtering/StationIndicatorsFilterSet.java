package com.example.mjg.migration_testing.suite1.data.filtering;

import com.example.mjg.data.DataFilterSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StationIndicatorsFilterSet implements DataFilterSet {
    public static enum FilterBy {
        NONE,
        ID_IN,
        INDICATOR_CODE_IN,
        STATION_CODE_IN
    }

    public static StationIndicatorsFilterSet takeAll() {
        return new StationIndicatorsFilterSet(FilterBy.NONE, null);
    }

    public static StationIndicatorsFilterSet filterByIdIn(Set<String> ids) {
        return new StationIndicatorsFilterSet(FilterBy.ID_IN, ids);
    }

    public static StationIndicatorsFilterSet filterByIndicatorCodeIn(Set<String> indicatorCodes) {
        return new StationIndicatorsFilterSet(FilterBy.INDICATOR_CODE_IN, indicatorCodes);
    }

    public static StationIndicatorsFilterSet filterByStationCodeIn(Set<String> stationCodes) {
        return new StationIndicatorsFilterSet(FilterBy.STATION_CODE_IN, stationCodes);
    }

    public boolean isTakeAll() {
        return filterBy == FilterBy.NONE;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getFilterByIdIn() {
        if (filterBy == FilterBy.ID_IN) {
            return (Set<String>) data;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getFilterByIndicatorCodeIn() {
        if (filterBy == FilterBy.INDICATOR_CODE_IN) {
            return (Set<String>) data;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getFilterByStationCodeIn() {
        if (filterBy == FilterBy.STATION_CODE_IN) {
            return (Set<String>) data;
        } else {
            return null;
        }
    }

    private final FilterBy filterBy;
    private final Object data;
}

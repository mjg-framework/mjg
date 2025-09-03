package com.example.mjg.migration_testing.suite1.data.filtering;

import com.example.mjg.data.DataFilterSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StationsFilterSet implements DataFilterSet {
    public static enum FilterBy {
        NONE,
        ID_IN,
        STATION_CODE_IN
    }

    public static StationsFilterSet takeAll() {
        return new StationsFilterSet(FilterBy.NONE, null);
    }

    public static StationsFilterSet filterByIdIn(Set<Integer> ids) {
        return new StationsFilterSet(FilterBy.ID_IN, ids);
    }

    public static StationsFilterSet filterByStationCodeIn(Set<String> stationCodes) {
        return new StationsFilterSet(FilterBy.STATION_CODE_IN, stationCodes);
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

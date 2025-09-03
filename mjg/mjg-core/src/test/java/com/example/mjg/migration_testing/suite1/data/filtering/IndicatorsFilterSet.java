package com.example.mjg.migration_testing.suite1.data.filtering;

import com.example.mjg.data.DataFilterSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndicatorsFilterSet implements DataFilterSet {
    public static enum FilterBy {
        NONE,
        ID_IN,
        INDICATOR_CODE_IN
    }

    public static IndicatorsFilterSet takeAll() {
        return new IndicatorsFilterSet(FilterBy.NONE, null);
    }

    public static IndicatorsFilterSet filterByIdIn(Set<Integer> ids) {
        return new IndicatorsFilterSet(FilterBy.ID_IN, ids);
    }

    public static IndicatorsFilterSet filterByIndicatorCodeIn(Set<String> indicatorCodes) {
        return new IndicatorsFilterSet(FilterBy.INDICATOR_CODE_IN, indicatorCodes);
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
    public Set<String> getFilterByIndicatorCodeIn() {
        if (filterBy == FilterBy.INDICATOR_CODE_IN) {
            return (Set<String>) data;
        } else {
            return null;
        }
    }

    private final FilterBy filterBy;
    private final Object data;
}

package com.example.mjg.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public final class DataPage<T extends MigratableEntity, FILTER_TYPE, FILTER_VALUE> {
    @Getter
    private final DataStore<T, FILTER_TYPE, FILTER_VALUE> store;

    @Getter
    private final Integer pageNumber;

    @Getter
    private final Map<FILTER_TYPE, FILTER_VALUE> filters;

    @Getter
    private List<T> records;

    public Integer getSize() {
        return getRecords().size();
    }
}

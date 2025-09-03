package com.example.mjg.data;

import java.io.Serializable;
import java.util.List;

public interface DataPage<T extends MigratableEntity, ID extends Serializable, F extends DataFilterSet> {
    DataStore<T, ID, F> getDataStore();

    F getFilterSet();

    Integer getPageNumber();

    List<T> getRecords();

    default Integer getSize() {
        return getRecords().size();
    }
}

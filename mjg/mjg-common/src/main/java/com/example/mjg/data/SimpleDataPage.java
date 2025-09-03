package com.example.mjg.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@AllArgsConstructor
public class SimpleDataPage<T extends MigratableEntity, ID extends Serializable, F extends DataFilterSet>
implements DataPage<T, ID, F> {
    private final DataStore<T, ID, F> dataStore;

    private final F filterSet;

    private final Integer pageNumber;

    private final List<T> records;
}

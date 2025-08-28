package com.example.mjg.data;

import com.example.mjg.exceptions.BaseMigrationException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DataStore<T extends MigratableEntity, FILTER_TYPE, FILTER_VALUE> {
    protected abstract List<T> doGetFirstPageOfRecordsWithFilter(
        Map<FILTER_TYPE, FILTER_VALUE> filters,
        int pageSize
    ) throws BaseMigrationException, RuntimeException;

    protected abstract Map<FILTER_TYPE, FILTER_VALUE> doMatchByIdIn(Set<Object> ids);

    protected abstract List<T> doGetNextPageOfRecordsAfter (
        DataPage<T, FILTER_TYPE, FILTER_VALUE> previousPage
    ) throws BaseMigrationException, RuntimeException;

    protected abstract void doSave(T record) throws BaseMigrationException, RuntimeException;

    protected abstract void doSaveMultiple(List<T> records) throws BaseMigrationException, RuntimeException;



    // public final DataPage<T, FILTER_TYPE, FILTER_VALUE> getFirstPageOfRecords(int pageSize)
    // throws BaseMigrationException, RuntimeException {
    //     return getFirstPageOfRecordsWithFilter(Map.of(), pageSize);
    // }

    public final DataPage<T, FILTER_TYPE, FILTER_VALUE> getFirstPageOfRecordsWithFilter(
        Map<FILTER_TYPE, FILTER_VALUE> filters,
        int pageSize
    ) throws BaseMigrationException, RuntimeException {
        List<T> records = doGetFirstPageOfRecordsWithFilter(filters, pageSize);
        return new DataPage<>(this, 0, filters, records);
    }

    // public final DataPage<T, FILTER_TYPE, FILTER_VALUE> getFirstPageOfRecordsByIdIn(
    //     Set<Object> ids,
    //     int pageSize
    // ) throws BaseMigrationException, RuntimeException {
    //     Map<FILTER_TYPE, FILTER_VALUE> filters = doMatchByIdIn(ids);
    //     return getFirstPageOfRecordsWithFilter(filters, pageSize);
    // }

    public final Map<FILTER_TYPE, FILTER_VALUE> getFiltersByIdIn(Set<Object> ids) {
        return doMatchByIdIn(ids);
    }

    public final DataPage<T, FILTER_TYPE, FILTER_VALUE> getNextPageOfRecordsAfter(
            DataPage<T, FILTER_TYPE, FILTER_VALUE> previousPage
    ) throws BaseMigrationException, RuntimeException {
        List<T> records = doGetNextPageOfRecordsAfter(previousPage);
        return new DataPage<>(
            this,
            previousPage.getPageNumber() + 1,
            previousPage.getFilters(),
            records
        );
    }

    public final void save(T record)
    throws BaseMigrationException, RuntimeException {
        doSave(record);
    }

    public final void saveMultiple(List<T> records)
    throws BaseMigrationException, RuntimeException {
        doSaveMultiple(records);
    }
}

package com.example.mjg.data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class DataStore<T extends MigratableEntity, ID extends Serializable, F extends DataFilterSet> {
    protected abstract DataPage<T, ID, F> doGetFirstPageOfRecords(
        F filterSet,
        int pageSize
    ) throws Exception;

    protected abstract F doMatchAll();

    protected abstract F doMatchByIdIn(Set<ID> ids);

    protected abstract DataPage<T, ID, F> doGetNextPageOfRecords(
        DataPage<T, ID, F> previousPage
    ) throws Exception;

    protected abstract void doSave(T record) throws Exception;

    protected abstract void doSaveAll(List<T> records) throws Exception;




    public final DataPage<T, ID, F> getFirstPageOfRecords(
        F filterSet,
        int pageSize
    ) throws Exception {
        return doGetFirstPageOfRecords(filterSet, pageSize);
    }

    public final F matchAll() {
        return doMatchAll();
    }

    public final F matchByIdIn(Set<ID> ids) {
        return doMatchByIdIn(ids);
    }

    public final DataPage<T, ID, F> getNextPageOfRecords(
            DataPage<T, ID, F> previousPage
    ) throws Exception {
        return doGetNextPageOfRecords(previousPage);
    }

    public final void save(T record)
    throws Exception {
        doSave(record);
    }

    public final void saveAll(List<T> records)
    throws Exception {
        doSaveAll(records);
    }
}

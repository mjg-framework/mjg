package com.example.mongo_migrate_multids.migrational.datastores.common.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;

import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.BaseMigrationException;

public abstract class SimpleCrudMigratableStore<T extends MigratableEntity, ID, FILTER_TYPE>
extends DataStore<T, FILTER_TYPE, Object>
{
    protected abstract CrudRepository<T, ID> doGetCrudRepository();

    protected abstract Page<T> doGetRecords(
        int pageSize,
        int pageNumber
    );

    protected abstract Page<T> doGetRecordsWithFilter(
        FILTER_TYPE filterType,
        Object filterValue,
        int pageSize,
        int pageNumber
    );

    protected abstract FILTER_TYPE doGetFilterByIdIn();






    private List<T> _internal_getRecordsAtPage(
        Map<FILTER_TYPE, Object> filters,
        int pageSize,
        int pageNumber
    ) {
        var keySet = filters.keySet();
        if (keySet.size() > 1) {
            throw new RuntimeException("a MongoDB-based data store cannot have more than one filter in a filter set.");
        }
        Page<T> dataPage;
        if (keySet.size() == 0) {
            dataPage = doGetRecords(pageSize, pageNumber);
        } else {
            FILTER_TYPE filterType = keySet.iterator().next();
            Object filterValue = filters.get(filterType);
            dataPage = doGetRecordsWithFilter(filterType, filterValue, pageSize, pageNumber);
        }

        return dataPage.getContent();
    }

    @Override
    protected List<T> doGetFirstPageOfRecordsWithFilter(
        Map<FILTER_TYPE, Object> filters,
        int pageSize
    ) throws BaseMigrationException, RuntimeException {
        return _internal_getRecordsAtPage(filters, pageSize, 0);
    }

    @Override
    protected Map<FILTER_TYPE, Object> doMatchByIdIn(Set<Object> ids) {
        FILTER_TYPE filterType = doGetFilterByIdIn();
        return Map.of(filterType, ids);
    }

    @Override
    protected List<T> doGetNextPageOfRecordsAfter (
        DataPage<T, FILTER_TYPE, Object> previousPage
    ) throws BaseMigrationException, RuntimeException {
        return _internal_getRecordsAtPage(
            previousPage.getFilters(),
            previousPage.getSize(),
            previousPage.getPageNumber() + 1
        );
    }

    @Override
    protected void doSave(T record) throws BaseMigrationException, RuntimeException {
        doGetCrudRepository().save(record);
    }

    @Override
    protected void doSaveMultiple(List<T> records) throws BaseMigrationException, RuntimeException {
        doGetCrudRepository().saveAll(records);
    }
}

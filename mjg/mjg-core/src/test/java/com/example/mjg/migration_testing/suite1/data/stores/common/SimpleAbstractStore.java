package com.example.mjg.migration_testing.suite1.data.stores.common;

import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.BaseMigrationException;
import com.example.mjg.exceptions.DuplicateDataException;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class SimpleAbstractStore<T extends MigratableEntity, ID, FILTER_TYPE, FILTER_VALUE> extends DataStore<T, FILTER_TYPE, FILTER_VALUE> {
    protected abstract Stream<T> applyFilter(Stream<T> recordStream, FILTER_TYPE filterType, FILTER_VALUE filterValue);
    protected abstract ID getRecordId(T record);
    protected abstract void setRecordId(T record, ID id);
    protected abstract void assignRecordExceptId(T dest, T src);
    protected abstract ID generateNewRecordId(Function<ID, Boolean> idAlreadyExists);




    @Getter
    private final List<T> records = new ArrayList<>();



    @Override
    protected List<T> doGetFirstPageOfRecordsWithFilter(
            Map<FILTER_TYPE, FILTER_VALUE> filters,
            int pageSize
    ) {
        return getRecordsAtPage(filters, 0, pageSize);
    }

    @Override
    protected List<T> doGetNextPageOfRecordsAfter(
            DataPage<T, FILTER_TYPE, FILTER_VALUE> previousPage
    ) {
        return getRecordsAtPage(
            previousPage.getFilters(),
            previousPage.getPageNumber() + 1,
            previousPage.getSize()
        );
    }

    @Override
    protected void doSave(T record)
    throws BaseMigrationException {
        for (T existingRecord : records) {
            if (getRecordId(existingRecord).equals(getRecordId(record))) {
                throw new DuplicateDataException("A record with the same ID already exist");
                // assignRecordExceptId(existingRecord, record);
                // return;
            }
        }

        if (getRecordId(record) == null) {
            setRecordId(record, generateNewRecordId(this::checkIdAlreadyExists));
        }
        records.add(record);
    }

    @Override
    protected void doSaveMultiple(List<T> records)
    throws BaseMigrationException {
        for (T newRecord : records) {
            doSave(newRecord);
        }
    }





    private Stream<T> applyFilters(Stream<T> recordStream, Map<FILTER_TYPE, FILTER_VALUE> filters) {
        Stream<T> filteredRecordStream = recordStream;
        for (Map.Entry<FILTER_TYPE, FILTER_VALUE> entry : filters.entrySet()) {
            filteredRecordStream = applyFilter(filteredRecordStream, entry.getKey(), entry.getValue());
        }
        return filteredRecordStream;
    }

    private List<T> getRecordsAtPage(Map<FILTER_TYPE, FILTER_VALUE> filters, int pageNumber, int pageSize) {
        List<T> filteredList = applyFilters(records.stream(), filters).toList();
        int offset = pageNumber * pageSize;

        if (offset >= filteredList.size()) {
            return List.of();
        }

        return filteredList
            .subList(
                offset,
                Math.min(offset + pageSize, filteredList.size())
            );
    }

    private boolean checkIdAlreadyExists(ID id) {
        for (T record : records) {
            if (getRecordId(record).equals(id)) {
                return true;
            }
        }
        return false;
    }
}

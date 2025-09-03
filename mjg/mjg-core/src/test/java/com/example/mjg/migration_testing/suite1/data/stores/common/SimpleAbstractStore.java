package com.example.mjg.migration_testing.suite1.data.stores.common;

import com.example.mjg.data.*;
import com.example.mjg.exceptions.DuplicateDataException;

import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class SimpleAbstractStore<T extends MigratableEntity, ID extends Serializable, F extends DataFilterSet>
extends DataStore<T, ID, F> {
    protected abstract Stream<T> applyFilterSet(Stream<T> recordStream, F filterSet);
    protected abstract ID getRecordId(T record);
    protected abstract void setRecordId(T record, ID id);
    protected abstract void assignRecordExceptId(T dest, T src);
    protected abstract ID generateNewRecordId(Function<ID, Boolean> idAlreadyExists);




    @Getter
    private final List<T> records = new ArrayList<>();



    @Override
    protected DataPage<T, ID, F> doGetFirstPageOfRecords(
            F filterSet,
            int pageSize
    ) {
        return getRecordsAtPage(filterSet, 0, pageSize);
    }

    @Override
    protected DataPage<T, ID, F> doGetNextPageOfRecords(
        DataPage<T, ID, F> previousPage
    ) {
        return getRecordsAtPage(
            previousPage.getFilterSet(),
            previousPage.getPageNumber() + 1,
            previousPage.getSize()
        );
    }

    @Override
    protected void doSave(T record)
    throws Exception {
        for (T existingRecord : records) {
            if (getRecordId(existingRecord).equals(getRecordId(record))) {
                throw new DuplicateDataException("A record with the same ID already exists: " + record.getMigratableDescription());
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
    protected void doSaveAll(List<T> records)
    throws Exception {
        for (T newRecord : records) {
            doSave(newRecord);
        }
    }





    private DataPage<T, ID, F> getRecordsAtPage(F filterSet, int pageNumber, int pageSize) {
        List<T> filteredList = applyFilterSet(records.stream(), filterSet).toList();
        int offset = pageNumber * pageSize;

        List<T> resultRecords;
        if (offset >= filteredList.size()) {
            resultRecords = List.of();
        } else {
            resultRecords = filteredList
                .subList(
                    offset,
                    Math.min(offset + pageSize, filteredList.size())
                );
        }

        return new SimpleDataPage<>(this, filterSet, pageNumber, resultRecords);
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

package com.example.mjg.spring.stores;

import com.example.mjg.data.DataPage;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.spring.filtering.SpringRepositoryFilterSet;
import com.example.mjg.spring.repositories.MigratableSpringRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class SpringRepositoryStore<
    T extends MigratableEntity,
    ID extends Serializable
>
extends DataStore<T, ID, SpringRepositoryFilterSet<T, ID>>
{
    public abstract MigratableSpringRepository<T, ID> getRepository();


    @Override
    protected DataPage<T, ID, SpringRepositoryFilterSet<T, ID>>
    doGetFirstPageOfRecords(SpringRepositoryFilterSet<T, ID> filterSet, int pageSize) throws Exception {
        return filterSet.executeAndReturnDataPage(this, PageRequest.of(0, pageSize));
    }

    @Override
    protected DataPage<T, ID, SpringRepositoryFilterSet<T, ID>>
    doGetNextPageOfRecords(DataPage<T, ID, SpringRepositoryFilterSet<T, ID>> previousPage) throws Exception {
        var filterSet = previousPage.getFilterSet();
        var pageNumber = previousPage.getPageNumber() + 1;
        var pageSize = previousPage.getSize();
        return filterSet.executeAndReturnDataPage(this, PageRequest.of(pageNumber, pageSize));
    }

    @Override
    protected SpringRepositoryFilterSet<T, ID> doMatchAll() {
        return SpringRepositoryFilterSet.findAll();
    }

    @Override
    protected SpringRepositoryFilterSet<T, ID> doMatchByIdIn(Set<ID> ids) {
        return SpringRepositoryFilterSet.findAllByIdIn(ids);
    }

    @Override
    protected void doSave(T record) throws Exception {
        try {
            getRepository().save(record);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateDataException(e);
        }
    }

    @Override
    protected void doSaveAll(List<T> records) throws Exception {
        if (records.isEmpty()) return;

        try {
            getRepository().saveAll(records);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateDataException(e);
        }
    }
}

package com.example.mongo_migrate_multids.migrational.datastores.common.common;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;

import com.example.mjg.data.MigratableEntity;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;

public abstract class SimpleMigratableMongoStore<T extends MigratableEntity, ID, FILTER_TYPE>
extends SimpleCrudMigratableStore<T, ID, FILTER_TYPE>
{
    protected abstract MigratableMongoRepository<T, ID> doGetMongoRepository();

    protected abstract Page<T> doGetRecordsWithFilter(
        FILTER_TYPE filterType,
        Object filterValue,
        Pageable pageable
    );



    @Override
    protected CrudRepository<T, ID> doGetCrudRepository() {
        return doGetMongoRepository();
    }

    @Override
    protected Page<T> doGetRecords(
        int pageSize,
        int pageNumber
    ) {
        Sort sort = Sort.by("id").ascending();

        Pageable pageable = PageRequest.of(
            pageNumber,
            pageSize,
            sort
        );

        return doGetMongoRepository().findAll(pageable);
    }

    @Override
    protected Page<T> doGetRecordsWithFilter(
        FILTER_TYPE filterType,
        Object filterValue,
        int pageSize,
        int pageNumber
    ) {
        Sort sort = Sort.by("id").ascending();

        Pageable pageable = PageRequest.of(
            pageNumber,
            pageSize,
            sort
        );

        if (filterType.equals(doGetFilterByIdIn())) {
            if (filterValue instanceof Collection realFilterValue) {
                @SuppressWarnings("unchecked")
                Collection<ID> cast = (Collection<ID>) realFilterValue;
                return doGetMongoRepository().findAllByIdIn(cast, pageable);
            } else {
                throw new RuntimeException("Filter by ID but you passed in: " + filterValue);
            }
        }
        
        
        @SuppressWarnings("unchecked")
        FILTER_TYPE realFilterType = (FILTER_TYPE) filterType;
        return doGetRecordsWithFilter(realFilterType, filterValue, pageable);
    }
}

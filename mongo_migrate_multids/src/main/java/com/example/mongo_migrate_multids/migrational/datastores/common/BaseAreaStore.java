package com.example.mongo_migrate_multids.migrational.datastores.common;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.migrational.datastores.common.common.SimpleMigratableMongoStore;
import com.example.mongo_migrate_multids.migrational.filtering.FilterAreasBy;
import com.example.mongo_migrate_multids.repository.common.BaseAreaRepository;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;

public abstract class BaseAreaStore
extends SimpleMigratableMongoStore<AreaEntity, String, FilterAreasBy>
{
    protected abstract BaseAreaRepository doGetAreaRepository();

    @Override
    protected MigratableMongoRepository<AreaEntity, String> doGetMongoRepository() {
        return doGetAreaRepository();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Page<AreaEntity> doGetRecordsWithFilter(
        FilterAreasBy filterType,
        Object filterValue,
        Pageable pageable
    ) {
        if (filterType == FilterAreasBy.AREA_CODE_IN) {
            return doGetAreaRepository().findAllByAreaCodeIn(((Collection) filterValue), pageable);
        }
        throw new RuntimeException("What type of filter is this? " + filterType);
    }

    @Override
    protected FilterAreasBy doGetFilterByIdIn() {
        return FilterAreasBy.ID_IN;
    }
}

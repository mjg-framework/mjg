package com.example.mongo_migrate_multids.migrational.datastores.common;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.migrational.datastores.common.common.SimpleMigratableMongoStore;
import com.example.mongo_migrate_multids.migrational.filtering.FilterStationsBy;
import com.example.mongo_migrate_multids.repository.common.BaseStationRepository;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;

public abstract class BaseStationStore
extends SimpleMigratableMongoStore<StationEntity, String, FilterStationsBy>
{
    protected abstract BaseStationRepository doGetStationRepository();

    @Override
    protected MigratableMongoRepository<StationEntity, String> doGetMongoRepository() {
        return doGetStationRepository();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Page<StationEntity> doGetRecordsWithFilter(
        FilterStationsBy filterType,
        Object filterValue,
        Pageable pageable
    ) {
        if (filterType == FilterStationsBy.STATION_CODE_IN) {
            return doGetStationRepository().findAllByStationCodeIn((Collection) filterValue, pageable);
        }
        throw new RuntimeException("What type of filter is this? " + filterType);
    }

    @Override
    protected FilterStationsBy doGetFilterByIdIn() {
        return FilterStationsBy.ID_IN;
    }
}

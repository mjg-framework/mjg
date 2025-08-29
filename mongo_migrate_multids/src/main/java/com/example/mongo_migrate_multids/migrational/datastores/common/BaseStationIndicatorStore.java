package com.example.mongo_migrate_multids.migrational.datastores.common;

import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;
import com.example.mongo_migrate_multids.migrational.datastores.common.common.SimpleMigratableMongoStore;
import com.example.mongo_migrate_multids.migrational.filtering.FilterStationIndicatorsBy;
import com.example.mongo_migrate_multids.repository.common.BaseStationIndicatorRepository;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public abstract class BaseStationIndicatorStore
extends SimpleMigratableMongoStore<StationIndicatorEntity, String, FilterStationIndicatorsBy>
{
    protected abstract BaseStationIndicatorRepository doGetStationIndicatorRepository();

    @Override
    protected MigratableMongoRepository<StationIndicatorEntity, String> doGetMongoRepository() {
        return doGetStationIndicatorRepository();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Page<StationIndicatorEntity> doGetRecordsWithFilter(
        FilterStationIndicatorsBy filterType,
        Object filterValue,
        Pageable pageable
    ) {
        if (filterType == FilterStationIndicatorsBy.STATION_ID_IN_AND_INDICATOR_ID_IN) {
            Collection<String>[] castFilterValue = (Collection[]) filterValue;
            Collection<String> stationIds = castFilterValue[0];
            Collection<String> indicatorIds = castFilterValue[1];

            return doGetStationIndicatorRepository()
                .findAllByStationIdInAndIndicatorIdIn(
                    stationIds,
                    indicatorIds,
                    pageable
                );
        }

        throw new RuntimeException("What type of filter is this? " + filterType);
    }

    @Override
    protected FilterStationIndicatorsBy doGetFilterByIdIn() {
        return FilterStationIndicatorsBy.ID_IN;
    }

}

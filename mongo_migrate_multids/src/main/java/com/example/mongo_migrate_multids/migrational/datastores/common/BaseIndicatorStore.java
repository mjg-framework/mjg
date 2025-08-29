package com.example.mongo_migrate_multids.migrational.datastores.common;

import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.migrational.datastores.common.common.SimpleMigratableMongoStore;
import com.example.mongo_migrate_multids.migrational.filtering.FilterIndicatorsBy;
import com.example.mongo_migrate_multids.repository.common.BaseIndicatorRepository;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public abstract class BaseIndicatorStore
extends SimpleMigratableMongoStore<IndicatorEntity, String, FilterIndicatorsBy>
{
    protected abstract BaseIndicatorRepository doGetIndicatorRepository();

    @Override
    protected MigratableMongoRepository<IndicatorEntity, String> doGetMongoRepository() {
        return doGetIndicatorRepository();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Page<IndicatorEntity> doGetRecordsWithFilter(
        FilterIndicatorsBy filterType,
        Object filterValue,
        Pageable pageable
    ) {
        if (filterType == FilterIndicatorsBy.INDICATOR_IN) {
            return doGetIndicatorRepository().findAllByIndicatorIn((Collection) filterValue, pageable);
        }

        if (filterType == FilterIndicatorsBy.INDICATOR_IN_AND_TYPE_IN) {
            Object[] filterValueAsArray = (Object[]) filterValue;
            Collection<String> indicatorIn = (Collection<String>) filterValueAsArray[0];
            Collection<Integer> indicatorTypeIn = (Collection<Integer>) filterValueAsArray[1];
            return doGetIndicatorRepository().findAllByIndicatorInAndIndicatorTypeIn(
                indicatorIn,
                indicatorTypeIn,
                pageable
            );
        }

        throw new RuntimeException("What type of filter is this? " + filterType);
    }

    @Override
    protected FilterIndicatorsBy doGetFilterByIdIn() {
        return FilterIndicatorsBy.ID_IN;
    }
}

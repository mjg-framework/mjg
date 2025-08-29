package com.example.mongo_migrate_multids.repository.common;

import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface BaseIndicatorRepository extends MigratableMongoRepository<IndicatorEntity, String> {
    Page<IndicatorEntity> findAllByIndicatorIn(Collection<String> indicators, Pageable pageable);

    Page<IndicatorEntity> findAllByIndicatorInAndIndicatorTypeIn(
        Collection<String> indicators,
        Collection<Integer> indicatorTypes,
        Pageable pageable
    );
}

package com.example.mongo_migrate_multids.repository.common;

import com.example.mjg.spring.mongo.repositories.MigratableMongoRepository;
import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface BaseStationIndicatorRepository extends MigratableMongoRepository<StationIndicatorEntity, String> {
    Page<StationIndicatorEntity> findAllByStationIdInAndIndicatorIdIn(
        Collection<String> stationIds,
        Collection<String> indicatorIds,
        Pageable pageable
    );
}

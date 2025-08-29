package com.example.mongo_migrate_multids.repository.common;

import java.util.Collection;

import org.springframework.data.domain.Page;

import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;
import org.springframework.data.domain.Pageable;

public interface BaseStationRepository extends MigratableMongoRepository<StationEntity, String> {
    Page<StationEntity> findAllByStationCodeIn(Collection<String> stationCodes, Pageable pageable);
}

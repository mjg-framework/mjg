package com.example.mongo_migrate_multids.repository.common;

import java.util.Collection;

import org.springframework.data.domain.Page;

import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;
import org.springframework.data.domain.Pageable;

public interface BaseAreaRepository extends MigratableMongoRepository<AreaEntity, String> {
    Page<AreaEntity> findAllByAreaCodeIn(Collection<String> areaCodes, Pageable pageable);
}

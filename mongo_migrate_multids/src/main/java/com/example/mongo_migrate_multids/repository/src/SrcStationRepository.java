package com.example.mongo_migrate_multids.repository.src;

import org.springframework.stereotype.Repository;

import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.repository.common.BaseStationRepository;
import com.example.mongo_migrate_multids.repository.src.common.SrcMongoRepositoryInterface;

@Repository
public interface SrcStationRepository
extends
    BaseStationRepository,
    SrcMongoRepositoryInterface<StationEntity, String>
{
}

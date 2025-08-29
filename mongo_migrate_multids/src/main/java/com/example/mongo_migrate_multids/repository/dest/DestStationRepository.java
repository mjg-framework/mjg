package com.example.mongo_migrate_multids.repository.dest;

import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.repository.common.BaseStationRepository;
import com.example.mongo_migrate_multids.repository.dest.common.DestMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface DestStationRepository
extends
    BaseStationRepository,
    DestMongoRepositoryInterface<StationEntity, String>
{
}

package com.example.mongo_migrate_multids.repository.dest;

import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;
import com.example.mongo_migrate_multids.repository.common.BaseStationIndicatorRepository;
import com.example.mongo_migrate_multids.repository.dest.common.DestMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface DestStationIndicatorRepository
extends
    BaseStationIndicatorRepository,
    DestMongoRepositoryInterface<StationIndicatorEntity, String>
{
}

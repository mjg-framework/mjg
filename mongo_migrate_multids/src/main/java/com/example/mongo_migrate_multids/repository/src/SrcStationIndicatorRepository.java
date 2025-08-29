package com.example.mongo_migrate_multids.repository.src;

import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;
import com.example.mongo_migrate_multids.repository.common.BaseStationIndicatorRepository;
import com.example.mongo_migrate_multids.repository.src.common.SrcMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface SrcStationIndicatorRepository
extends
    BaseStationIndicatorRepository,
    SrcMongoRepositoryInterface<StationIndicatorEntity, String>
{
}

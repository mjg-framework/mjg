package com.example.mongo_migrate_multids.repository.dest;

import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.repository.common.BaseIndicatorRepository;
import com.example.mongo_migrate_multids.repository.dest.common.DestMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface DestIndicatorRepository
extends
    BaseIndicatorRepository,
    DestMongoRepositoryInterface<IndicatorEntity, String>
{
}

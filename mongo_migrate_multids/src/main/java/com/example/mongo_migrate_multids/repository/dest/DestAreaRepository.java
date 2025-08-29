package com.example.mongo_migrate_multids.repository.dest;

import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.repository.common.BaseAreaRepository;
import com.example.mongo_migrate_multids.repository.dest.common.DestMongoRepositoryInterface;

public interface DestAreaRepository
extends
    BaseAreaRepository,
    DestMongoRepositoryInterface<AreaEntity, String>
{
}

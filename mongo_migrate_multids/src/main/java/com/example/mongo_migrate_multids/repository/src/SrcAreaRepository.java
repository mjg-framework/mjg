package com.example.mongo_migrate_multids.repository.src;

import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.repository.common.BaseAreaRepository;
import com.example.mongo_migrate_multids.repository.src.common.SrcMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface SrcAreaRepository
extends
    BaseAreaRepository,
    SrcMongoRepositoryInterface<AreaEntity, String>
{
}

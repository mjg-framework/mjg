package com.example.mongo_migrate_multids.repository.src;

import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.repository.common.BaseIndicatorRepository;
import com.example.mongo_migrate_multids.repository.src.common.SrcMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface SrcIndicatorRepository
extends
    BaseIndicatorRepository,
    SrcMongoRepositoryInterface<IndicatorEntity, String>
{
}

package com.example.mongo_migrate_multids.repository.source;

import com.example.migrate.entity.StationEntity;
import com.example.migrate.repository.source.common.SourceMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceStationRepository extends SourceMongoRepositoryInterface<StationEntity, String> {
}
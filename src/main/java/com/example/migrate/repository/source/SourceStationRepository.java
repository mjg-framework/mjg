package com.example.migrate.repository.source;

import com.example.migrate.entity.StationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SourceStationRepository extends MongoRepository<StationEntity, String> {
}
package com.example.mongo_migrate_multids.service;

import com.example.migrate.entity.StationEntity;
import com.example.mongo_migrate_multids.repository.source.SourceStationRepository;
import com.example.mongo_migrate_multids.repository.target.TargetStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    private final MongoTemplate sourceMongo;
    private final MongoTemplate targetMongo;
    private final SourceStationRepository sourceStationRepository;
    private final TargetStationRepository targetStationRepository;

    public MigrationService(@Qualifier("sourceMongoTemplate") MongoTemplate sourceMongo,
                            @Qualifier("targetMongoTemplate") MongoTemplate targetMongo,
                            SourceStationRepository sourceUserRepository,
                            TargetStationRepository targetUserRepository) {
        this.sourceMongo = sourceMongo;
        this.targetMongo = targetMongo;
        this.sourceStationRepository = sourceUserRepository;
        this.targetStationRepository = targetUserRepository;
    }

    public long migrateUsers(int batchSize) {
        List<StationEntity> sourceStations = sourceStationRepository.findAll();
        List<StationEntity> targetStations = targetStationRepository.findAll();

        System.out.println("HELLO WORLD");
        return 1;
    }
}

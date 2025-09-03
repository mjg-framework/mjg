package com.example.mongo_migrate_multids.migrational.datastores.dest;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationIndicatorStore;
import com.example.mongo_migrate_multids.repository.dest.DestStationIndicatorRepository;
import com.mongodb.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DestStationIndicatorStore extends BaseStationIndicatorStore {
    @Getter
    private final DestStationIndicatorRepository repository;

    @Getter
    private final MongoTemplate mongoTemplate;

    @Getter
    @Nullable
    private final MongoTransactionManager txManager;
}

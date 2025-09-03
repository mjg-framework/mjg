package com.example.mongo_migrate_multids.migrational.datastores.dest;

import lombok.Getter;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationStore;
import com.example.mongo_migrate_multids.repository.dest.DestStationRepository;
import com.mongodb.lang.Nullable;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DestStationStore extends BaseStationStore {
    @Getter
    private final DestStationRepository repository;

    @Getter
    private final MongoTemplate mongoTemplate;

    @Getter
    @Nullable
    private final MongoTransactionManager txManager;
}

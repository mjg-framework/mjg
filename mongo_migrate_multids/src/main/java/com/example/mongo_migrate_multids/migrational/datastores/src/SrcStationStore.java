package com.example.mongo_migrate_multids.migrational.datastores.src;

import lombok.Getter;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationStore;
import com.example.mongo_migrate_multids.repository.src.SrcStationRepository;
import com.mongodb.lang.Nullable;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SrcStationStore extends BaseStationStore {
    @Getter
    private final SrcStationRepository repository;

    @Getter
    private final MongoTemplate mongoTemplate;

    @Getter
    @Nullable
    private final MongoTransactionManager txManager;
}

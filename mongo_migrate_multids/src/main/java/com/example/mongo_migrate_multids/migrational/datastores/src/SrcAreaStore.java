package com.example.mongo_migrate_multids.migrational.datastores.src;

import lombok.Getter;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseAreaStore;
import com.example.mongo_migrate_multids.repository.src.SrcAreaRepository;
import com.mongodb.lang.Nullable;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SrcAreaStore extends BaseAreaStore {
    @Getter
    private final SrcAreaRepository repository;

    @Getter
    private final MongoTemplate mongoTemplate;

    @Getter
    @Nullable
    private final MongoTransactionManager txManager;
}

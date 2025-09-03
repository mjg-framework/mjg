package com.example.mongo_migrate_multids.repository.dest.common;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.spring.mongo.repositories.MigratableMongoRepository;

import java.io.Serializable;

public interface DestMongoRepositoryInterface<T extends MigratableEntity, ID extends Serializable>
extends MigratableMongoRepository<T, ID>, DestRepositoryInterface<T, ID>
{}

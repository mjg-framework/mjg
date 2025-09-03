package com.example.mongo_migrate_multids.migrational.datastores.common;

import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;
import com.example.mongo_migrate_multids.entity.IndicatorEntity;

public abstract class BaseIndicatorStore
extends MongoRepositoryStore<IndicatorEntity, String>
{}

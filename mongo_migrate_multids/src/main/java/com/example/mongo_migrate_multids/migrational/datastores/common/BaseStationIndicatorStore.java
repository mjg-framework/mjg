package com.example.mongo_migrate_multids.migrational.datastores.common;

import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;
import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;

public abstract class BaseStationIndicatorStore
extends MongoRepositoryStore<StationIndicatorEntity, String>
{}

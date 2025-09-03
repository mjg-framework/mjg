package com.example.mongo_migrate_multids.migrational.datastores.common;

import com.example.mjg.spring.mongo.stores.MongoRepositoryStore;

import com.example.mongo_migrate_multids.entity.StationEntity;

public abstract class BaseStationStore
extends MongoRepositoryStore<StationEntity, String>
{}

package com.example.mjg.spring.mongo.stores;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.spring.stores.SpringRepositoryStore;

import java.io.Serializable;

public abstract class MongoRepositoryStore<
    T extends MigratableEntity,
    ID extends Serializable
>
extends SpringRepositoryStore<T, ID>
{
}

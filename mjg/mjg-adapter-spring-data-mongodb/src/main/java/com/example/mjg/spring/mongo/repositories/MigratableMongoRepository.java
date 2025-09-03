package com.example.mjg.spring.mongo.repositories;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.spring.repositories.MigratableSpringRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.io.Serializable;

public interface MigratableMongoRepository<T extends MigratableEntity, ID extends Serializable>
extends MigratableSpringRepository<T, ID>, MongoRepository<T, ID>
{
}

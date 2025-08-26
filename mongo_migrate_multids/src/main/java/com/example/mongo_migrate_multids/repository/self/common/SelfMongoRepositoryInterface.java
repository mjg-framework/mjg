package com.example.mongo_migrate_multids.repository.self.common;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SelfMongoRepositoryInterface<T, ID> extends MongoRepository<T, ID>, SelfRepositoryInterface<T, ID> {
}

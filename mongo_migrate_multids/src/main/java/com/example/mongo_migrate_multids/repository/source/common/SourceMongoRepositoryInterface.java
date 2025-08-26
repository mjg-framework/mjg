package com.example.mongo_migrate_multids.repository.source.common;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SourceMongoRepositoryInterface<T, ID> extends MongoRepository<T, ID>, SourceRepositoryInterface<T, ID> {
}

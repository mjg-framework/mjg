package com.example.mongo_migrate_multids.repository.dest.common;

import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;

public interface DestMongoRepositoryInterface<T, ID> extends MigratableMongoRepository<T, ID>, DestRepositoryInterface<T, ID> {
}

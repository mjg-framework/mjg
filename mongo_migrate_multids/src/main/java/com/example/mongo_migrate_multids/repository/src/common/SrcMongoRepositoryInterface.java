package com.example.mongo_migrate_multids.repository.src.common;

import com.example.mongo_migrate_multids.repository.common.common.MigratableMongoRepository;

public interface SrcMongoRepositoryInterface<T, ID> extends MigratableMongoRepository<T, ID>, SrcRepositoryInterface<T, ID> {
}

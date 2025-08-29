package com.example.mongo_migrate_multids.repository.common.common;

import java.util.Collection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.lang.NonNull;

public interface MigratableMongoRepository<T, ID>
extends MongoRepository<T, ID> {
    @NonNull Page<T> findAll(@NonNull Pageable pageable);

    Page<T> findAllByIdIn(Collection<ID> ids, Pageable pageable);
}

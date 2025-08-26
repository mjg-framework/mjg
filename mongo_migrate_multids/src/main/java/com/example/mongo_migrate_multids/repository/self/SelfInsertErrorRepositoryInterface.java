package com.example.mongo_migrate_multids.repository.self;

import com.example.migrate.entity.self.InsertErrorEntity;
import com.example.migrate.repository.self.common.SelfMongoRepositoryInterface;
import org.springframework.stereotype.Repository;

@Repository
public interface SelfInsertErrorRepositoryInterface extends SelfMongoRepositoryInterface<InsertErrorEntity, String> {
}

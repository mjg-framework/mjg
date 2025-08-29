package com.example.mongo_migrate_multids.migrational.datastores.src;

import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseAreaStore;
import com.example.mongo_migrate_multids.repository.common.BaseAreaRepository;
import com.example.mongo_migrate_multids.repository.src.SrcAreaRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SrcAreaStore extends BaseAreaStore {
    private final SrcAreaRepository repository;

    @Override
    protected BaseAreaRepository doGetAreaRepository() {
        return repository;
    }
}

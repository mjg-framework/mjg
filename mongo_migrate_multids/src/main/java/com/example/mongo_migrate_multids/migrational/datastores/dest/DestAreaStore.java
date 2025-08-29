package com.example.mongo_migrate_multids.migrational.datastores.dest;

import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseAreaStore;
import com.example.mongo_migrate_multids.repository.common.BaseAreaRepository;
import com.example.mongo_migrate_multids.repository.dest.DestAreaRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DestAreaStore extends BaseAreaStore {
    private final DestAreaRepository repository;

    @Override
    protected BaseAreaRepository doGetAreaRepository() {
        return repository;
    }
}

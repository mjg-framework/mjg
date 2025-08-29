package com.example.mongo_migrate_multids.migrational.datastores.dest;

import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationStore;
import com.example.mongo_migrate_multids.repository.common.BaseStationRepository;
import com.example.mongo_migrate_multids.repository.dest.DestStationRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DestStationStore extends BaseStationStore {
    private final DestStationRepository repository;

    @Override
    protected BaseStationRepository doGetStationRepository() {
        return repository;
    }
}

package com.example.mongo_migrate_multids.migrational.datastores.src;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationIndicatorStore;
import com.example.mongo_migrate_multids.repository.common.BaseStationIndicatorRepository;
import com.example.mongo_migrate_multids.repository.src.SrcStationIndicatorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SrcStationIndicatorStore
extends BaseStationIndicatorStore {
    private final SrcStationIndicatorRepository repository;

    @Override
    protected BaseStationIndicatorRepository doGetStationIndicatorRepository() {
        return repository;
    }
}

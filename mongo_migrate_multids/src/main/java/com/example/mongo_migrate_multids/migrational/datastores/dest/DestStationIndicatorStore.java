package com.example.mongo_migrate_multids.migrational.datastores.dest;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationIndicatorStore;
import com.example.mongo_migrate_multids.repository.dest.DestStationIndicatorRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DestStationIndicatorStore extends BaseStationIndicatorStore {
    @Getter
    private final DestStationIndicatorRepository repository;
}

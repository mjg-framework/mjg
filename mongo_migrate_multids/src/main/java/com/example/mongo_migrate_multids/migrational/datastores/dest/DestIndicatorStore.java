package com.example.mongo_migrate_multids.migrational.datastores.dest;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseIndicatorStore;
import com.example.mongo_migrate_multids.repository.common.BaseIndicatorRepository;
import com.example.mongo_migrate_multids.repository.dest.DestIndicatorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DestIndicatorStore extends BaseIndicatorStore {
    private final DestIndicatorRepository repository;

    @Override
    protected BaseIndicatorRepository doGetIndicatorRepository() {
        return repository;
    }
}

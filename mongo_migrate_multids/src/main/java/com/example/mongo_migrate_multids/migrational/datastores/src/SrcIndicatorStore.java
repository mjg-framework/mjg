package com.example.mongo_migrate_multids.migrational.datastores.src;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseIndicatorStore;
import com.example.mongo_migrate_multids.repository.common.BaseIndicatorRepository;
import com.example.mongo_migrate_multids.repository.src.SrcIndicatorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SrcIndicatorStore extends BaseIndicatorStore {
    private final SrcIndicatorRepository repository;

    @Override
    protected BaseIndicatorRepository doGetIndicatorRepository() {
        return repository;
    }
}

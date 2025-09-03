package com.example.mongo_migrate_multids.migrational.datastores.dest;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseIndicatorStore;
import com.example.mongo_migrate_multids.repository.dest.DestIndicatorRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DestIndicatorStore extends BaseIndicatorStore {
    @Getter
    private final DestIndicatorRepository repository;
}

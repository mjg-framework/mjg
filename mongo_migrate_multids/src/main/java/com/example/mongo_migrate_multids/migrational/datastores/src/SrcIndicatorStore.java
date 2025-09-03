package com.example.mongo_migrate_multids.migrational.datastores.src;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseIndicatorStore;
import com.example.mongo_migrate_multids.repository.src.SrcIndicatorRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SrcIndicatorStore extends BaseIndicatorStore {
    @Getter
    private final SrcIndicatorRepository repository;
}

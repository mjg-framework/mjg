package com.example.mongo_migrate_multids.migrational.datastores.src;

import lombok.Getter;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseStationStore;
import com.example.mongo_migrate_multids.repository.src.SrcStationRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class SrcStationStore extends BaseStationStore {
    @Getter
    private final SrcStationRepository repository;
}

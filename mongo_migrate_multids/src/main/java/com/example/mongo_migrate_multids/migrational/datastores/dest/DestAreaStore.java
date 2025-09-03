package com.example.mongo_migrate_multids.migrational.datastores.dest;

import lombok.Getter;
import org.springframework.stereotype.Component;

import com.example.mongo_migrate_multids.migrational.datastores.common.BaseAreaStore;
import com.example.mongo_migrate_multids.repository.dest.DestAreaRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DestAreaStore extends BaseAreaStore {
    @Getter
    private final DestAreaRepository repository;
}

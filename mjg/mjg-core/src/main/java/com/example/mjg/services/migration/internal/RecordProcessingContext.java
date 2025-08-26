package com.example.mjg.services.migration.internal;

import java.util.HashMap;
import java.util.Map;

import com.example.mjg.data.MigratableEntity;

import lombok.Getter;

@Getter
public class RecordProcessingContext {
    private final MigratableEntity record;

    private final Map<String, Object> aggregates;

    public RecordProcessingContext(MigratableEntity record) {
        this.record = record;

        this.aggregates = new HashMap<>();
    }
}

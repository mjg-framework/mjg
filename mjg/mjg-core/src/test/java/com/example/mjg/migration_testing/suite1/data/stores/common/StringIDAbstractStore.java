package com.example.mjg.migration_testing.suite1.data.stores.common;

import com.example.mjg.data.MigratableEntity;

import java.util.function.Function;

public abstract class StringIDAbstractStore<T extends MigratableEntity, FILTER_TYPE, FILTER_VALUE>
extends SimpleAbstractStore<T, String, FILTER_TYPE, FILTER_VALUE> {
    @Override
    protected String generateNewRecordId(Function<String, Boolean> idAlreadyExists) {
        String id;
        do {
            id = String.format("FAKE_ID_%f", Math.random());
        } while (idAlreadyExists.apply(id));
        return id;
    }
}

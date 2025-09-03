package com.example.mjg.migration_testing.suite1.data.stores.common;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.MigratableEntity;

import java.security.SecureRandom;
import java.util.function.Function;

public abstract class IntegerIDAbstractStore<T extends MigratableEntity, F extends DataFilterSet>
        extends SimpleAbstractStore<T, Integer, F> {
    @Override
    protected Integer generateNewRecordId(Function<Integer, Boolean> idAlreadyExists) {
        SecureRandom secureRandom = new SecureRandom();
        int id;
        do {
            id = secureRandom.nextInt();
        } while (idAlreadyExists.apply(id));
        return id;
    }
}

package com.example.mjg.migration_testing.suite1.data.stores.common;

import com.example.mjg.data.MigratableEntity;

import java.util.Random;
import java.util.function.Function;

public abstract class IntegerIDAbstractStore<T extends MigratableEntity, FILTER_TYPE, FILTER_VALUE>
        extends SimpleAbstractStore<T, Integer, FILTER_TYPE, FILTER_VALUE> {
    @Override
    protected Integer generateNewRecordId(Function<Integer, Boolean> idAlreadyExists) {
        Random rnd = new Random();
        int id;
        do {
            id = rnd.nextInt(1, Integer.MAX_VALUE);
        } while (idAlreadyExists.apply(id));
        return id;
    }
}

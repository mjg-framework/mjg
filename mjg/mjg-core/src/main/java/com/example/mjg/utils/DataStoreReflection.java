package com.example.mjg.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DataStoreReflection {
    private final Class<? extends DataStore<?, ?, ?>> storeClass;
    private final ParameterizedType rootStoreClass;

    private final Class<? extends MigratableEntity> entityClass;

    private final Class<?> filterTypeClass;

    private final Class<?> filterValueClass;

    public DataStoreReflection(Class<? extends DataStore<?, ?, ?>> storeClass) {
        this.storeClass = storeClass;

        this.rootStoreClass = (ParameterizedType) ReflectionUtils.getSuperclassType(
            storeClass,
            DataStore.class
        );

        if (rootStoreClass == null) {
            throw new RuntimeException("Could not find root store class for " + storeClass);
        }
        Type entityType = rootStoreClass.getActualTypeArguments()[0];

        if (entityType instanceof Class<?> rawEntityClass && MigratableEntity.class.isAssignableFrom(rawEntityClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends MigratableEntity> entityClass = (Class<? extends MigratableEntity>) rawEntityClass;
            this.entityClass = entityClass;
        } else {
            throw new RuntimeException("DataStore<T, F1, F2> where T is not a Migratable, but " + entityType);
        }

        this.filterTypeClass = (Class<?>) rootStoreClass.getActualTypeArguments()[1];

        this.filterValueClass = (Class<?>) rootStoreClass.getActualTypeArguments()[2];
    }
}

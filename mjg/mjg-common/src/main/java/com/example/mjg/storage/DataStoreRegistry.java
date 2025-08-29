package com.example.mjg.storage;

import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DataStoreRegistry extends AbstractRegistry<DataStore<? extends MigratableEntity, Object, Object>> {
    public <ENTITY extends MigratableEntity, FILTER_TYPE, FILTER_VALUE>
    void set(
        Class<? extends DataStore<ENTITY, FILTER_TYPE, FILTER_VALUE>> clazz,
        DataStore<ENTITY, FILTER_TYPE, FILTER_VALUE> instance)
    {
        @SuppressWarnings("unchecked")
        DataStore<? extends MigratableEntity, Object, Object> realInstance = (
            DataStore<? extends MigratableEntity, Object, Object>
        ) instance;

        set(clazz.getCanonicalName(), realInstance);
    }
}

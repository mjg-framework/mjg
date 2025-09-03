package com.example.mjg.storage;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
public class DataStoreRegistry extends AbstractRegistry<DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> {
    public <T extends MigratableEntity, ID extends Serializable, F extends DataFilterSet>
    void set(
        Class<? extends DataStore<T, ID, F>> clazz,
        DataStore<T, ID, F> instance
    ) {
        set(clazz.getCanonicalName(), instance);
    }
}

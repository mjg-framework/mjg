package com.example.mjg.migration_testing.suite1.data.mocking.common;

import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.migration_testing.suite1.data.stores.common.SimpleAbstractStore;
import com.example.mjg.services.migration.MigrationService;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MockDataLoader {
    public static <T extends MigratableEntity> void load(
        Class<? extends DataStore<T, ?, ?>> dataStoreClass,
        List<T> records
    ) {
        SimpleAbstractStore<T, ?, ?, ?> mockableStore = getStore(dataStoreClass);
        mockableStore.getRecords().clear();
        mockableStore.getRecords().addAll(records);
    }

    public static <T extends MigratableEntity> void reset(
        Class<? extends DataStore<T, ?, ?>> dataStoreClass
    ) {
        MockDataLoader.load(dataStoreClass, List.of());
    }

    public static <T extends MigratableEntity> SimpleAbstractStore<T, ?, ?, ?> getStore(
        Class<? extends DataStore<?, ?, ?>> dataStoreClass
    ) {
        @SuppressWarnings("unchecked")
        DataStore<T, ?, ?> dataStore = (DataStore<T, ?, ?>) MigrationService._getInstForTesting()
            .getDataStoreRegistry()
            .get(dataStoreClass.getCanonicalName());

        if (dataStore instanceof SimpleAbstractStore<T, ?, ?, ?> mockableStore) {
            return mockableStore;
        } else {
            throw new IllegalArgumentException(
                "Not a SimpleAbstractStore: " + dataStoreClass.getCanonicalName()
            );
        }
    }

//    public static <T extends MigratableEntity> void loadAll(
//        Map<
//            Class<? extends DataStore<T, Object, Object>>,
//            List<T>
//        > dataSet
//    ) {
//        dataSet.forEach(MockDataLoader::load);
//    }
}

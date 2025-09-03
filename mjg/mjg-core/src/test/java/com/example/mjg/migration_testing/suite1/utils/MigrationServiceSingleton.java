package com.example.mjg.migration_testing.suite1.utils;

import com.example.mjg.services.migration.MigrationService;
import com.example.mjg.storage.DataStoreRegistry;

/**
 * For testing only.
 */
public class MigrationServiceSingleton {
    private static class InstanceHolder {
        private static final MigrationService instance = new MigrationService(new DataStoreRegistry());
    };

    public static MigrationService getInstance() {
        return InstanceHolder.instance;
    }
}

package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.services.migration.internal.MigrationRunner;

import java.util.Set;

public interface RecordGroup {
    Set<Object> getRecordIds();

    MigrationRunner getMigrationRunner();
}

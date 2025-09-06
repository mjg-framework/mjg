package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;

public interface RecordGroup {
    MigrationRunner getMigrationRunner();
}

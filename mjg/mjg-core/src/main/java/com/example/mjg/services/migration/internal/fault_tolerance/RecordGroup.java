package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.MigrationRunner;

import java.util.List;

public interface RecordGroup {
    List<MigratableEntity> getRecords();

    MigrationRunner getMigrationRunner();
}

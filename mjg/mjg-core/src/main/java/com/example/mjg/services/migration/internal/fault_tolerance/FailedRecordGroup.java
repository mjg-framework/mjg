package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FailedRecordGroup implements RecordGroup {
    private List<MigratableEntity> records;

    private MigrationRunner migrationRunner;

    private ErrorResolution errorResolution;

    private Exception exception;
}

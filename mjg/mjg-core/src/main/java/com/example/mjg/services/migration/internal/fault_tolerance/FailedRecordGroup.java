package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.config.ErrorResolution;
import com.example.mjg.services.migration.internal.MigrationRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class FailedRecordGroup implements RecordGroup {
    private Set<Object> recordIds;

    private MigrationRunner migrationRunner;

    private ErrorResolution errorResolution;

    private Exception exception;
}

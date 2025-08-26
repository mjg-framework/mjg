package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.services.migration.internal.MigrationRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class SuccessfulRecordGroup implements RecordGroup {
    private Set<Object> recordIds;

    private MigrationRunner migrationRunner;
}

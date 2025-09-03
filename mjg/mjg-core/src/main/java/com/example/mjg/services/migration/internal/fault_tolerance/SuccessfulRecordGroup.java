package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.migration_runner.MigrationRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class SuccessfulRecordGroup implements RecordGroup {
    private Set<Serializable> recordIds;

    private MigrationRunner migrationRunner;
}

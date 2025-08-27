package com.example.mjg.services.migration.internal.fault_tolerance;

import com.example.mjg.data.MigratableEntity;
import com.example.mjg.services.migration.internal.MigrationRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SuccessfulRecordGroup implements RecordGroup {
    private List<MigratableEntity> records;

    private MigrationRunner migrationRunner;
}

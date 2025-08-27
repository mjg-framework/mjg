package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationProgressPerMigrationClass implements Serializable {
    private String fqcn = "";

    private Set<Object> migratedRecordIds = new HashSet<>();

    private Set<FailedRecord> failedRecords = new HashSet<>();

    public MigrationProgressPerMigrationClass(String fqcn) {
        this(fqcn, new HashSet<Object>(), new HashSet<FailedRecord>());
    }
}

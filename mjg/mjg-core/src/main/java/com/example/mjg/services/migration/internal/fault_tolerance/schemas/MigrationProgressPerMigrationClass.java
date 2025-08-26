package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationProgressPerMigrationClass implements Serializable {
    private String fqcn = "";

    private ArrayList<MigratedRecord> migratedRecords = new ArrayList<>();

    private ArrayList<FailedRecord> failedRecords = new ArrayList<>();

    public MigrationProgressPerMigrationClass(String fqcn) {
        this(fqcn, new ArrayList<MigratedRecord>(), new ArrayList<FailedRecord>());
    }
}

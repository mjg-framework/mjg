package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationProgressPerMigrationClass implements Serializable {
    private String fqcn = "";

    private Set<Serializable> succeededRecordIds = new HashSet<>();

    private List<FailedRecord> failedRecords = new ArrayList<>();

    public MigrationProgressPerMigrationClass(String fqcn) {
        this(fqcn, new HashSet<Serializable>(), new ArrayList<FailedRecord>());
    }
}

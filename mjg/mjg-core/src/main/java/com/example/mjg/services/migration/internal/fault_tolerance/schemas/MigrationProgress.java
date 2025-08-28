package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MigrationProgress {
    private MigrationProgressMetadata metadata = new MigrationProgressMetadata();

    private HashMap<String, MigrationProgressPerMigrationClass> migrationProgress = new HashMap<>();

    private ArrayList<String> fatalErrors = new ArrayList<>();
}

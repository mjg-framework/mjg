package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationProgressMetadata implements Serializable {
    private LocalDateTime timestamp = LocalDateTime.now();

    private Set<String> completedMigrationFQCNs = new HashSet<>();

    private Set<String> inProgressMigrationFQCNs = new HashSet<>();
}

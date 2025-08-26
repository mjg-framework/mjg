package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationProgressMetadata implements Serializable {
    private LocalDateTime timestamp = LocalDateTime.now();

    private ArrayList<MigrationClassMetadata> migrationClasses = new ArrayList<>();
}

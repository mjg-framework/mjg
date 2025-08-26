package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MigrationClassMetadata implements Serializable {
    private String fqcn = "";

    private HashMap<String, String> forEachEntityFrom = new HashMap<>();

    private ArrayList<HashMap<String, String>> matchWiths = new ArrayList<>();

    private HashMap<String, String> transformAndSaveTo = new HashMap<>();
}

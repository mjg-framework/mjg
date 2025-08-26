package com.example.mjg.services.migration.internal.reflective;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.utils.DataStoreReflection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RForEachRecordFrom {
    private final Class<?> migrationClass;
    
    private final ForEachRecordFrom forEachRecordFrom;

    private final DataStoreReflection dataStoreReflection;
}

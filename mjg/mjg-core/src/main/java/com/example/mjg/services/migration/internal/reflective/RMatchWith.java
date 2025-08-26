package com.example.mjg.services.migration.internal.reflective;

import com.example.mjg.annotations.MatchWith;
import com.example.mjg.utils.DataStoreReflection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RMatchWith {
    private final Class<?> migrationClass;
    
    private final MatchWith matchWith;

    private final DataStoreReflection dataStoreReflection;
}

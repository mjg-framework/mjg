package com.example.mjg.services.migration.internal.reflective;

import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.utils.DataStoreReflection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RTransformAndSaveTo {
    private final Class<?> migrationClass;

    private final TransformAndSaveTo transformAndSaveTo;

    private final DataStoreReflection dataStoreReflection;
}

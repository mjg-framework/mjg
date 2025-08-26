package com.example.mjg.processors.Migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class PTransformAndSaveTo implements ProcessedAnnotation {
    @Getter
    private final String fqcn;

    @Getter
    private final Map<String, Object> annotationValues;
}

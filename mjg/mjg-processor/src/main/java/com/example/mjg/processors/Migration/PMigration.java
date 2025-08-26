package com.example.mjg.processors.Migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
public class PMigration implements ProcessedAnnotation {
    @Getter
    private String fqcn;

    @Getter
    private Map<String, Object> annotationValues;

    @Getter
    private int order;
}

package com.example.mjg.processors.Migration;

import java.util.Map;

public interface ProcessedAnnotation {
    String getFqcn();

    default String getFQCN() {
        return getFqcn();
    }

    Map<String, Object> getAnnotationValues();
}

package com.example.mjg.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.mjg.config.ErrorResolution;

public class AnnotationInstantiationTest {
    @Test
    public void testCreateErrorResolution() {
        ErrorResolution r = AnnotationInstantiation.createErrorResolution(
            11,
            35
        );

        assertEquals(
            11,
            r.retryTimes()
        );

        assertEquals(
            35,
            r.retryDelayInSeconds()
        );
    }
}

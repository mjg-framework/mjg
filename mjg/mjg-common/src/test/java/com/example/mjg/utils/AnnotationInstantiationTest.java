package com.example.mjg.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.mjg.config.ErrorResolution;

public class AnnotationInstantiationTest {
    @Test
    public void testCreateErrorResolution() {
        ErrorResolution r = AnnotationInstantiation.createErrorResolution(
            ErrorResolution.Strategy.FINISH_THE_MIGRATION_THEN_STOP_AND_REPORT,
            11,
            35
        );

        assertEquals(
            ErrorResolution.Strategy.FINISH_THE_MIGRATION_THEN_STOP_AND_REPORT,
            r.strategy()
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

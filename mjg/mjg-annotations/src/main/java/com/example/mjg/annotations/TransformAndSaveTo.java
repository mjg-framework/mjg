package com.example.mjg.annotations;

import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.DataStore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransformAndSaveTo {
    Class<? extends DataStore<?, ?, ?>> value();

    Cardinality cardinality() default Cardinality.EXACTLY_ONE;

    ErrorResolution inCaseOfError() default @ErrorResolution;

    int batchSize() default 512;

    int retries() default 1;

    int retryDelayInSeconds() default 3;
}

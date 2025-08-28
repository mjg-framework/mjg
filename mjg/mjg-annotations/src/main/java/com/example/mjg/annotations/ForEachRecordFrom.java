package com.example.mjg.annotations;

import com.example.mjg.data.DataStore;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForEachRecordFrom {
    Class<? extends DataStore<?, ?, ?>> value();

    int batchSize() default 512;

    int retries() default 1;

    int retryDelayInSeconds() default 3;
}

package com.example.mjg.annotations;

import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import java.io.Serializable;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ForEachRecordFrom {
    Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> value();

    int batchSize() default 512;

    ErrorResolution inCaseOfError() default @ErrorResolution;
}

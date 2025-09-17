package com.example.mjg.annotations;

import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransformAndSaveTo {
    Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> value();

    Cardinality cardinality() default Cardinality.ZERO_OR_MORE;

    ErrorResolution inCaseOfError() default @ErrorResolution;

    int batchSize() default 512;
}

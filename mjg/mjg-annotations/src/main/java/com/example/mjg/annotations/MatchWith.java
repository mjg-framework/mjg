package com.example.mjg.annotations;

import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.DataStore;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Repeatable(MatchWithEntries.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MatchWith {
    Class<? extends DataStore<?, ?, ?>> value();

    Cardinality cardinality() default Cardinality.EXACTLY_ONE;

    ErrorResolution inCaseOfError() default @ErrorResolution;

    int batchSize() default 512;

    int order() default 0;
}

package com.example.mjg.annotations;

import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataStore;
import com.example.mjg.data.MigratableEntity;

import java.io.Serializable;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Repeatable(MatchWithEntries.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MatchWith {
    Class<? extends DataStore<? extends MigratableEntity, ? extends Serializable, ? extends DataFilterSet>> value();

    Cardinality cardinality() default Cardinality.ZERO_OR_MORE;

    ErrorResolution inCaseOfError() default @ErrorResolution;

    int batchSize() default 512;

    int order() default 0;
}

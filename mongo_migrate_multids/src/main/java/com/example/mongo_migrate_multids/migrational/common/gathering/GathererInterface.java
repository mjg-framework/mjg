package com.example.mongo_migrate_multids.migrational.common.gathering;

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface GathererInterface<T> {
    GatherResult<T> gather(
        @Nullable GatherResult<T> previousResult
    );

    Map<Object, Stream<T>> filterByMappingValues(
        Integer mappingValueType,
        Set<Object> mappingValues
    );
}

package com.example.mongo_migrate_multids.migrational.common;

import com.example.migrate.migrational.common.gathering.GathererInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Getter
@Setter
@AllArgsConstructor
public abstract class Dependency<A, B> {
    private GathererInterface<A> gathererA;

    private GathererInterface<B> gathererB;

    protected abstract Object getMappingValueFromBEntity(B b);

    protected abstract Map<Object, Collection<A>> getAEntitiesByMappingValues(
        Set<Object> mappingValues
    );

    protected abstract Stream<B> transformBEntities(
        Stream<B> BEntities,
        Object mappingValue,
        Collection<A> mappedAEntities
    );
}

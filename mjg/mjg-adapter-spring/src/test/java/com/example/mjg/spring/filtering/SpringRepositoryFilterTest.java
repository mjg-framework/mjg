package com.example.mjg.spring.filtering;

import com.example.mjg.spring.filtering.testing.MyEntity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpringRepositoryFilterTest {
    @Test
    public void testIdentity_findAll() {
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet1 =
            SpringRepositoryFilterSet.findAll();

        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2 =
            SpringRepositoryFilterSet.findAll();

        SpringRepositoryFilterSet<MyEntity, Integer> unrelatedFilterSet1 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of());

        assertEquals(filterSet1, filterSet1);
        assertEquals(filterSet2, filterSet2);
        assertEquals(unrelatedFilterSet1, unrelatedFilterSet1);
        assertEquals(filterSet1, filterSet2);
        assertEquals(filterSet2, filterSet1);

        assertNotEquals(filterSet1, unrelatedFilterSet1);
        assertNotEquals(filterSet2, unrelatedFilterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet2);
    }

    @Test
    public void testIdentity_findAllByIdIn() {
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet1 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(1));

        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(1));

        SpringRepositoryFilterSet<MyEntity, Integer> unrelatedFilterSet1 =
            SpringRepositoryFilterSet.findAll();

        assertEquals(filterSet1, filterSet1);
        assertEquals(filterSet2, filterSet2);
        assertEquals(unrelatedFilterSet1, unrelatedFilterSet1);
        assertEquals(filterSet1, filterSet2);
        assertEquals(filterSet2, filterSet1);

        assertNotEquals(filterSet1, unrelatedFilterSet1);
        assertNotEquals(filterSet2, unrelatedFilterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet2);
    }

    @Test
    public void testIdentity_findAllByIdIn_differentArgs() {
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet1 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1));

        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1));

        SpringRepositoryFilterSet<MyEntity, Integer> unrelatedFilterSet1 =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1, 2));

        assertEquals(filterSet1, filterSet1);
        assertEquals(filterSet2, filterSet2);
        assertEquals(unrelatedFilterSet1, unrelatedFilterSet1);
        assertEquals(filterSet1, filterSet2);
        assertEquals(filterSet2, filterSet1);

        assertNotEquals(filterSet1, unrelatedFilterSet1);
        assertNotEquals(filterSet2, unrelatedFilterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet1);
        assertNotEquals(unrelatedFilterSet1, filterSet2);
    }

    @Test
    public void testIdentity_inMap() {
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet1a =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1));
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet1b =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1));


        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2a =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1, 2));
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2b =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1, 2));
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet2c =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of(-1, 2));


        SpringRepositoryFilterSet<MyEntity, Integer> filterSet3a =
            SpringRepositoryFilterSet.findAll();
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet3b =
            SpringRepositoryFilterSet.findAll();


        SpringRepositoryFilterSet<MyEntity, Integer> filterSet4a =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of());
        SpringRepositoryFilterSet<MyEntity, Integer> filterSet4b =
            SpringRepositoryFilterSet.findAllByIdIn(Set.of());



        Map<
            SpringRepositoryFilterSet<MyEntity, Integer>,
            Integer
        > map = new HashMap<>();

        map.merge(filterSet1a, 1, Integer::sum);
        map.merge(filterSet1b, 1, Integer::sum);
        map.merge(filterSet2a, 1, Integer::sum);
        map.merge(filterSet2b, 1, Integer::sum);
        map.merge(filterSet2c, 1, Integer::sum);
        map.merge(filterSet3a, 1, Integer::sum);
        map.merge(filterSet3b, 1, Integer::sum);
        map.merge(filterSet4a, 1, Integer::sum);
        map.merge(filterSet4b, 1, Integer::sum);

        map.merge(filterSet1a, 1, Integer::sum);
        map.merge(filterSet1b, 1, Integer::sum);
        map.merge(filterSet2a, 1, Integer::sum);
        map.merge(filterSet2b, 1, Integer::sum);
        map.merge(filterSet2c, 1, Integer::sum);
        map.merge(filterSet3a, 1, Integer::sum);
        map.merge(filterSet3b, 1, Integer::sum);
        map.merge(filterSet4a, 1, Integer::sum);
        map.merge(filterSet4b, 1, Integer::sum);

        map.computeIfAbsent(filterSet1a, k -> 0);
        map.computeIfAbsent(filterSet1b, k -> 0);
        map.computeIfAbsent(filterSet2a, k -> 0);
        map.computeIfAbsent(filterSet2b, k -> 0);
        map.computeIfAbsent(filterSet2c, k -> 0);
        map.computeIfAbsent(filterSet3a, k -> 0);
        map.computeIfAbsent(filterSet3b, k -> 0);
        map.computeIfAbsent(filterSet4a, k -> 0);
        map.computeIfAbsent(filterSet4b, k -> 0);

        assertEquals(4, map.get(filterSet1a));
        assertEquals(4, map.get(filterSet1b));
        assertEquals(6, map.get(filterSet2a));
        assertEquals(6, map.get(filterSet2b));
        assertEquals(6, map.get(filterSet2c));
        assertEquals(4, map.get(filterSet3a));
        assertEquals(4, map.get(filterSet3b));
        assertEquals(4, map.get(filterSet4a));
        assertEquals(4, map.get(filterSet4b));
    }
}

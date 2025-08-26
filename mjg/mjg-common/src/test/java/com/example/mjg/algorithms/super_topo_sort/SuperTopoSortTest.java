package com.example.mjg.algorithms.super_topo_sort;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SuperTopoSortTest {
    @Test
    public void test() {
        testWithType(Function.identity());

        testWithType(String::valueOf);

        testWithType(i -> new Object());
    }

    public <T> void testWithType(Function<Integer, T> typeMapper) {
        final Map<Integer, T> mappedValues = new HashMap<>();
        final Function<Integer, T> t = (Integer i) -> mappedValues.computeIfAbsent(
                i,
                typeMapper
        );

        testWithPossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(4), t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(2)), Set.of(t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3))),
                        new SuperEdge<T>(Set.of(t.apply(2)), Set.of(t.apply(5)))
                )
        );

        testWithPossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3), t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(5), t.apply(6)), Set.of(t.apply(7))),
                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8)))
                )
        );

        testWithImpossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3), t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(5), t.apply(6)), Set.of(t.apply(7))),
                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8))),

                        new SuperEdge<T>(Set.of(t.apply(3)), Set.of(t.apply(2)))
                )
        );

        testWithImpossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3), t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(5), t.apply(6)), Set.of(t.apply(7))),
                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8))),

                        new SuperEdge<T>(Set.of(t.apply(8)), Set.of(t.apply(1)))
                )
        );

        testWithImpossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3), t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(5), t.apply(6)), Set.of(t.apply(7))),
                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8))),

                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(4)), Set.of(t.apply(1), t.apply(2)))
                )
        );

        testWithImpossible(
                List.of(
                        new SuperEdge<T>(Set.of(t.apply(1), t.apply(2)), Set.of(t.apply(3), t.apply(4))),
                        new SuperEdge<T>(Set.of(t.apply(5)), Set.of(t.apply(6))),
                        new SuperEdge<T>(Set.of(t.apply(3), t.apply(5), t.apply(6)), Set.of(t.apply(7))),
                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8))),

                        new SuperEdge<T>(Set.of(t.apply(7)), Set.of(t.apply(8)))
                )
        );
    }

    private <T> void testWithImpossible(List<SuperEdge<T>> inputSuperEdges) {
        assertThrows(IllegalArgumentException.class, () -> {
            SuperTopoSort<T> sorter = new SuperTopoSort<>(inputSuperEdges);
            sorter.toSorted();
        });
    }

    private <T> void testWithPossible(List<SuperEdge<T>> inputSuperEdges) {
        SuperTopoSort<T> sorter = new SuperTopoSort<>(inputSuperEdges);
        validateSolution(
            sorter.toSorted()
        );
    }

    private <T> void validateSolution(List<SuperEdge<T>> sortedSuperEdges) {
        Set<T> allValues = collectAllValuesFromEdges(sortedSuperEdges);

        HashSet<T> resolvedValues = getAllRootValuesFromEdges(allValues, sortedSuperEdges);

        for (SuperEdge<T> edge : sortedSuperEdges) {
            assertTrue(
                resolvedValues.containsAll(edge.getSourceNodeValues())
            );

            resolvedValues.addAll(edge.getTargetNodeValues());
        }
    }

    private<T> Set<T> collectAllValuesFromEdges(Collection<SuperEdge<T>> superEdges) {
        return superEdges
                .stream()
                .flatMap(
                        superEdge -> Stream.concat(
                                superEdge.getSourceNodeValues().stream(),
                                superEdge.getTargetNodeValues().stream()
                        )
                )
                .collect(Collectors.toSet());
    }

    /**
     * Root nodes are nodes without predecessors
     */
    private<T> HashSet<T> getAllRootValuesFromEdges(Set<T> allNodeValues, Collection<SuperEdge<T>> superEdges) {
        Set<T> valuesWithPredecessors = new HashSet<>();
        for (SuperEdge<T> e : superEdges) {
            valuesWithPredecessors.addAll(e.getTargetNodeValues());
        }
        HashSet<T> valuesWithoutPredecessors = new HashSet<>(allNodeValues);
        valuesWithoutPredecessors.removeAll(valuesWithPredecessors);
        return valuesWithoutPredecessors;
    }
}

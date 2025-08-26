package com.example.mjg.algorithms.super_topo_sort;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
class InternalSuperEdge<T> {
    private final SuperEdge<T> originalSuperEdge;

    private final Set<InternalNode<T>> sourceNodes;

    private final Set<InternalNode<T>> targetNodes;
}

package com.example.mjg.algorithms.super_topo_sort;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class SuperEdge<T> {
    private Set<T> sourceNodeValues;

    private Set<T> targetNodeValues;

    @Override
    public String toString() {
        return "(" + sourceNodeValues + " -> " + targetNodeValues + ")";
    }
}

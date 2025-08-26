package com.example.mjg.algorithms.super_topo_sort;

import lombok.Getter;

import java.util.*;

@Getter
class InternalNode<T> {
    InternalNode(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;  // same reference
        if (obj == null || getClass() != obj.getClass()) return false; // must be Node<?>

        InternalNode<?> other = (InternalNode<?>) obj;

        // Ensure same runtime type for values
        if (this.value != null && other.value != null &&
                this.value.getClass() != other.value.getClass()) {
            return false;
        }

        return Objects.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    private final T value;

    private final Set<InternalNode<T>> predecessors = new HashSet<>();

    private final Set<InternalNode<T>> successors = new HashSet<>();
}

package com.example.mjg.algorithms.super_topo_sort;

import java.util.*;
import java.util.stream.Collectors;

public class SuperTopoSort<T> {
    private final Set<InternalNode<T>> allNodes;

    private final List<InternalSuperEdge<T>> superEdges;

    public SuperTopoSort(Collection<SuperEdge<T>> superEdges) {
        Map<T, InternalNode<T>> valueToNodeMap = new HashMap<>(superEdges.size());

        for (SuperEdge<T> e : superEdges) {
            e.getSourceNodeValues().forEach(sourceValue -> {
                InternalNode<T> sourceNode = valueToNodeMap.computeIfAbsent(sourceValue, InternalNode::new);
                sourceNode.getSuccessors().addAll(
                    e.getTargetNodeValues().stream().map(
                        targetValue -> valueToNodeMap.computeIfAbsent(targetValue, InternalNode::new)
                    ).collect(Collectors.toSet())
                );
            });

            e.getTargetNodeValues().forEach(targetValue -> {
                InternalNode<T> targetNode = valueToNodeMap.computeIfAbsent(targetValue, InternalNode::new);
                targetNode.getPredecessors().addAll(
                    e.getSourceNodeValues().stream().map(
                            sourceValue -> valueToNodeMap.computeIfAbsent(sourceValue, InternalNode::new)
                    ).collect(Collectors.toSet())
                );
            });
        }

        List<InternalSuperEdge<T>> establishedEdges = superEdges.stream()
                .map(superEdge -> new InternalSuperEdge<>(
                        superEdge,
                        superEdge.getSourceNodeValues().stream().map(valueToNodeMap::get).collect(Collectors.toSet()),
                        superEdge.getTargetNodeValues().stream().map(valueToNodeMap::get).collect(Collectors.toSet())
                ))
                .toList();

        this.allNodes = new HashSet<>(valueToNodeMap.values());
        this.superEdges = establishedEdges;
    }

    public List<SuperEdge<T>> toSorted() {
        Set<InternalNode<T>> resolvedNodes = allNodes
                .stream()
                .filter(node -> node.getPredecessors().isEmpty())
                .collect(Collectors.toSet());

        List<SuperEdge<T>> sortedList = new ArrayList<>(superEdges.size());
        int iteration = 0;
        boolean done = false;
        boolean sanityChecked = false;
        while (!done) {
            done = true;

            for (InternalSuperEdge<T> e : superEdges) {
                if (sortedList.contains(e.getOriginalSuperEdge())) {
                    continue;
                }

                try {
                    if (!sanityChecked) {
                        if (e.getTargetNodes().stream().anyMatch(e.getSourceNodes()::contains)) {
                            throw new IllegalArgumentException("A source node must not be a target node.");
                        }
                    }

                    if (resolvedNodes.containsAll(e.getSourceNodes())) {
                        if (e.getTargetNodes().stream().anyMatch(resolvedNodes::contains)) {
                            throw new IllegalArgumentException("Cycle detected, or at least, there are two super-edges with some same target node(s)!");
                        }
                        // This super edge resolves
                        resolvedNodes.addAll(e.getTargetNodes());
                        sortedList.add(e.getOriginalSuperEdge());
                    } else {
                        done = false;
                    }
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "While processing edge having:"
                                    + "\n    source nodes: " + e.getSourceNodes()
                                    + "\n    target nodes: " + e.getTargetNodes()
                                    + "\n\nMeanwhile, the currently resolved nodes are:"
                                    + "\n    " + resolvedNodes.stream().map(String::valueOf).collect(Collectors.joining(",\n    "))
                                    + "\n\nand the resolved/sorted edges are:"
                                    + "\n    " + sortedList.stream().map(String::valueOf).collect(Collectors.joining(",\n    "))
                                    + "\n\nAll the edges are:"
                                    + "\n    " + superEdges.stream()
                                    .map(InternalSuperEdge::getOriginalSuperEdge)
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(",\n    "))
                                    + "\n\nException occurred:"
                                    + "\n    " + exception.getMessage(),

                            exception
                    );
                }
            }

            sanityChecked = true;

            ++iteration;
            if (iteration > superEdges.size()) {
                break;
            }
        }

        if (!done) {
            throw new IllegalArgumentException("Resolution does not converge - there is a loop.");
        }

        if (sortedList.size() != superEdges.size()) {
            throw new RuntimeException("Algorithm gone wrong");
        }

        return sortedList;
    }

}

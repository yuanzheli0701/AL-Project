package com.isep.asnap.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed weighted graph backed by adjacency lists.
 * <p>
 * Storage: outgoing and incoming edge maps keyed by node id, giving O(1) average
 * lookup of a node's neighbours and O(deg) iteration.
 * <p>
 * Space complexity: O(V + E).
 */
public class DirectedWeightedGraph {

    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<Long, List<Edge>> outgoing = new HashMap<>();
    private final Map<Long, List<Edge>> incoming = new HashMap<>();
    private int edgeCount = 0;

    /** O(1) average. */
    public void addNode(Node node) {
        if (nodes.putIfAbsent(node.id(), node) == null) {
            outgoing.put(node.id(), new ArrayList<>());
            incoming.put(node.id(), new ArrayList<>());
        }
    }

    /** O(1) average. Both endpoints must already exist. */
    public void addEdge(Edge edge) {
        if (!nodes.containsKey(edge.source()) || !nodes.containsKey(edge.target())) {
            throw new IllegalArgumentException(
                    "edge endpoints must exist as nodes: " + edge.source() + " -> " + edge.target());
        }
        outgoing.get(edge.source()).add(edge);
        incoming.get(edge.target()).add(edge);
        edgeCount++;
    }

    public boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public Node getNode(long id) {
        return nodes.get(id);
    }

    public Collection<Node> allNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Set<Long> nodeIds() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public List<Edge> outgoingEdges(long id) {
        return outgoing.getOrDefault(id, List.of());
    }

    public List<Edge> incomingEdges(long id) {
        return incoming.getOrDefault(id, List.of());
    }

    public int outDegree(long id) {
        return outgoing.getOrDefault(id, List.of()).size();
    }

    public int inDegree(long id) {
        return incoming.getOrDefault(id, List.of()).size();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }
}

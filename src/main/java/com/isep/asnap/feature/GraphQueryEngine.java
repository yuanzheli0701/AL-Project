package com.isep.asnap.feature;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.Node;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Sub-graph extraction for visualisation and front-end rendering.
 */
@Service
public class GraphQueryEngine {

    public record SubGraph(List<Node> nodes, List<Edge> edges) {}

    private final DirectedWeightedGraph graph;

    public GraphQueryEngine(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /** BFS expansion up to {@code depth} hops around {@code center}. */
    public SubGraph neighbourhood(long center, int depth) {
        if (!graph.hasNode(center)) return new SubGraph(List.of(), List.of());

        Map<Long, Integer> visited = new HashMap<>();
        Queue<Long> queue = new ArrayDeque<>();
        visited.put(center, 0);
        queue.add(center);
        while (!queue.isEmpty()) {
            long u = queue.poll();
            int d = visited.get(u);
            if (d >= depth) continue;
            for (Edge e : graph.outgoingEdges(u)) {
                if (!visited.containsKey(e.target())) {
                    visited.put(e.target(), d + 1);
                    queue.add(e.target());
                }
            }
        }

        Set<Long> ids = visited.keySet();
        List<Node> nodes = new ArrayList<>();
        for (long id : ids) nodes.add(graph.getNode(id));

        List<Edge> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (long id : ids) {
            for (Edge e : graph.outgoingEdges(id)) {
                if (!ids.contains(e.target())) continue;
                String key = e.source() + ":" + e.target() + ":" + e.type();
                if (seen.add(key)) edges.add(e);
            }
        }
        return new SubGraph(nodes, edges);
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("nodes", graph.nodeCount());
        m.put("edges", graph.edgeCount());
        return m;
    }
}

package com.isep.asnap.feature;

import com.isep.asnap.algo.BFS;
import com.isep.asnap.algo.Dijkstra;
import com.isep.asnap.core.DirectedWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * "X degrees of separation" service. Wraps BFS for hop-count queries and
 * Dijkstra for weighted (interaction-strength) shortest path.
 */
@Service
public class ConnectionFinder {

    private final DirectedWeightedGraph graph;

    public ConnectionFinder(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    public List<Long> shortestFollowPath(long fromUser, long toUser) {
        return BFS.shortestPath(graph, fromUser, toUser);
    }

    public int degreesOfSeparation(long fromUser, long toUser) {
        List<Long> path = BFS.shortestPath(graph, fromUser, toUser);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    public Dijkstra.PathResult strongestPath(long fromUser, long toUser) {
        return Dijkstra.shortestPath(graph, fromUser, toUser);
    }

    public Map<Long, Integer> reachableWithin(long fromUser) {
        return BFS.degreesOfSeparation(graph, fromUser);
    }
}

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth-First Search on a directed graph (edges weights are ignored —
 * each hop counts as one step).
 *
 * <p>Time complexity:  O(V + E)
 * <br>Space complexity: O(V)
 *
 * <p>Use cases on X:
 * <ul>
 *   <li>Shortest follow-path between two users (degrees of separation)</li>
 *   <li>k-th neighbourhood expansion for friend-of-friend recommendations</li>
 * </ul>
 */
public final class BFS {

    private BFS() {}

    /**
     * Returns the shortest unweighted path from {@code source} to {@code target}.
     * Empty list when no path exists. Single-element list when source == target.
     */
    public static List<Long> shortestPath(DirectedWeightedGraph graph, long source, long target) {
        if (!graph.hasNode(source) || !graph.hasNode(target)) return List.of();
        if (source == target) return List.of(source);

        Map<Long, Long> parent = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(source);
        visited.add(source);

        while (!queue.isEmpty()) {
            long current = queue.poll();
            for (Edge e : graph.outgoingEdges(current)) {
                long next = e.target();
                if (visited.add(next)) {
                    parent.put(next, current);
                    if (next == target) {
                        return reconstruct(parent, source, target);
                    }
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    /**
     * Computes the degree of separation (in hops) from {@code source} to every
     * reachable node. Equivalent to BFS depth labelling.
     */
    public static Map<Long, Integer> degreesOfSeparation(DirectedWeightedGraph graph, long source) {
        Map<Long, Integer> depth = new HashMap<>();
        if (!graph.hasNode(source)) return depth;
        depth.put(source, 0);
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty()) {
            long current = queue.poll();
            int d = depth.get(current);
            for (Edge e : graph.outgoingEdges(current)) {
                if (!depth.containsKey(e.target())) {
                    depth.put(e.target(), d + 1);
                    queue.add(e.target());
                }
            }
        }
        return depth;
    }

    private static List<Long> reconstruct(Map<Long, Long> parent, long source, long target) {
        Deque<Long> stack = new ArrayDeque<>();
        long cur = target;
        while (cur != source) {
            stack.push(cur);
            cur = parent.get(cur);
        }
        stack.push(source);
        List<Long> path = new ArrayList<>(stack.size());
        while (!stack.isEmpty()) path.add(stack.pop());
        return Collections.unmodifiableList(path);
    }
}

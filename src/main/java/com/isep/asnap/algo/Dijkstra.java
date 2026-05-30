package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra's shortest-path algorithm using a binary heap as priority queue.
 *
 * <p>Time complexity:  O((V + E) log V)
 * <br>Space complexity: O(V)
 *
 * <p>Edge weights are interpreted as <b>cost</b> (non-negative).
 * For X, weights are derived as {@code 1 / interactionStrength} so that strongly
 * connected users have low cost and Dijkstra finds the strongest path.
 */
public final class Dijkstra {

    private Dijkstra() {}

    public record PathResult(List<Long> path, double totalCost) {
        public boolean isReachable() { return !path.isEmpty(); }
        public static PathResult unreachable() { return new PathResult(List.of(), Double.POSITIVE_INFINITY); }
    }

    public static PathResult shortestPath(DirectedWeightedGraph graph, long source, long target) {
        if (!graph.hasNode(source) || !graph.hasNode(target)) return PathResult.unreachable();
        if (source == target) return new PathResult(List.of(source), 0.0);

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        dist.put(source, 0.0);

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Double.compare(
                Double.longBitsToDouble(a[1]), Double.longBitsToDouble(b[1])));
        pq.add(new long[]{source, Double.doubleToLongBits(0.0)});

        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            long u = top[0];
            double du = Double.longBitsToDouble(top[1]);
            if (du > dist.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;
            if (u == target) break;

            for (Edge e : graph.outgoingEdges(u)) {
                double alt = du + e.weight();
                if (alt < dist.getOrDefault(e.target(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.target(), alt);
                    parent.put(e.target(), u);
                    pq.add(new long[]{e.target(), Double.doubleToLongBits(alt)});
                }
            }
        }

        if (!dist.containsKey(target)) return PathResult.unreachable();
        return new PathResult(reconstruct(parent, source, target), dist.get(target));
    }

    public static Map<Long, Double> distancesFrom(DirectedWeightedGraph graph, long source) {
        Map<Long, Double> dist = new HashMap<>();
        if (!graph.hasNode(source)) return dist;
        dist.put(source, 0.0);

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Double.compare(
                Double.longBitsToDouble(a[1]), Double.longBitsToDouble(b[1])));
        pq.add(new long[]{source, Double.doubleToLongBits(0.0)});

        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            long u = top[0];
            double du = Double.longBitsToDouble(top[1]);
            if (du > dist.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;

            for (Edge e : graph.outgoingEdges(u)) {
                double alt = du + e.weight();
                if (alt < dist.getOrDefault(e.target(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.target(), alt);
                    pq.add(new long[]{e.target(), Double.doubleToLongBits(alt)});
                }
            }
        }
        return dist;
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

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Iterative PageRank.
 *
 * <p>Time complexity:  O(k * (V + E))   where k = iterations
 * <br>Space complexity: O(V)
 *
 * <p>The implementation respects edge weights: outgoing rank is distributed
 * proportionally to {@code edge.weight() / sum(out-weights)}. Dangling nodes
 * (no outgoing edges) redistribute their mass uniformly.
 */
public final class PageRank {

    private PageRank() {}

    public static final double DEFAULT_DAMPING = 0.85;
    public static final int DEFAULT_ITERATIONS = 30;
    public static final double DEFAULT_TOLERANCE = 1e-6;

    public static Map<Long, Double> compute(DirectedWeightedGraph graph) {
        return compute(graph, DEFAULT_DAMPING, DEFAULT_ITERATIONS, DEFAULT_TOLERANCE);
    }

    public static Map<Long, Double> compute(DirectedWeightedGraph graph,
                                            double damping,
                                            int maxIterations,
                                            double tolerance) {
        Set<Long> ids = graph.nodeIds();
        int n = ids.size();
        if (n == 0) return Map.of();

        double initial = 1.0 / n;
        Map<Long, Double> rank = new HashMap<>(n * 2);
        Map<Long, Double> outWeightSum = new HashMap<>(n * 2);
        for (long id : ids) {
            rank.put(id, initial);
            double sum = 0.0;
            for (Edge e : graph.outgoingEdges(id)) sum += e.weight();
            outWeightSum.put(id, sum);
        }

        double teleport = (1.0 - damping) / n;

        for (int iter = 0; iter < maxIterations; iter++) {
            Map<Long, Double> next = new HashMap<>(n * 2);
            double danglingMass = 0.0;
            for (long id : ids) {
                if (outWeightSum.get(id) == 0.0) {
                    danglingMass += rank.get(id);
                }
                next.put(id, teleport);
            }
            double danglingShare = damping * danglingMass / n;

            for (long id : ids) {
                double r = rank.get(id);
                double sum = outWeightSum.get(id);
                if (sum == 0.0) continue;
                for (Edge e : graph.outgoingEdges(id)) {
                    double share = damping * r * (e.weight() / sum);
                    next.merge(e.target(), share, Double::sum);
                }
            }
            if (danglingShare > 0) {
                for (long id : ids) next.merge(id, danglingShare, Double::sum);
            }

            double diff = 0.0;
            for (long id : ids) diff += Math.abs(next.get(id) - rank.get(id));
            rank = next;
            if (diff < tolerance) break;
        }
        return rank;
    }
}

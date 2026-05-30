package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Personalized PageRank with biased teleport vector.
 *
 * <p>Unlike global PageRank where teleport is uniform, PPR concentrates the
 * teleport mass on a specific user's followees, producing rankings tailored
 * to that user's interest graph. This powers the "For You" feed.
 *
 * <p>Time complexity:  O(k * (V + E))  where k = iterations
 * <br>Space complexity: O(V)
 */
public final class PersonalizedPageRank {

    private PersonalizedPageRank() {}

    public static final double DEFAULT_DAMPING = 0.85;
    public static final int DEFAULT_ITERATIONS = 30;
    public static final double DEFAULT_TOLERANCE = 1e-6;

    /**
     * Computes personalized PageRank biased toward {@code seedUser}'s followees.
     */
    public static Map<Long, Double> compute(DirectedWeightedGraph graph, long seedUser) {
        return compute(graph, seedUser, DEFAULT_DAMPING, DEFAULT_ITERATIONS, DEFAULT_TOLERANCE);
    }

    public static Map<Long, Double> compute(DirectedWeightedGraph graph,
                                            long seedUser,
                                            double damping,
                                            int maxIterations,
                                            double tolerance) {
        Set<Long> ids = graph.nodeIds();
        int n = ids.size();
        if (n == 0) return Map.of();

        // Build personalization vector: seedUser's followees get higher weight
        Set<Long> followees = new HashSet<>();
        double followWeight = 0.0;
        for (Edge e : graph.outgoingEdges(seedUser)) {
            if (e.type() == Edge.EdgeType.FOLLOW) {
                followees.add(e.target());
                followWeight += e.weight();
            }
        }

        double initial = 1.0 / n;
        Map<Long, Double> rank = new HashMap<>(n * 2);
        Map<Long, Double> outWeightSum = new HashMap<>(n * 2);
        for (long id : ids) {
            rank.put(id, initial);
            double sum = 0.0;
            for (Edge e : graph.outgoingEdges(id)) sum += e.weight();
            outWeightSum.put(id, sum);
        }

        // Build teleport vector biased to followees
        Map<Long, Double> teleportVec = new HashMap<>();
        if (followees.isEmpty() || followWeight == 0.0) {
            double uniform = 1.0 / n;
            for (long id : ids) teleportVec.put(id, uniform);
        } else {
            double uniformShare = 0.3; // 30% uniform, 70% personalized
            double uniform = uniformShare / n;
            double personalizedShare = (1.0 - uniformShare) / followWeight;
            for (long id : ids) {
                double val = uniform;
                if (followees.contains(id)) {
                    double w = 0.0;
                    for (Edge e : graph.outgoingEdges(seedUser)) {
                        if (e.type() == Edge.EdgeType.FOLLOW && e.target() == id) w += e.weight();
                    }
                    val += personalizedShare * w;
                }
                teleportVec.put(id, val);
            }
        }

        for (int iter = 0; iter < maxIterations; iter++) {
            Map<Long, Double> next = new HashMap<>(n * 2);
            double danglingMass = 0.0;
            for (long id : ids) {
                if (outWeightSum.get(id) == 0.0) {
                    danglingMass += rank.get(id);
                }
                next.put(id, (1.0 - damping) * teleportVec.get(id));
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

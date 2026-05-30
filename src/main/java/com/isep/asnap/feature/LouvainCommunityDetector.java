package com.isep.asnap.feature;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Louvain community detection algorithm.
 *
 * <p>Iteratively optimizes modularity by moving nodes between communities
 * until no further improvement is possible. Operates on the undirected
 * projection of the FOLLOW sub-graph.
 *
 * <p>Time complexity: O(V log V) per pass (typical)
 * <br>Space complexity: O(V + E)
 */
@Service
public class LouvainCommunityDetector {

    private final DirectedWeightedGraph graph;

    public LouvainCommunityDetector(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /**
     * Result of Louvain community detection.
     * Each community is a list of user IDs.
     */
    public record CommunityResult(Map<Long, List<Long>> communities, double modularity, int iterations) {}

    /**
     * Runs Louvain algorithm and returns communities.
     */
    public CommunityResult detect() {
        // Build undirected weighted adjacency for FOLLOW edges between users
        Map<Long, Map<Long, Double>> adj = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        double totalWeight = 0.0;

        for (long id : graph.nodeIds()) {
            if (graph.getNode(id) instanceof UserNode) {
                userIds.add(id);
                adj.put(id, new HashMap<>());
            }
        }

        for (long u : userIds) {
            for (Edge e : graph.outgoingEdges(u)) {
                if (e.type() == Edge.EdgeType.FOLLOW && userIds.contains(e.target())) {
                    double w = 1.0 / Math.max(e.weight(), 0.01); // Convert cost to strength
                    adj.get(u).merge(e.target(), w, Double::sum);
                    adj.get(e.target()).merge(u, w, Double::sum);
                    totalWeight += w;
                }
            }
        }

        if (userIds.isEmpty() || totalWeight == 0.0) {
            Map<Long, List<Long>> singleton = new HashMap<>();
            for (long id : userIds) {
                singleton.put(id, List.of(id));
            }
            return new CommunityResult(singleton, 0.0, 0);
        }

        // Initialize: each node in its own community
        Map<Long, Long> community = new HashMap<>();
        for (long id : userIds) community.put(id, id);

        // Community weights: sum of internal edge weights
        Map<Long, Double> communityWeight = new HashMap<>();
        for (long id : userIds) communityWeight.put(id, 0.0);

        // Node degree (sum of edge weights)
        Map<Long, Double> degree = new HashMap<>();
        for (long u : userIds) {
            double d = adj.get(u).values().stream().mapToDouble(Double::doubleValue).sum();
            degree.put(u, d);
        }

        boolean changed = true;
        int iterations = 0;
        int maxIterations = 50;

        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;

            // Shuffle order for fairness
            List<Long> nodeOrder = new ArrayList<>(userIds);
            Collections.shuffle(nodeOrder, new Random(42L));

            for (long u : nodeOrder) {
                long currentComm = community.get(u);

                // Remove node from its current community
                double removeGain = modularityGain(adj, community, communityWeight, degree, u, currentComm, -1, totalWeight);
                communityWeight.merge(currentComm, -degree.get(u) * degree.get(u) / (2 * totalWeight), Double::sum);

                // Find best community to move to
                Map<Long, Double> neighborComms = new HashMap<>();
                for (var entry : adj.get(u).entrySet()) {
                    long v = entry.getKey();
                    long vComm = community.get(v);
                    if (vComm != currentComm) {
                        neighborComms.merge(vComm, entry.getValue(), Double::sum);
                    }
                }

                long bestComm = currentComm;
                double bestGain = 0.0;
                for (var entry : neighborComms.entrySet()) {
                    long candComm = entry.getKey();
                    double gain = modularityGain(adj, community, communityWeight, degree, u, candComm, entry.getValue(), totalWeight);
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestComm = candComm;
                    }
                }

                // Move if beneficial
                community.put(u, bestComm);
                if (bestComm != currentComm) {
                    changed = true;
                }
                communityWeight.merge(bestComm, degree.get(u) * degree.get(u) / (2 * totalWeight), Double::sum);
            }
        }

        // Build result communities
        Map<Long, List<Long>> result = new HashMap<>();
        for (long id : userIds) {
            long root = community.get(id);
            result.computeIfAbsent(root, k -> new ArrayList<>()).add(id);
        }

        double q = computeModularity(adj, community, degree, totalWeight);
        return new CommunityResult(result, q, iterations);
    }

    private double modularityGain(Map<Long, Map<Long, Double>> adj,
                                   Map<Long, Long> community,
                                   Map<Long, Double> communityWeight,
                                   Map<Long, Double> degree,
                                   long node, long targetComm,
                                   double weightToTarget,
                                   double totalWeight) {
        double k_i = degree.get(node);
        double sigma_tot = communityWeight.getOrDefault(targetComm, 0.0);
        return (weightToTarget / totalWeight) - (k_i * sigma_tot) / (2 * totalWeight * totalWeight);
    }

    private double computeModularity(Map<Long, Map<Long, Double>> adj,
                                      Map<Long, Long> community,
                                      Map<Long, Double> degree,
                                      double totalWeight) {
        double q = 0.0;
        for (long u : adj.keySet()) {
            for (var entry : adj.get(u).entrySet()) {
                long v = entry.getKey();
                if (community.get(u).equals(community.get(v))) {
                    double w = entry.getValue();
                    q += w - (degree.get(u) * degree.get(v)) / (2 * totalWeight);
                }
            }
        }
        return q / (2 * totalWeight);
    }
}

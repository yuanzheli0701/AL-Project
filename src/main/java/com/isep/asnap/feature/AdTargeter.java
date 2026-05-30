package com.isep.asnap.feature;

import com.isep.asnap.algo.Dijkstra;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Ad targeting engine that matches advertisers to high-value audiences.
 *
 * <p>Strategy:
 * <ul>
 *   <li>Find users within N degrees of the advertiser via BFS reachability</li>
 *   <li>Rank targets by PageRank influence score × community relevance</li>
 *   <li>Prefer users in the same community (higher engagement probability)</li>
 * </ul>
 */
@Service
public class AdTargeter {

    public record AdTarget(long userId, String username, double relevanceScore, int distance, String communityId) {}

    private final DirectedWeightedGraph graph;
    private final CommunityDetector communityDetector;
    private final FeedRanker feedRanker;

    public AdTargeter(DirectedWeightedGraph graph,
                      CommunityDetector communityDetector,
                      FeedRanker feedRanker) {
        this.graph = graph;
        this.communityDetector = communityDetector;
        this.feedRanker = feedRanker;
    }

    /**
     * Finds the best N users to target for an ad campaign from a given advertiser.
     */
    public List<AdTarget> findTargets(long advertiserId, int maxDepth, int limit) {
        if (!(graph.getNode(advertiserId) instanceof UserNode)) return List.of();

        // Get communities for relevance matching
        Map<Long, List<Long>> communities = communityDetector.connectedComponents();
        String advertiserCommunity = findCommunity(advertiserId, communities);

        // BFS to find reachable users within maxDepth
        Map<Long, Integer> distances = new HashMap<>();
        Queue<Long> queue = new ArrayDeque<>();
        distances.put(advertiserId, 0);
        queue.add(advertiserId);

        while (!queue.isEmpty()) {
            long u = queue.poll();
            int d = distances.get(u);
            if (d >= maxDepth) continue;
            for (Edge e : graph.outgoingEdges(u)) {
                if (!distances.containsKey(e.target())) {
                    distances.put(e.target(), d + 1);
                    queue.add(e.target());
                }
            }
        }

        var ranks = feedRanker.ranks();

        return distances.entrySet().stream()
                .filter(e -> e.getKey() != advertiserId)
                .filter(e -> graph.getNode(e.getKey()) instanceof UserNode)
                .map(e -> {
                    UserNode u = (UserNode) graph.getNode(e.getKey());
                    String userCommunity = findCommunity(u.id(), communities);
                    double communityBonus = advertiserCommunity.equals(userCommunity) ? 1.5 : 1.0;
                    double relevance = ranks.getOrDefault(u.id(), 0.0) * communityBonus / (1.0 + e.getValue());
                    return new AdTarget(u.id(), u.username(), relevance, e.getValue(), userCommunity);
                })
                .sorted(Comparator.comparingDouble(AdTarget::relevanceScore).reversed())
                .limit(limit)
                .toList();
    }

    private String findCommunity(long userId, Map<Long, List<Long>> communities) {
        for (var entry : communities.entrySet()) {
            if (entry.getValue().contains(userId)) return "c" + entry.getKey();
        }
        return "none";
    }
}

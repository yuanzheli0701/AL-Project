package com.isep.asnap.feature;

import com.isep.asnap.algo.PersonalizedPageRank;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.TweetNode;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Personalized "For You" feed ranking using Personalized PageRank.
 * Each user gets their own ranking biased toward their followees.
 */
@Service
public class PersonalizedFeedRanker {

    public record ScoredTweet(long id, long authorId, String content, long likes, long timestamp, double score) {}

    private final DirectedWeightedGraph graph;
    private final Map<Long, Map<Long, Double>> cache = new HashMap<>();

    public PersonalizedFeedRanker(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns personalized tweet rankings for a user, with recency boost.
     */
    public List<ScoredTweet> personalizedFeed(long userId, int limit) {
        Map<Long, Double> ppr = cache.computeIfAbsent(userId,
                id -> PersonalizedPageRank.compute(graph, id));

        long now = System.currentTimeMillis();
        long hourMs = 3_600_000L;

        return graph.allNodes().stream()
                .filter(TweetNode.class::isInstance)
                .map(TweetNode.class::cast)
                .map(t -> {
                    double baseScore = ppr.getOrDefault(t.id(), 0.0);
                    double hoursAgo = (now - t.timestamp()) / (double) hourMs;
                    double recencyBoost = 1.0 / (1.0 + Math.log1p(Math.max(0, hoursAgo)));
                    double finalScore = baseScore * (1.0 + 0.5 * recencyBoost);
                    return new ScoredTweet(t.id(), t.authorId(), t.content(),
                            t.likes(), t.timestamp(), finalScore);
                })
                .sorted(Comparator.comparingDouble(ScoredTweet::score).reversed())
                .limit(limit)
                .toList();
    }

    public void refresh(long userId) {
        cache.remove(userId);
    }

    public void refreshAll() {
        cache.clear();
    }
}

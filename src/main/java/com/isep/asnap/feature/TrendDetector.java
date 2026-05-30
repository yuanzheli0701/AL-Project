package com.isep.asnap.feature;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.TweetNode;
import com.isep.asnap.core.HashtagNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Real-time trend detection using sliding-window analysis.
 *
 * <p>Monitors tweet interaction velocity (LIKES + RETWEETS per minute)
 * and uses Z-score anomaly detection to flag trending content.
 */
@Service
public class TrendDetector {

    public record TrendingItem(long nodeId, String label, String type, double velocity, double zScore) {}

    private final DirectedWeightedGraph graph;

    public TrendDetector(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /**
     * Detects trending tweets by computing interaction velocity
     * and flagging statistical outliers via Z-score.
     */
    public List<TrendingItem> trendingTweets(int topN) {
        // Collect interaction counts per tweet (LIKE + RETWEET edges)
        Map<Long, Double> interactions = new HashMap<>();
        for (long id : graph.nodeIds()) {
            if (graph.getNode(id) instanceof TweetNode) {
                double count = 0;
                for (Edge e : graph.incomingEdges(id)) {
                    if (e.type() == Edge.EdgeType.LIKE || e.type() == Edge.EdgeType.RETWEET) {
                        count += e.weight();
                    }
                }
                interactions.put(id, count);
            }
        }

        if (interactions.isEmpty()) return List.of();

        // Compute mean and stddev for Z-score
        double mean = interactions.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = interactions.values().stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stddev = Math.sqrt(variance);
        if (stddev == 0) stddev = 1.0;

        // Compute recency-adjusted velocity
        long now = System.currentTimeMillis();
        double finalStddev = stddev;
        double finalMean = mean;

        return interactions.entrySet().stream()
                .map(e -> {
                    TweetNode t = (TweetNode) graph.getNode(e.getKey());
                    double hoursAgo = (now - t.timestamp()) / 3_600_000.0;
                    double velocity = e.getValue() / Math.max(1.0, hoursAgo + 1.0);
                    double zScore = (e.getValue() - finalMean) / finalStddev;
                    return new TrendingItem(t.id(), t.content(), "tweet", velocity, zScore);
                })
                .filter(item -> item.zScore() > 1.0) // Only above-average
                .sorted(Comparator.comparingDouble(TrendingItem::zScore).reversed())
                .limit(topN)
                .toList();
    }

    /**
     * Detects trending hashtags based on their connection density.
     */
    public List<TrendingItem> trendingHashtags(int topN) {
        Map<Long, Double> tagScores = new HashMap<>();
        for (long id : graph.nodeIds()) {
            if (graph.getNode(id) instanceof HashtagNode h) {
                double score = graph.inDegree(id) * 1.0;
                tagScores.put(id, score);
            }
        }
        if (tagScores.isEmpty()) return List.of();

        double mean = tagScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stddev = Math.sqrt(tagScores.values().stream()
                .mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(1.0));
        if (stddev == 0) stddev = 1.0;
        double finalStddev = stddev;

        return tagScores.entrySet().stream()
                .map(e -> {
                    HashtagNode h = (HashtagNode) graph.getNode(e.getKey());
                    double zScore = (e.getValue() - mean) / finalStddev;
                    return new TrendingItem(h.id(), h.tag(), "hashtag", e.getValue(), zScore);
                })
                .sorted(Comparator.comparingDouble(TrendingItem::zScore).reversed())
                .limit(topN)
                .toList();
    }
}

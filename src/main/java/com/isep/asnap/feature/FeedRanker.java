package com.isep.asnap.feature;

import com.isep.asnap.algo.PageRank;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Node;
import com.isep.asnap.core.TweetNode;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Computes PageRank-based scores. Used both for user influence ranking and
 * for tweet feed ordering. Ranks are cached after the first call; recompute
 * via {@link #refresh()} when the graph changes.
 */
@Service
public class FeedRanker {

    public record ScoredUser(long id, String username, double score) {}
    public record ScoredTweet(long id, long authorId, String content, long likes, double score) {}

    private final DirectedWeightedGraph graph;
    private volatile Map<Long, Double> cache;

    public FeedRanker(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    public synchronized Map<Long, Double> ranks() {
        if (cache == null) cache = PageRank.compute(graph);
        return cache;
    }

    public synchronized void refresh() {
        cache = PageRank.compute(graph);
    }

    public List<ScoredUser> topInfluencers(int limit) {
        Map<Long, Double> r = ranks();
        return graph.allNodes().stream()
                .filter(UserNode.class::isInstance)
                .map(UserNode.class::cast)
                .map(u -> new ScoredUser(u.id(), u.username(), r.getOrDefault(u.id(), 0.0)))
                .sorted(Comparator.comparingDouble(ScoredUser::score).reversed())
                .limit(limit)
                .toList();
    }

    public List<ScoredTweet> topTweets(int limit) {
        Map<Long, Double> r = ranks();
        return graph.allNodes().stream()
                .filter(TweetNode.class::isInstance)
                .map(TweetNode.class::cast)
                .map(t -> new ScoredTweet(t.id(), t.authorId(), t.content(), t.likes(),
                        r.getOrDefault(t.id(), 0.0)))
                .sorted(Comparator.comparingDouble(ScoredTweet::score).reversed())
                .limit(limit)
                .toList();
    }

    public double scoreOf(long nodeId) {
        return ranks().getOrDefault(nodeId, 0.0);
    }

    public Node node(long id) { return graph.getNode(id); }
}

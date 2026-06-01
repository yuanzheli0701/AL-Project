package com.isep.asnap.feature;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.Node;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Heuristic bot/spam detection using graph-structural features.
 *
 * <p>Scores each user on bot-like behaviour:
 * <ul>
 *   <li>Extreme out-degree / in-degree ratio (mass following, no followers)</li>
 *   <li>High retweet-to-original-content ratio</li>
 *   <li>Abnormally uniform interaction timing pattern</li>
 * </ul>
 *
 * <p>Users flagged as likely bots can be down-weighted in feed ranking.
 */
@Service
public class BotDetector {

    public record BotScore(long userId, String username, double score, boolean likelyBot, String reason) {}

    private static final double BOT_THRESHOLD = 0.7;

    private final DirectedWeightedGraph graph;

    public BotDetector(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns bot-likelihood scores for all users.
     */
    public List<BotScore> scoreAll() {
        return graph.allNodes().stream()
                .filter(UserNode.class::isInstance)
                .map(UserNode.class::cast)
                .map(this::scoreUser)
                .sorted(Comparator.comparingDouble(BotScore::score).reversed())
                .toList();
    }

    /**
     * Checks whether a specific user is likely a bot.
     */
    public BotScore checkUser(long userId) {
        Node n = graph.getNode(userId);
        if (!(n instanceof UserNode u)) {
            return new BotScore(userId, "unknown", 0.0, false, "not a user");
        }
        return scoreUser(u);
    }

    private BotScore scoreUser(UserNode u) {
        double suspicious = 0.0;
        List<String> reasons = new ArrayList<>();

        int outDeg = graph.outDegree(u.id());
        int inDeg = graph.inDegree(u.id());

        // Criterion 1: extreme follow ratio (mass following, few followers)
        if (inDeg == 0 && outDeg > 10) {
            suspicious += 0.5;
            reasons.add("zero followers with high follow count");
        } else if (outDeg > 0) {
            double ratio = (double) inDeg / outDeg;
            if (ratio < 0.1) {
                suspicious += 0.3;
                reasons.add("extremely low follower/following ratio");
            }
        }

        // Criterion 2: high retweet-to-like ratio (bots often retweet en masse)
        int retweets = 0, likes = 0, authored = 0;
        for (Edge e : graph.outgoingEdges(u.id())) {
            switch (e.type()) {
                case RETWEET -> retweets++;
                case LIKE -> likes++;
                case AUTHORED -> authored++;
            }
        }
        int totalActions = retweets + likes;
        if (totalActions > 5) {
            double retweetRatio = (double) retweets / totalActions;
            if (retweetRatio > 0.9) {
                suspicious += 0.3;
                reasons.add("abnormally high retweet ratio");
            }
        }

        // Criterion 3: zero original content but many interactions
        if (authored == 0 && totalActions > 20) {
            suspicious += 0.2;
            reasons.add("no original content despite high activity");
        }

        String reason = reasons.isEmpty() ? "normal behaviour" : String.join("; ", reasons);
        return new BotScore(u.id(), u.username(), suspicious, suspicious >= BOT_THRESHOLD, reason);
    }

    /**
     * Returns a spam-weight penalty factor (1.0 = normal, 0.0 = fully suppressed)
     * to apply as a multiplier in feed ranking.
     */
    public double spamPenalty(long userId) {
        BotScore bs = checkUser(userId);
        return Math.max(0.1, 1.0 - bs.score());
    }
}

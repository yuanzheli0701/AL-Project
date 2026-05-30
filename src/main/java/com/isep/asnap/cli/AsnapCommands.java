package com.isep.asnap.cli;

import com.isep.asnap.feature.*;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class AsnapCommands {

    private final ConnectionFinder connectionFinder;
    private final FeedRanker feedRanker;
    private final PersonalizedFeedRanker personalizedFeedRanker;
    private final CommunityDetector communityDetector;
    private final LouvainCommunityDetector louvainDetector;
    private final TrendDetector trendDetector;
    private final BotDetector botDetector;
    private final AdTargeter adTargeter;

    public AsnapCommands(ConnectionFinder connectionFinder,
                         FeedRanker feedRanker,
                         PersonalizedFeedRanker personalizedFeedRanker,
                         CommunityDetector communityDetector,
                         LouvainCommunityDetector louvainDetector,
                         TrendDetector trendDetector,
                         BotDetector botDetector,
                         AdTargeter adTargeter) {
        this.connectionFinder = connectionFinder;
        this.feedRanker = feedRanker;
        this.personalizedFeedRanker = personalizedFeedRanker;
        this.communityDetector = communityDetector;
        this.louvainDetector = louvainDetector;
        this.trendDetector = trendDetector;
        this.botDetector = botDetector;
        this.adTargeter = adTargeter;
    }

    @ShellMethod(key = "path-bfs", value = "Find shortest follow path via BFS")
    public String bfsPath(@ShellOption long from, @ShellOption long to) {
        var path = connectionFinder.shortestFollowPath(from, to);
        if (path.isEmpty()) return "No path found.";
        return "BFS path (" + (path.size() - 1) + " hops): " + path;
    }

    @ShellMethod(key = "path-dijkstra", value = "Find strongest weighted path via Dijkstra")
    public String dijkstraPath(@ShellOption long from, @ShellOption long to) {
        var r = connectionFinder.strongestPath(from, to);
        if (!r.isReachable()) return "No path found.";
        return "Dijkstra path (cost=" + String.format("%.4f", r.totalCost()) + "): " + r.path();
    }

    @ShellMethod(key = "top-influencers", value = "Show top influencers by PageRank")
    public String topInfluencers(@ShellOption(defaultValue = "10") int limit) {
        var list = feedRanker.topInfluencers(limit);
        StringBuilder sb = new StringBuilder("Top " + limit + " influencers:" + System.lineSeparator());
        for (int i = 0; i < list.size(); i++) {
            var u = list.get(i);
            sb.append(String.format("  %2d. @%s (score=%.4f)%n", i + 1, u.username(), u.score()));
        }
        return sb.toString();
    }

    @ShellMethod(key = "top-tweets", value = "Show top tweets by PageRank")
    public String topTweets(@ShellOption(defaultValue = "10") int limit) {
        var list = feedRanker.topTweets(limit);
        StringBuilder sb = new StringBuilder("Top " + limit + " tweets:" + System.lineSeparator());
        for (int i = 0; i < list.size(); i++) {
            var t = list.get(i);
            sb.append(String.format("  %2d. [%d likes] %s (score=%.4f)%n",
                    i + 1, t.likes(), t.content(), t.score()));
        }
        return sb.toString();
    }

    @ShellMethod(key = "for-you", value = "Get personalized feed for a user")
    public String forYou(@ShellOption long userId, @ShellOption(defaultValue = "10") int limit) {
        var feed = personalizedFeedRanker.personalizedFeed(userId, limit);
        StringBuilder sb = new StringBuilder("For You feed (user " + userId + "):" + System.lineSeparator());
        for (int i = 0; i < feed.size(); i++) {
            var t = feed.get(i);
            sb.append(String.format("  %2d. [%d likes] %s (score=%.4f)%n",
                    i + 1, t.likes(), t.content(), t.score()));
        }
        return sb.toString();
    }

    @ShellMethod(key = "trending", value = "Show trending content")
    public String trending(@ShellOption(defaultValue = "10") int limit) {
        var tweets = trendDetector.trendingTweets(limit);
        StringBuilder sb = new StringBuilder("Trending tweets:" + System.lineSeparator());
        for (var t : tweets) {
            sb.append(String.format("  z=%.2f vel=%.1f | %s%n", t.zScore(), t.velocity(), t.label()));
        }
        var hashtags = trendDetector.trendingHashtags(limit);
        if (!hashtags.isEmpty()) {
            sb.append(System.lineSeparator() + "Trending hashtags:" + System.lineSeparator());
            for (var h : hashtags) {
                sb.append(String.format("  z=%.2f | #%s%n", h.zScore(), h.label()));
            }
        }
        return sb.toString();
    }

    @ShellMethod(key = "bots", value = "Detect likely bot accounts")
    public String bots() {
        var bots = botDetector.scoreAll().stream()
                .filter(BotDetector.BotScore::likelyBot)
                .toList();
        if (bots.isEmpty()) return "No bots detected.";
        StringBuilder sb = new StringBuilder("Likely bots:" + System.lineSeparator());
        for (var b : bots) {
            sb.append(String.format("  @%s (score=%.2f) - %s%n", b.username(), b.score(), b.reason()));
        }
        return sb.toString();
    }

    @ShellMethod(key = "ad-target", value = "Find best ad targets near an advertiser")
    public String adTarget(@ShellOption long advertiserId,
                           @ShellOption(defaultValue = "3") int depth,
                           @ShellOption(defaultValue = "10") int limit) {
        var targets = adTargeter.findTargets(advertiserId, depth, limit);
        StringBuilder sb = new StringBuilder("Ad targets for user " + advertiserId + ":" + System.lineSeparator());
        for (int i = 0; i < targets.size(); i++) {
            var t = targets.get(i);
            sb.append(String.format("  %2d. @%s (relevance=%.4f, dist=%d, community=%s)%n",
                    i + 1, t.username(), t.relevanceScore(), t.distance(), t.communityId()));
        }
        return sb.toString();
    }

    @ShellMethod(key = "communities", value = "Show Louvain community detection results")
    public String communities() {
        var result = louvainDetector.detect();
        StringBuilder sb = new StringBuilder(String.format(
                "Louvain communities: %d communities, modularity=%.4f, iterations=%d%n",
                result.communities().size(), result.modularity(), result.iterations()));
        int i = 1;
        for (var entry : result.communities().entrySet()) {
            if (i > 10) { sb.append("  ... and more" + System.lineSeparator()); break; }
            sb.append(String.format("  Community %d: %d users%n", i++, entry.getValue().size()));
        }
        return sb.toString();
    }
}
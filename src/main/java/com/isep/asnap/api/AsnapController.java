package com.isep.asnap.api;

import com.isep.asnap.algo.Dijkstra;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.Node;
import com.isep.asnap.core.TweetNode;
import com.isep.asnap.core.UserNode;
import com.isep.asnap.feature.CommunityDetector;
import com.isep.asnap.feature.ConnectionFinder;
import com.isep.asnap.feature.FeedRanker;
import com.isep.asnap.feature.AdTargeter;
import com.isep.asnap.feature.BotDetector;
import com.isep.asnap.feature.GraphQueryEngine;
import com.isep.asnap.feature.LouvainCommunityDetector;
import com.isep.asnap.feature.PersonalizedFeedRanker;
import com.isep.asnap.feature.TrendDetector;
import com.isep.asnap.feature.VirusPropagator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AsnapController {

    private final DirectedWeightedGraph graph;
    private final ConnectionFinder connectionFinder;
    private final FeedRanker feedRanker;
    private final CommunityDetector communityDetector;
    private final GraphQueryEngine queryEngine;
    private final PersonalizedFeedRanker personalizedFeedRanker;
    private final TrendDetector trendDetector;
    private final BotDetector botDetector;
    private final AdTargeter adTargeter;
    private final LouvainCommunityDetector louvainDetector;
    private final VirusPropagator virusPropagator;

    public AsnapController(DirectedWeightedGraph graph,
                           ConnectionFinder connectionFinder,
                           FeedRanker feedRanker,
                           CommunityDetector communityDetector,
                           GraphQueryEngine queryEngine,
                           PersonalizedFeedRanker personalizedFeedRanker,
                           TrendDetector trendDetector,
                           BotDetector botDetector,
                           AdTargeter adTargeter,
                           LouvainCommunityDetector louvainDetector,
                           VirusPropagator virusPropagator) {
        this.graph = graph;
        this.connectionFinder = connectionFinder;
        this.feedRanker = feedRanker;
        this.communityDetector = communityDetector;
        this.queryEngine = queryEngine;
        this.personalizedFeedRanker = personalizedFeedRanker;
        this.trendDetector = trendDetector;
        this.botDetector = botDetector;
        this.adTargeter = adTargeter;
        this.louvainDetector = louvainDetector;
        this.virusPropagator = virusPropagator;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>(queryEngine.stats());
        m.put("communities", communityDetector.communityCount());
        return m;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return graph.allNodes().stream()
                .filter(UserNode.class::isInstance)
                .map(UserNode.class::cast)
                .sorted((a, b) -> Long.compare(b.followersCount(), a.followersCount()))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.id());
                    m.put("username", u.username());
                    m.put("followersCount", u.followersCount());
                    return m;
                })
                .toList();
    }

    @GetMapping("/path/bfs")
    public Map<String, Object> bfsPath(@RequestParam long from, @RequestParam long to) {
        List<Long> path = connectionFinder.shortestFollowPath(from, to);
        Map<String, Object> m = new HashMap<>();
        m.put("from", from);
        m.put("to", to);
        m.put("path", path);
        m.put("hops", path.isEmpty() ? -1 : path.size() - 1);
        m.put("usernames", path.stream().map(this::username).toList());
        return m;
    }

    @GetMapping("/path/dijkstra")
    public Map<String, Object> dijkstraPath(@RequestParam long from, @RequestParam long to) {
        Dijkstra.PathResult r = connectionFinder.strongestPath(from, to);
        Map<String, Object> m = new HashMap<>();
        m.put("from", from);
        m.put("to", to);
        m.put("path", r.path());
        m.put("totalCost", r.totalCost());
        m.put("usernames", r.path().stream().map(this::username).toList());
        return m;
    }

    @GetMapping("/influence/top")
    public List<FeedRanker.ScoredUser> topInfluencers(@RequestParam(defaultValue = "10") int limit) {
        return feedRanker.topInfluencers(limit);
    }

    @GetMapping("/feed/top")
    public List<FeedRanker.ScoredTweet> topTweets(@RequestParam(defaultValue = "10") int limit) {
        return feedRanker.topTweets(limit);
    }

    @GetMapping("/communities")
    public Map<String, Object> communities() {
        Map<Long, List<Long>> c = communityDetector.connectedComponents();
        Map<String, Object> m = new HashMap<>();
        m.put("count", c.size());
        m.put("communities", c.entrySet().stream()
                .map(e -> Map.of("rootId", e.getKey(), "members", e.getValue()))
                .toList());
        return m;
    }

    @GetMapping("/graph")
    public Map<String, Object> subgraph(@RequestParam long center,
                                        @RequestParam(defaultValue = "2") int depth) {
        GraphQueryEngine.SubGraph sub = queryEngine.neighbourhood(center, depth);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Node n : sub.nodes()) {
            Map<String, Object> nm = new HashMap<>();
            nm.put("id", n.id());
            if (n instanceof UserNode u) {
                nm.put("type", "user");
                nm.put("label", u.username());
                nm.put("followers", u.followersCount());
            } else if (n instanceof TweetNode t) {
                nm.put("type", "tweet");
                nm.put("label", "tweet#" + t.id());
                nm.put("content", t.content());
                nm.put("likes", t.likes());
            }
            nm.put("score", feedRanker.scoreOf(n.id()));
            nodes.add(nm);
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Edge e : sub.edges()) {
            Map<String, Object> em = new HashMap<>();
            em.put("source", e.source());
            em.put("target", e.target());
            em.put("type", e.type().name());
            em.put("weight", e.weight());
            edges.add(em);
        }
        return Map.of("nodes", nodes, "edges", edges);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> user(@PathVariable long id) {
        Node n = graph.getNode(id);
        if (!(n instanceof UserNode u)) return ResponseEntity.notFound().build();
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.id());
        m.put("username", u.username());
        m.put("followersCount", u.followersCount());
        m.put("outDegree", graph.outDegree(id));
        m.put("inDegree", graph.inDegree(id));
        m.put("score", feedRanker.scoreOf(id));
        return ResponseEntity.ok(m);
    }

    @GetMapping("/feed/personalized")
    public List<PersonalizedFeedRanker.ScoredTweet> personalizedFeed(
            @RequestParam long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return personalizedFeedRanker.personalizedFeed(userId, limit);
    }

    @GetMapping("/trending/tweets")
    public List<TrendDetector.TrendingItem> trendingTweets(
            @RequestParam(defaultValue = "10") int limit) {
        return trendDetector.trendingTweets(limit);
    }

    @GetMapping("/trending/hashtags")
    public List<TrendDetector.TrendingItem> trendingHashtags(
            @RequestParam(defaultValue = "10") int limit) {
        return trendDetector.trendingHashtags(limit);
    }

    @GetMapping("/bots")
    public List<BotDetector.BotScore> bots() {
        return botDetector.scoreAll();
    }

    @GetMapping("/bots/{userId}")
    public BotDetector.BotScore botCheck(@PathVariable long userId) {
        return botDetector.checkUser(userId);
    }

    @GetMapping("/ad/targets")
    public List<AdTargeter.AdTarget> adTargets(
            @RequestParam long advertiserId,
            @RequestParam(defaultValue = "3") int depth,
            @RequestParam(defaultValue = "10") int limit) {
        return adTargeter.findTargets(advertiserId, depth, limit);
    }

    @GetMapping("/communities/louvain")
    public Map<String, Object> louvainCommunities() {
        var result = louvainDetector.detect();
        Map<String, Object> m = new HashMap<>();
        m.put("count", result.communities().size());
        m.put("modularity", result.modularity());
        m.put("iterations", result.iterations());
        m.put("communities", result.communities().entrySet().stream()
                .map(e -> Map.of("rootId", e.getKey(), "members", e.getValue(), "size", e.getValue().size()))
                .sorted((a, b) -> Integer.compare((int) b.get("size"), (int) a.get("size")))
                .toList());
        return m;
    }

    private String username(long id) {
        Node n = graph.getNode(id);
        return n instanceof UserNode u ? u.username() : "node#" + id;
    }

    @GetMapping("/virus/simulate")
    public Map<String, Object> virusSimulate(
            @RequestParam long patientZero,
            @RequestParam(defaultValue = "0.3") double infectivity,
            @RequestParam(defaultValue = "3") int recoverySteps,
            @RequestParam(defaultValue = "20") int maxSteps) {
        VirusPropagator.SimulationResult result = virusPropagator.simulate(patientZero, infectivity, recoverySteps, maxSteps);
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers", result.totalUsers());
        m.put("maxInfected", result.maxInfected());
        m.put("finalRecovered", result.finalRecovered());
        m.put("steps", result.steps().stream().map(step -> {
            Map<String, Object> sm = new HashMap<>();
            sm.put("step", step.step());
            sm.put("newlyInfected", step.newlyInfected());
            sm.put("totalInfected", step.totalInfected());
            // Send states as map of userId -> state string
            Map<String, String> stateMap = new HashMap<>();
            step.states().forEach((id, state) -> stateMap.put(String.valueOf(id), state.name()));
            sm.put("states", stateMap);
            return sm;
        }).toList());
        return m;
    }}

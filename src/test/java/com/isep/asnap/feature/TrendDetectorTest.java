package com.isep.asnap.feature;

import com.isep.asnap.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrendDetectorTest {

    @Test
    void detectsTrendingTweets() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 5; i++) g.addNode(new UserNode(i, "u" + i, 0));
        g.addNode(new TweetNode(100, 1, "hot tweet", System.currentTimeMillis(), 1000));
        g.addNode(new TweetNode(101, 2, "cold tweet", System.currentTimeMillis() - 86400000L, 10));

        for (int i = 2; i <= 5; i++) {
            g.addEdge(new Edge(i, 100, Edge.EdgeType.LIKE, 1));
        }
        g.addEdge(new Edge(3, 101, Edge.EdgeType.LIKE, 1));

        TrendDetector td = new TrendDetector(g);
        var trending = td.trendingTweets(5);

        assertFalse(trending.isEmpty());
        assertEquals(100L, trending.get(0).nodeId());
        assertTrue(trending.get(0).zScore() > 0);
    }

    @Test
    void emptyGraphReturnsEmpty() {
        TrendDetector td = new TrendDetector(new DirectedWeightedGraph());
        assertTrue(td.trendingTweets(10).isEmpty());
    }
}

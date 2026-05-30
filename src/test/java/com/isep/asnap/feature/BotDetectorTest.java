package com.isep.asnap.feature;

import com.isep.asnap.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotDetectorTest {

    @Test
    void detectsMassFollowerWithNoFollowers() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "botuser", 0));
        for (int i = 2; i <= 15; i++) {
            g.addNode(new UserNode(i, "u" + i, 100));
            g.addEdge(Edge.follow(1, i, 1));
        }

        BotDetector bd = new BotDetector(g);
        var score = bd.checkUser(1);
        assertTrue(score.likelyBot());
        assertTrue(score.reason().contains("followers"));
    }

    @Test
    void normalUserNotFlagged() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "normal", 500));
        g.addNode(new UserNode(2, "friend", 300));
        g.addEdge(Edge.follow(1, 2, 1));
        g.addEdge(Edge.follow(2, 1, 1));

        BotDetector bd = new BotDetector(g);
        var score = bd.checkUser(1);
        assertFalse(score.likelyBot());
    }
}

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersonalizedPageRankTest {

    @Test
    void personalizedRanksForSeedUser() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 5; i++) g.addNode(new UserNode(i, "u" + i, 0));
        // Star: 1 follows 2,3,4,5  -> personalized ranks should boost 2-5
        g.addEdge(Edge.follow(1, 2, 1));
        g.addEdge(Edge.follow(1, 3, 1));
        g.addEdge(Edge.follow(1, 4, 1));
        g.addEdge(Edge.follow(1, 5, 1));
        // Mutual: 2 follows 1 back
        g.addEdge(Edge.follow(2, 1, 1));

        Map<Long, Double> r = PersonalizedPageRank.compute(g, 1);
        // 2-5 should all have non-zero scores
        for (long id : new long[]{2, 3, 4, 5}) {
            assertTrue(r.get(id) > 0, "followee " + id + " should have positive rank");
        }
    }

    @Test
    void emptyGraphReturnsEmpty() {
        assertTrue(PersonalizedPageRank.compute(new DirectedWeightedGraph(), 1).isEmpty());
    }
}

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageRankTest {

    @Test
    void ranksSumToOne() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 4; i++) g.addNode(new UserNode(i, "u" + i, 0));
        g.addEdge(Edge.follow(1, 2, 1));
        g.addEdge(Edge.follow(2, 3, 1));
        g.addEdge(Edge.follow(3, 4, 1));
        g.addEdge(Edge.follow(4, 1, 1));
        g.addEdge(Edge.follow(2, 1, 1));

        Map<Long, Double> r = PageRank.compute(g);
        double sum = r.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 1e-3);
    }

    @Test
    void hubBeatsLeaf() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 5; i++) g.addNode(new UserNode(i, "u" + i, 0));
        // 2..5 all follow 1 → 1 should rank highest
        g.addEdge(Edge.follow(2, 1, 1));
        g.addEdge(Edge.follow(3, 1, 1));
        g.addEdge(Edge.follow(4, 1, 1));
        g.addEdge(Edge.follow(5, 1, 1));

        Map<Long, Double> r = PageRank.compute(g);
        double hub = r.get(1L);
        for (long leaf : new long[]{2, 3, 4, 5}) {
            assertTrue(hub > r.get(leaf), "hub should outrank leaf " + leaf);
        }
    }

    @Test
    void emptyGraphReturnsEmpty() {
        assertTrue(PageRank.compute(new DirectedWeightedGraph()).isEmpty());
    }
}

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {

    @Test
    void picksLowestCostPathOverFewerHops() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 4; i++) g.addNode(new UserNode(i, "u" + i, 0));
        g.addEdge(Edge.follow(1, 4, 10));
        g.addEdge(Edge.follow(1, 2, 1));
        g.addEdge(Edge.follow(2, 3, 1));
        g.addEdge(Edge.follow(3, 4, 1));

        Dijkstra.PathResult r = Dijkstra.shortestPath(g, 1, 4);
        assertEquals(List.of(1L, 2L, 3L, 4L), r.path());
        assertEquals(3.0, r.totalCost(), 1e-9);
    }

    @Test
    void returnsUnreachableWhenDisconnected() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "a", 0));
        g.addNode(new UserNode(2, "b", 0));
        Dijkstra.PathResult r = Dijkstra.shortestPath(g, 1, 2);
        assertFalse(r.isReachable());
    }

    @Test
    void distancesFromContainsAllReachable() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 3; i++) g.addNode(new UserNode(i, "u" + i, 0));
        g.addEdge(Edge.follow(1, 2, 2));
        g.addEdge(Edge.follow(2, 3, 3));
        var d = Dijkstra.distancesFrom(g, 1);
        assertEquals(0.0, d.get(1L));
        assertEquals(2.0, d.get(2L));
        assertEquals(5.0, d.get(3L));
    }
}

package com.isep.asnap.algo;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BFSTest {

    private DirectedWeightedGraph chain() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        for (int i = 1; i <= 5; i++) g.addNode(new UserNode(i, "u" + i, 0));
        g.addEdge(Edge.follow(1, 2, 1));
        g.addEdge(Edge.follow(2, 3, 1));
        g.addEdge(Edge.follow(3, 4, 1));
        g.addEdge(Edge.follow(4, 5, 1));
        return g;
    }

    @Test
    void findsShortestUnweightedPath() {
        DirectedWeightedGraph g = chain();
        g.addEdge(Edge.follow(1, 5, 1));
        List<Long> path = BFS.shortestPath(g, 1, 5);
        assertEquals(List.of(1L, 5L), path);
    }

    @Test
    void returnsEmptyWhenUnreachable() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "a", 0));
        g.addNode(new UserNode(2, "b", 0));
        assertTrue(BFS.shortestPath(g, 1, 2).isEmpty());
    }

    @Test
    void selfPathIsTrivial() {
        DirectedWeightedGraph g = chain();
        assertEquals(List.of(1L), BFS.shortestPath(g, 1, 1));
    }

    @Test
    void degreesOfSeparationLabelsCorrectly() {
        DirectedWeightedGraph g = chain();
        Map<Long, Integer> d = BFS.degreesOfSeparation(g, 1);
        assertEquals(0, d.get(1L));
        assertEquals(4, d.get(5L));
    }
}

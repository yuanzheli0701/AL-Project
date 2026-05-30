package com.isep.asnap.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectedWeightedGraphTest {

    @Test
    void addingSameNodeTwiceIsIdempotent() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "alice", 100));
        g.addNode(new UserNode(1, "alice", 100));
        assertEquals(1, g.nodeCount());
    }

    @Test
    void edgeRequiresExistingEndpoints() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "alice", 100));
        assertThrows(IllegalArgumentException.class,
                () -> g.addEdge(Edge.follow(1, 99, 1.0)));
    }

    @Test
    void inOutDegreeIsTracked() {
        DirectedWeightedGraph g = new DirectedWeightedGraph();
        g.addNode(new UserNode(1, "a", 1));
        g.addNode(new UserNode(2, "b", 1));
        g.addNode(new UserNode(3, "c", 1));
        g.addEdge(Edge.follow(1, 2, 1.0));
        g.addEdge(Edge.follow(3, 2, 1.0));
        assertEquals(2, g.inDegree(2));
        assertEquals(0, g.inDegree(1));
        assertEquals(1, g.outDegree(1));
        assertEquals(2, g.edgeCount());
    }

    @Test
    void negativeWeightRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Edge.follow(1, 2, -0.5));
    }
}

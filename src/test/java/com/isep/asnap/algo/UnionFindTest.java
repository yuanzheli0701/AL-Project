package com.isep.asnap.algo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnionFindTest {

    @Test
    void singletonsAreOwnRoot() {
        UnionFind uf = new UnionFind();
        uf.makeSet(1);
        uf.makeSet(2);
        assertEquals(2, uf.componentCount());
        assertEquals(1, uf.find(1));
        assertEquals(2, uf.find(2));
        assertFalse(uf.connected(1, 2));
    }

    @Test
    void unionMergesComponents() {
        UnionFind uf = new UnionFind();
        for (long x = 1; x <= 4; x++) uf.makeSet(x);
        uf.union(1, 2);
        uf.union(3, 4);
        assertEquals(2, uf.componentCount());
        uf.union(2, 3);
        assertEquals(1, uf.componentCount());
        assertTrue(uf.connected(1, 4));
    }

    @Test
    void unionTwiceReportsNoChange() {
        UnionFind uf = new UnionFind();
        uf.makeSet(1);
        uf.makeSet(2);
        assertTrue(uf.union(1, 2));
        assertFalse(uf.union(1, 2));
    }

    @Test
    void findRejectsUnknown() {
        UnionFind uf = new UnionFind();
        assertThrows(IllegalStateException.class, () -> uf.find(42));
    }
}

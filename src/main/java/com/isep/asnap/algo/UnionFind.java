package com.isep.asnap.algo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Disjoint Set Union (Union-Find) with path compression and union-by-rank.
 *
 * <p>Time complexity:  O(α(V)) amortized per operation
 * <br>Space complexity: O(V)
 *
 * <p>Used here for fast connectivity / connected-components queries on the
 * underlying undirected projection of the follow graph.
 */
public final class UnionFind {

    private final Map<Long, Long> parent = new HashMap<>();
    private final Map<Long, Integer> rank = new HashMap<>();
    private int components = 0;

    public void makeSet(long x) {
        if (parent.containsKey(x)) return;
        parent.put(x, x);
        rank.put(x, 0);
        components++;
    }

    public long find(long x) {
        if (!parent.containsKey(x)) {
            throw new IllegalStateException("element not in any set: " + x);
        }
        long root = x;
        while (parent.get(root) != root) root = parent.get(root);
        long cur = x;
        while (parent.get(cur) != root) {
            long next = parent.get(cur);
            parent.put(cur, root);
            cur = next;
        }
        return root;
    }

    /** Returns true if a merge actually happened. */
    public boolean union(long x, long y) {
        long rx = find(x);
        long ry = find(y);
        if (rx == ry) return false;
        int rrx = rank.get(rx);
        int rry = rank.get(ry);
        if (rrx < rry) {
            parent.put(rx, ry);
        } else if (rrx > rry) {
            parent.put(ry, rx);
        } else {
            parent.put(ry, rx);
            rank.put(rx, rrx + 1);
        }
        components--;
        return true;
    }

    public boolean connected(long x, long y) {
        if (!parent.containsKey(x) || !parent.containsKey(y)) return false;
        return find(x) == find(y);
    }

    public int componentCount() {
        return components;
    }

    /** Returns the set of distinct roots — one per component. */
    public Set<Long> roots() {
        Set<Long> roots = new HashSet<>();
        for (long id : parent.keySet()) roots.add(find(id));
        return roots;
    }
}

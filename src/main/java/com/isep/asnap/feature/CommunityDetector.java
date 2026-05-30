package com.isep.asnap.feature;

import com.isep.asnap.algo.UnionFind;
import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Community detection using Union-Find on the undirected projection of the
 * FOLLOW sub-graph. For higher-quality modularity-based communities, see
 * {@link LouvainCommunityDetector}.
 */
@Service
public class CommunityDetector {

    private final DirectedWeightedGraph graph;

    public CommunityDetector(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    public Map<Long, List<Long>> connectedComponents() {
        UnionFind uf = new UnionFind();
        for (long id : graph.nodeIds()) {
            if (graph.getNode(id) instanceof UserNode) uf.makeSet(id);
        }
        for (long id : graph.nodeIds()) {
            if (!(graph.getNode(id) instanceof UserNode)) continue;
            for (Edge e : graph.outgoingEdges(id)) {
                if (e.type() == Edge.EdgeType.FOLLOW
                        && graph.getNode(e.target()) instanceof UserNode) {
                    uf.union(e.source(), e.target());
                }
            }
        }
        Map<Long, List<Long>> communities = new HashMap<>();
        for (long id : graph.nodeIds()) {
            if (!(graph.getNode(id) instanceof UserNode)) continue;
            long root = uf.find(id);
            communities.computeIfAbsent(root, k -> new ArrayList<>()).add(id);
        }
        return communities;
    }

    public int communityCount() {
        return connectedComponents().size();
    }
}

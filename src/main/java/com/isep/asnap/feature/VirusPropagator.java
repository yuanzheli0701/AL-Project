package com.isep.asnap.feature;

import com.isep.asnap.core.DirectedWeightedGraph;
import com.isep.asnap.core.Edge;
import com.isep.asnap.core.UserNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Virus / information propagation simulator using SIR model on the social graph.
 *
 * <p>Models how a piece of content (or virus) spreads through the network:
 * <ul>
 *   <li><b>S</b>usceptible: not yet exposed</li>
 *   <li><b>I</b>nfected: currently spreading</li>
 *   <li><b>R</b>ecovered: no longer spreading (immune)</li>
 * </ul>
 *
 * <p>Spread follows FOLLOW edges: an infected user can infect their followers.
 * Edge weight (closeness) affects transmission probability.
 */
@Service
public class VirusPropagator {

    public enum State { SUSCEPTIBLE, INFECTED, RECOVERED }

    public record StepInfo(int step, Map<Long, State> states, List<Long> newlyInfected, int totalInfected) {}

    public record SimulationResult(List<StepInfo> steps, int totalUsers, int maxInfected, int finalRecovered) {}

    private final DirectedWeightedGraph graph;
    private final Random rng = new Random();

    public VirusPropagator(DirectedWeightedGraph graph) {
        this.graph = graph;
    }

    /**
     * Runs a full SIR simulation starting from a single infected user.
     *
     * @param patientZero   the starting infected user ID
     * @param infectivity   base infection probability per contact (0.0 - 1.0)
     * @param recoverySteps number of steps before an infected user recovers
     * @param maxSteps      maximum simulation steps
     */
    public SimulationResult simulate(long patientZero, double infectivity, int recoverySteps, int maxSteps) {
        List<Long> userIds = graph.allNodes().stream()
                .filter(UserNode.class::isInstance)
                .map(n -> n.id())
                .toList();

        Map<Long, State> states = new HashMap<>();
        Map<Long, Integer> infectedSince = new HashMap<>();
        for (long id : userIds) {
            states.put(id, State.SUSCEPTIBLE);
        }

        // Infect patient zero
        states.put(patientZero, State.INFECTED);
        infectedSince.put(patientZero, 0);

        List<StepInfo> steps = new ArrayList<>();
        steps.add(new StepInfo(0, new HashMap<>(states), List.of(patientZero), 1));

        int maxInfected = 1;

        for (int step = 1; step <= maxSteps; step++) {
            List<Long> newlyInfected = new ArrayList<>();
            Map<Long, State> newStates = new HashMap<>(states);

            // Find all currently infected users
            List<Long> currentlyInfected = new ArrayList<>();
            for (long id : userIds) {
                if (states.get(id) == State.INFECTED) {
                    currentlyInfected.add(id);
                }
            }

            if (currentlyInfected.isEmpty()) break;

            // Each infected user tries to infect their followers
            for (long infectedId : currentlyInfected) {
                for (Edge e : graph.outgoingEdges(infectedId)) {
                    if (e.type() != Edge.EdgeType.FOLLOW) continue;
                    long targetId = e.target();
                    if (!(graph.getNode(targetId) instanceof UserNode)) continue;
                    if (states.get(targetId) != State.SUSCEPTIBLE) continue;

                    // Infection probability: base infectivity * edge closeness
                    double closeness = 1.0 / Math.max(e.weight(), 0.1);
                    double prob = infectivity * Math.min(closeness, 1.0);
                    if (rng.nextDouble() < prob) {
                        newlyInfected.add(targetId);
                        newStates.put(targetId, State.INFECTED);
                        infectedSince.put(targetId, 0);
                    }
                }
            }

            // Recover users who have been infected long enough
            List<Long> toRecover = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : infectedSince.entrySet()) {
                long id = entry.getKey();
                if (newStates.get(id) == State.INFECTED && entry.getValue() >= recoverySteps) {
                    toRecover.add(id);
                }
            }
            for (long id : toRecover) {
                newStates.put(id, State.RECOVERED);
            }

            // Increment infection counters
            for (long id : newlyInfected) {
                infectedSince.put(id, 0);
            }
            for (long id : currentlyInfected) {
                infectedSince.merge(id, 1, Integer::sum);
            }

            states = newStates;

            int totalInfected = 0;
            for (State s : states.values()) {
                if (s == State.INFECTED) totalInfected++;
            }
            maxInfected = Math.max(maxInfected, totalInfected);

            // Count recovered for final step
            int recovered = 0;
            for (State s : states.values()) {
                if (s == State.RECOVERED) recovered++;
            }

            steps.add(new StepInfo(step, new HashMap<>(states), newlyInfected, totalInfected));

            // Stop if no one is infected anymore
            if (totalInfected == 0) break;
        }

        int finalRecovered = 0;
        for (State s : states.values()) {
            if (s == State.RECOVERED) finalRecovered++;
        }

        return new SimulationResult(steps, userIds.size(), maxInfected, finalRecovered);
    }
}
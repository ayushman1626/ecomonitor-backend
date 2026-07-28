package com.example.demo.service;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CvrpSolverService {

    static {
        try {
            log.info("Loading Google OR-Tools native libraries...");
            Loader.loadNativeLibraries();
            log.info("Google OR-Tools native libraries loaded successfully.");
        } catch (Throwable e) {
            log.error("Failed to load Google OR-Tools native libraries! CVRP solving will fail.", e);
        }
    }

    public static class SolverResult {
        public final List<VehicleRoute> routes;
        public final List<Integer> droppedNodes;

        public SolverResult(List<VehicleRoute> routes, List<Integer> droppedNodes) {
            this.routes = routes;
            this.droppedNodes = droppedNodes;
        }
    }

    public static class VehicleRoute {
        public final int vehicleIndex;
        public final List<Integer> nodeIndices; // Indices of visited nodes (excluding depot)

        public VehicleRoute(int vehicleIndex, List<Integer> nodeIndices) {
            this.vehicleIndex = vehicleIndex;
            this.nodeIndices = nodeIndices;
        }
    }

    /**
     * Solves the Capacitated Vehicle Routing Problem (CVRP)
     *
     * @param distanceMatrix   Distance matrix [numLocations][numLocations]
     * @param demands          Demand at each node (demands[0] is depot and must be 0)
     * @param vehicleCapacities Capacity of each vehicle in the fleet
     * @param timeLimitSeconds Max search time for the solver
     * @return SolverResult containing routes per vehicle and dropped node indices
     */
    public SolverResult solve(
            double[][] distanceMatrix,
            long[] demands,
            long[] vehicleCapacities,
            int timeLimitSeconds
    ) {
        return solve(
                distanceMatrix,
                demands,
                vehicleCapacities,
                timeLimitSeconds,
                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC,
                LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH
        );
    }

    /**
     * Solves the Capacitated Vehicle Routing Problem (CVRP) with custom search strategies
     *
     * @param distanceMatrix         Distance matrix [numLocations][numLocations]
     * @param demands                Demand at each node
     * @param vehicleCapacities       Capacity of each vehicle
     * @param timeLimitSeconds       Max search time for local search
     * @param firstSolutionStrategy   First solution strategy
     * @param localSearchMetaheuristic Local search metaheuristic
     * @return SolverResult containing routes and dropped nodes
     */
    public SolverResult solve(
            double[][] distanceMatrix,
            long[] demands,
            long[] vehicleCapacities,
            int timeLimitSeconds,
            FirstSolutionStrategy.Value firstSolutionStrategy,
            LocalSearchMetaheuristic.Value localSearchMetaheuristic
    ) {
        int numLocations = distanceMatrix.length;
        int numVehicles = vehicleCapacities.length;
        int depotIndex = 0;

        log.info("Solving CVRP for {} locations and {} vehicles", numLocations, numVehicles);

        RoutingIndexManager manager = new RoutingIndexManager(numLocations, numVehicles, depotIndex);
        RoutingModel routing = new RoutingModel(manager);

        // Define cost callback
        final int transitCallbackIndex = routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            // Convert double distance to long for solver compatibility
            return (long) Math.round(distanceMatrix[fromNode][toNode]);
        });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        // Define demands callback
        final int demandCallbackIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
            int node = manager.indexToNode(fromIndex);
            return demands[node];
        });

        // Add capacity dimension
        routing.addDimensionWithVehicleCapacity(
                demandCallbackIndex,
                0, // Null capacity slack
                vehicleCapacities, // Vehicle capacities
                true, // Start cumulative to 0
                "Capacity"
        );

        // Add disjunctions (dropped nodes / penalty for skipping)
        // Bins with higher demand get a significantly larger penalty to force prioritizing them.
        long basePenalty = 1_000_000L;
        for (int i = 1; i < numLocations; ++i) {
            long penalty = basePenalty + (demands[i] * 1000L);
            routing.addDisjunction(new long[]{manager.nodeToIndex(i)}, penalty);
        }

        // Configure search parameters
        RoutingSearchParameters.Builder searchParametersBuilder =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(firstSolutionStrategy);

        if (localSearchMetaheuristic != null && localSearchMetaheuristic != LocalSearchMetaheuristic.Value.UNSET) {
            searchParametersBuilder.setLocalSearchMetaheuristic(localSearchMetaheuristic);
        }
        if (timeLimitSeconds > 0) {
            searchParametersBuilder.setTimeLimit(Duration.newBuilder().setSeconds(timeLimitSeconds).build());
        }

        RoutingSearchParameters searchParameters = searchParametersBuilder.build();

        // Solve the problem
        Assignment solution = routing.solveWithParameters(searchParameters);

        if (solution == null) {
            log.warn("CVRP solver returned null solution.");
            return new SolverResult(new ArrayList<>(), new ArrayList<>());
        }

        // Parse routes
        List<VehicleRoute> routes = new ArrayList<>();
        for (int i = 0; i < numVehicles; i++) {
            List<Integer> routeNodes = new ArrayList<>();
            long index = routing.start(i);
            while (!routing.isEnd(index)) {
                int node = manager.indexToNode(index);
                if (node != depotIndex) {
                    routeNodes.add(node);
                }
                index = solution.value(routing.nextVar(index));
            }
            routes.add(new VehicleRoute(i, routeNodes));
        }

        // Parse dropped nodes
        List<Integer> droppedNodes = new ArrayList<>();
        for (int node = 1; node < numLocations; ++node) {
            long nodeIndex = manager.nodeToIndex(node);
            if (solution.value(routing.nextVar(nodeIndex)) == nodeIndex 
                    || solution.value(routing.vehicleVar(nodeIndex)) < 0) {
                droppedNodes.add(node);
            }
        }

        log.info("CVRP solved. Dropped nodes count: {}", droppedNodes.size());
        return new SolverResult(routes, droppedNodes);
    }
}

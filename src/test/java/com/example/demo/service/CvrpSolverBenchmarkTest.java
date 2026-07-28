package com.example.demo.service;

import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
public class CvrpSolverBenchmarkTest {

    @Autowired
    private CvrpSolverService cvrpSolverService;

    private static class VrpData {
        int dimension;
        long capacity;
        double[][] nodeCoords; // x, y coordinates
        long[] demands;
    }

    private VrpData parseVrpFile(String resourceName) throws Exception {
        VrpData data = new VrpData();
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                boolean readingCoords = false;
                boolean readingDemands = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("EOF")) {
                        break;
                    }
                    if (line.startsWith("NAME") || line.startsWith("COMMENT") || line.startsWith("TYPE")) {
                        continue;
                    }
                    if (line.startsWith("DIMENSION")) {
                        String[] parts = line.split(":");
                        data.dimension = Integer.parseInt(parts[1].trim());
                        data.nodeCoords = new double[data.dimension][2];
                        data.demands = new long[data.dimension];
                        continue;
                    }
                    if (line.startsWith("CAPACITY")) {
                        String[] parts = line.split(":");
                        data.capacity = Long.parseLong(parts[1].trim());
                        continue;
                    }
                    if (line.startsWith("EDGE_WEIGHT_TYPE")) {
                        continue;
                    }
                    if (line.startsWith("NODE_COORD_SECTION")) {
                        readingCoords = true;
                        readingDemands = false;
                        continue;
                    }
                    if (line.startsWith("DEMAND_SECTION")) {
                        readingCoords = false;
                        readingDemands = true;
                        continue;
                    }
                    if (line.startsWith("DEPOT_SECTION")) {
                        readingCoords = false;
                        readingDemands = false;
                        continue;
                    }

                    if (readingCoords) {
                        String[] tokens = line.split("\\s+");
                        int index = Integer.parseInt(tokens[0]) - 1;
                        data.nodeCoords[index][0] = Double.parseDouble(tokens[1]);
                        data.nodeCoords[index][1] = Double.parseDouble(tokens[2]);
                    } else if (readingDemands) {
                        String[] tokens = line.split("\\s+");
                        int index = Integer.parseInt(tokens[0]) - 1;
                        data.demands[index] = Long.parseLong(tokens[1]);
                    }
                }
            }
        }
        return data;
    }

    private double[][] computeDistanceMatrix(double[][] coords) {
        int n = coords.length;
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double dx = coords[i][0] - coords[j][0];
                double dy = coords[i][1] - coords[j][1];
                // TSPLIB / CVRPLIB standard uses round(sqrt(dx^2 + dy^2)) for EUC_2D
                matrix[i][j] = Math.round(Math.sqrt(dx * dx + dy * dy));
            }
        }
        return matrix;
    }

    @Test
    public void runBenchmark() throws Exception {
        VrpData vrpData = parseVrpFile("/A-n54-k7.vrp");
        assertNotNull(vrpData);

        double[][] distanceMatrix = computeDistanceMatrix(vrpData.nodeCoords);
        long[] vehicleCapacities = new long[7]; // A-n32-k5 requires 5 vehicles
        for (int i = 0; i < vehicleCapacities.length; i++) {
            vehicleCapacities[i] = vrpData.capacity;
        }

        double bks = 1167.0;

        log.info("=== START BENCHMARK: A-n54-k7 (BKS: {}) ===", bks);

        // 1. Run solver once with PATH_CHEAPEST_ARC strategy and local search disabled
        log.info("Running Solver with PATH_CHEAPEST_ARC (First Solution Only)...");
        CvrpSolverService.SolverResult resultFirst = cvrpSolverService.solve(
                distanceMatrix,
                vrpData.demands,
                vehicleCapacities,
                0, // 0 time limit to stop after first solution
                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC,
                LocalSearchMetaheuristic.Value.UNSET
        );

        double firstDistance = calculateTotalDistance(resultFirst, distanceMatrix);
        double firstGap = ((firstDistance - bks) / bks) * 100.0;
        log.info("PATH_CHEAPEST_ARC - Total Distance: {}, Gap to BKS: {}%", firstDistance, String.format("%.2f", firstGap));

        // 2. Run solver again with GUIDED_LOCAL_SEARCH and 2-second time limit
        log.info("Running Solver with GUIDED_LOCAL_SEARCH (Local Search, 10s limit)...");
        CvrpSolverService.SolverResult resultGLS = cvrpSolverService.solve(
                distanceMatrix,
                vrpData.demands,
                vehicleCapacities,
                10, // 2-second time limit
                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC,
                LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH
        );

        double glsDistance = calculateTotalDistance(resultGLS, distanceMatrix);
        double glsGap = ((glsDistance - bks) / bks) * 100.0;
        log.info("GUIDED_LOCAL_SEARCH - Total Distance: {}, Gap to BKS: {}%", glsDistance, String.format("%.2f", glsGap));

        log.info("=== BENCHMARK COMPLETE ===");

        // Verify that Guided Local Search is better or equal to First Solution
        log.info("Improvement: {} -> {} (Difference: {})", firstDistance, glsDistance, firstDistance - glsDistance);
    }

    private double calculateTotalDistance(CvrpSolverService.SolverResult result, double[][] distanceMatrix) {
        double total = 0;
        for (CvrpSolverService.VehicleRoute route : result.routes) {
            if (route.nodeIndices.isEmpty()) {
                continue;
            }
            int prev = 0;
            for (int node : route.nodeIndices) {
                total += distanceMatrix[prev][node];
                prev = node;
            }
            total += distanceMatrix[prev][0];
        }
        return total;
    }
}

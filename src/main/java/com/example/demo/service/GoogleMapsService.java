package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleMapsService {

    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GoogleMapsService() {
        this.restTemplate = new RestTemplate();
    }

    public double getDistance(String origin, String destination) {
        String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/distancematrix/json")
                .queryParam("origins", origin)
                .queryParam("destinations", destination)
                .queryParam("key", apiKey)
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && "OK".equals(response.get("status"))) {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
            if (!rows.isEmpty()) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
                if (!elements.isEmpty()) {
                    Map<String, Object> element = elements.get(0);
                    if ("OK".equals(element.get("status"))) {
                        Map<String, Object> distance = (Map<String, Object>) element.get("distance");
                        return ((Number) distance.get("value")).doubleValue();
                    }
                }
            }
        }
        return Double.MAX_VALUE;
    }

    public double[][] getDistanceMatrix(List<String> locations) {
        int size = locations.size();
        double[][] matrix = new double[size][size];
        String locationsStr = locations.stream()
                .map(s -> s.replaceAll("\\s+", ""))
                .collect(Collectors.joining("|"));

        String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/distancematrix/json")
                .queryParam("origins", locationsStr)
                .queryParam("destinations", locationsStr)
                .queryParam("key", apiKey)
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response != null && "OK".equals(response.get("status"))) {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
            for (int i = 0; i < size && i < rows.size(); i++) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(i).get("elements");
                for (int j = 0; j < size && j < elements.size(); j++) {
                    Map<String, Object> element = elements.get(j);
                    if ("OK".equals(element.get("status"))) {
                        Map<String, Object> distance = (Map<String, Object>) element.get("distance");
                        matrix[i][j] = ((Number) distance.get("value")).doubleValue();
                    } else {
                        matrix[i][j] = Double.MAX_VALUE;
                    }
                }
            }
        } else {
            // Fill with MAX_VALUE if API fails
            for (int i = 0; i < size; i++)
                Arrays.fill(matrix[i], Double.MAX_VALUE);
        }
        return matrix;
    }

    public Map<String, Object> getDirections(List<String> waypoints) {
        if (waypoints.size() < 2)
            return new HashMap<>();

        String origin = waypoints.get(0).replaceAll("\\s+", "");
        String destination = waypoints.get(waypoints.size() - 1).replaceAll("\\s+", "");

        // Google Directions API expects waypoints separated by |
        String waypointsStr = waypoints.subList(1, waypoints.size() - 1).stream()
                .map(s -> s.replaceAll("\\s+", ""))
                .collect(Collectors.joining("|"));

        System.out.println("DEBUG Directions API Inputs:");
        System.out.println("Origin: [" + origin + "]");
        System.out.println("Destination: [" + destination + "]");
        System.out.println("WaypointsStr: [" + waypointsStr + "]");

        URI uri = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/directions/json")
                .queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("waypoints", waypointsStr)
                .queryParam("key", apiKey)
                .build()
                .toUri();

        System.out.println("Calling Directions API with URL: " + uri.toString().replace(apiKey, "API_KEY_HIDDEN")); // LOG
        Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
        System.out.println("Directions API Full Response: " + response); // LOG
        return response;
    }
}

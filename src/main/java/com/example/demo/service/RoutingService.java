package com.example.demo.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.util.shapes.GHPoint;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class RoutingService {

    @Value("${routing.osm.path:}")
    private String osmPath;

    @Value("${routing.graphhopper.cache:graphhopper-cache}")
    private String cachePath;

    private GraphHopper hopper;
    private boolean useGraphHopper = false;

    @PostConstruct
    public void init() {
        if (osmPath == null || osmPath.trim().isEmpty()) {
            log.warn("routing.osm.path is not configured. Falling back to Haversine routing.");
            return;
        }

        File osmFile = new File(osmPath);
        if (!osmFile.exists()) {
            log.warn("OSM file not found at: {}. Falling back to Haversine routing.", osmPath);
            return;
        }

        try {
            log.info("Initializing GraphHopper with OSM file: {}", osmPath);
            hopper = new GraphHopper();
            hopper.setOSMFile(osmPath);
            hopper.setGraphHopperLocation(cachePath);
            hopper.setEncodedValuesString("car_access,car_average_speed");
            
            // Configure vehicle profile for car
            Profile profile = new Profile("car")
                    .setWeighting("custom")
                    .setCustomModel(com.graphhopper.util.GHUtility.loadCustomModelFromJar("car.json"));
            hopper.setProfiles(profile);
            
            hopper.importOrLoad();
            useGraphHopper = true;
            log.info("GraphHopper initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize GraphHopper. Falling back to Haversine routing.", e);
            useGraphHopper = false;
        }
    }

    @PreDestroy
    public void close() {
        if (hopper != null) {
            hopper.close();
        }
    }

    public static class RouteInfo {
        public final double distanceMeters;
        public final double timeSeconds;
        public final String polyline;

        public RouteInfo(double distanceMeters, double timeSeconds, String polyline) {
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
            this.polyline = polyline;
        }
    }

    public RouteInfo calculateRoute(double fromLat, double fromLng, double toLat, double toLng) {
        if (useGraphHopper && hopper != null) {
            try {
                GHRequest req = new GHRequest(fromLat, fromLng, toLat, toLng)
                        .setProfile("car")
                        .setLocale(java.util.Locale.US);
                GHResponse rsp = hopper.route(req);
                if (rsp.hasErrors()) {
                    log.warn("GraphHopper routing errors: {}. Falling back to Haversine.", rsp.getErrors());
                } else {
                    com.graphhopper.ResponsePath path = rsp.getBest();
                    double distance = path.getDistance(); // meters
                    double time = path.getTime() / 1000.0; // ms to seconds
                    String polyline = encodePolyline(path.getPoints());
                    return new RouteInfo(distance, time, polyline);
                }
            } catch (Exception e) {
                log.error("Error during GraphHopper routing. Falling back to Haversine.", e);
            }
        }

        // Fallback: Haversine distance
        double distance = calculateHaversineDistance(fromLat, fromLng, toLat, toLng);
        double speedMps = 8.33; // ~30 km/h
        double time = distance / speedMps;
        String polyline = generateDirectPolyline(fromLat, fromLng, toLat, toLng);
        return new RouteInfo(distance, time, polyline);
    }

    public double calculateDistance(double fromLat, double fromLng, double toLat, double toLng) {
        return calculateRoute(fromLat, fromLng, toLat, toLng).distanceMeters;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String generateDirectPolyline(double lat1, double lon1, double lat2, double lon2) {
        com.graphhopper.util.PointList points = new com.graphhopper.util.PointList(2, false);
        points.add(lat1, lon1);
        points.add(lat2, lon2);
        return encodePolyline(points);
    }

    private String encodePolyline(com.graphhopper.util.PointList points) {
        StringBuilder encodedString = new StringBuilder();
        int lastLat = 0;
        int lastLng = 0;
        for (int i = 0; i < points.size(); i++) {
            double lat = points.getLat(i);
            double lng = points.getLon(i);
            int late5 = (int) Math.round(lat * 1e5);
            int lnge5 = (int) Math.round(lng * 1e5);

            int dLat = late5 - lastLat;
            int dLng = lnge5 - lastLng;

            encodeValue(dLat, encodedString);
            encodeValue(dLng, encodedString);

            lastLat = late5;
            lastLng = lnge5;
        }
        return encodedString.toString();
    }

    private void encodeValue(int value, StringBuilder encodedString) {
        value = value < 0 ? ~(value << 1) : value << 1;
        while (value >= 0x20) {
            encodedString.append(Character.toChars((0x20 | (value & 0x1f)) + 63));
            value >>= 5;
        }
        encodedString.append(Character.toChars(value + 63));
    }
}

package com.example.demo.service;

import com.example.demo.model.Device;
import com.example.demo.model.Dtos.route.RouteResponseDto;
import com.example.demo.model.Dtos.route.RouteStopDto;
import com.example.demo.model.Interface;
import com.example.demo.model.Route;
import com.example.demo.model.RouteStop;
import com.example.demo.model.enums.RouteStatus;
import com.example.demo.model.User;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.RouteRepository;
import com.example.demo.repo.RouteStopRepository;
import com.example.demo.repo.UserRepo;
import com.example.demo.service.GoogleMapsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import com.example.demo.model.Vehicle;
import com.example.demo.repo.VehicleRepository;
import com.example.demo.repo.CollectionLogRepository;
import com.example.demo.repo.UserInterfaceRepo;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private CvrpSolverService cvrpSolverService;

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RouteStopRepository routeStopRepository;
    @Autowired
    private DeviceRepo deviceRepository;
    @Autowired
    private InterfaceRepo interfaceRepository;
    @Autowired
    private GoogleMapsService googleMapsService;
    @Autowired
    private UserRepo userRepository;
    @Autowired
    private CollectionLogRepository collectionLogRepository;
    @Autowired
    private UserInterfaceRepo userInterfaceRepo;

    @Transactional
    public void deleteRoute(UUID routeId, String username) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        verifyAdminOrOwner(route, username);

        // Delete collection logs associated with the route
        var collectionLogs = collectionLogRepository.findByRouteId(routeId);
        collectionLogRepository.deleteAll(collectionLogs);

        // Delete route stops associated with the route
        var routeStops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        routeStopRepository.deleteAll(routeStops);

        // Delete the route itself
        routeRepository.delete(route);
    }

    private void verifyAdminOrOwner(Route route, String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("User not found");
        }
        Interface iface = route.getInterfaceEntity();
        if (iface.getCreatedBy() != null && iface.getCreatedBy().getUsername().equals(username)) {
            return; // Owner has access
        }
        boolean isAdmin = userInterfaceRepo.findByUserAndInterfaceId(user, iface)
                .map(ui -> ui.getRole() == com.example.demo.model.enums.Role.ADMIN)
                .orElse(false);
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("User does not have ADMIN or OWNER access on this interface");
        }
    }

    /* =========================================================
       PUBLIC API
       ========================================================= */

    public List<RouteResponseDto> generateMultipleRoutes(
            UUID interfaceId,
            String username,
            String startLocation,
            String endLocation
    ) {
        Interface iface = getAuthorizedInterface(interfaceId, username);

        String startLoc = (startLocation != null && !startLocation.isEmpty()) ? startLocation : iface.getStartLocation();
        String endLoc = (endLocation != null && !endLocation.isEmpty()) ? endLocation : iface.getEndLocation();

        if (startLoc == null || startLoc.trim().isEmpty() || endLoc == null || endLoc.trim().isEmpty()) {
            throw new IllegalArgumentException("Start and End locations must be set on the Interface or provided in the request.");
        }

        List<Vehicle> activeVehicles = vehicleRepository.findByInterfaceEntityIdAndIsActiveTrue(interfaceId);
        if (activeVehicles.isEmpty()) {
            throw new IllegalStateException("No active vehicles found for this interface.");
        }

        List<Device> devices = getDevicesRequiringCollection(interfaceId);

        // Build list of coordinates: index 0 is depot, 1..N are devices
        List<double[]> coords = new ArrayList<>();
        coords.add(parseCoords(startLoc));
        for (Device d : devices) {
            coords.add(parseCoords(d.getLocation()));
        }

        int numLocations = coords.size();
        double[][] distanceMatrix = new double[numLocations][numLocations];
        for (int i = 0; i < numLocations; i++) {
            for (int j = 0; j < numLocations; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                } else {
                    distanceMatrix[i][j] = routingService.calculateDistance(
                            coords.get(i)[0], coords.get(i)[1],
                            coords.get(j)[0], coords.get(j)[1]
                    );
                }
            }
        }

        long[] demands = new long[numLocations];
        demands[0] = 0; // Depot
        for (int i = 1; i < numLocations; i++) {
            Device d = devices.get(i - 1);
            double fillPercentage = Math.max(
                    d.getLastValue1() != null ? d.getLastValue1().doubleValue() : 0.0,
                    d.getLastValue2() != null ? d.getLastValue2().doubleValue() : 0.0
            );
            double capacity = d.getMaxCapacity() != null ? d.getMaxCapacity() : 100.0;
            demands[i] = (long) Math.round((fillPercentage / 100.0) * capacity);
        }

        int numVehicles = activeVehicles.size();
        long[] vehicleCapacities = new long[numVehicles];
        for (int i = 0; i < numVehicles; i++) {
            vehicleCapacities[i] = (long) Math.round(activeVehicles.get(i).getCapacity());
        }

        CvrpSolverService.SolverResult solverResult = cvrpSolverService.solve(
                distanceMatrix, demands, vehicleCapacities, 15
        );

        List<RouteResponseDto> generatedRoutes = new ArrayList<>();

        for (CvrpSolverService.VehicleRoute r : solverResult.routes) {
            if (r.nodeIndices.isEmpty()) {
                continue; // Skip idle vehicles
            }

            Vehicle vehicle = activeVehicles.get(r.vehicleIndex);
            List<Device> routeDevices = new ArrayList<>();
            for (int nodeIdx : r.nodeIndices) {
                routeDevices.add(devices.get(nodeIdx - 1));
            }

            DirectionsData directions = fetchLocalDirections(startLoc, endLoc, routeDevices);

            Route route = new Route();
            route.setInterfaceEntity(iface);
            route.setVehicle(vehicle);
            route.setStartLocation(startLoc);
            route.setEndLocation(endLoc);
            route.setTotalDistance(directions.totalDistance);
            route.setTotalDuration(directions.totalDuration);
            route.setStatus(RouteStatus.PLANNED);
            route.setPolyline(directions.polyline);

            if (vehicle.getDefaultDriver() != null) {
                route.setAssignedWorkerId(vehicle.getDefaultDriver().getId());
                route.setStatus(RouteStatus.ASSIGNED);
            }

            Route savedRoute = routeRepository.save(route);
            List<RouteStop> stops = saveRouteStops(savedRoute, routeDevices, directions);

            generatedRoutes.add(mapToDto(savedRoute, stops));
        }

        return generatedRoutes;
    }

    @Deprecated
    public RouteResponseDto generateRoute(
            UUID interfaceId,
            String vehicleId,
            String username,
            String startLocation,
            String endLocation
    ) {
        List<RouteResponseDto> routes = generateMultipleRoutes(interfaceId, username, startLocation, endLocation);
        if (routes.isEmpty()) {
            throw new IllegalStateException("No routes could be generated.");
        }
        return routes.get(0);
    }

    private double[] parseCoords(String loc) {
        String[] parts = loc.split(",");
        return new double[] {
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim())
        };
    }

    private DirectionsData fetchLocalDirections(
            String start,
            String end,
            List<Device> order
    ) {
        List<String> waypoints = new ArrayList<>();
        waypoints.add(start);
        order.forEach(d -> waypoints.add(d.getLocation()));
        waypoints.add(end);

        List<Map<String, Object>> legs = new ArrayList<>();
        double totalDistance = 0;
        double totalDuration = 0;
        
        com.graphhopper.util.PointList allPoints = new com.graphhopper.util.PointList(100, false);

        for (int i = 0; i < waypoints.size() - 1; i++) {
            double[] from = parseCoords(waypoints.get(i));
            double[] to = parseCoords(waypoints.get(i + 1));
            
            RoutingService.RouteInfo legInfo = routingService.calculateRoute(from[0], from[1], to[0], to[1]);
            
            totalDistance += legInfo.distanceMeters;
            totalDuration += legInfo.timeSeconds;
            
            decodePolylineIntoPointList(legInfo.polyline, allPoints);

            Map<String, Object> legMap = new HashMap<>();
            Map<String, Object> distanceMap = new HashMap<>();
            distanceMap.put("value", legInfo.distanceMeters);
            Map<String, Object> durationMap = new HashMap<>();
            durationMap.put("value", legInfo.timeSeconds);
            
            legMap.put("distance", distanceMap);
            legMap.put("duration", durationMap);
            legs.add(legMap);
        }

        String polyline = encodePointList(allPoints);
        return new DirectionsData(polyline, totalDistance, totalDuration, legs);
    }

    private void decodePolylineIntoPointList(String encoded, com.graphhopper.util.PointList points) {
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            points.add(lat / 1e5, lng / 1e5);
        }
    }

    private String encodePointList(com.graphhopper.util.PointList points) {
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

    public RouteResponseDto getRouteById(UUID routeId, String username) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        authorize(route.getInterfaceEntity(), username);

        List<RouteStop> stops =
                routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);

        return mapToDto(route, stops);
    }

    public List<RouteResponseDto> getRoutesByInterfaceId(UUID interfaceId, String username) {
        Interface iface = getAuthorizedInterface(interfaceId, username);

        return routeRepository.findByInterfaceEntityIdAndStatusNot(interfaceId, RouteStatus.COMPLETED).stream()
                .map(route -> {
                    List<RouteStop> stops =
                            routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
                    return mapToDto(route, stops);
                })
                .toList();
    }

    public List<RouteResponseDto> getCompletedRoutesByInterfaceId(UUID interfaceId, String username) {
        Interface iface = getAuthorizedInterface(interfaceId, username);

        return routeRepository.findByInterfaceEntityIdAndStatus(interfaceId, RouteStatus.COMPLETED).stream()
                .map(route -> {
                    List<RouteStop> stops =
                            routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
                    return mapToDto(route, stops);
                })
                .toList();
    }

    /* =========================================================
       CORE LOGIC //NOT USED
       ========================================================= */

    private List<Device> optimizeRoute(
            List<Device> devices,
            String startLocation,
            String endLocation
    ) {
        if (devices.isEmpty()) return List.of();

        List<String> allLocations = new ArrayList<>();
        allLocations.add(startLocation);
        devices.forEach(d -> allLocations.add(d.getLocation()));
        allLocations.add(endLocation);

        double[][] matrix =
                googleMapsService.getDistanceMatrix(allLocations);

        Set<Integer> unvisited = new HashSet<>();
        for (int i = 1; i <= devices.size(); i++) unvisited.add(i);

        List<Device> path = new ArrayList<>();
        int currentIdx = 0;

        while (!unvisited.isEmpty()) {
            int nearest = -1;
            double min = Double.MAX_VALUE;

            for (int idx : unvisited) {
                double dist = matrix[currentIdx][idx];
                if (dist < min) {
                    min = dist;
                    nearest = idx;
                }
            }

            currentIdx = nearest;
            unvisited.remove(nearest);
            path.add(devices.get(nearest - 1));
        }

        return path;
    }

    /* =========================================================
       GOOGLE DIRECTIONS
       ========================================================= */

    private DirectionsData fetchDirections(
            String start,
            String end,
            List<Device> order
    ) {

        List<String> waypoints = new ArrayList<>();
        waypoints.add(start);
        order.forEach(d -> waypoints.add(d.getLocation()));
        waypoints.add(end);

        Map<String, Object> response =
                googleMapsService.getDirections(waypoints);

        if (response == null || !"OK".equals(response.get("status"))) {
            throw new IllegalStateException("Directions API failed");
        }

        Map<String, Object> route =
                ((List<Map<String, Object>>) response.get("routes")).get(0);

        List<Map<String, Object>> legs =
                (List<Map<String, Object>>) route.get("legs");

        String polyline =
                (String) ((Map<?, ?>) route.get("overview_polyline")).get("points");

        double totalDistance = 0;
        double totalDuration = 0;

        for (Map<String, Object> leg : legs) {
            totalDistance += ((Number)
                    ((Map<?, ?>) leg.get("distance")).get("value")).doubleValue();
            totalDuration += ((Number)
                    ((Map<?, ?>) leg.get("duration")).get("value")).doubleValue();
        }

        return new DirectionsData(polyline, totalDistance, totalDuration, legs);
    }

    /* =========================================================
       PERSISTENCE
       ========================================================= */

    private Route saveRoute(
            Interface iface,
            String vehicleId,
            DirectionsData data,
            String startLocation,
            String endLocation
    ) {
        Route route = new Route();
        route.setInterfaceEntity(iface);
        route.setVehicleId(vehicleId);
        route.setStartLocation(startLocation);
        route.setEndLocation(endLocation);
        route.setTotalDistance(data.totalDistance);
        route.setTotalDuration(data.totalDuration);
        route.setStatus(RouteStatus.PLANNED);
        route.setPolyline(data.polyline);
        return routeRepository.save(route);
    }

    private List<RouteStop> saveRouteStops(
            Route route,
            List<Device> order,
            DirectionsData data
    ) {

        List<RouteStop> stops = new ArrayList<>();
        double cumulativeSeconds = 0;

        for (int i = 0; i < order.size(); i++) {

            Map<String, Object> leg = data.legs.get(i);
            double legSeconds = ((Number)
                    ((Map<?, ?>) leg.get("duration")).get("value")).doubleValue();

            cumulativeSeconds += legSeconds;

            RouteStop stop = new RouteStop();
            stop.setRoute(route);
            stop.setDevice(order.get(i));
            stop.setStopOrder(i + 1);
            stop.setEstimatedArrival(
                    LocalDateTime.now().plusSeconds((long) cumulativeSeconds)
            );

            stops.add(stop);
        }

        return routeStopRepository.saveAll(stops);
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    private Interface getAuthorizedInterface(UUID id, String username) {
        Interface iface = interfaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Interface not found"));
        authorize(iface, username);
        return iface;
    }

    private void authorize(Interface iface, String username) {
        if (iface.getCreatedBy() == null ||
                !iface.getCreatedBy().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User does not have access");
        }
    }

    private List<Device> getDevicesRequiringCollection(UUID interfaceId) {
        List<Device> devices =
                deviceRepository.findByInterfaceEntityId(interfaceId).stream()
                        .filter(d ->
                                (d.getLastValue1() != null &&
                                        d.getLastValue1().compareTo(BigDecimal.valueOf(70)) > 0)
                                        || (d.getLastValue2() != null &&
                                        d.getLastValue2().compareTo(BigDecimal.valueOf(70)) > 0)
                        )
                        .toList();

        if (devices.isEmpty()) {
            throw new IllegalStateException("No devices require collection");
        }
        return devices;
    }

    private void validateInputs(
            UUID interfaceId,
            String username,
            String start,
            String end
    ) {
        Objects.requireNonNull(interfaceId, "Interface ID required");
        Objects.requireNonNull(username, "Username required");
        Objects.requireNonNull(start, "Start location required");
        Objects.requireNonNull(end, "End location required");
    }

    /* =========================================================
       DTO MAPPING (UNCHANGED BEHAVIOR)
       ========================================================= */

    public RouteResponseDto mapToDto(Route route, List<RouteStop> stops) {
        RouteResponseDto dto = new RouteResponseDto();
        dto.setRouteId(route.getId());
        dto.setVehicleId(route.getVehicleId());
        dto.setTotalDistance(route.getTotalDistance() / 1000.0);
        dto.setTotalDuration(route.getTotalDuration() / 60.0);
        dto.setPolyline(route.getPolyline());
        dto.setStatus(route.getStatus() != null ? route.getStatus().name() : null);
        dto.setStartLocation(route.getStartLocation());
        dto.setStops(stops.stream().map(this::mapStopToDto).toList());
        dto.setEndLocation(route.getEndLocation());
        
        dto.setAssignedWorkerId(route.getAssignedWorkerId());
        if (route.getAssignedWorkerId() != null) {
            userRepository.findById(route.getAssignedWorkerId())
                    .ifPresent(user -> dto.setAssignedWorkerName(user.getFullName()));
        }
        dto.setStartedAt(route.getStartedAt() != null ? route.getStartedAt().toString() : null);
        dto.setCompletedAt(route.getCompletedAt() != null ? route.getCompletedAt().toString() : null);
        dto.setTotalCollected(route.getTotalCollected() != null ? route.getTotalCollected() : 0);
        dto.setTotalSkipped(route.getTotalSkipped() != null ? route.getTotalSkipped() : 0);
        return dto;
    }

    public RouteStopDto mapStopToDto(RouteStop stop) {
        RouteStopDto dto = new RouteStopDto();
        dto.setStopId(stop.getId());
        dto.setSequence(stop.getStopOrder());
        dto.setDeviceId(stop.getDevice().getId());
        dto.setName(stop.getDevice().getName());
        dto.setLocation(stop.getDevice().getLocation());
        dto.setFillLevel(
                Math.max(
                        stop.getDevice().getLastValue1() != null
                                ? stop.getDevice().getLastValue1().doubleValue() : 0.0,
                        stop.getDevice().getLastValue2() != null
                                ? stop.getDevice().getLastValue2().doubleValue() : 0.0
                )
        );
        dto.setType("PICKUP");
        dto.setStatus(
                stop.getStatus() != null ? stop.getStatus().name() : "PENDING");
        
        dto.setCollectedAt(stop.getCollectedAt() != null ? stop.getCollectedAt().toString() : null);
        dto.setRfidTag(stop.getRfidTag());
        dto.setRfidVerified(stop.getRfidVerified());
        dto.setSkipReason(stop.getSkipReason());
        dto.setWorkerLat(stop.getWorkerLat());
        dto.setWorkerLng(stop.getWorkerLng());
        return dto;
    }

    /* =========================================================
       INTERNAL DATA HOLDER
       ========================================================= */

    private static class DirectionsData {
        String polyline;
        double totalDistance;
        double totalDuration;
        List<Map<String, Object>> legs;

        DirectionsData(String p, double d, double t, List<Map<String, Object>> l) {
            polyline = p;
            totalDistance = d;
            totalDuration = t;
            legs = l;
        }
    }
}

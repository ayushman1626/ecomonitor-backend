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

@Service
public class RouteService {

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

    /* =========================================================
       PUBLIC API
       ========================================================= */

    public RouteResponseDto generateRoute(
            UUID interfaceId,
            String vehicleId,
            String username,
            String startLocation,
            String endLocation
    ) {

        validateInputs(interfaceId, username, startLocation, endLocation);

        Interface iface = getAuthorizedInterface(interfaceId, username);

        List<Device> devices = getDevicesRequiringCollection(interfaceId);

        List<Device> optimizedOrder =
                optimizeRoute(devices, startLocation, endLocation);

        DirectionsData directions =
                fetchDirections(startLocation, endLocation, optimizedOrder);

        Route route = saveRoute(iface, vehicleId, directions,startLocation, endLocation);

        List<RouteStop> stops =
                saveRouteStops(route, optimizedOrder, directions);

        return mapToDto(route, stops);
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

        return routeRepository.findByInterfaceEntityId(interfaceId).stream()
                .map(route -> {
                    List<RouteStop> stops =
                            routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
                    return mapToDto(route, stops);
                })
                .toList();
    }

    /* =========================================================
       CORE LOGIC
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

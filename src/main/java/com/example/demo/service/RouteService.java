package com.example.demo.service;

import com.example.demo.model.Device;
import com.example.demo.model.Dtos.route.RouteResponseDto;
import com.example.demo.model.Dtos.route.RouteStopDto;
import com.example.demo.model.Interface;
import com.example.demo.model.Route;
import com.example.demo.model.RouteStop;
import com.example.demo.model.enums.RouteStatus;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.repo.InterfaceRepo;
import com.example.demo.repo.RouteRepository;
import com.example.demo.repo.RouteStopRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    public RouteResponseDto generateRoute(UUID interfaceId, String vehicleId, String username, String startLocation,
            String endLocation) {
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new EntityNotFoundException("Interface not found with ID: " + interfaceId));

        if (interfaceEntity.getCreatedBy() == null || !interfaceEntity.getCreatedBy().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User does not have access to this interface");
        }

        // 1. Fetch devices with fill level > 80%
        List<Device> devices = deviceRepository.findByInterfaceEntityId(interfaceId).stream()
                .filter(d -> d.getLastValue1() != null && d.getLastValue1().compareTo(BigDecimal.valueOf(80)) > 0)
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new IllegalStateException("No devices found requiring collection for this interface.");
        }

        // 2. Optimize Route
        List<Device> optimizedOrder = optimizeRoute(devices, startLocation);

        // 3. Get Polyline and Total Metrics from Directions API
        List<String> waypoints = optimizedOrder.stream().map(Device::getLocation).collect(Collectors.toList());
        if (startLocation != null)
            waypoints.add(0, startLocation);
        if (endLocation != null)
            waypoints.add(endLocation);

        Map<String, Object> directions = googleMapsService.getDirections(waypoints);
        String polyline = "";
        double totalDistanceMeters = 0;
        double totalDurationSeconds = 0;

        if (directions != null && "OK".equals(directions.get("status"))) {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) directions.get("routes");
            if (!routes.isEmpty()) {
                Map<String, Object> routeObj = routes.get(0);
                Map<String, Object> overviewPolyline = (Map<String, Object>) routeObj.get("overview_polyline");
                if (overviewPolyline != null) {
                    polyline = (String) overviewPolyline.get("points");
                }

                List<Map<String, Object>> legs = (List<Map<String, Object>>) routeObj.get("legs");
                System.out.println("DEBUG: Found " + legs.size() + " legs in route."); // LOG
                for (Map<String, Object> leg : legs) {
                    Map<String, Object> dist = (Map<String, Object>) leg.get("distance");
                    Map<String, Object> dur = (Map<String, Object>) leg.get("duration");
                    double d = ((Number) dist.get("value")).doubleValue();
                    double t = ((Number) dur.get("value")).doubleValue();
                    System.out.println("DEBUG: Leg Distance=" + d + ", Duration=" + t); // LOG
                    totalDistanceMeters += d;
                    totalDurationSeconds += t;
                }
            } else {
                System.out.println("DEBUG: Routes list from Google API is EMPTY."); // LOG
            }
        } else {
            System.out.println("DEBUG: Directions API response is null or status is NOT OK."); // LOG
        }

        System.out.println(
                "DEBUG: Final Calculation -> Distance: " + totalDistanceMeters + ", Duration: " + totalDurationSeconds); // LOG

        // 4. Save Route
        Route route = new Route();
        route.setInterfaceEntity(interfaceEntity);
        route.setVehicleId(vehicleId);
        route.setTotalDistance(totalDistanceMeters);
        route.setTotalDuration(totalDurationSeconds);
        route.setStatus(RouteStatus.PLANNED);
        route.setPolyline(polyline);
        route = routeRepository.save(route);

        // 5. Save Stops
        List<RouteStop> stops = new ArrayList<>();
        int order = 1;
        for (Device device : optimizedOrder) {
            RouteStop stop = new RouteStop();
            stop.setRoute(route);
            stop.setDevice(device);
            stop.setStopOrder(order++);
            stop.setEstimatedArrival(LocalDateTime.now().plusSeconds((long) totalDurationSeconds));
            stops.add(stop);
        }
        routeStopRepository.saveAll(stops);

        return mapToDto(route, stops);
    }

    private RouteResponseDto mapToDto(Route route, List<RouteStop> stops) {
        RouteResponseDto dto = new RouteResponseDto();
        dto.setRouteId(route.getId());
        dto.setVehicleId(route.getVehicleId());
        dto.setTotalDistance(route.getTotalDistance() / 1000.0); // Convert to km
        dto.setTotalDuration(route.getTotalDuration() / 60.0); // Convert to minutes
        dto.setPolyline(route.getPolyline());

        List<RouteStopDto> stopDtos = stops.stream().map(stop -> {
            RouteStopDto stopDto = new RouteStopDto();
            stopDto.setSequence(stop.getStopOrder());
            stopDto.setDeviceId(stop.getDevice().getId());
            stopDto.setName(stop.getDevice().getName());
            stopDto.setLocation(stop.getDevice().getLocation());
            stopDto.setFillLevel(
                    stop.getDevice().getLastValue1() != null ? stop.getDevice().getLastValue1().doubleValue() : 0.0);
            stopDto.setType("PICKUP");
            return stopDto;
        }).collect(Collectors.toList());

        dto.setStops(stopDtos);
        return dto;
    }

    private List<Device> optimizeRoute(List<Device> devices, String startLocation) {
        if (devices.isEmpty())
            return new ArrayList<>();

        // 1. Prepare locations for Matrix API
        List<String> locations = devices.stream()
                .map(Device::getLocation)
                .collect(Collectors.toList());

        // 2. Fetch Distance Matrix
        double[][] distanceMatrix = googleMapsService.getDistanceMatrix(locations);

        List<Device> unvisited = new ArrayList<>(devices);
        List<Device> path = new ArrayList<>();

        // 3. Route = [StartNode]
        // Assuming the first device is the StartNode/Depot
        Device startNode = unvisited.remove(0);
        path.add(startNode);
        Device currentNode = startNode;

        // 4. WHILE Unvisited is NOT Empty:
        while (!unvisited.isEmpty()) {
            Device nextNode = null;
            double minDistance = Double.MAX_VALUE;

            int currentIndex = devices.indexOf(currentNode);

            // c. FOR EACH Bin IN Unvisited:
            for (Device candidate : unvisited) {
                int candidateIndex = devices.indexOf(candidate);

                // Fetch real-time duration (distance) from cached Matrix
                double distance = distanceMatrix[currentIndex][candidateIndex];

                if (distance < minDistance) {
                    minDistance = distance;
                    nextNode = candidate;
                }
            }

            // d. Add NextNode to Route
            if (nextNode != null) {
                path.add(nextNode);
                // e. Remove NextNode from Unvisited
                unvisited.remove(nextNode);
                // f. CurrentNode = NextNode
                currentNode = nextNode;
            } else {
                break;
            }
        }

        // 6. Add Depot to Route (Return to Start)
        path.add(startNode);

        return path;
    }

    public RouteResponseDto getRouteById(UUID routeId, String username) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found with ID: " + routeId));

        Interface interfaceEntity = route.getInterfaceEntity();
        if (interfaceEntity.getCreatedBy() == null || !interfaceEntity.getCreatedBy().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User does not have access to this route");
        }

        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        return mapToDto(route, stops);
    }

    public List<RouteResponseDto> getRoutesByInterfaceId(UUID interfaceId, String username) {
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                .orElseThrow(() -> new EntityNotFoundException("Interface not found with ID: " + interfaceId));

        if (interfaceEntity.getCreatedBy() == null || !interfaceEntity.getCreatedBy().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User does not have access to this interface");
        }

        List<Route> routes = routeRepository.findByInterfaceEntityId(interfaceId);
        return routes.stream().map(route -> {
            List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
            return mapToDto(route, stops);
        }).collect(Collectors.toList());
    }

}

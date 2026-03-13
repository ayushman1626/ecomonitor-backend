package com.example.demo.service;

import com.example.demo.model.Dtos.tracking.LocationUpdateRequest;
import com.example.demo.model.Route;
import com.example.demo.model.VehicleLog;
import com.example.demo.repo.RouteRepository;
import com.example.demo.repo.VehicleLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final VehicleLogRepository vehicleLogRepository;
    private final RouteRepository routeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void processLocationUpdate(LocationUpdateRequest request) {
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        VehicleLog log = new VehicleLog();
        log.setDeviceId(route.getVehicleId());
        log.setLatitude(request.getLatitude());
        log.setLongitude(request.getLongitude());
        log.setSpeed(request.getSpeed());
        log.setTimestamp(LocalDateTime.now());

        vehicleLogRepository.save(log);

        // Broadcast to WebSocket
        messagingTemplate.convertAndSend("/topic/tracking/" + request.getRouteId(), log);
    }

    public List<VehicleLog> getVehicleLogsForRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        // Find logs for the vehicle during the route's active time
        // If route is active, use current time as end
        LocalDateTime start = route.getStartedAt();
        LocalDateTime end = route.getCompletedAt();

        if (start == null) {
            return List.of(); // Not started yet
        }

        if (end == null) {
            end = LocalDateTime.now();
        }

        return vehicleLogRepository.findByDeviceIdAndTimestampBetween(route.getVehicleId(), start, end);
    }
}

package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.model.Dtos.collection.*;
import com.example.demo.model.Dtos.route.*;
import com.example.demo.model.enums.RouteStatus;
import com.example.demo.model.enums.RouteStopStatus;
import com.example.demo.repo.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.example.demo.exceptions.RfidVerificationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final CollectionLogRepository collectionLogRepository;
    private final UserRepo userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteService routeService;
    private final UserInterfaceRepo userInterfaceRepo;

    @Transactional
    public RouteResponseDto assignWorker(UUID routeId, UUID workerId, String currentUsername) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        verifyAdminOrOwner(route, currentUsername);

        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new IllegalStateException("Route must be in PLANNED status to assign a worker");
        }

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Worker not found"));

        // Todo: Verify worker has access to the interface if needed

        log.debug("Assigning worker: {}", worker.getEmail());

        route.setAssignedWorkerId(workerId);
        route.setStatus(RouteStatus.ASSIGNED);
        Route savedRoute = routeRepository.save(route);

        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        return routeService.mapToDto(savedRoute, stops);
    }

    @Transactional
    public RouteResponseDto startCollection(UUID routeId, UUID workerId, StartCollectionRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        if (!route.getAssignedWorkerId().equals(workerId)) {
            throw new IllegalArgumentException("Worker is not assigned to this route");
        }

        if (route.getStatus() != RouteStatus.ASSIGNED) {
            throw new IllegalStateException("Route must be in ASSIGNED status to start");
        }

        route.setStatus(RouteStatus.ACTIVE);
        route.setStartedAt(LocalDateTime.now());
        Route savedRoute = routeRepository.save(route);

        logAction(routeId, null, workerId, "ROUTE_STARTED", null, request.getLatitude(), request.getLongitude(), null);

        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        return routeService.mapToDto(savedRoute, stops);
    }

    @Transactional
    public RouteStopDto collectStop(UUID routeId, UUID stopId, UUID workerId, CollectStopRequest request) {
        verifyRouteActive(routeId, workerId);

        RouteStop stop = routeStopRepository.findById(stopId)
                .orElseThrow(() -> new EntityNotFoundException("Stop not found"));

        if (!stop.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException("Stop does not belong to this route");
        }

        // Verify RFID
        boolean verified = false;
        if (request.getRfidTag() != null && !request.getRfidTag().isEmpty()) {
            Device bin = stop.getDevice();

            if (bin.getHardwareId() != null && bin.getHardwareId().equalsIgnoreCase(request.getRfidTag())) {
                verified = true;
            }
        }

        if (!verified) {
            throw new RfidVerificationException("RFID verification failed. Cannot collect stop without a verified RFID tag.");
        }

        stop.setStatus(RouteStopStatus.COLLECTED);
        stop.setCollectedAt(LocalDateTime.now());
        stop.setRfidTag(request.getRfidTag());
        stop.setRfidVerified(verified);
        stop.setWorkerLat(request.getLatitude());
        stop.setWorkerLng(request.getLongitude());

        RouteStop savedStop = routeStopRepository.save(stop);

        logAction(routeId, stopId, workerId, "COLLECTED", request.getRfidTag(), request.getLatitude(),
                request.getLongitude(), request.getNotes());

        RouteStopDto dto = routeService.mapStopToDto(savedStop);

        // Broadcast update
        messagingTemplate.convertAndSend("/topic/route/" + routeId + "/updates", dto);

        return dto;
    }

    @Transactional
    public RouteStopDto skipStop(UUID routeId, UUID stopId, UUID workerId, SkipStopRequest request) {
        verifyRouteActive(routeId, workerId);

        RouteStop stop = routeStopRepository.findById(stopId)
                .orElseThrow(() -> new EntityNotFoundException("Stop not found"));

        if (!stop.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException("Stop does not belong to this route");
        }

        stop.setStatus(RouteStopStatus.SKIPPED);
        stop.setSkipReason(request.getReason());
        stop.setWorkerLat(request.getLatitude());
        stop.setWorkerLng(request.getLongitude());

        RouteStop savedStop = routeStopRepository.save(stop);

        logAction(routeId, stopId, workerId, "SKIPPED", null, request.getLatitude(), request.getLongitude(),
                request.getReason());

        RouteStopDto dto = routeService.mapStopToDto(savedStop);

        // Broadcast update
        messagingTemplate.convertAndSend("/topic/route/" + routeId + "/updates", dto);

        return dto;
    }

    @Transactional
    public RouteResponseDto completeRoute(UUID routeId, UUID workerId, CompleteRouteRequest request) {
        Route route = verifyRouteActive(routeId, workerId);

        // Calculate totals
        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        int collected = 0;
        int skipped = 0;
        for (RouteStop stop : stops) {
            if (stop.getStatus() == RouteStopStatus.COLLECTED)
                collected++;
            else if (stop.getStatus() == RouteStopStatus.SKIPPED)
                skipped++;
        }

        route.setStatus(RouteStatus.COMPLETED);
        route.setCompletedAt(LocalDateTime.now());
        route.setTotalCollected(collected);
        route.setTotalSkipped(skipped);

        Route savedRoute = routeRepository.save(route);

        logAction(routeId, null, workerId, "ROUTE_COMPLETED", null, request.getLatitude(), request.getLongitude(),
                null);

        return routeService.mapToDto(savedRoute, stops);
    }

    public List<CollectionLog> getAuditLogs(UUID routeId, String currentUsername) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));
        verifyAdminOrOwner(route, currentUsername);
        return collectionLogRepository.findByRouteId(routeId);
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

    // Helper to get assigned routes for a worker
    public List<RouteResponseDto> getAssignedRoutes(UUID workerId) {
        List<Route> assigned = routeRepository.findByAssignedWorkerIdAndStatus(workerId, RouteStatus.ASSIGNED);
        assigned.addAll(routeRepository.findByAssignedWorkerIdAndStatus(workerId, RouteStatus.ACTIVE));

        return assigned.stream().map(route -> {
            List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
            return routeService.mapToDto(route, stops);
        }).toList();
    }

    private Route verifyRouteActive(UUID routeId, UUID workerId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found"));

        if (!route.getAssignedWorkerId().equals(workerId)) {
            throw new IllegalArgumentException("Worker is not assigned to this route");
        }

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new IllegalStateException("Route is not ACTIVE");
        }
        return route;
    }

    private void logAction(UUID routeId, UUID stopId, UUID workerId, String action, String rfid, Double lat, Double lng,
            String notes) {
        CollectionLog log = new CollectionLog();
        log.setRouteId(routeId);
        log.setStopId(stopId);
        log.setWorkerId(workerId);
        log.setAction(action);
        log.setRfidTag(rfid);
        log.setLatitude(lat);
        log.setLongitude(lng);
        log.setTimestamp(LocalDateTime.now());
        log.setNotes(notes);
        collectionLogRepository.save(log);
    }
}

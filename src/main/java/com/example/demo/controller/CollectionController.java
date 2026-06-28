package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.model.Dtos.collection.*;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.route.RouteResponseDto;
import com.example.demo.model.Dtos.route.RouteStopDto;
import com.example.demo.repo.UserRepo;
import com.example.demo.service.CollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final UserRepo userRepository;

    @PutMapping("/{routeId}/assign")
    public ResponseEntity<ApiResponse<RouteResponseDto>> assignWorker(
            @PathVariable UUID routeId,
            @RequestBody AssignWorkerRequest request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {
        if (userPrinciple == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        RouteResponseDto route = collectionService.assignWorker(routeId, request.getWorkerId(), userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Worker assigned successfully", route));
    }

    @PutMapping("/{routeId}/start")
    public ResponseEntity<ApiResponse<RouteResponseDto>> startCollection(
            @PathVariable UUID routeId,
            @Valid @RequestBody StartCollectionRequest request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        User user = userRepository.findByUsername(userPrinciple.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        RouteResponseDto route = collectionService.startCollection(routeId, user.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Collection started successfully", route));
    }

    @PutMapping("/{routeId}/stops/{stopId}/collect")
    public ResponseEntity<ApiResponse<RouteStopDto>> collectStop(
            @PathVariable UUID routeId,
            @PathVariable UUID stopId,
            @Valid @RequestBody CollectStopRequest request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        User user = userRepository.findByUsername(userPrinciple.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        RouteStopDto stop = collectionService.collectStop(routeId, stopId, user.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stop collected successfully", stop));
    }

    @PutMapping("/{routeId}/stops/{stopId}/skip")
    public ResponseEntity<ApiResponse<RouteStopDto>> skipStop(
            @PathVariable UUID routeId,
            @PathVariable UUID stopId,
            @Valid @RequestBody SkipStopRequest request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        User user = userRepository.findByUsername(userPrinciple.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        RouteStopDto stop = collectionService.skipStop(routeId, stopId, user.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Stop skipped successfully", stop));
    }

    @PutMapping("/{routeId}/complete")
    public ResponseEntity<ApiResponse<RouteResponseDto>> completeRoute(
            @PathVariable UUID routeId,
            @Valid @RequestBody CompleteRouteRequest request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        User user = userRepository.findByUsername(userPrinciple.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        RouteResponseDto route = collectionService.completeRoute(routeId, user.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Route completed successfully", route));
    }

    @GetMapping("/{routeId}/audit-logs")
    public ResponseEntity<ApiResponse<List<CollectionLog>>> getAuditLogs(
            @PathVariable UUID routeId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {
        if (userPrinciple == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }
        List<CollectionLog> logs = collectionService.getAuditLogs(routeId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs fetched successfully", logs));
    }
}

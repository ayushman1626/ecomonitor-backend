package com.example.demo.controller;

import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.tracking.LocationUpdateRequest;
import com.example.demo.model.VehicleLog;
import com.example.demo.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.model.UserPrinciple;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking", description = "API endpoints for vehicle/device tracking and logs")
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/location")
    @Operation(summary = "Submit a location update for a vehicle")
    public ResponseEntity<ApiResponse<String>> updateLocation(@RequestBody LocationUpdateRequest request) {
        trackingService.processLocationUpdate(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Location recorded", null));
    }

    @GetMapping("/route/{routeId}/logs")
    @Operation(summary = "Get vehicle location logs for a specific route")
    public ResponseEntity<ApiResponse<List<VehicleLog>>> getVehicleLogs(
            @PathVariable UUID routeId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {
        if (userPrinciple == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }
        List<VehicleLog> logs = trackingService.getVehicleLogsForRoute(routeId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle logs fetched", logs));
    }
}

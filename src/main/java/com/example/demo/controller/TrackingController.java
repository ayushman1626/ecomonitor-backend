package com.example.demo.controller;

import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.tracking.LocationUpdateRequest;
import com.example.demo.model.VehicleLog;
import com.example.demo.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<String>> updateLocation(@RequestBody LocationUpdateRequest request) {
        trackingService.processLocationUpdate(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Location recorded", null));
    }

    @GetMapping("/route/{routeId}/logs")
    public ResponseEntity<ApiResponse<List<VehicleLog>>> getVehicleLogs(@PathVariable UUID routeId) {
        List<VehicleLog> logs = trackingService.getVehicleLogsForRoute(routeId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle logs fetched", logs));
    }
}

package com.example.demo.controller;

import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.vehicle.VehicleRequestDto;
import com.example.demo.model.Dtos.vehicle.VehicleResponseDto;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicle", description = "API endpoints for vehicle management and driver assignment")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @PostMapping
    @Operation(summary = "Create a new vehicle")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> createVehicle(
            @Valid @RequestBody VehicleRequestDto request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        VehicleResponseDto vehicle = vehicleService.createVehicle(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle created successfully", vehicle));
    }

    @GetMapping("/interface/{interfaceId}")
    @Operation(summary = "Get all vehicles associated with an interface")
    public ResponseEntity<ApiResponse<List<VehicleResponseDto>>> getVehiclesByInterface(
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        List<VehicleResponseDto> vehicles = vehicleService.getVehiclesByInterface(interfaceId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicles fetched successfully", vehicles));
    }

    @PutMapping("/{vehicleId}/driver/{driverId}")
    @Operation(summary = "Assign a driver to a vehicle")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> assignDriver(
            @PathVariable UUID vehicleId,
            @PathVariable UUID driverId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        VehicleResponseDto vehicle = vehicleService.assignDriver(vehicleId, driverId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Driver assigned successfully", vehicle));
    }

    @PutMapping("/{vehicleId}/status")
    @Operation(summary = "Toggle active/inactive status of a vehicle")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> toggleActiveStatus(
            @PathVariable UUID vehicleId,
            @RequestParam boolean isActive,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        VehicleResponseDto vehicle = vehicleService.toggleActiveStatus(vehicleId, isActive);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle status updated successfully", vehicle));
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Delete a vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle deleted successfully", null));
    }
}

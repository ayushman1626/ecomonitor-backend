package com.example.demo.controller;

import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.route.RouteRequestDto;
import com.example.demo.model.Dtos.route.RouteResponseDto;
import com.example.demo.model.User;
import com.example.demo.model.UserPrinciple;
import com.example.demo.repo.UserRepo;
import com.example.demo.service.CollectionService;
import com.example.demo.service.RouteService;
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
@RequestMapping("/api/routes")
@Tag(name = "Route", description = "API endpoints for route generation, optimization, and management")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private CollectionService collectionService;

    @PostMapping("/optimize")
    @Operation(summary = "Generate and optimize routes")
    public ResponseEntity<ApiResponse<List<RouteResponseDto>>> optimizeRoute(
            @Valid @RequestBody RouteRequestDto request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        List<RouteResponseDto> routes = routeService.generateMultipleRoutes(
                request.getInterfaceId(),
                userPrinciple.getUsername(),
                request.getStartLocation(),
                request.getEndLocation());
        return ResponseEntity.ok(new ApiResponse<>(true, "Routes optimized successfully", routes));
    }

    @GetMapping("/{routeId}")
    @Operation(summary = "Get route details by ID")
    public ResponseEntity<ApiResponse<RouteResponseDto>> getRouteById(
            @PathVariable UUID routeId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        RouteResponseDto route = routeService.getRouteById(routeId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Route fetched successfully", route));
    }

    @GetMapping("/interface/{interfaceId}")
    @Operation(summary = "Get routes by interface ID")
    public ResponseEntity<ApiResponse<List<RouteResponseDto>>> getRoutesByInterfaceId(
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        List<RouteResponseDto> routes = routeService.getRoutesByInterfaceId(interfaceId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Routes fetched successfully", routes));
    }

    @GetMapping("/interface/{interfaceId}/completed")
    @Operation(summary = "Get completed routes by interface ID")
    public ResponseEntity<ApiResponse<List<RouteResponseDto>>> getCompletedRoutesByInterfaceId(
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        List<RouteResponseDto> routes = routeService.getCompletedRoutesByInterfaceId(interfaceId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Completed routes fetched successfully", routes));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Get assigned routes for current user")
    public ResponseEntity<ApiResponse<List<RouteResponseDto>>> getMyAssignedRoutes(
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        User user = userRepository.findByUsername(userPrinciple.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<RouteResponseDto> routes = collectionService.getAssignedRoutes(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Assigned routes fetched successfully", routes));
    }

    @DeleteMapping("/{routeId}")
    @Operation(summary = "Delete route and associated records")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable UUID routeId,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        routeService.deleteRoute(routeId, userPrinciple.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Route and all associated records deleted successfully", null));
    }
}

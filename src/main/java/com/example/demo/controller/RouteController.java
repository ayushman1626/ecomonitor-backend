package com.example.demo.controller;

import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.route.RouteRequestDto;
import com.example.demo.model.Dtos.route.RouteResponseDto;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.RouteService;
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
public class RouteController {

    @Autowired
    private RouteService routeService;

    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<RouteResponseDto>> optimizeRoute(
            @Valid @RequestBody RouteRequestDto request,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        RouteResponseDto route = routeService.generateRoute(
                request.getInterfaceId(),
                request.getVehicleId(),
                userPrinciple.getUsername(),
                request.getStartLocation(),
                request.getEndLocation());
        return ResponseEntity.ok(new ApiResponse<>(true, "Route optimized successfully", route));
    }

    @GetMapping("/{routeId}")
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
}

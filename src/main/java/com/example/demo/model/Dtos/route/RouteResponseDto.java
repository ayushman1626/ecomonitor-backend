package com.example.demo.model.Dtos.route;

import com.example.demo.model.enums.RouteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RouteResponseDto {
    private UUID routeId;
    private String vehicleId;
    private Double totalDistance; // in km
    private Double totalDuration; // in minutes
    private String polyline;
    private List<RouteStopDto> stops;
}

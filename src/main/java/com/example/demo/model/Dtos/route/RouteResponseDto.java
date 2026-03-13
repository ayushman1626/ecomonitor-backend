package com.example.demo.model.Dtos.route;

import com.example.demo.model.enums.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDto {
    private UUID routeId;
    private String vehicleId;
    private Double totalDistance; // in km
    private Double totalDuration; // in minutes
    private String polyline;
    private String startLocation;
    private List<RouteStopDto> stops;
    private String endLocation;
    // New fields
    private String status;
    private UUID assignedWorkerId;
    private String assignedWorkerName; // Todo: fetch name
    private String startedAt;
    private String completedAt;
    private Integer totalCollected;
    private Integer totalSkipped;
}

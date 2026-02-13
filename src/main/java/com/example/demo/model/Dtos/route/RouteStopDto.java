package com.example.demo.model.Dtos.route;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RouteStopDto {
    private Integer sequence;
    private UUID deviceId;
    private String name;
    private String location;
    private Double fillLevel;
    private String type; // e.g., "PICKUP"
}

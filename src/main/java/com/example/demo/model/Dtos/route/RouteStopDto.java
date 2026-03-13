package com.example.demo.model.Dtos.route;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
public class RouteStopDto {
    private Integer sequence;
    private UUID deviceId;
    private String name;
    private String location;
    private Double fillLevel;
    private String type;

    // New fields
    private String status;
    private String collectedAt;
    private String rfidTag;
    private Boolean rfidVerified;
    private String skipReason;
    private Double workerLat;
    private Double workerLng; // e.g., "PICKUP"
}

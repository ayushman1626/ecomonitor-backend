package com.example.demo.model.Dtos.tracking;

import lombok.Data;

import java.util.UUID;

@Data
public class LocationUpdateRequest {
    private UUID routeId;
    private Double latitude;
    private Double longitude;
    private Double speed;
}

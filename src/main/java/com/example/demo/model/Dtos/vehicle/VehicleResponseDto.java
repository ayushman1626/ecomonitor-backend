package com.example.demo.model.Dtos.vehicle;

import lombok.Data;

import java.util.UUID;

@Data
public class VehicleResponseDto {
    private UUID id;
    private String name;
    private String licensePlate;
    private Double capacity;
    private UUID defaultDriverId;
    private String defaultDriverName;
    private UUID interfaceId;
    private Boolean isActive;
    private String createdAt;
}

package com.example.demo.model.Dtos.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VehicleRequestDto {
    @NotBlank(message = "Vehicle name is required")
    private String name;

    private String licensePlate;

    @NotNull(message = "Capacity is required")
    private Double capacity;

    private UUID defaultDriverId;

    @NotNull(message = "Interface ID is required")
    private UUID interfaceId;

    private Boolean isActive = true;
}

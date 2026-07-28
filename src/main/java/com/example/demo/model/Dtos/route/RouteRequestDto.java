package com.example.demo.model.Dtos.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RouteRequestDto {
    @NotNull(message = "Interface ID is required")
    private UUID interfaceId;

    private String vehicleId;

    private String startLocation; // Optional: Depot location (Lat,Lng)
    private String endLocation; // Optional: Return to depot
}

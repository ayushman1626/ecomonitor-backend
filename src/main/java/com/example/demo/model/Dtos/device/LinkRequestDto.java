package com.example.demo.model.Dtos.device;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkRequestDto {

    @NotBlank(message = "device id is required")
    private UUID deviceId;

    @NotBlank(message = "hardware id is required")
    private String hardwareId;
}

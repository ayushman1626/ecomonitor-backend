package com.example.demo.model.Dtos.device;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkRequestDto {

    @NotBlank(message = "hardware id is required")
    private String hardwareId;
}

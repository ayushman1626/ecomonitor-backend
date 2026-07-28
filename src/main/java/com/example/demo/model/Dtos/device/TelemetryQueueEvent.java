package com.example.demo.model.Dtos.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryQueueEvent {
    private UUID deviceId;
    private String hardwareId;
    private BigDecimal value1;
    private BigDecimal value2;
    private BigDecimal value3; // battery_status
    private String recordedAt;
}

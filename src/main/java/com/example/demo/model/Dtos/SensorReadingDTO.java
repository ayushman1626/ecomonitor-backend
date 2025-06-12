package com.example.demo.model.Dtos;

import lombok.Data;

@Data
public class SensorReadingDTO {
    private String value;
    private String deviceId;
    private String timestamp;

    public SensorReadingDTO(String deviceId, String value, String timestamp) {
        this.deviceId = deviceId;
        this.value = value;
        this.timestamp = timestamp;
    }
}

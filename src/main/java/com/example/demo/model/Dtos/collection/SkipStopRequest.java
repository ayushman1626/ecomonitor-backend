package com.example.demo.model.Dtos.collection;

import lombok.Data;

@Data
public class SkipStopRequest {
    private String reason;
    private Double latitude;
    private Double longitude;
}

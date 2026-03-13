package com.example.demo.model.Dtos.collection;

import lombok.Data;

@Data
public class CollectStopRequest {
    private String rfidTag;
    private Double latitude;
    private Double longitude;
    private String notes;
}

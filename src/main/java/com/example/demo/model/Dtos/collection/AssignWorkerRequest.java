package com.example.demo.model.Dtos.collection;

import lombok.Data;

import java.util.UUID;

@Data
public class AssignWorkerRequest {
    private UUID workerId;
}

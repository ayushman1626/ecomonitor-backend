package com.example.demo.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInterfaceId implements java.io.Serializable {
    private UUID user;
    private UUID interfaceId;
}

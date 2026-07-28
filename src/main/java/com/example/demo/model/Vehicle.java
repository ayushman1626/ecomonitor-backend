package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "license_plate", length = 50)
    private String licensePlate;

    @Column(nullable = false)
    private Double capacity; // Heterogeneous capacity

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_driver_id", referencedColumnName = "id", nullable = true)
    private User defaultDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interface_id", referencedColumnName = "id", nullable = false)
    private Interface interfaceEntity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();
}

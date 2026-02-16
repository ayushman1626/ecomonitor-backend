package com.example.demo.model;

import com.example.demo.model.enums.DeviceType;
import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
public class Device {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false, unique = true)
    private UUID id;

    @Column(name="hardware_id", unique = true)
    private String hardwareId;

    @ManyToOne
    @JoinColumn(name = "interface_id", referencedColumnName = "id")
    private Interface interfaceEntity;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeviceType type;

    @Column(length = 255)
    private String location;

    @Column(name = "placement_date")
    private LocalDate placementDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "last_value1", precision = 5, scale = 2)
    private BigDecimal lastValue1;

    @Column(name = "last_value2", precision = 5, scale = 2)
    private BigDecimal lastValue2;

    @Column(name = "battery_status", precision = 5, scale = 2)
    private BigDecimal battery_status;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


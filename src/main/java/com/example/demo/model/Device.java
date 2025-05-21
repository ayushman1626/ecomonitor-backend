package com.example.demo.model;

import com.example.demo.model.enums.DeviceType;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "device")
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false, unique = true)
    private UUID id;

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

    @Column(name = "last_value", precision = 5, scale = 2)
    private BigDecimal lastValue;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Interface getInterfaceEntity() {
        return interfaceEntity;
    }

    public void setInterfaceEntity(Interface interfaceEntity) {
        this.interfaceEntity = interfaceEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getPlacementDate() {
        return placementDate;
    }

    public void setPlacementDate(LocalDate placementDate) {
        this.placementDate = placementDate;
    }

    public BigDecimal getLastValue() {
        return lastValue;
    }

    public void setLastValue(BigDecimal lastValue) {
        this.lastValue = lastValue;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", interfaceEntity=" + interfaceEntity +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", location='" + location + '\'' +
                ", placementDate=" + placementDate +
                ", lastValue=" + lastValue +
                ", lastUpdated=" + lastUpdated +
                ", createdAt=" + createdAt +
                '}';
    }
}


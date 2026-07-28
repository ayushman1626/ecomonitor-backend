package com.example.demo.model;

import com.example.demo.model.enums.RouteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "route")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "interface_id", referencedColumnName = "id", nullable = false)
    private Interface interfaceEntity;

    @Column(name = "start_location")
    private String startLocation;

    @Column(name = "end_location")
    private String endLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", referencedColumnName = "id", nullable = true)
    private Vehicle vehicle;

    public String getVehicleId() {
        return vehicle != null ? vehicle.getId().toString() : null;
    }

    public void setVehicleId(String vehicleId) {
        // Deprecated fallback: we will use setVehicle directly
    }

    @Column(name = "assigned_worker_id")
    private UUID assignedWorkerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RouteStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_collected")
    private Integer totalCollected = 0;

    @Column(name = "total_skipped")
    private Integer totalSkipped = 0;

    @Column(name = "total_distance")
    private Double totalDistance;

    @Column(name = "total_duration")
    private Double totalDuration;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String polyline;
}

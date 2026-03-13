package com.example.demo.model;

import com.example.demo.model.enums.RouteStopStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_stop")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false, unique = true)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "route_id", referencedColumnName = "id", nullable = false)
    private Route route;

    @ManyToOne
    @JoinColumn(name = "device_id", referencedColumnName = "id", nullable = false)
    private Device device;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Enumerated(EnumType.STRING)
    private RouteStopStatus status;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "rfid_tag")
    private String rfidTag;

    @Column(name = "rfid_verified")
    private Boolean rfidVerified;

    @Column(name = "skip_reason")
    private String skipReason;

    @Column(name = "worker_lat")
    private Double workerLat;

    @Column(name = "worker_lng")
    private Double workerLng;

    @Column(name = "estimated_arrival")
    private LocalDateTime estimatedArrival;
}

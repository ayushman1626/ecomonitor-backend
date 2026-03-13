package com.example.demo.model;

import com.example.demo.model.enums.RouteStopStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @Column(name = "stop_id")
    private UUID stopId;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "action", nullable = false)
    private String action; // ROUTE_STARTED, ARRIVED, RFID_SCANNED, COLLECTED, SKIPPED, ROUTE_COMPLETED

    @Column(name = "rfid_tag")
    private String rfidTag;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "notes")
    private String notes;
}

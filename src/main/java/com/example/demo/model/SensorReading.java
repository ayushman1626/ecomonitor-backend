package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_reading")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SensorReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false, unique = true)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sensor_id", referencedColumnName = "id", nullable = false)
    private Device sensor;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal value1;

    @Column(nullable = true, precision = 5, scale = 2)
    private BigDecimal value2;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal value3;

    @Column(name = "recorded_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime recordedAt = LocalDateTime.now();
}

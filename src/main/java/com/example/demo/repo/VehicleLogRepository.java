package com.example.demo.repo;

import com.example.demo.model.VehicleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleLogRepository extends JpaRepository<VehicleLog, Long> {
    List<VehicleLog> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime start, LocalDateTime end);
}

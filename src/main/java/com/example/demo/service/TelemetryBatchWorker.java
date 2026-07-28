package com.example.demo.service;

import com.example.demo.model.Dtos.device.TelemetryQueueEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TelemetryBatchWorker {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryBatchWorker.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedRate = 1000) // Run every 2 seconds
    public void processBatch() {
        // Pop up to 1000 items in a single iteration
        List<String> rawEvents = redisService.popTelemetryBatch(1000);
        if (rawEvents == null || rawEvents.isEmpty()) {
            return;
        }

        List<TelemetryQueueEvent> events = new ArrayList<>();
        Map<UUID, TelemetryQueueEvent> latestDeviceStates = new HashMap<>();

        for (String raw : rawEvents) {
            try {
                TelemetryQueueEvent event = objectMapper.readValue(raw, TelemetryQueueEvent.class);
                events.add(event);
                // Track the latest event for each device to update device table status in batch
                latestDeviceStates.put(event.getDeviceId(), event);
            } catch (Exception e) {
                logger.error("Failed to deserialize telemetry event: {}", raw, e);
            }
        }

        if (events.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        // 1. Batch Insert into sensor_reading
        insertSensorReadings(events);

        // 2. Batch Update devices' latest values and activity state
        updateDeviceStates(latestDeviceStates.values());

        logger.info("Processed telemetry batch of {} records (device updates: {}) in {} ms",
                events.size(), latestDeviceStates.size(), (System.currentTimeMillis() - startTime));
    }

    private void insertSensorReadings(List<TelemetryQueueEvent> events) {
        String sql = "INSERT INTO sensor_reading (sensor_id, value1, value2, value3, recorded_at) VALUES (?, ?, ?, ?, ?)";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TelemetryQueueEvent event = events.get(i);
                    ps.setObject(1, event.getDeviceId());
                    ps.setBigDecimal(2, event.getValue1());
                    ps.setBigDecimal(3, event.getValue2());
                    ps.setBigDecimal(4, event.getValue3());
                    
                    LocalDateTime recordedAt;
                    try {
                        recordedAt = LocalDateTime.parse(event.getRecordedAt());
                    } catch (Exception e) {
                        recordedAt = LocalDateTime.now();
                    }
                    ps.setTimestamp(5, Timestamp.valueOf(recordedAt));
                }

                @Override
                public int getBatchSize() {
                    return events.size();
                }
            });
        } catch (Exception ex) {
            logger.warn("Batch insert of sensor readings failed: {}. Falling back to individual inserts...", ex.getMessage());
            for (TelemetryQueueEvent event : events) {
                try {
                    LocalDateTime recordedAt;
                    try {
                        recordedAt = LocalDateTime.parse(event.getRecordedAt());
                    } catch (Exception e) {
                        recordedAt = LocalDateTime.now();
                    }
                    jdbcTemplate.update(sql,
                            event.getDeviceId(),
                            event.getValue1(),
                            event.getValue2(),
                            event.getValue3(),
                            Timestamp.valueOf(recordedAt)
                    );
                } catch (Exception e) {
                    logger.error("Failed to insert individual sensor reading for deviceId: {}, hardwareId: {}. Error: {}", 
                            event.getDeviceId(), event.getHardwareId(), e.getMessage());
                }
            }
        }
    }

    private void updateDeviceStates(Collection<TelemetryQueueEvent> deviceStates) {
        List<TelemetryQueueEvent> statesList = new ArrayList<>(deviceStates);
        String sql = "UPDATE device SET is_active = true, last_value1 = ?, last_value2 = ?, battery_status = ?, last_updated = ? WHERE id = ?";
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TelemetryQueueEvent state = statesList.get(i);
                    ps.setBigDecimal(1, state.getValue1());
                    ps.setBigDecimal(2, state.getValue2());
                    ps.setBigDecimal(3, state.getValue3());
                    
                    LocalDateTime recordedAt;
                    try {
                        recordedAt = LocalDateTime.parse(state.getRecordedAt());
                    } catch (Exception e) {
                        recordedAt = LocalDateTime.now();
                    }
                    ps.setTimestamp(4, Timestamp.valueOf(recordedAt));
                    ps.setObject(5, state.getDeviceId());
                }

                @Override
                public int getBatchSize() {
                    return statesList.size();
                }
            });
        } catch (Exception ex) {
            logger.warn("Batch update of device states failed: {}. Falling back to individual updates...", ex.getMessage());
            for (TelemetryQueueEvent state : statesList) {
                try {
                    LocalDateTime recordedAt;
                    try {
                        recordedAt = LocalDateTime.parse(state.getRecordedAt());
                    } catch (Exception e) {
                        recordedAt = LocalDateTime.now();
                    }
                    jdbcTemplate.update(sql,
                            state.getValue1(),
                            state.getValue2(),
                            state.getValue3(),
                            Timestamp.valueOf(recordedAt),
                            state.getDeviceId()
                    );
                } catch (Exception e) {
                    logger.error("Failed to update individual device state for deviceId: {}, hardwareId: {}. Error: {}", 
                            state.getDeviceId(), state.getHardwareId(), e.getMessage());
                }
            }
        }
    }
}

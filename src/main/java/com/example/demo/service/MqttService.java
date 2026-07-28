package com.example.demo.service;

import com.example.demo.model.Device;
import com.example.demo.model.Dtos.SensorReadingDTO;
import com.example.demo.model.Dtos.device.DeviceCacheDTO;
import com.example.demo.model.Dtos.device.TelemetryQueueEvent;
import com.example.demo.model.enums.DeviceType;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.utils.DeviceStreamManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MqttService {

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private DeviceStreamManager streamManager;

    @Autowired
    private RedisService redisService;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    public void handleIncomingData(String payload){
        try {
            // Parse the incoming JSON payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payload);

            String hardwareId = node.get("id").asText();
            BigDecimal value1 = node.get("v1").decimalValue();
            BigDecimal value2 = node.get("v2") != null ? node.get("v2").decimalValue() : null;
            BigDecimal battery_status = node.get("battery") != null ? node.get("battery").decimalValue() : new BigDecimal("70");

            System.out.println("Received data for hardwareId: " + hardwareId + " with value1: " + value1 + " and value2 :" + value2 + " and battery: " + battery_status);

            // Fetch device details from cache, fallback to database supplier
            DeviceCacheDTO cachedDevice = redisService.getDevice(hardwareId, () -> {
                Device deviceEntity = deviceRepo.findByHardwareId(hardwareId).orElse(null);
                if (deviceEntity == null) {
                    return null;
                }
                return DeviceCacheDTO.builder()
                        .id(deviceEntity.getId())
                        .hardwareId(deviceEntity.getHardwareId())
                        .type(deviceEntity.getType())
                        .build();
            });

            if (cachedDevice == null) {
                logger.error("Device not found with hardware id: {}", hardwareId);
                throw new IllegalArgumentException("Device not found with hardware id: " + hardwareId);
            }

            // Validate values
            if (value1 == null || value1.compareTo(BigDecimal.ZERO) < 0 ) {
                logger.error("Invalid sensor value1: {}", value1);
                throw new IllegalArgumentException("Invalid sensor value: " + value1);
            }
            if (battery_status == null || battery_status.compareTo(BigDecimal.ZERO) < 0 ) {
                logger.error("Invalid sensor battery_status: {}", battery_status);
                throw new IllegalArgumentException("Invalid sensor value: " + battery_status);
            }
            // If the device is a dual bin, value2 must be non-negative and not null
            if (cachedDevice.getType().compareTo(DeviceType.DUAL_BIN) == 0
                    && (value2 == null || value2.compareTo(BigDecimal.ZERO) < 0)){
                logger.error("Invalid sensor value2: {}", value2);
                throw new IllegalArgumentException("Invalid sensor value2: " + value2);
            }

            LocalDateTime recordedAt = LocalDateTime.now();

            // 1. Queue to Redis
            TelemetryQueueEvent queueEvent = TelemetryQueueEvent.builder()
                    .deviceId(cachedDevice.getId())
                    .hardwareId(cachedDevice.getHardwareId())
                    .value1(value1)
                    .value2(value2)
                    .value3(battery_status)
                    .recordedAt(recordedAt.toString())
                    .build();
            redisService.queueTelemetry(queueEvent);
            logger.info("Telemetry event queued to Redis: {}", queueEvent);

            // 2. Push to active SSE clients
            SensorReadingDTO dataDto = new SensorReadingDTO(
                    cachedDevice.getId().toString(),
                    value1.toString(),
                    value2 != null ? value2.toString() : null,
                    battery_status.toString(),
                    recordedAt.toString()
            );
            streamManager.broadcast(cachedDevice.getId(), dataDto);

        } catch (Exception e) {
            logger.error("Failed to process MQTT payload: {}", payload, e);
        }
    }
}

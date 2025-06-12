package com.example.demo.service;


import com.example.demo.model.Device;
import com.example.demo.model.Dtos.SensorReadingDTO;
import com.example.demo.model.SensorReading;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.repo.SensorReadingRepo;
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
    private SensorReadingRepo sensorDataRepo;

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private DeviceStreamManager streamManager;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    //mosquitto_pub -t "reading/waste-level" -m "{\"sensor_id\":\"5fac3f05-4494-4e52-84a5-0a77f23a1c8d\",\"value\":86}"
    //mosquitto_pub -h broker.hivemq.com -p 1883 -t reading/waste-level -m "{\"sensor_id\":\"5fac3f05-4494-4e52-84a5-0a77f23a1c8d\", \"value\":42}"
    public void handleIncomingData(String payload){
        try {
            // Parse the incoming JSON payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payload);

            UUID deviceId = UUID.fromString(node.get("sensor_id").asText());
            BigDecimal value = node.get("value").decimalValue();

            //System.out.println("Received data for sensor_id: " + deviceId + " with value: " + value);


            // Validate device ID and value and
            Device device = deviceRepo.findById(deviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
                logger.error("Invalid sensor value: {}", value);
                throw new IllegalArgumentException("Invalid sensor value: " + value);
            }


            // 1. Save to sensor_data
            SensorReading data = new SensorReading(device, value, LocalDateTime.now());
            sensorDataRepo.save(data);

            // 2. Update last value in device and set active
            device.setActive(true);
            device.setLastValue(value);
            device.setLastUpdated(LocalDateTime.now());
            deviceRepo.save(device); // or load & save

            // 3. Push to active SSE clients
            SensorReadingDTO dataDto = new SensorReadingDTO(
                    device.getId().toString(),data.getValue().toString(),data.getRecordedAt().toString());
            streamManager.broadcast(deviceId, dataDto);
            // ... rest of your logic
        } catch (Exception e) {
            logger.error("Failed to parse payload: {}", payload, e);
            // Optionally, handle or rethrow
        }
    }


}

package com.example.demo.service;

import com.example.demo.model.Dtos.device.DeviceCacheDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private static final String DEVICE_CACHE_PREFIX = "device:hardware:";
    private static final String TELEMETRY_QUEUE_KEY = "telemetry:queue";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Get device metadata from cache, or load it via the database supplier if missing.
     */
    public DeviceCacheDTO getDevice(String hardwareId, Supplier<DeviceCacheDTO> dbSupplier) {
        String key = DEVICE_CACHE_PREFIX + hardwareId;
        try {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                return objectMapper.readValue(cachedValue, DeviceCacheDTO.class);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve device from Redis cache for hardwareId: {}", hardwareId, e);
        }

        // Cache miss: Load from database
        DeviceCacheDTO dbDevice = dbSupplier.get();
        if (dbDevice != null) {
            try {
                String serialized = objectMapper.writeValueAsString(dbDevice);
                // Cache for 24 hours
                redisTemplate.opsForValue().set(key, serialized, 24, TimeUnit.HOURS);
            } catch (Exception e) {
                logger.error("Failed to write device to Redis cache for hardwareId: {}", hardwareId, e);
            }
        }
        return dbDevice;
    }

    /**
     * Push raw telemetry message payload to the Redis queue.
     */
    public void queueTelemetry(Object telemetryPayload) {
        try {
            String serialized = objectMapper.writeValueAsString(telemetryPayload);
            redisTemplate.opsForValue().getOperations().opsForList().leftPush(TELEMETRY_QUEUE_KEY, serialized);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize telemetry data for queuing", e);
        }
    }

    /**
     * Pop a batch of telemetry payloads from the Redis queue.
     */
    public List<String> popTelemetryBatch(int batchSize) {
        List<String> batch = new ArrayList<>();
        try {
            for (int i = 0; i < batchSize; i++) {
                String val = redisTemplate.opsForList().rightPop(TELEMETRY_QUEUE_KEY);
                if (val == null) {
                    break;
                }
                batch.add(val);
            }
        } catch (Exception e) {
            logger.error("Failed to pop batch from Redis queue", e);
        }
        return batch;
    }

    /**
     * Evict device metadata from Redis cache.
     */
    public void evictDevice(String hardwareId) {
        String key = DEVICE_CACHE_PREFIX + hardwareId;
        try {
            redisTemplate.delete(key);
            logger.info("Evicted device from Redis cache for hardwareId: {}", hardwareId);
        } catch (Exception e) {
            logger.error("Failed to evict device from Redis cache for hardwareId: {}", hardwareId, e);
        }
    }
}

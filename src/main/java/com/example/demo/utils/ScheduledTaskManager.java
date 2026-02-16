package com.example.demo.utils;

import com.example.demo.config.MqttConfig;
import com.example.demo.model.Device;
import com.example.demo.repo.DeviceRepo;
import com.example.demo.service.MqttService;
import com.example.demo.service.MqttStatusService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledTaskManager {

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private MqttStatusService mqttStatusService;

    // Runs every 30 seconds
    @Scheduled(fixedRate = 30_000)
    public void markOfflineDevices() {
        if (mqttStatusService.isMqttConnected()) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);
            List<Device> outdatedDevices = deviceRepo.findByIsActiveTrueAndLastUpdatedBefore(cutoff);
            for (Device device : outdatedDevices) {
                device.setIsActive(false);
                deviceRepo.save(device);
            }
        }
    }

    //Remove unverified users every 24 hours
    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    public void removeUnverifiedUsers() {
       userService.removeUnverifiedUsers();
    }
}

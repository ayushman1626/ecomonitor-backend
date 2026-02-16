package com.example.demo.model.Dtos.device;


import com.example.demo.model.Device;
import com.example.demo.model.enums.DeviceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeviceDTO {
    private String id;
    private String name;
    private DeviceType type;
    private String location;
    private BigDecimal lastValue1;
    private BigDecimal lastValue2;
    private BigDecimal battery_status;
    private LocalDateTime lastUpdated;
    private String onlineStatus;
    private String interfaceId;
    private String interfaceName;

    public DeviceDTO(Device device) {
        this.id = device.getId().toString();
        this.name = device.getName();
        this.type = device.getType();
        this.location = device.getLocation();
        this.lastValue1 = device.getLastValue1();
        this.lastValue2 = device.getLastValue2();
        this.battery_status = device.getBattery_status();
        this.lastUpdated = device.getLastUpdated();
        this.interfaceId = device.getInterfaceEntity().getId().toString();
        this.interfaceName = device.getInterfaceEntity().getName();
        this.onlineStatus = device.getIsActive() ? "Online" : "Offline";
    }
}

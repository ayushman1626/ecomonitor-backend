package com.example.demo.model.Dtos;


import com.example.demo.model.Device;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeviceDTO {
    private String id;
    private String name;
    private String type;
    private String location;
    private BigDecimal lastValue;
    private LocalDateTime lastUpdated;
    private String interfaceId;
    private String interfaceName;

    public DeviceDTO(Device device) {
        this.id = device.getId().toString();
        this.name = device.getName();
        this.type = device.getType().name();
        this.location = device.getLocation();
        this.lastValue = device.getLastValue();
        this.lastUpdated = device.getLastUpdated();
        this.interfaceId = device.getInterfaceEntity().getId().toString();
        this.interfaceName = device.getInterfaceEntity().getName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getLastValue() {
        return lastValue;
    }

    public void setLastValue(BigDecimal lastValue) {
        this.lastValue = lastValue;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    @Override
    public String toString() {
        return "DeviceDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", lastValue=" + lastValue +
                ", lastUpdated=" + lastUpdated +
                ", interfaceId='" + interfaceId + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                '}';
    }
}

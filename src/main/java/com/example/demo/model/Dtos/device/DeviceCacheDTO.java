package com.example.demo.model.Dtos.device;

import com.example.demo.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCacheDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String hardwareId;
    private DeviceType type;
}

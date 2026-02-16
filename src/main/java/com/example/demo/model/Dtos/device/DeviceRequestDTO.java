package com.example.demo.model.Dtos.device;

import com.example.demo.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class DeviceRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private DeviceType type;

    @NotBlank(message = "Location is required")
    private String location;
}

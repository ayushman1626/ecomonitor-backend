package com.example.demo.controller;

import com.example.demo.model.Device;
import com.example.demo.model.Dtos.DeviceDTO;
import com.example.demo.model.Dtos.DeviceRequestDTO;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("api/device")
@RestController
@Tag(name = "Device", description = "API endpoints for auth")
public class DeviceController {

    @Autowired
    DeviceService deviceService;

    @PostMapping("/{interfaceId}")
    @Operation(summary = "Create Device")
    public ResponseEntity<ApiResponse<DeviceDTO>> createDevice (
            @RequestBody DeviceRequestDTO device,
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
            ) throws Exception{
        if(userPrinciple == null){
            return new ResponseEntity<>(
                    new ApiResponse<>(false,"UNAUTHENTICATED",null), HttpStatus.UNAUTHORIZED);
        }
        DeviceDTO savedDevice = deviceService.createDevice(device, interfaceId,userPrinciple.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Device created successfully", savedDevice));
    }

    @GetMapping("")
    @Operation(summary = "get All Devices")
    public ResponseEntity<ApiResponse<List<DeviceDTO>>> getDevices(
            @AuthenticationPrincipal UserPrinciple userPrinciple
    ){
        if(userPrinciple == null){
            return new ResponseEntity<>(new ApiResponse<>(false,"UNAUTHENTICATED",null), HttpStatus.UNAUTHORIZED);
        }

        List<DeviceDTO> devices = deviceService.getAllDevices(userPrinciple);

        return ResponseEntity.ok(new ApiResponse<>(
                true, "All devices fetched successfully",devices));
    }
}

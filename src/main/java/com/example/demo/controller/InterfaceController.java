package com.example.demo.controller;


import com.example.demo.model.Dtos.inface.InterfaceDTO;
import com.example.demo.model.Dtos.inface.InterfaceWithDevicesDTO;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Interface;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.InterfaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RequestMapping("/api/interface")
@RestController
@Tag(name = "Interface", description = "API endpoints for Interface")
public class InterfaceController {

    @Autowired
    InterfaceService interfaceService;

    @PostMapping("")
    @Operation(summary = "Create Interface")
    public ResponseEntity<ApiResponse<InterfaceDTO>> saveInterface(
            @RequestBody Interface iface,
            @AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        try {
            InterfaceDTO savedInterface = interfaceService.saveInterface(iface, userPrinciple.getUsername());
            return ResponseEntity.ok(new ApiResponse<>(true,"Interface created successfully",savedInterface));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse<>(false,"Something went wrong : "+e.getMessage(),null)
            );
        }
    }


    @GetMapping("")
    @Operation(summary = "Get Interface")
    public ResponseEntity<ApiResponse<List<InterfaceDTO>>> getInterfaces(@AuthenticationPrincipal UserPrinciple userPrinciple) {

        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }

        List<InterfaceDTO> interfaces = interfaceService.getInterfaceForUser(userPrinciple.getUsername());
        if (interfaces.isEmpty()) {
            Map<String, String> error = Map.of("message", "No interfaces found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(true,"No Interfaces present",null)
            );
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(true,"Interfaces fetched successfully",interfaces)
        );
    }

    @GetMapping("/{interface_id}")
    @Operation(summary = "Get Interface By Id")
    public ResponseEntity<ApiResponse<?>> getInterfaceById(
            @PathVariable("interface_id") UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
    ){
        if (userPrinciple == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not authenticated.", null));
        }
        InterfaceWithDevicesDTO response = interfaceService.getInterfaceWithDevices(interfaceId,userPrinciple.getUsername());
        return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(true, "Interface fetched successfully", response));
    }
}

package com.example.demo.controller;

import com.example.demo.model.Dtos.access.AddAccessRequestDTO;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.access.InterfaceAccessDTO;
import com.example.demo.model.UserPrinciple;
import com.example.demo.service.AccessService;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;


@RestController
@Tag(name = "Access" , description = "API endpoint for Manage Access")
public class AccessController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccessService accessService;

    @PostMapping("api/interface/{interfaceId}/add-access")
    @Operation(summary = "Give Access")
    public ResponseEntity<ApiResponse<?>> giveAccess(
            @RequestBody AddAccessRequestDTO input,
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
            ) throws AccessDeniedException{
        if(userPrinciple == null){
            return new ResponseEntity<>(
                    new ApiResponse<>(false,"UNAUTHENTICATED",null), HttpStatus.UNAUTHORIZED);
        }
        Boolean accessGiven = accessService.giveAccess(
                interfaceId,input.getUsername(), input.getRole(),userPrinciple.getUsername()
        );

        return new ResponseEntity<>(
                new ApiResponse<>(true,"New user granted access as " + input.getRole(),null),
                HttpStatus.OK);
    }

    @GetMapping("api/interface/{interfaceId}/access")
    @Operation(summary = "Show given access of Interface")
    public ResponseEntity<ApiResponse<List<InterfaceAccessDTO>>> showAccessesByInterface(
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
    )throws AccessDeniedException{
        if(userPrinciple == null){
            return new ResponseEntity<>(
                    new ApiResponse<>(false,"UNAUTHENTICATED",null), HttpStatus.UNAUTHORIZED);
        }

        List<InterfaceAccessDTO> accessList = accessService.getAllAccess(interfaceId,userPrinciple.getUsername());
        return new ResponseEntity<>(new ApiResponse<>(true,"All access fetched successfully",accessList),HttpStatus.OK);
    }
}

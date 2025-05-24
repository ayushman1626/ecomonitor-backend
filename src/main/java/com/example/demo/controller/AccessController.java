package com.example.demo.controller;

import com.example.demo.model.Dtos.InterfaceAccessDTO;
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
import java.util.Map;
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
    public ResponseEntity<?> giveAccess(
            @RequestBody Map<String, String> input,
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
            ){
        if(userPrinciple == null){
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        try{
            Boolean accessGiven = accessService.giveAccess(
                    interfaceId,input.get("username"), input.get("role"),userPrinciple.getUsername()
                    );

            Map<String, String> success = Map.of(
                    "message", "New user granted access as " + input.get("role")
            );
            return new ResponseEntity<>(success,HttpStatus.CREATED);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("api/interface/{interfaceId}/access")
    @Operation(summary = "Show given access of Interface")
    public ResponseEntity<?> showAccessesByInterface(
            @PathVariable UUID interfaceId,
            @AuthenticationPrincipal UserPrinciple userPrinciple
    ){
        if(userPrinciple == null){
            return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        try{
            List<InterfaceAccessDTO> accessList = accessService.getAllAccess(interfaceId,userPrinciple.getUsername());
            return new ResponseEntity<>(accessList,HttpStatus.OK);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}

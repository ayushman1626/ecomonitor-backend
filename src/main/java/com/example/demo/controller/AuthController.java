package com.example.demo.controller;

import com.example.demo.model.Dtos.Auth.RegisterRequest;
import com.example.demo.model.Dtos.Auth.RegistrationResponse;
import com.example.demo.model.User;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.Dtos.common.ApiResponse;
import com.example.demo.model.Dtos.Auth.LoginRequest;
import com.example.demo.model.Dtos.Auth.LoginResponse;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequestMapping("api/auth")
@RestController
@Tag(name = "Auth", description = "API endpoints for auth")
public class AuthController {

    @Autowired
    AuthService authService;


    @PostMapping("/register")
    @Operation(summary = "Register user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        //passing to service
        RegistrationResponse response = authService.registerUser(request);

        //response from service
        HttpStatus status = response.isSuccess() ? HttpStatus.CREATED : HttpStatus.CONFLICT;

        //api-response
        if (!response.isSuccess()) {
            return ResponseEntity.status(status).body(
                    new ApiResponse<>(false, response.getMessage(), null));
        } else {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", response.getEmail());
            responseData.put("otpExpiresInSeconds", 600);

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(true, response.getMessage(), responseData));
        }
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "Verify Otp")
    ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@RequestBody Map<String, String> input){

        RegistrationResponse response = authService.verifyOtp(input.get("email"), input.get("otp"));

        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

        if (!response.isSuccess()) {
            return ResponseEntity.status(status).body(
                    new ApiResponse<>(false, response.getMessage(), null));
        } else {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", response.getEmail());

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(true, response.getMessage(), responseData));
        }
    }
    @PostMapping("/resend-otp")
    @Operation(summary = "resend otp")
    ResponseEntity<?> resendOtp(@RequestBody Map<String, String> emailData) {
        try{
            String response = authService.resendOtp(emailData.get("email"));
            return ResponseEntity.ok(response);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/login")
    @Operation(summary = "login")
    ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request){
        LoginResponse response;
        response = authService.loginUser(request);

        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(true, response.getMessage(), response.getData()));
    }
    //forget-password
    //change-password






    @GetMapping("api/users")
    @Operation(summary = "For dev")
    ResponseEntity<List<User>> getUsers(){
        return ResponseEntity.ok(authService.getUsers());
    }
}

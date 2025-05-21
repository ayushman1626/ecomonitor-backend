package com.example.demo.service;

import com.example.demo.exceptions.UserAlreadyExistsException;
import com.example.demo.model.Dtos.Auth.LoginRequest;
import com.example.demo.model.Dtos.Auth.LoginResponse;
import com.example.demo.model.Dtos.Auth.RegisterRequest;
import com.example.demo.model.Dtos.Auth.RegistrationResponse;
import com.example.demo.model.User;
import com.example.demo.repo.UserRepo;
import com.example.demo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    UserRepo userRepo;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    OtpService otpService;

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public RegistrationResponse registerUser(RegisterRequest request) throws RuntimeException{

        if(userRepo.findByEmailAndIsVerifiedTrue(request.getEmail()).isPresent()){
            throw new UserAlreadyExistsException("User already exist with the email");
        }
        if(userRepo.findByUsernameAndIsVerifiedTrue(request.getUsername()).isPresent()){
            throw new UserAlreadyExistsException("User already exist with the username");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setVerified(false);
        user.setPassword(encoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepo.save(user);

        // Sending OTP email
        try {
           otpService.generateAndSendOtp(user.getEmail(),"Verify your email");
            return new RegistrationResponse(user.getEmail(), "Registration successful. Verification email sent.", true);
        } catch (Exception e) {
            userRepo.delete(user);
            return new RegistrationResponse(user.getEmail(), "Registration failed. Could not send verification email.", false);
        }
    }

    public RegistrationResponse verifyOtp(String email, String otp) throws BadCredentialsException {
        User user = userRepo.findLatestByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(!otpService.verifyOtp(email,otp)){
            throw new BadCredentialsException("OTP invalid or expired");
        }
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiration(null);
        userRepo.save(user);
        return new RegistrationResponse(user.getEmail(),"OTP verification successful",true);
    }

    public String resendOtp(String email){
        User user = userRepo.findLatestByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return otpService.generateAndSendOtp(user.getEmail(),"Verify your email");
    }



    public LoginResponse loginUser(LoginRequest request) throws BadCredentialsException{

        User user = userRepo.findByEmailAndIsVerifiedTrue(request.getEmail()).orElseThrow(()->
                new BadCredentialsException("NO VERIFIED USER FOUND WITH THAT EMAIL")
        );

        System.out.println(request);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("token",token);
        data.put("email", user.getEmail());
        data.put("fullName",user.getFullName());
        data.put("id",user.getId().toString());

        return new LoginResponse(data,true,"Login successful");
    }

    public List<User> getUsers(){
        return userRepo.findByIsVerifiedTrue();
    }
}

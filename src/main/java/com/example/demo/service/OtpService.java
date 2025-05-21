package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private EmailService emailService;


     //Generate OTP, save it to the database with expiration, and send via email

    public String generateAndSendOtp(String email, String subject) throws RuntimeException{
        User user = userRepository.findLatestByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(5)); // OTP valid for 5 minutes
        userRepository.save(user);

        emailService.sendEmail(user.getEmail(), subject, "Your OTP: " + otp);
        return "OTP sent to your email.";
    }


    public Boolean verifyOtp(String email, String otp){
        User user = userRepository.findLatestByEmail(email).orElse(null);
        if(user == null || user.getOtp() == null || user.getOtpExpiration() == null){
            return false;
        }
        if(LocalDateTime.now().isAfter(user.getOtpExpiration())){
            return false;
        }
        return user.getOtp().equals(otp);
    }

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}


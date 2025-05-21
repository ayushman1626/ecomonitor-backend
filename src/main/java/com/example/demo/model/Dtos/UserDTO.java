package com.example.demo.model.Dtos;

import com.example.demo.model.User;

import java.time.LocalDateTime;
import java.util.UUID;


public class UserDTO {
    private UUID id;
    private String fullName;
    private String email;
    private boolean isVerified;
    private LocalDateTime createdAt;

    // Constructor
    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.isVerified = user.getVerified();
        this.createdAt = user.getCreatedAt();
        this.fullName = user.getFullName();
    }

    // Getters
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public boolean isVerified() { return isVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}


package com.example.demo.model.Dtos.inface;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteInterfaceRequest {

    @NotNull(message = "Password is required")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}

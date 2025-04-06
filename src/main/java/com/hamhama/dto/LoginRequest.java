package com.hamhama.dto;

import jakarta.validation.constraints.NotBlank; // Add validation
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username cannot be blank") // Add validation
    private String username; // Can be username or email if you adapt UserDetailsService

    @NotBlank(message = "Password cannot be blank") // Add validation
    private String password;

    // Lombok generates getters/setters
}
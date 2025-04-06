package com.hamhama.dto;

import jakarta.validation.constraints.Email;  // Add validation
import jakarta.validation.constraints.NotBlank; // Add validation
import jakarta.validation.constraints.Size;   // Add validation
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    // Lombok generates getters/setters
}
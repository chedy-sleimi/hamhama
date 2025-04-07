package com.hamhama.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    // Optional: Include refresh token if implemented
    // private String refreshToken;
    private String username; // Include username for frontend convenience
    private java.util.List<String> roles; // Include roles for frontend convenience
    private Long userId;
}
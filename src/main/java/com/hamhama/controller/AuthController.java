package com.hamhama.controller;

import com.hamhama.dto.LoginRequest;
import com.hamhama.dto.RegisterRequest;
import com.hamhama.model.Role;
import com.hamhama.model.User;
import com.hamhama.repository.UserRepository;
import com.hamhama.security.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Changed to accept RegisterRequest DTO instead of User model
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
        System.out.println("Received user: " + registerRequest.getUsername());  // Log the received data

        // Create a new User object from the RegisterRequest DTO
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));  // Encode password before saving

        // Assign default role as USER
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);

        // Save the user to the database
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest loginRequest) {
        // Find the user by username (or email if you're using email)
        User foundUser = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the password matches
        if (!passwordEncoder.matches(loginRequest.getPassword(), foundUser.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate the token
        String token = jwtUtil.generateToken(loginRequest.getUsername());

        // Return the token in a map
        return Collections.singletonMap("token", token);
    }

}

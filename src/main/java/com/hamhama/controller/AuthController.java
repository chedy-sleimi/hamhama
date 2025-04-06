package com.hamhama.controller;

import com.hamhama.dto.AuthenticationResponse;
import com.hamhama.dto.LoginRequest;
import com.hamhama.dto.RegisterRequest;
import com.hamhama.model.Role;
import com.hamhama.model.User;
import com.hamhama.repository.UserRepository;
import com.hamhama.security.JwtUtil;
import jakarta.validation.Valid; // Import Valid
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // Import specific exception
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.GrantedAuthority; // Import GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails;
// Import UserDetailsService if needed for direct lookup, but Authentication object is preferred after successful login
// import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set; // Import Set
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor // Use Lombok for constructor injection
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Inject necessary beans
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository; // Keep for registration checks
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    // private final UserDetailsService userDetailsService; // Usually not needed directly here after AuthenticationManager

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) { // Add @Valid
        log.info("Register request received for username: {}", registerRequest.getUsername());

        // Check if username or email already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists.", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: Email '{}' already exists.", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already in use!");
        }

        // Create a new User object
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Encode password

        // Assign default role(s)
        user.setRoles(Set.of(Role.USER)); // Use Set.of for immutability if just one role initially

        try {
            userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.ok("User registered successfully!");
        } catch (Exception e) {
            log.error("Error during user registration for {}", registerRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering user.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) { // Add @Valid
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        try {
            // 1. Use AuthenticationManager to validate credentials
            // This uses the configured AuthenticationProvider (DaoAuthenticationProvider)
            // which in turn uses UserDetailsService and PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), // Principal
                            loginRequest.getPassword()  // Credentials
                    )
            );

            // 2. If authentication is successful, the Authentication object is populated.
            // SecurityContextHolder.getContext().setAuthentication(authentication); // Handled by filter for subsequent requests, not needed here usually

            // The principal should be our UserDetails object (our User entity)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            log.info("User '{}' authenticated successfully.", userDetails.getUsername());

            // 3. Generate JWT token
            String token = jwtUtil.generateToken(userDetails);
            log.trace("Generated JWT for user {}: {}", userDetails.getUsername(), token); // Careful logging tokens

            // 4. Extract roles for the response DTO
            java.util.List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // 5. Create and return the response DTO
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(token)
                    .username(userDetails.getUsername())
                    .roles(roles)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: Invalid credentials", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            log.error("Error during login for user {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login.");
        }
    }
}
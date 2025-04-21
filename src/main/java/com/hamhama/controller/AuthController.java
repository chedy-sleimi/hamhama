package com.hamhama.controller;

import com.hamhama.dto.AuthenticationResponse;
import com.hamhama.dto.LoginRequest;
import com.hamhama.dto.RegisterRequest;
import com.hamhama.model.Role;
import com.hamhama.model.User; // Import User
import com.hamhama.repository.UserRepository;
import com.hamhama.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
// Removed unused UserDetails import, as User directly implements it or is returned by UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException; // Use for consistency

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth") // Keep path as is, standard for auth
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login.")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details. Assigns the USER role by default.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Username or email already exists, or validation fails",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to save user",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    // Public endpoint - no @SecurityRequirement
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User registration details (username, email, password)", required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
            // Spring's RequestBody annotation remains on the parameter
            @Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Register request received for username: {}", registerRequest.getUsername());

        // Consider moving existence checks to the service layer if complex logic is involved
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists.", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: Email '{}' already exists.", registerRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(Set.of(Role.USER)); // Ensure Role enum/entity is correctly handled

        try {
            userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            // Return simple text response for success
            return ResponseEntity.ok("User registered successfully!");
        } catch (Exception e) {
            log.error("Error during user registration for {}", registerRequest.getUsername(), e);
            // Use ResponseStatusException for consistency, although returning body here is okay too
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering user.");
        }
    }

    @Operation(summary = "Authenticate user and get JWT token", description = "Authenticates a user with username and password, returning a JWT token, username, roles, and user ID upon success.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid username or password",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error during authentication process",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    // Public endpoint - no @SecurityRequirement
    @PostMapping("/login")
    public ResponseEntity<?> login(
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User login credentials (username, password)", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class)))
            // Spring's RequestBody annotation remains on the parameter
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("Login attempt for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Assuming your UserDetailsService loads your User entity which includes roles and ID
            User userDetails = (User) authentication.getPrincipal();
            log.info("User '{}' authenticated successfully.", userDetails.getUsername());

            String token = jwtUtil.generateToken(userDetails);
            log.trace("Generated JWT for user {}: {}", userDetails.getUsername(), token); // Be careful logging tokens even at trace

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            Long userId = userDetails.getId(); // Get ID from the User entity

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .token(token)
                    .username(userDetails.getUsername())
                    .roles(roles)
                    .userId(userId) // Include userId
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: Invalid credentials", loginRequest.getUsername());
            // Return 401 Unauthorized with a clear message
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (ClassCastException e) {
            log.error("Error casting authentication principal to User entity for {}. Check UserDetailsService configuration.", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing user details.");
        } catch (Exception e) {
            log.error("Unexpected error during login for user {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login.");
        }
    }
}
package com.hamhama.config;

import com.hamhama.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Make sure HttpMethod is imported
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService; // Ensure this bean is correctly configured elsewhere

    // Define constants for Swagger paths for clarity and easy maintenance
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/swagger-resources/**" // Often needed too
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Make sure CORS is configured properly if frontend is on different origin
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF as using stateless JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless session policy

                .authorizeHttpRequests(auth -> auth
                        // ---- PUBLICLY ACCESSIBLE ENDPOINTS ----
                        .requestMatchers(SWAGGER_WHITELIST).permitAll() // Permit all Swagger paths defined above
                        .requestMatchers("/auth/**").permitAll() // Registration and Login
                        .requestMatchers("/profile-pictures/**").permitAll() // Static profile pictures
                        .requestMatchers("/recipe-pictures/**").permitAll() // Static recipe pictures

                        // Public Recipe Reads
                        .requestMatchers(HttpMethod.GET, "/api/recipes", "/api/recipes/*", "/api/recipes/search", "/api/recipes/category/*", "/api/recipes/*/nutrition").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/categories").permitAll() // Get by list of categories (Using POST)

                        // Public Ingredient Reads
                        .requestMatchers(HttpMethod.GET, "/api/ingredients", "/api/ingredients/*").permitAll()
                        // Public Comment Reads
                        .requestMatchers(HttpMethod.GET, "/comments/recipe/**").permitAll()
                        // Public Rating Reads
                        .requestMatchers(HttpMethod.GET, "/ratings/recipe/*/average").permitAll()
                        // Rating Management (Authenticated - own or by admin)
                        .requestMatchers("/ratings/rate", "/ratings/delete").authenticated()
                        // Ingredient Features (Authenticated)
                        .requestMatchers("/api/ingredients/*/substitutes", "/api/ingredients/generate-image").authenticated()
                        // User Interactions & Self-Management (Authenticated)
                        // Needs careful ordering. Put more specific authenticated paths before this generic one.
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated() // Get own profile
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile-picture").authenticated() // Update own picture
                        .requestMatchers(HttpMethod.DELETE, "/api/users/profile-picture").authenticated() // Delete own picture
                        .requestMatchers(HttpMethod.GET, "/api/users/liked-recipes").authenticated() // Get own liked recipes
                        .requestMatchers(HttpMethod.GET, "/api/users/blocked-users").authenticated() // Get own blocked list
                        .requestMatchers(HttpMethod.PUT, "/api/users/privacy").authenticated() // Update own privacy
                        .requestMatchers(HttpMethod.GET, "/api/users/privacy").authenticated() // Get own privacy
                        .requestMatchers(HttpMethod.POST, "/api/users/follow/*").authenticated() // Follow user
                        .requestMatchers(HttpMethod.DELETE, "/api/users/unfollow/*").authenticated() // Unfollow user
                        .requestMatchers(HttpMethod.POST, "/api/users/like/*").authenticated() // Like recipe
                        .requestMatchers(HttpMethod.DELETE, "/api/users/unlike/*").authenticated() // Unlike recipe
                        .requestMatchers(HttpMethod.POST, "/api/users/block/*").authenticated() // Block user
                        .requestMatchers(HttpMethod.DELETE, "/api/users/unblock/*").authenticated() // Unblock user
                        .requestMatchers(HttpMethod.GET, "/api/users/*/following").authenticated() // Get someone's following list (privacy checked in service/controller)
                        .requestMatchers(HttpMethod.GET, "/api/users/*/followers").authenticated() // Get someone's followers list (privacy checked in service/controller)
                        .requestMatchers(HttpMethod.GET, "/api/users/*/profile").authenticated() // Get someone's public profile (privacy checked in service/controller)
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").authenticated() // General update (for self, if ID matches - or admin handled by role check earlier) - place carefully
                        // ---- ADMIN ONLY ENDPOINTS ----
                        // User Management (by Admin)
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN") // Get all users - Added this based on UserController
                        .requestMatchers(HttpMethod.GET, "/api/users/*").hasRole("ADMIN") // Get specific user (admin view) - Ensure it doesn't clash with authenticated /api/users/** below if ID is numeric
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN") // Create user (admin only)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN") // Delete any user - Ensure it doesn't clash with authenticated /api/users/** below if ID is numeric
                        // Ingredient Management (by Admin)
                        .requestMatchers(HttpMethod.POST, "/api/ingredients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ingredients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ingredients/**").hasRole("ADMIN")

                        // ---- AUTHENTICATED USER ENDPOINTS (USER or ADMIN) ----
                        // Recipe Management (Authenticated - own or by admin)
                        .requestMatchers(HttpMethod.POST, "/api/recipes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/recipes/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/recipes/**").authenticated()
                        // Comment Management (Authenticated - own or by admin)
                        .requestMatchers("/comments/add", "/comments/delete/**").authenticated()
                        // ---- FALLBACK - All other unmatached requests require authentication ----
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
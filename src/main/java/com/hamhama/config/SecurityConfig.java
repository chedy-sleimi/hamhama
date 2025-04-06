package com.hamhama.config;

import com.hamhama.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
    private final UserDetailsService userDetailsService;

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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ---- PUBLICLY ACCESSIBLE ENDPOINTS ----
                        .requestMatchers("/auth/**").permitAll() // Registration and Login
                        .requestMatchers("/profile-pictures/**").permitAll() // Static profile pictures (adjust path if needed)
                        // Public Recipe Reads
                        .requestMatchers(HttpMethod.GET, "/api/recipes", "/api/recipes/*", "/api/recipes/search", "/api/recipes/category/*", "/api/recipes/*/nutrition").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/categories").permitAll() // Get by list of categories
                        // Public Ingredient Reads
                        .requestMatchers(HttpMethod.GET, "/api/ingredients", "/api/ingredients/*").permitAll()
                        // Public Comment Reads
                        .requestMatchers(HttpMethod.GET, "/comments/recipe/**").permitAll()
                        // Public Rating Reads
                        .requestMatchers(HttpMethod.GET, "/ratings/recipe/*/average").permitAll()

                        // ---- ADMIN ONLY ENDPOINTS ----
                        // User Management (by Admin)
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN") // List all users
                        .requestMatchers(HttpMethod.GET, "/api/users/*").hasRole("ADMIN") // Get specific user (admin view)
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN") // Create user (admin only)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN") // Delete any user
                        // Ingredient Management (by Admin)
                        .requestMatchers(HttpMethod.POST, "/api/ingredients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ingredients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ingredients/**").hasRole("ADMIN")

                        // ---- AUTHENTICATED USER ENDPOINTS (USER or ADMIN) ----
                        // NOTE: Fine-grained checks (ownership, etc.) for PUT/DELETE should be in service/controller (@PreAuthorize recommended)
                        // Recipe Management (Authenticated - own or by admin)
                        .requestMatchers(HttpMethod.POST, "/api/recipes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/recipes/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/recipes/**").authenticated()
                        // Comment Management (Authenticated - own or by admin)
                        .requestMatchers("/comments/add", "/comments/delete/**").authenticated()
                        // Rating Management (Authenticated - own or by admin)
                        .requestMatchers("/ratings/rate", "/ratings/delete").authenticated()
                        // Ingredient Features (Authenticated)
                        .requestMatchers("/api/ingredients/*/substitutes", "/api/ingredients/generate-image").authenticated()
                        // User Interactions & Self-Management (Authenticated)
                        // This covers all remaining /api/users/** endpoints like follow, like, block, profile, privacy, picture updates etc.
                        // The admin-specific GET/POST/DELETE on /api/users/* above take precedence.
                        .requestMatchers("/api/users/**").authenticated()


                        // ---- FALLBACK - All other unmatached requests require authentication ----
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
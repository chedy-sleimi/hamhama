package com.hamhama.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // Import StringUtils
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor // Lombok for constructor injection
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger filterLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService; // Use the UserDetailsService interface

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Check if the header is present and correctly formatted
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterLogger.trace("No JWT token found in request header, passing to next filter.");
            filterChain.doFilter(request, response); // Pass to the next filter
            return;
        }

        jwt = authHeader.substring(7); // Extract token after "Bearer "
        filterLogger.trace("Extracted JWT: {}", jwt); // Be careful logging tokens in production

        try {
            username = jwtUtil.extractUsername(jwt);
            filterLogger.trace("Username extracted from JWT: {}", username);

            // 2. Check if username is valid and user is not already authenticated
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                filterLogger.trace("UserDetails loaded for username: {}", username);

                // 3. Validate the token against the UserDetails
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    filterLogger.trace("JWT token is valid for user: {}", username);
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials are null for JWT-based auth
                            userDetails.getAuthorities() // Get authorities from UserDetails
                    );
                    // Set details (e.g., IP address, session ID from the request)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // 4. Update SecurityContextHolder - THIS IS THE AUTHENTICATION STEP
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterLogger.debug("User '{}' authenticated successfully. Setting SecurityContext.", username);
                } else {
                    filterLogger.warn("JWT token validation failed for user: {}", username);
                }
            } else {
                filterLogger.trace("Username is null or user is already authenticated.");
            }
        } catch (UsernameNotFoundException e) {
            filterLogger.warn("User not found for username extracted from JWT: {}", e.getMessage());
            // Optionally clear context or handle differently
        }
        catch (Exception e) {
            // Catch other potential JWT exceptions (ExpiredJwtException, SignatureException etc.)
            filterLogger.error("JWT Token processing error: {}", e.getMessage());
            // Depending on policy, you might want to clear the context or send an error response earlier
            // SecurityContextHolder.clearContext();
            // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token: " + e.getMessage());
            // return; // Stop filter chain if token is invalid and you sent a response
        }

        // Always continue the filter chain
        filterChain.doFilter(request, response);
    }
}
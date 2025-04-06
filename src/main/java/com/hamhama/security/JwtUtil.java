package com.hamhama.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final Key secretKey;
    private final long jwtExpirationMs;
    // private final long refreshTokenExpirationMs; // Optional: if using refresh tokens

    // Inject values from application.properties
    public JwtUtil(@Value("${jwt.secret.key}") String secret,
                   @Value("${jwt.expiration.ms}") long jwtExpirationMs
            /*@Value("${jwt.refresh.token.expiration.ms}") long refreshTokenExpirationMs*/) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.jwtExpirationMs = jwtExpirationMs;
        // this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Could not parse JWT token: {}", e.getMessage());
            // Consider throwing a specific exception or returning null based on policy
            throw new RuntimeException("Invalid JWT token", e); // Or handle more gracefully
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            // If expiration cannot be extracted, consider the token invalid/expired
            return true;
        }
    }

    // Generate token FOR a specific user (UserDetails contains username, roles etc.)
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Add roles/authorities to claims for authorization checks later
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")); // e.g., "ROLE_USER,ROLE_ADMIN"
        claims.put("roles", roles);
        // You could add other claims like user ID, email etc. if needed
        // claims.put("userId", ((User) userDetails).getId()); // Example if User model has getId()

        return createToken(claims, userDetails.getUsername(), jwtExpirationMs);
    }

    // Optional: Generate Refresh Token
    // public String generateRefreshToken(UserDetails userDetails) {
    //    return createToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpirationMs);
    // }


    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token against UserDetails
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
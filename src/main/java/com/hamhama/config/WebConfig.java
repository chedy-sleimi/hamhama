package com.hamhama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull; // Import NonNull
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**") // Apply CORS configuration to all paths
                        .allowedOrigins("http://localhost:4200") // Allow requests from Angular app's origin
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers (e.g., Content-Type, Authorization)
                        .allowCredentials(true) // Allow credentials (cookies, authorization headers) for authenticated requests
                        .maxAge(3600); // How long the results of a pre-flight request can be cached (in seconds)
            }
        };
    }
}
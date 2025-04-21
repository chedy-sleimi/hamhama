package com.hamhama;

// Add these imports:
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
// ---

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc // Keep this annotation if you specifically need it, otherwise it's often not necessary with Spring Boot autoconfiguration.
@SpringBootApplication
// Add the OpenAPI annotations here:
@OpenAPIDefinition(info = @Info( // Defines the general API info
        title = "Hamhama API",
        version = "v0.0.1", // Use the version from your pom.xml
        description = "Backend API for the Hamhama project (Recipe Management)"
        // You can add other info like contact, license here if you want
))
@SecurityScheme( // Defines how JWT authentication works for Swagger
        name = "bearerAuth", // This is the reference name we'll use to secure endpoints
        type = SecuritySchemeType.HTTP, // Authentication type is HTTP
        scheme = "bearer", // The scheme is 'Bearer'
        bearerFormat = "JWT", // The token format
        in = SecuritySchemeIn.HEADER, // The token is sent in the Header
        description = "Enter JWT token **without** the 'Bearer ' prefix" // Help text for the user in Swagger UI
)
public class HamhamaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HamhamaApplication.class, args);
    }

}
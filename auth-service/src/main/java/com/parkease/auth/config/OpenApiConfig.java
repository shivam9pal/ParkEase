package com.parkease.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "ParkEase Auth Service API",
                version     = "v1",
                description = "Authentication & User Management — Security gateway for the ParkEase platform",
                contact     = @Contact(name = "ParkEase", email = "dev@parkease.com")
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local Development Server")
        }
)
@SecurityScheme(
        name          = "bearerAuth",
        type          = SecuritySchemeType.HTTP,
        scheme        = "bearer",
        bearerFormat  = "JWT",
        description   = "Enter JWT token obtained from POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
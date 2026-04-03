package com.parkease.vehicle.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3.0 configuration.
 *
 * Accessible at: http://localhost:8086/swagger-ui.html
 *
 * All protected endpoints show a 🔒 lock icon in Swagger UI.
 * Click "Authorize" and paste the Bearer token from auth-service login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "ParkEase — Vehicle Service API",
                version     = "v1",
                description = "Manages vehicle registration, lookup, and EV/type queries for the ParkEase platform. " +
                        "Tokens are issued by auth-service (port 8081). " +
                        "Use POST /api/v1/auth/login to obtain a token, then click Authorize below.",
                contact     = @Contact(
                        name  = "ParkEase Engineering",
                        email = "dev@parkease.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8086", description = "Local Development")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name        = "bearerAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "bearer",
        bearerFormat = "JWT",
        in          = SecuritySchemeIn.HEADER,
        description = "Enter the JWT token obtained from POST /api/v1/auth/login on auth-service (port 8081)"
)
public class OpenApiConfig {
    // Configuration is fully annotation-driven — no beans needed
}
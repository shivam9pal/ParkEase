package com.parkease.spot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger UI configuration for spot-service.
 *
 * <p>Swagger UI available at: http://localhost:8083/swagger-ui.html
 * OpenAPI JSON spec at:       http://localhost:8083/v3/api-docs
 *
 * <p>Authentication: Click the "Authorize" button in Swagger UI
 * and paste a JWT token obtained from POST /api/v1/auth/login on port 8081.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase — Spot Service API")
                        .version("v1")
                        .description(
                                "Manages individual parking spaces within lots. " +
                                        "Handles spot creation, availability browsing, " +
                                        "and booking lifecycle status transitions " +
                                        "(AVAILABLE → RESERVED → OCCUPIED → AVAILABLE)."
                        )
                        .contact(new Contact()
                                .name("ParkEase")
                                .email("dev@parkease.com")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local Development")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Enter JWT token from POST /api/v1/auth/login " +
                                                        "(auth-service on port 8081). " +
                                                        "Format: Bearer <token>"
                                        )
                        )
                );
    }
}
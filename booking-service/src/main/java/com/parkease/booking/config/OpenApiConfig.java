package com.parkease.booking.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger configuration for booking-service.
 *
 * Access Swagger UI at: http://localhost:8084/swagger-ui.html
 * OpenAPI JSON at:      http://localhost:8084/v3/api-docs
 *
 * Authentication:
 *   1. Get JWT from POST http://localhost:8081/api/v1/auth/login
 *   2. Click "Authorize" in Swagger UI
 *   3. Enter: Bearer <your_token>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ParkEase — Booking Service API",
                version = "1.0.0",
                description = "Core orchestration service for the ParkEase Smart Parking platform. " +
                        "Manages the full booking lifecycle: create, check-in, check-out, cancel, extend. " +
                        "Coordinates with spot-service, parkinglot-service, and vehicle-service via OpenFeign. " +
                        "Publishes lifecycle events to RabbitMQ for notification-service and analytics-service.",
                contact = @Contact(
                        name = "ParkEase Platform Team",
                        email = "support@parkease.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8084", description = "Local development server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token obtained from POST /api/v1/auth/login on auth-service (port 8081). " +
                "Enter value as: Bearer <token>"
)
public class OpenApiConfig {
    // Configuration is fully annotation-driven — no bean methods required
}
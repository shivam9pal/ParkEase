package com.parkease.parkinglot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ParkEase — Parking Lot Service API")
                        .version("v1")
                        .description("Parking Lot Management — Profile, geo-proximity search, " +
                                "approval workflow, and spot counter management")
                        .contact(new Contact()
                                .name("ParkEase")
                                .email("dev@parkease.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Development")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token from POST /api/v1/auth/login")));
    }
}
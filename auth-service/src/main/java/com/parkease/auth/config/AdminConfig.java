package com.parkease.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "admin")
@Data
public class AdminConfig {

    private String seedEmail;
    private String seedPassword;
    private String seedName;
}
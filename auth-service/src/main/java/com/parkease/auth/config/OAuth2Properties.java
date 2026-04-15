package com.parkease.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "oauth2")
@Getter
@Setter
public class OAuth2Properties {

    private Frontend frontend = new Frontend();

    @Getter
    @Setter
    public static class Frontend {

        private String callbackUrl;
    }
}

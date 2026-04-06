package com.parkease.analytics.feign;

import com.parkease.analytics.security.SystemTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final SystemTokenProvider systemTokenProvider;

    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return requestTemplate -> {
            // If we are in an HTTP request context (controller thread), forward the user's JWT
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    return;
                }
            }
            // Scheduler or RabbitMQ consumer thread — no HTTP context, use system JWT
            requestTemplate.header("Authorization",
                    "Bearer " + systemTokenProvider.generateSystemToken());
        };
    }
}
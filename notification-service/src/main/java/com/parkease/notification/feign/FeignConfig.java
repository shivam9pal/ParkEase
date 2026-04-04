package com.parkease.notification.feign;

import com.parkease.notification.security.SystemTokenProvider;
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

    /**
     * Context-aware JWT forwarder:
     * - Normal HTTP thread  → forwards the incoming Bearer token from the request
     * - RabbitMQ consumer thread (no HTTP context) → generates a system JWT
     */
    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    return;
                }
            }

            // RabbitMQ consumer thread — no HTTP context, use system JWT
            String systemToken = systemTokenProvider.generateSystemToken();
            requestTemplate.header("Authorization", "Bearer " + systemToken);
        };
    }
}
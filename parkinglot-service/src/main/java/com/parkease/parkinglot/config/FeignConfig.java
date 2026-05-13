package com.parkease.parkinglot.config;

import com.parkease.parkinglot.security.SystemTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Global Feign configuration for service-to-service communication.
 *
 * JWT Strategy:
 *
 * Case 1 — Normal HTTP request (API call from frontend/user):
 * RequestContextHolder has a live HttpServletRequest. The user's incoming JWT
 * is forwarded directly to downstream service. Example: Admin rejects lot →
 * parkinglot-service → notification-service
 *
 * Case 2 — Non-HTTP context (scheduled job, message consumer):
 * RequestContextHolder returns NULL — no HTTP request exists.
 * SystemTokenProvider generates a fresh signed JWT with role=ADMIN. Downstream
 * services validate it with the same shared JWT_SECRET. Example: Message
 * consumer from RabbitMQ → notification-service
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignConfig {

    private final SystemTokenProvider systemTokenProvider;

    /**
     * Interceptor applied to ALL Feign clients configured with this class.
     * Automatically adds Authorization header with appropriate JWT token.
     */
    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return requestTemplate -> {
            // ── Case 1: Normal HTTP request — forward user's JWT ──────────────
            ServletRequestAttributes attrs
                    = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    log.debug("[FeignConfig] User JWT forwarded to: {}", requestTemplate.url());
                    return; // ← Done, user token sent
                }
            }

            // ── Case 2: No HTTP context — generate system token ────────────────
            // This happens when called from:
            //   - RabbitMQ message consumer
            //   - Scheduled jobs
            //   - Background tasks
            String systemToken = systemTokenProvider.generateSystemToken();
            requestTemplate.header("Authorization", "Bearer " + systemToken);
            log.debug("[FeignConfig] System token applied for: {}", requestTemplate.url());
        };
    }
}

package com.parkease.booking.config;

import com.parkease.booking.security.SystemTokenProvider;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Global Feign configuration — applied to ALL three Feign clients.
 *
 * JWT Forwarding Strategy:
 *
 *   Case 1 — Normal HTTP request (any API call from user):
 *     RequestContextHolder has a live HttpServletRequest.
 *     The user's incoming JWT is forwarded directly to downstream service.
 *     Works for: createBooking, checkIn, checkOut, cancel, extend, all GETs.
 *
 *   Case 2 — Scheduler thread (BookingExpiryScheduler every 5 min):
 *     RequestContextHolder returns NULL — no HTTP request exists.
 *     SystemTokenProvider generates a fresh signed JWT (1 hour, role=ADMIN).
 *     Downstream services validate it with the same shared JWT_SECRET.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FeignConfig {

    // Injected — used ONLY for scheduler fallback (Case 2)
    private final SystemTokenProvider systemTokenProvider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {

            // ── Case 1: Normal HTTP request — forward user's JWT ──────────────
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    return; // ← user JWT forwarded, done
                }
            }

            // ── Case 2: No HTTP context — scheduler thread ────────────────────
            // Generate a fresh short-lived system JWT using the shared secret.
            // This is the ONLY case this branch executes.
            String systemToken = systemTokenProvider.generateSystemToken();
            requestTemplate.header("Authorization", "Bearer " + systemToken);
            log.debug("[FeignConfig] Scheduler context detected — system token applied for: {}",
                    requestTemplate.url());
        };
    }
}
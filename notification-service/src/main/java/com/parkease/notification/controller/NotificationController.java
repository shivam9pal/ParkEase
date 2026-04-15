package com.parkease.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parkease.notification.dto.BroadcastNotificationRequest;
import com.parkease.notification.dto.NotificationResponse;
import com.parkease.notification.dto.UnreadCountResponse;
import com.parkease.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ──────────────────────────────────────────────────────
    // GET /api/v1/notifications/my
    // Role: DRIVER, MANAGER
    // Returns all APP notifications for the JWT user, newest first
    // ──────────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        log.info("GET /my — userId={}", userId);
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    // ──────────────────────────────────────────────────────
    // GET /api/v1/notifications/my/unread
    // Role: DRIVER, MANAGER
    // Returns unread APP notifications for the JWT user
    // ──────────────────────────────────────────────────────
    @GetMapping("/my/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        log.info("GET /my/unread — userId={}", userId);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    // ──────────────────────────────────────────────────────
    // GET /api/v1/notifications/my/unread/count
    // Role: DRIVER, MANAGER
    // Returns unread count for notification bell badge
    // ──────────────────────────────────────────────────────
    @GetMapping("/my/unread/count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        log.info("GET /my/unread/count — userId={}", userId);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/v1/notifications/{notificationId}/read
    // Role: DRIVER, MANAGER (own only)
    // Marks a single notification as read
    // ──────────────────────────────────────────────────────
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId,
            Authentication auth) {

        UUID userId = (UUID) auth.getPrincipal();
        log.info("PUT /{}/read — requesterId={}", notificationId, userId);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/v1/notifications/my/read-all
    // Role: DRIVER, MANAGER
    // Marks ALL APP notifications as read for JWT user
    // ──────────────────────────────────────────────────────
    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        log.info("PUT /my/read-all — userId={}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/v1/notifications/{notificationId}
    // Role: DRIVER (own only), ADMIN (any)
    // ──────────────────────────────────────────────────────
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication auth) {

        UUID userId = (UUID) auth.getPrincipal();
        String role = extractRole(auth);
        log.info("DELETE /{} — requesterId={}, role={}", notificationId, userId, role);
        notificationService.deleteNotification(notificationId, userId, role);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────
    // Helper method to extract role from authentication
    // ──────────────────────────────────────────────────────
    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DRIVER");
    }

    // ──────────────────────────────────────────────────────
    // GET /api/v1/notifications/all
    // Role: ADMIN only
    // Returns ALL notifications across ALL users and channels, newest first
    // ──────────────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("GET /all — admin request");
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    // ──────────────────────────────────────────────────────
    // POST /api/v1/notifications/broadcast
    // Role: ADMIN only
    // Sends a PROMO notification to all users of a target role
    // ──────────────────────────────────────────────────────
    @PostMapping("/broadcast")
    public ResponseEntity<Void> sendBroadcast(
            @Valid @RequestBody BroadcastNotificationRequest request) {

        log.info("POST /broadcast — targetRole={}, title='{}'",
                request.getTargetRole(), request.getTitle());
        notificationService.sendBroadcast(request);
        return ResponseEntity.accepted().build();   // 202 — broadcast may be async
    }
}

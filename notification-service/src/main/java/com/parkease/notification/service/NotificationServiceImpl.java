package com.parkease.notification.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.parkease.notification.dto.BroadcastNotificationRequest;
import com.parkease.notification.dto.NotificationResponse;
import com.parkease.notification.dto.UnreadCountResponse;
import com.parkease.notification.entity.Notification;
import com.parkease.notification.enums.NotificationChannel;
import com.parkease.notification.enums.NotificationType;
import com.parkease.notification.external.ResendEmailService;
import com.parkease.notification.external.TwilioSmsService;
import com.parkease.notification.feign.AuthServiceClient;
import com.parkease.notification.feign.dto.UserDetailDto;
import com.parkease.notification.rabbitmq.dto.BookingEventPayload;
import com.parkease.notification.rabbitmq.dto.PaymentEventPayload;
import com.parkease.notification.repository.NotificationRepository;
import com.parkease.notification.util.NotificationMessageBuilder;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ResendEmailService emailService;
    private final TwilioSmsService smsService;
    private final AuthServiceClient authServiceClient;
    private final NotificationMessageBuilder messageBuilder;

    // ══════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ══════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public void handleBookingEvent(BookingEventPayload payload, String routingKey) {
        NotificationType type = switch (routingKey) {
            case "booking.created" ->
                NotificationType.BOOKING_CREATED;
            case "booking.checkin" ->
                NotificationType.CHECKIN;
            case "booking.checkout" ->
                NotificationType.CHECKOUT;
            case "booking.cancelled" ->
                NotificationType.BOOKING_CANCELLED;
            case "booking.extended" ->
                NotificationType.BOOKING_EXTENDED;
            default -> {
                log.warn("Unknown booking routing key — skipping: {}", routingKey);
                yield null;
            }
        };

        if (type == null) {
            return;
        }

        String title = messageBuilder.buildBookingTitle(type);
        String message = messageBuilder.buildBookingMessage(type, payload);

        UserDetailDto user = safeGetUser(payload.getUserId());

        dispatchAll(type, payload.getUserId(), payload.getBookingId(),
                "BOOKING", title, message, user);
    }

    @Override
    @Transactional
    public void handlePaymentEvent(PaymentEventPayload payload, String routingKey) {
        NotificationType type = switch (routingKey) {
            case "payment.completed" ->
                NotificationType.PAYMENT_COMPLETED;
            case "payment.refunded" ->
                NotificationType.PAYMENT_REFUNDED;
            default -> {
                log.warn("Unknown payment routing key — skipping: {}", routingKey);
                yield null;
            }
        };

        if (type == null) {
            return;
        }

        String title = messageBuilder.buildPaymentTitle(type);
        String message = messageBuilder.buildPaymentMessage(type, payload);

        UserDetailDto user = safeGetUser(payload.getUserId());

        dispatchAll(type, payload.getUserId(), payload.getPaymentId(),
                "PAYMENT", title, message, user);
    }

    // ══════════════════════════════════════════════════════════════
    // CORE DISPATCH LOGIC
    // ══════════════════════════════════════════════════════════════
    private void dispatchAll(NotificationType type, UUID recipientId, UUID relatedId,
            String relatedType, String title, String message,
            UserDetailDto user) {

        // 1. Always save APP record first — in-app notification bell
        saveNotification(recipientId, type, title, message,
                NotificationChannel.APP, relatedId, relatedType);

        // 2. EMAIL — per channel routing matrix
        if (requiresEmail(type) && user != null && user.getEmail() != null) {
            emailService.sendEmail(user.getEmail(), title, message);
            saveNotification(recipientId, type, title, message,
                    NotificationChannel.EMAIL, relatedId, relatedType);
        }

        // 3. SMS — per channel routing matrix
        if (requiresSms(type) && user != null && user.getPhone() != null) {
            smsService.sendSms(user.getPhone(), buildSmsBody(title, message));
            saveNotification(recipientId, type, title, message,
                    NotificationChannel.SMS, relatedId, relatedType);
        }
    }

    private void saveNotification(UUID recipientId, NotificationType type,
            String title, String message,
            NotificationChannel channel,
            UUID relatedId, String relatedType) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .channel(channel)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .build();
        notificationRepository.save(notification);
        log.debug("Saved {} notification: type={}, recipient={}", channel, type, recipientId);
    }

    private boolean requiresEmail(NotificationType type) {
        return Set.of(
                NotificationType.BOOKING_CREATED,
                NotificationType.CHECKOUT,
                NotificationType.BOOKING_CANCELLED,
                NotificationType.PAYMENT_COMPLETED,
                NotificationType.PAYMENT_REFUNDED,
                NotificationType.PROMO
        ).contains(type);
    }

    private boolean requiresSms(NotificationType type) {
        return Set.of(
                NotificationType.BOOKING_CANCELLED,
                NotificationType.PAYMENT_REFUNDED
        ).contains(type);
    }

    private String buildSmsBody(String title, String message) {
        String full = "ParkEase: " + title + ". " + message;
        return full.length() > 160 ? full.substring(0, 157) + "..." : full;
    }

    // ══════════════════════════════════════════════════════════════
    // FAULT-TOLERANT AUTH FEIGN CALLS
    // ══════════════════════════════════════════════════════════════
    private UserDetailDto safeGetUser(UUID userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (FeignException.NotFound e) {
            log.warn("User not found in auth-service: userId={}", userId);
            return null;
        } catch (FeignException e) {
            log.error("Auth-service unavailable for userId={}: {}", userId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<UserDetailDto> safeGetUsersByRole(String role) {
        try {
            if ("ALL".equalsIgnoreCase(role)) {
                // Fetch DRIVER and MANAGER separately, combine
                List<UserDetailDto> drivers = authServiceClient.getUsersByRole("DRIVER");
                List<UserDetailDto> managers = authServiceClient.getUsersByRole("MANAGER");
                drivers.addAll(managers);
                return drivers;
            }
            return authServiceClient.getUsersByRole(role);
        } catch (FeignException e) {
            log.error("Auth-service unavailable while fetching users by role={}: {}", role, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching users by role={}: {}", role, e.getMessage());
            return List.of();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DRIVER / MANAGER REST API METHODS
    // ══════════════════════════════════════════════════════════════
    @Override
    public List<NotificationResponse> getMyNotifications(UUID recipientId) {
        return notificationRepository
                .findByRecipientIdAndChannelOrderBySentAtDesc(recipientId, NotificationChannel.APP)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(UUID recipientId) {
        return notificationRepository
                .findByRecipientIdAndChannelAndIsReadFalseOrderBySentAtDesc(recipientId, NotificationChannel.APP)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UnreadCountResponse getUnreadCount(UUID recipientId) {
        long count = notificationRepository
                .countByRecipientIdAndChannelAndIsReadFalse(recipientId, NotificationChannel.APP);
        return new UnreadCountResponse(recipientId, count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID requesterId) {
        Notification notification = notificationRepository
                .findByNotificationId(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Notification not found: " + notificationId));

        if (!notification.getRecipientId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only mark your own notifications as read.");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID recipientId) {
        int updated = notificationRepository.markAllAsReadForUser(recipientId);
        log.info("Marked {} APP notifications as read for recipientId={}", updated, recipientId);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID requesterId, String requesterRole) {
        Notification notification = notificationRepository
                .findByNotificationId(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Notification not found: " + notificationId));

        // DRIVER can only delete own; ADMIN can delete any
        if ("DRIVER".equals(requesterRole) && !notification.getRecipientId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete your own notifications.");
        }

        notificationRepository.deleteByNotificationId(notificationId);
        log.info("Deleted notification: notificationId={}, requestedBy={}, role={}", notificationId, requesterId, requesterRole);
    }

    // ══════════════════════════════════════════════════════════════
    // ADMIN REST API METHODS
    // ══════════════════════════════════════════════════════════════
    @Override
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderBySentAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendBroadcast(BroadcastNotificationRequest request) {
        List<UserDetailDto> recipients = safeGetUsersByRole(request.getTargetRole());

        if (recipients.isEmpty()) {
            log.warn("Broadcast aborted — no recipients found for role={}", request.getTargetRole());
            return;
        }

        for (UserDetailDto user : recipients) {
            dispatchAll(
                    NotificationType.PROMO,
                    user.getUserId(),
                    null,
                    null,
                    request.getTitle(),
                    request.getMessage(),
                    user
            );
        }

        log.info("Broadcast complete: title='{}', recipients={}, role={}",
                request.getTitle(), recipients.size(), request.getTargetRole());
    }

    // ══════════════════════════════════════════════════════════════
    // MAPPER
    // ══════════════════════════════════════════════════════════════
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .recipientId(n.getRecipientId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .channel(n.getChannel().name())
                .relatedId(n.getRelatedId())
                .relatedType(n.getRelatedType())
                .isRead(n.isRead())
                .sentAt(n.getSentAt())
                .build();
    }
}

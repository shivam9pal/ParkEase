package com.parkease.notification.service;

import java.util.List;
import java.util.UUID;

import com.parkease.notification.dto.BroadcastNotificationRequest;
import com.parkease.notification.dto.NotificationResponse;
import com.parkease.notification.dto.UnreadCountResponse;
import com.parkease.notification.rabbitmq.dto.BookingEventPayload;
import com.parkease.notification.rabbitmq.dto.PaymentEventPayload;

public interface NotificationService {

    // ─── Event Handlers (called from RabbitMQ consumers) ───
    void handleBookingEvent(BookingEventPayload payload, String routingKey);

    void handlePaymentEvent(PaymentEventPayload payload, String routingKey);

    // ─── Driver / Manager REST API ───
    List<NotificationResponse> getMyNotifications(UUID recipientId);

    List<NotificationResponse> getUnreadNotifications(UUID recipientId);

    UnreadCountResponse getUnreadCount(UUID recipientId);

    NotificationResponse markAsRead(UUID notificationId, UUID requesterId);

    void markAllAsRead(UUID recipientId);

    void deleteNotification(UUID notificationId, UUID requesterId, String requesterRole);

    // ─── Admin REST API ───
    List<NotificationResponse> getAllNotifications();

    void sendBroadcast(BroadcastNotificationRequest request);
}

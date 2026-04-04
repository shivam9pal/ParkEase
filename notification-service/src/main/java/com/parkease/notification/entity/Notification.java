package com.parkease.notification.entity;

import com.parkease.notification.enums.NotificationChannel;
import com.parkease.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;           // userId of the person receiving the notification

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;      // BOOKING_CREATED, CHECKIN, CHECKOUT, etc.

    @Column(name = "title", nullable = false, length = 200)
    private String title;               // Short subject line

    @Column(name = "message", nullable = false, length = 1000)
    private String message;             // Full notification body

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel; // APP, EMAIL, SMS

    @Column(name = "related_id")
    private UUID relatedId;             // bookingId or paymentId that triggered this

    @Column(name = "related_type", length = 20)
    private String relatedType;         // "BOOKING" or "PAYMENT"

    @Column(name = "is_read", nullable = false)
    private boolean isRead;             // Default false; only meaningful for APP channel

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
}
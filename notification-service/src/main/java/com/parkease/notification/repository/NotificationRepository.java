package com.parkease.notification.repository;

import com.parkease.notification.entity.Notification;
import com.parkease.notification.enums.NotificationChannel;
import com.parkease.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 1. All APP notifications for a user (notification inbox) — newest first
    List<Notification> findByRecipientIdAndChannelOrderBySentAtDesc(
            UUID recipientId, NotificationChannel channel);

    // 2. Unread APP notifications for a user
    List<Notification> findByRecipientIdAndChannelAndIsReadFalseOrderBySentAtDesc(
            UUID recipientId, NotificationChannel channel);

    // 3. Unread count — used for notification bell badge
    long countByRecipientIdAndChannelAndIsReadFalse(
            UUID recipientId, NotificationChannel channel);

    // 4. Find single notification by UUID (used for mark-as-read + delete)
    Optional<Notification> findByNotificationId(UUID notificationId);

    // 5. Find by type — analytics / diagnostics
    List<Notification> findByType(NotificationType type);

    // 6. Find by related entity — all notifications triggered by a booking or payment
    List<Notification> findByRelatedId(UUID relatedId);

    // 7. Delete by notification UUID
    void deleteByNotificationId(UUID notificationId);

    // 8. Bulk mark-as-read for a user's APP notifications
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.recipientId = :recipientId AND n.channel = 'APP' AND n.isRead = false")
    int markAllAsReadForUser(@Param("recipientId") UUID recipientId);

    // 9. All notifications — admin view, newest first
    List<Notification> findAllByOrderBySentAtDesc();
}
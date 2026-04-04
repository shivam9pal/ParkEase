package com.parkease.notification.enums;

public enum NotificationChannel {
    APP,    // Stored in DB; driver fetches via REST; has isRead state
    EMAIL,  // Sent via Resend API; stored in DB for audit
    SMS     // Sent via Twilio; stored in DB for audit
}
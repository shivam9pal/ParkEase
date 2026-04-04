package com.parkease.notification.util;

import com.parkease.notification.enums.NotificationType;
import com.parkease.notification.rabbitmq.dto.BookingEventPayload;
import com.parkease.notification.rabbitmq.dto.PaymentEventPayload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class NotificationMessageBuilder {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────
    // BOOKING MESSAGE BUILDERS
    // ─────────────────────────────────────────────

    public String buildBookingTitle(NotificationType type) {
        return switch (type) {
            case BOOKING_CREATED  -> "Booking Confirmed 🅿️";
            case CHECKIN          -> "Checked In ✅";
            case CHECKOUT         -> "Checkout Complete 🚗";
            case BOOKING_CANCELLED -> "Booking Cancelled ❌";
            case BOOKING_EXTENDED -> "Booking Extended ⏱️";
            default -> "ParkEase Notification";
        };
    }

    public String buildBookingMessage(NotificationType type, BookingEventPayload p) {
        return switch (type) {

            case BOOKING_CREATED -> String.format(
                    "Your spot has been reserved! Booking ID: %s. Vehicle: %s. " +
                            "Slot: %s to %s. Arrive and check in within the grace period.",
                    shorten(p.getBookingId()),
                    safe(p.getVehiclePlate()),
                    format(p.getStartTime()),
                    format(p.getEndTime())
            );

            case CHECKIN -> String.format(
                    "You have successfully checked in. Vehicle: %s. " +
                            "Check-in time: %s. Enjoy your parking!",
                    safe(p.getVehiclePlate()),
                    format(p.getCheckInTime())
            );

            case CHECKOUT -> String.format(
                    "You have checked out. Vehicle: %s. " +
                            "Duration: %s to %s. Total fare: ₹%s. Receipt available in the app.",
                    safe(p.getVehiclePlate()),
                    format(p.getCheckInTime()),
                    format(p.getCheckOutTime()),
                    p.getTotalAmount() != null ? p.getTotalAmount().toPlainString() : "N/A"
            );

            case BOOKING_CANCELLED -> String.format(
                    "Your booking %s has been cancelled. Vehicle: %s. " +
                            "If a payment was made, a refund will be processed shortly.",
                    shorten(p.getBookingId()),
                    safe(p.getVehiclePlate())
            );

            case BOOKING_EXTENDED -> String.format(
                    "Your parking has been extended. Vehicle: %s. New end time: %s.",
                    safe(p.getVehiclePlate()),
                    format(p.getEndTime())
            );

            default -> "Your booking status has been updated.";
        };
    }

    // ─────────────────────────────────────────────
    // PAYMENT MESSAGE BUILDERS
    // ─────────────────────────────────────────────

    public String buildPaymentTitle(NotificationType type) {
        return switch (type) {
            case PAYMENT_COMPLETED -> "Payment Successful 💳";
            case PAYMENT_REFUNDED  -> "Refund Processed 💰";
            default -> "ParkEase Payment Update";
        };
    }

    public String buildPaymentMessage(NotificationType type, PaymentEventPayload p) {
        return switch (type) {

            case PAYMENT_COMPLETED -> String.format(
                    "Payment of ₹%s received for booking %s. " +
                            "Mode: %s. Transaction ID: %s. Download your receipt from the app.",
                    p.getAmount() != null ? p.getAmount().toPlainString() : "N/A",
                    shorten(p.getBookingId()),
                    safe(p.getMode()),
                    safe(p.getTransactionId())
            );

            case PAYMENT_REFUNDED -> String.format(
                    "A refund of ₹%s has been processed for booking %s. " +
                            "It will be credited to your original payment method within 3-5 business days.",
                    p.getAmount() != null ? p.getAmount().toPlainString() : "N/A",
                    shorten(p.getBookingId())
            );

            default -> "Your payment status has been updated.";
        };
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private String format(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : "N/A";
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }

    /** Shows last 8 chars of UUID for readability in messages */
    private String shorten(java.util.UUID id) {
        if (id == null) return "N/A";
        String s = id.toString();
        return "..." + s.substring(s.length() - 8);
    }
}
package com.parkease.booking.messaging;

import com.parkease.booking.config.RabbitMQConfig;
import com.parkease.booking.dto.BookingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes booking lifecycle events to RabbitMQ.
 *
 * Design principles:
 *   1. FIRE-AND-FORGET — publishing failures are logged, never propagated.
 *      If RabbitMQ is down, the booking operation still succeeds.
 *      The try-catch in each method ensures RabbitMQ issues never cause
 *      a 500 response to the client.
 *
 *   2. BookingResponse is the event payload — it contains all fields
 *      that notification-service and analytics-service need:
 *      userId, lotId, spotId, vehiclePlate, status, totalAmount, etc.
 *
 *   3. Both queues (notification + analytics) receive every event via
 *      the "booking.*" wildcard binding defined in RabbitMQConfig.
 *      Each consumer service handles only what it needs.
 *
 * Called by BookingServiceImpl after every successful state transition.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    // ─── Event: Booking Created ───────────────────────────────────────────────

    /**
     * Published after a new booking is persisted to DB.
     * Triggers: booking confirmation notification + analytics occupancy log.
     *
     * Routing key: booking.created
     */
    public void publishBookingCreated(BookingResponse booking) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_CREATED_KEY,
                    booking
            );
            log.info("[RabbitMQ] Published BOOKING_CREATED for bookingId={}, userId={}",
                    booking.getBookingId(), booking.getUserId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish BOOKING_CREATED for bookingId={}: {}",
                    booking.getBookingId(), e.getMessage());
            // Do NOT rethrow — RabbitMQ failure must never fail the booking transaction
        }
    }

    // ─── Event: Check-In ──────────────────────────────────────────────────────

    /**
     * Published when a PRE_BOOKING transitions RESERVED → ACTIVE.
     * Triggers: "You have checked in" notification + real-time occupancy update.
     *
     * Routing key: booking.checkin
     */
    public void publishCheckIn(BookingResponse booking) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_CHECKIN_KEY,
                    booking
            );
            log.info("[RabbitMQ] Published BOOKING_CHECKIN for bookingId={}, spotId={}",
                    booking.getBookingId(), booking.getSpotId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish BOOKING_CHECKIN for bookingId={}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }

    // ─── Event: Check-Out ─────────────────────────────────────────────────────

    /**
     * Published when a booking transitions ACTIVE → COMPLETED.
     * Triggers:
     *   - notification-service: "Your total is ₹X, payment initiated"
     *   - payment-service: listens to this event to initiate payment (future)
     *   - analytics-service: logs revenue, session duration, lot utilization
     *
     * Routing key: booking.checkout
     * NOTE: totalAmount is populated in the payload at this point.
     */
    public void publishCheckOut(BookingResponse booking) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_CHECKOUT_KEY,
                    booking
            );
            log.info("[RabbitMQ] Published BOOKING_CHECKOUT for bookingId={}, totalAmount={}",
                    booking.getBookingId(), booking.getTotalAmount());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish BOOKING_CHECKOUT for bookingId={}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }

    // ─── Event: Cancellation ──────────────────────────────────────────────────

    /**
     * Published when a booking is cancelled — by driver, manager, admin,
     * or the auto-expiry scheduler.
     * Triggers: cancellation notification + analytics cancellation log.
     *
     * Routing key: booking.cancelled
     * NOTE: Called by both cancelBooking() AND autoExpireBookings() —
     *       the payload's status field will be CANCELLED in both cases.
     */
    public void publishCancellation(BookingResponse booking) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_CANCELLED_KEY,
                    booking
            );
            log.info("[RabbitMQ] Published BOOKING_CANCELLED for bookingId={}, userId={}",
                    booking.getBookingId(), booking.getUserId());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish BOOKING_CANCELLED for bookingId={}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }

    // ─── Event: Extension ─────────────────────────────────────────────────────

    /**
     * Published when a driver extends their booking's endTime.
     * Triggers: "Your booking has been extended to HH:MM" notification.
     *
     * Routing key: booking.extended
     * NOTE: payload contains the updated endTime for the notification.
     */
    public void publishExtension(BookingResponse booking) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BOOKING_EXCHANGE,
                    RabbitMQConfig.BOOKING_EXTENDED_KEY,
                    booking
            );
            log.info("[RabbitMQ] Published BOOKING_EXTENDED for bookingId={}, newEndTime={}",
                    booking.getBookingId(), booking.getEndTime());
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish BOOKING_EXTENDED for bookingId={}: {}",
                    booking.getBookingId(), e.getMessage());
        }
    }
}
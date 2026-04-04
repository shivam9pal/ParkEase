package com.parkease.notification.rabbitmq;

import com.parkease.notification.config.RabbitMQConfig;
import com.parkease.notification.rabbitmq.dto.BookingEventPayload;
import com.parkease.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_BOOKING_QUEUE)
    public void handleBookingEvent(
            BookingEventPayload payload,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("Received booking event: routingKey={}, bookingId={}, status={}",
                routingKey, payload.getBookingId(), payload.getStatus());

        try {
            notificationService.handleBookingEvent(payload, routingKey);
        } catch (Exception e) {
            // NEVER rethrow — prevents infinite RabbitMQ requeue loop
            log.error("Failed to process booking notification for bookingId={}, routingKey={}: {}",
                    payload.getBookingId(), routingKey, e.getMessage(), e);
        }
    }
}
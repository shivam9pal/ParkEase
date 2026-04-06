package com.parkease.analytics.rabbitmq;

import com.parkease.analytics.rabbitmq.dto.BookingEventPayload;
import com.parkease.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final AnalyticsService analyticsService;

    @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
    public void handleBookingEvent(
            BookingEventPayload payload,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("Analytics received event: routingKey={}, bookingId={}, lotId={}",
                routingKey, payload.getBookingId(), payload.getLotId());

        try {
            analyticsService.processBookingEvent(payload, routingKey);
        } catch (Exception e) {
            // ⚠️ NEVER rethrow — rethrowing causes infinite RabbitMQ requeue loop
            // Log and swallow; let the next event proceed normally
            log.error("Failed to process analytics event for bookingId={}: {}",
                    payload.getBookingId(), e.getMessage(), e);
        }
    }
}
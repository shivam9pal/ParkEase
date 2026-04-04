package com.parkease.payment.rabbitmq;

import com.parkease.payment.rabbitmq.dto.BookingEventPayload;
import com.parkease.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void handleBookingEvent(BookingEventPayload payload) {
        log.info("Received booking event: bookingId={}, status={}", payload.getBookingId(), payload.getStatus());
        try {
            if ("COMPLETED".equals(payload.getStatus())) {
                paymentService.processPaymentFromEvent(payload);
            } else if ("CANCELLED".equals(payload.getStatus())) {
                paymentService.processRefundFromEvent(payload);
            } else {
                log.debug("Ignoring booking event with status={}", payload.getStatus());
            }
        } catch (Exception e) {
            // Never rethrow — prevents infinite RabbitMQ requeue loop
            log.error("Failed to process booking event for bookingId={}: {}",
                    payload.getBookingId(), e.getMessage(), e);
        }
    }
}
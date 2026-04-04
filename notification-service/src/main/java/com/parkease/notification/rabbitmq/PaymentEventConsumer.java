package com.parkease.notification.rabbitmq;

import com.parkease.notification.config.RabbitMQConfig;
import com.parkease.notification.rabbitmq.dto.PaymentEventPayload;
import com.parkease.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PAYMENT_QUEUE)
    public void handlePaymentEvent(
            PaymentEventPayload payload,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("Received payment event: routingKey={}, paymentId={}, status={}",
                routingKey, payload.getPaymentId(), payload.getStatus());

        try {
            notificationService.handlePaymentEvent(payload, routingKey);
        } catch (Exception e) {
            // NEVER rethrow — prevents infinite RabbitMQ requeue loop
            log.error("Failed to process payment notification for paymentId={}, routingKey={}: {}",
                    payload.getPaymentId(), routingKey, e.getMessage(), e);
        }
    }
}
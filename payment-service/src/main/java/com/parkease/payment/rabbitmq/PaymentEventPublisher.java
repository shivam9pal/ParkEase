package com.parkease.payment.rabbitmq;

import com.parkease.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(PaymentResponse payload) {
        publishEvent("payment.completed", payload);
    }

    public void publishPaymentRefunded(PaymentResponse payload) {
        publishEvent("payment.refunded", payload);
    }

    private void publishEvent(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, payload);
            log.info("Published {} event", routingKey);
        } catch (Exception e) {
            // Fire-and-forget: never fail the payment for a messaging error
            log.error("Failed to publish {} event: {}", routingKey, e.getMessage());
        }
    }
}
package com.parkease.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Booking Exchange (declared by booking-service; redeclare idempotently) ───
    public static final String BOOKING_EXCHANGE            = "parkease.booking.exchange";
    public static final String NOTIFICATION_BOOKING_QUEUE  = "parkease.notification.queue";
    public static final String BOOKING_ROUTING_PATTERN     = "booking.*";

    // ─── Payment Exchange (declared by payment-service; redeclare idempotently) ───
    public static final String PAYMENT_EXCHANGE            = "parkease.payment.exchange";
    public static final String NOTIFICATION_PAYMENT_QUEUE  = "parkease.payment.notification.queue";
    public static final String PAYMENT_ROUTING_PATTERN     = "payment.*";

    // ⚠️ Redeclaring with IDENTICAL params (durable=true, autoDelete=false) is idempotent.
    // If params differ → RabbitMQ throws 406 PRECONDITION_FAILED → service won't start.
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationBookingQueue() {
        return QueueBuilder.durable(NOTIFICATION_BOOKING_QUEUE).build();
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_QUEUE).build();
    }

    @Bean
    public Binding notificationBookingBinding() {
        return BindingBuilder
                .bind(notificationBookingQueue())
                .to(bookingExchange())
                .with(BOOKING_ROUTING_PATTERN);
    }

    @Bean
    public Binding notificationPaymentBinding() {
        return BindingBuilder
                .bind(notificationPaymentQueue())
                .to(paymentExchange())
                .with(PAYMENT_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
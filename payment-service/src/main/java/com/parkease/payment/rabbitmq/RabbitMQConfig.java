package com.parkease.payment.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE        = "parkease.booking.exchange";
    public static final String PAYMENT_QUEUE           = "parkease.payment.queue";
    public static final String BOOKING_ROUTING_PATTERN = "booking.*";

    public static final String PAYMENT_EXCHANGE              = "parkease.payment.exchange";
    public static final String NOTIFICATION_PAYMENT_QUEUE   = "parkease.payment.notification.queue";
    public static final String ANALYTICS_PAYMENT_QUEUE      = "parkease.payment.analytics.queue";
    public static final String PAYMENT_ROUTING_PATTERN      = "payment.*";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public Binding paymentQueueBinding() {
        return BindingBuilder.bind(paymentQueue()).to(bookingExchange()).with(BOOKING_ROUTING_PATTERN);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return QueueBuilder.durable(NOTIFICATION_PAYMENT_QUEUE).build();
    }

    @Bean
    public Queue analyticsPaymentQueue() {
        return QueueBuilder.durable(ANALYTICS_PAYMENT_QUEUE).build();
    }

    @Bean
    public Binding notifPaymentBinding() {
        return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_PATTERN);
    }

    @Bean
    public Binding analyticsPaymentBinding() {
        return BindingBuilder.bind(analyticsPaymentQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter());
        return t;
    }
}
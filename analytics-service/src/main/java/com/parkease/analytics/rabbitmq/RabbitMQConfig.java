package com.parkease.analytics.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Consume from booking-service exchange ONLY ───
    public static final String BOOKING_EXCHANGE         = "parkease.booking.exchange";
    public static final String ANALYTICS_QUEUE          = "parkease.analytics.queue";
    public static final String BOOKING_ROUTING_PATTERN  = "booking.*";

    // ⚠️ CRITICAL: analyticsQueue is ALREADY declared in booking-service.
    // These params MUST match exactly (durable=true, autoDelete=false).
    // Any mismatch → RabbitMQ throws 406 PRECONDITION_FAILED → service won't start.

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Binding analyticsBinding() {
        return BindingBuilder
                .bind(analyticsQueue())
                .to(bookingExchange())
                .with(BOOKING_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
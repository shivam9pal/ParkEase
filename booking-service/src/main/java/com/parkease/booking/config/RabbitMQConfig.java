package com.parkease.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for booking-service.
 *
 * Architecture: Topic Exchange (1) → Queues (2)
 *
 *   parkease.booking.exchange  (TopicExchange)
 *          │
 *          ├── booking.*  ──► parkease.notification.queue  (consumed by notification-service :8087)
 *          └── booking.*  ──► parkease.analytics.queue     (consumed by analytics-service :8088)
 *
 * Both queues receive ALL booking events via wildcard "booking.*" binding.
 * Each downstream service filters and processes only the events it cares about.
 *
 * Routing keys published by BookingEventPublisher:
 *   booking.created   → new booking created (PRE or WALK_IN)
 *   booking.checkin   → PRE_BOOKING checked in (RESERVED → ACTIVE)
 *   booking.checkout  → booking completed, fare calculated
 *   booking.cancelled → booking cancelled by driver/manager/admin/system
 *   booking.extended  → booking endTime extended
 *
 * Fire-and-forget: if notification-service or analytics-service is down,
 * the message is held in the durable queue until they recover.
 * The booking operation itself NEVER fails due to RabbitMQ issues.
 */
@Configuration
public class RabbitMQConfig {

    // ─── Exchange ─────────────────────────────────────────────────────────────

    public static final String BOOKING_EXCHANGE = "parkease.booking.exchange";

    // ─── Queues ───────────────────────────────────────────────────────────────

    public static final String NOTIFICATION_QUEUE = "parkease.notification.queue";
    public static final String ANALYTICS_QUEUE    = "parkease.analytics.queue";

    // ─── Routing Keys ─────────────────────────────────────────────────────────

    public static final String BOOKING_CREATED_KEY   = "booking.created";
    public static final String BOOKING_CHECKIN_KEY   = "booking.checkin";
    public static final String BOOKING_CHECKOUT_KEY  = "booking.checkout";
    public static final String BOOKING_CANCELLED_KEY = "booking.cancelled";
    public static final String BOOKING_EXTENDED_KEY  = "booking.extended";

    // ─── Exchange Bean ────────────────────────────────────────────────────────

    /**
     * TopicExchange allows wildcard routing ("booking.*").
     * durable = true (default) — survives RabbitMQ restarts.
     */
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    // ─── Queue Beans ──────────────────────────────────────────────────────────

    /**
     * Durable queue — messages survive broker restarts.
     * Consumed by notification-service to send email/SMS/push alerts.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    /**
     * Durable queue — messages survive broker restarts.
     * Consumed by analytics-service to log occupancy and revenue metrics.
     */
    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    // ─── Binding Beans ────────────────────────────────────────────────────────

    /**
     * Binds notification queue to ALL booking events.
     * Pattern "booking.*" matches:
     *   booking.created, booking.checkin, booking.checkout,
     *   booking.cancelled, booking.extended
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(bookingExchange)
                .with("booking.*");
    }

    /**
     * Binds analytics queue to ALL booking events.
     * Same wildcard — analytics-service receives every lifecycle event.
     */
    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(analyticsQueue)
                .to(bookingExchange)
                .with("booking.*");
    }

    // ─── Message Converter ────────────────────────────────────────────────────

    /**
     * Serialize/deserialize messages as JSON using Jackson.
     * Without this, Spring AMQP uses Java serialization by default —
     * which breaks cross-service message consumption (different JVM classpaths).
     *
     * With this: BookingResponse is serialized to JSON bytes in the queue,
     * and any consumer (notification-service, analytics-service) can
     * deserialize it into their own local mirror DTO.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ─── RabbitTemplate ──────────────────────────────────────────────────────

    /**
     * Overrides the default RabbitTemplate to use our Jackson message converter.
     * This bean is used by BookingEventPublisher to publish all events.
     *
     * Mandatory — if not overridden here, RabbitTemplate uses the default
     * SimpleMessageConverter (Java serialization), ignoring our messageConverter bean.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
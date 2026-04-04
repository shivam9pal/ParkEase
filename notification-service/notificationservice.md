# ParkEase Notification Microservice - Complete Documentation

**Version:** 0.0.1-SNAPSHOT  
**Framework:** Spring Boot 3.5.13  
**Java Version:** 17  
**Spring Cloud:** 2025.0.0  
**Port:** 8087  
**Package:** `com.parkease.notification`

---

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture & Core Components](#architecture--core-components)
3. [Dependencies & Versions](#dependencies--versions)  
4. [Configuration](#configuration)
5. [Database Design](#database-design)
6. [REST API Specification](#rest-api-specification)
7. [Event-Driven Architecture (RabbitMQ)](#event-driven-architecture-rabbitmq)
8. [Notification Routing Logic](#notification-routing-logic)
9. [External Services Integration](#external-services-integration)
10. [Security & Authentication](#security--authentication)
11. [Data Models & DTOs](#data-models--dtos)
12. [Repository & Database Queries](#repository--database-queries)
13. [Message Building & Formatting](#message-building--formatting)
14. [Error Handling](#error-handling)
15. [File Structure](#file-structure)
16. [Key Design Patterns](#key-design-patterns)

---

## 📱 Project Overview

**Purpose:**  
The Notification Service is a microservice responsible for:
- Receiving real-time events from **booking-service** and **payment-service** via RabbitMQ
- Creating **multi-channel notifications** (APP, EMAIL, SMS) based on business events
- Providing **REST APIs** for users to manage their notifications (fetch, mark as read, delete)
- Enabling **admin broadcast** notifications to specific user roles
- Maintaining an **audit trail** of all notifications in the database

**Core Responsibilities:**
- Event consumption from RabbitMQ topic exchanges
- Notification persistence to PostgreSQL
- Multi-channel dispatch (in-app storage + email + SMS)
- Fault-tolerant integration with auth-service
- JWT-based security for all REST endpoints
- Clean separation between APP (user-readable) and EMAIL/SMS (external channels)

---

## 🏗️ Architecture & Core Components

### Layered Architecture

```
┌─────────────────────────────────────────────┐
│         REST API Layer                      │
│  (NotificationController)                   │
├─────────────────────────────────────────────┤
│         Service Layer                       │
│  (NotificationService/NotificationServiceImpl) │
├─────────────────────────────────────────────┤
│         Data Access Layer                   │
│  (NotificationRepository)                   │
├─────────────────────────────────────────────┤
│         Database (PostgreSQL)                  │
│  (Notification entity)                      │
└─────────────────────────────────────────────┘

    ┌──────────────────────────────┐
    │  Event Listeners (RabbitMQ)  │
    │  - BookingEventConsumer      │
    │  - PaymentEventConsumer      │
    └──────────────────────────────┘
    │         │
    └─────────┴─→ NotificationService
```

### Core Components

| Component | Purpose | Key Responsibility |
|-----------|---------|-------------------|
| **NotificationController** | REST API gateway | Routes all HTTP requests to service layer |
| **NotificationService** (interface) | Business logic contract | Defines public API methods |
| **NotificationServiceImpl** | Business logic implementation | Handles all notification processing |
| **BookingEventConsumer** | RabbitMQ listener | Consumes `booking.*` events |
| **PaymentEventConsumer** | RabbitMQ listener | Consumes `payment.*` events |
| **NotificationRepository** | Data access | JPA queries for Notification entity |
| **ResendEmailService** | Email dispatch | Sends emails via Resend API |
| **TwilioSmsService** | SMS dispatch | Sends SMS via Twilio API |
| **AuthServiceClient** | Feign client | Fetches user details (email, phone, role) from auth-service |
| **JwtUtil** | JWT validation | Extracts claims (userId, role, email) from tokens |
| **JwtAuthFilter** | Security filter | Validates JWT and sets Spring Security context |
| **SystemTokenProvider** | System JWT generation | Creates internal tokens for Feign calls from RabbitMQ threads |
| **NotificationMessageBuilder** | Message formatting | Builds human-readable notification titles/bodies |

---

## 📦 Dependencies & Versions

### Spring Boot Starters
```xml
<!-- Core Web Framework -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Security (JWT validation, role-based access) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Data Persistence (JPA/Hibernate) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Input Validation (@Valid annotations) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Monitoring & Health Checks -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Message Broker Integration (RabbitMQ) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### Spring Cloud & Microservices
```xml
<!-- Service Discovery & Load Balancing (via spring-cloud-dependencies 2025.0.0) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### Database Driver
```xml
<!-- PostgreSQL JDBC Driver (runtime scope) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### JWT (CRITICAL: Unified across all services)
```xml
<!-- JJWT 0.11.5 — compatible with auth-service, booking-service, payment-service -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### External Services
```xml
<!-- Email Delivery via Resend -->
<dependency>
    <groupId>com.resend</groupId>
    <artifactId>resend-java</artifactId>
    <version>3.1.0</version>
</dependency>

<!-- SMS Delivery via Twilio -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>10.1.0</version>
</dependency>
```

### Utilities & Documentation
```xml
<!-- Reduced Boilerplate (Getters, Setters, Builders, Logging) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Swagger/OpenAPI Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>

<!-- Testing Framework -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Build Plugins:**
- `spring-boot-maven-plugin`: Builds executable JAR with embedded Tomcat

---

## ⚙️ Configuration

### Environment Variables (Required)
```yaml
# Database
DB_USER: PostgreSQL username
DB_PASSWORD: PostgreSQL password

# RabbitMQ
RABBITMQ_HOST: RabbitMQ server address (default: localhost)
RABBITMQ_PORT: RabbitMQ port (default: 5672)
RABBITMQ_USER: RabbitMQ username (default: guest)
RABBITMQ_PASSWORD: RabbitMQ password (default: guest)

# JWT (CRITICAL: MUST match auth-service byte-for-byte)
JWT_SECRET: Base64-encoded HMAC SHA-256 key

# External auth-service
AUTH_SERVICE_URL: URL of auth-service (default: http://localhost:8081)

# Email (Resend)
RESEND_API_KEY: Resend API key for email delivery
RESEND_FROM_EMAIL: Sender email address (default: reply@smartmeeter.online)
RESEND_FROM_NAME: Sender display name (default: ParkEase)

# SMS (Twilio)
TWILIO_ACCOUNT_SID: Twilio Account SID
TWILIO_AUTH_TOKEN: Twilio Auth Token
TWILIO_PHONE_NUMBER: From phone number (E.164 format: +1234567890)
TWILIO_ENABLED: Enable SMS delivery (default: true) — set false in dev to avoid charges
```

### application.yaml Configuration
```yaml
server:
  port: 8087

spring:
  application:
    name: notification-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/parkease_notification
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update  # ⚠️ NEVER use create-drop — notification history is permanent audit data
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /

jwt:
  secret: ${JWT_SECRET}       # MUST match auth-service byte-for-byte
  expiry: 86400000            # 24 hours in milliseconds

services:
  auth:
    url: ${AUTH_SERVICE_URL:http://localhost:8081}

resend:
  api-key: ${RESEND_API_KEY}
  from-email: ${RESEND_FROM_EMAIL:reply@smartmeeter.online}
  from-name: ${RESEND_FROM_NAME:ParkEase}

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN}
  phone-number: ${TWILIO_PHONE_NUMBER}
  enabled: ${TWILIO_ENABLED:true}  # Set false in dev to avoid SMS charges

management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## 🗄️ Database Design

### Notification Table
```sql
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL,                    -- User ID from auth-service
    type VARCHAR(30) NOT NULL,                     -- Enum: BOOKING_CREATED, CHECKIN, CHECKOUT, etc.
    title VARCHAR(200) NOT NULL,                   -- Subject line
    message VARCHAR(1000) NOT NULL,                -- Full body text
    channel VARCHAR(10) NOT NULL,                  -- Enum: APP, EMAIL, SMS
    related_id UUID,                               -- bookingId or paymentId that triggered this
    related_type VARCHAR(20),                      -- "BOOKING" or "PAYMENT"
    is_read BOOLEAN NOT NULL DEFAULT false,        -- Only meaningful for APP channel
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- Immutable creation time
    
    -- Indexes for fast queries
    INDEX idx_recipient_channel (recipient_id, channel),
    INDEX idx_recipient_channel_read (recipient_id, channel, is_read),
    INDEX idx_related_id (related_id)
);
```

### Entity Class
```java
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;
    
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;                       // userId of the person receiving the notification
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;                 // BOOKING_CREATED, CHECKIN, CHECKOUT, etc.
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;                          // Short subject line
    
    @Column(name = "message", nullable = false, length = 1000)
    private String message;                        // Full notification body
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;           // APP, EMAIL, SMS
    
    @Column(name = "related_id")
    private UUID relatedId;                        // bookingId or paymentId that triggered this
    
    @Column(name = "related_type", length = 20)
    private String relatedType;                    // "BOOKING" or "PAYMENT"
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead;                        // Default false; only meaningful for APP channel
    
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;                  // Immutable creation timestamp
    
    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }
}
```

### Key Design Notes
- **UUID Primary Keys:** Generated by database for consistency
- **sent_at Immutable:** Set at creation; never updated to maintain audit trail integrity
- **channel Column:** Separates APP (in-app notifications) from EMAIL/SMS (external dispatch)
- **isRead Flag:** Only meaningful for APP notifications (users read inbox items)
- **relatedId & relatedType:** Track which booking/payment triggered the notification for analytics
- **No Foreign Keys:** Loosely coupled — references are IDs only, allowing independent deletion
- **Indexes Optimized:** For typical query patterns (by recipient, by channel, by read status)

---

## 🔌 REST API Specification

### Base URL
`http://localhost:8087/api/v1/notifications`

### Authentication
All endpoints (except public paths) require JWT Bearer token:
```
Authorization: Bearer <jwt_token>
```

**Token Content:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "DRIVER",
  "email": "user@example.com",
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

### 1. GET /my (Fetch My Notifications)
**Role:** DRIVER, MANAGER  
**Purpose:** Retrieve all APP notifications for the authenticated user (newest first)

**Request:**
```http
GET /api/v1/notifications/my
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "notificationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "recipientId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "BOOKING_CREATED",
    "title": "Booking Confirmed 🅿️",
    "message": "Your spot has been reserved! Booking ID: ...abc123. Vehicle: DL01AB1234...",
    "channel": "APP",
    "relatedId": "a47ac10b-58cc-4372-a567-0e02b2c3e475",
    "relatedType": "BOOKING",
    "isRead": false,
    "sentAt": "2026-04-04T14:25:33"
  },
  ...
]
```

### 2. GET /my/unread (Fetch Unread Notifications)
**Role:** DRIVER, MANAGER  
**Purpose:** Retrieve only unread APP notifications for the authenticated user

**Request:**
```http
GET /api/v1/notifications/my/unread
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "notificationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "recipientId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "BOOKING_CREATED",
    "title": "Booking Confirmed 🅿️",
    "message": "...",
    "channel": "APP",
    "relatedId": "a47ac10b-58cc-4372-a567-0e02b2c3e475",
    "relatedType": "BOOKING",
    "isRead": false,
    "sentAt": "2026-04-04T14:25:33"
  }
]
```

### 3. GET /my/unread/count (Fetch Unread Count)
**Role:** DRIVER, MANAGER  
**Purpose:** Get unread notification count for notification bell badge

**Request:**
```http
GET /api/v1/notifications/my/unread/count
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440000",
  "count": 3
}
```

### 4. PUT /{notificationId}/read (Mark Single as Read)
**Role:** DRIVER, MANAGER (must own the notification)  
**Purpose:** Mark a specific notification as read

**Request:**
```http
PUT /api/v1/notifications/f47ac10b-58cc-4372-a567-0e02b2c3d479/read
Authorization: Bearer <jwt_token>
```

**Response:** `200 OK`
```json
{
  "notificationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "recipientId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "BOOKING_CREATED",
  "title": "Booking Confirmed 🅿️",
  "message": "...",
  "channel": "APP",
  "relatedId": "a47ac10b-58cc-4372-a567-0e02b2c3e475",
  "relatedType": "BOOKING",
  "isRead": true,
  "sentAt": "2026-04-04T14:25:33"
}
```

**Error Responses:**
- `404 NOT_FOUND`: Notification does not exist
- `403 FORBIDDEN`: Notification belongs to another user

### 5. PUT /my/read-all (Mark All as Read)
**Role:** DRIVER, MANAGER  
**Purpose:** Mark ALL APP notifications as read for current user

**Request:**
```http
PUT /api/v1/notifications/my/read-all
Authorization: Bearer <jwt_token>
```

**Response:** `204 No Content`

### 6. DELETE /{notificationId} (Delete Single Notification)
**Role:** DRIVER (own only), ADMIN (any)  
**Purpose:** Delete a notification (for user cleanup or admin management)

**Request:**
```http
DELETE /api/v1/notifications/f47ac10b-58cc-4372-a567-0e02b2c3d479
Authorization: Bearer <jwt_token>
```

**Response:** `204 No Content`

**Error Responses:**
- `404 NOT_FOUND`: Notification does not exist
- `403 FORBIDDEN`: DRIVER trying to delete another user's notification

### 7. GET /all (Admin: Fetch All Notifications)
**Role:** ADMIN only  
**Purpose:** Fetch ALL notifications across ALL users and channels (for auditing/analytics)

**Request:**
```http
GET /api/v1/notifications/all
Authorization: Bearer <jwt_admin_token>
```

**Response:** `200 OK`
```json
[
  {
    "notificationId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "recipientId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "BOOKING_CREATED",
    "title": "Booking Confirmed 🅿️",
    "message": "...",
    "channel": "APP",
    "relatedId": "a47ac10b-58cc-4372-a567-0e02b2c3e475",
    "relatedType": "BOOKING",
    "isRead": false,
    "sentAt": "2026-04-04T14:25:33"
  },
  ...
]
```

### 8. POST /broadcast (Admin: Send Broadcast)
**Role:** ADMIN only  
**Purpose:** Send a PROMO notification to all users of a specific role

**Request:**
```http
POST /api/v1/notifications/broadcast
Authorization: Bearer <jwt_admin_token>
Content-Type: application/json

{
  "targetRole": "DRIVER",
  "title": "New Feature Available!",
  "message": "We've added vehicle type filters to help you find parking faster."
}
```

**Request Body Validation:**
- `targetRole`: Required. Valid values: `DRIVER`, `MANAGER`, `ALL`
- `title`: Required. Max 200 characters
- `message`: Required. Max 1000 characters

**Response:** `202 Accepted`
```json
{}
```
*(Broadcast processing may be asynchronous)*

**Error Responses:**
- `400 BAD_REQUEST`: Missing or invalid fields
- `403 FORBIDDEN`: User is not ADMIN

---

## 📡 Event-Driven Architecture (RabbitMQ)

### RabbitMQ Configuration

```java
public class RabbitMQConfig {
    
    // ─── Booking Exchange & Queues ───
    public static final String BOOKING_EXCHANGE = "parkease.booking.exchange";
    public static final String NOTIFICATION_BOOKING_QUEUE = "parkease.notification.queue";
    public static final String BOOKING_ROUTING_PATTERN = "booking.*";
    
    // ─── Payment Exchange & Queues ───
    public static final String PAYMENT_EXCHANGE = "parkease.payment.exchange";
    public static final String NOTIFICATION_PAYMENT_QUEUE = "parkease.payment.notification.queue";
    public static final String PAYMENT_ROUTING_PATTERN = "payment.*";
}
```

### Booking Events (booking.*)
**Routing keys consumed:**
- `booking.created` → Booking confirmed
- `booking.checkin` → Driver checked in to parking spot
- `booking.checkout` → Driver checked out, fare calculated
- `booking.cancelled` → Booking cancelled
- `booking.extended` → Duration extended

**Payload (from booking-service):**
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "lotId": "770e8400-e29b-41d4-a716-446655440002",
  "spotId": "880e8400-e29b-41d4-a716-446655440003",
  "vehicleId": "990e8400-e29b-41d4-a716-446655440004",
  "vehiclePlate": "DL01AB1234",
  "vehicleType": "FOUR_WHEELER",
  "bookingType": "PRE_BOOKING",
  "status": "ACTIVE",
  "totalAmount": 120.50,
  "pricePerHour": 30.00,
  "startTime": "2026-04-04T14:00:00",
  "endTime": "2026-04-04T16:00:00",
  "checkInTime": "2026-04-04T14:05:00",
  "checkOutTime": "2026-04-04T15:55:00",
  "createdAt": "2026-04-04T13:50:00"
}
```

### Payment Events (payment.*)
**Routing keys consumed:**
- `payment.completed` → Payment processed successfully
- `payment.refunded` → Refund issued

**Payload (from payment-service):**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440100",
  "bookingId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "lotId": "770e8400-e29b-41d4-a716-446655440002",
  "amount": 120.50,
  "status": "PAID",
  "mode": "CARD",
  "transactionId": "TXN102030405",
  "currency": "INR",
  "paidAt": "2026-04-04T14:02:00",
  "refundedAt": null,
  "description": "Parking fee for Lot: Sector 5 Central",
  "createdAt": "2026-04-04T14:01:00"
}
```

### Message Queue Declaration
As part of RabbitMQ configuration, the notification-service **redeclares exchanges and queues idempotently:**

```java
@Bean
public TopicExchange bookingExchange() {
    // Declared by booking-service; we redeclare with identical params
    return new TopicExchange(BOOKING_EXCHANGE, true, false);
}

@Bean
public Queue notificationBookingQueue() {
    return QueueBuilder.durable(NOTIFICATION_BOOKING_QUEUE).build();
}

@Bean
public Binding notificationBookingBinding() {
    return BindingBuilder
            .bind(notificationBookingQueue())
            .to(bookingExchange())
            .with(BOOKING_ROUTING_PATTERN);
}
```

**⚠️ Critical:** If declaration parameters differ → RabbitMQ throws `406 PRECONDITION_FAILED` → service won't start.

### Event Consumers

**BookingEventConsumer:**
```java
@Component
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_BOOKING_QUEUE)
public void handleBookingEvent(
        BookingEventPayload payload,
        @Header("amqp_receivedRoutingKey") String routingKey) {
    
    log.info("Received booking event: routingKey={}, bookingId={}", routingKey, payload.getBookingId());
    
    try {
        notificationService.handleBookingEvent(payload, routingKey);
    } catch (Exception e) {
        // NEVER rethrow — prevents infinite RabbitMQ requeue loop
        log.error("Failed to process booking notification: {}", e.getMessage());
    }
}
```

**PaymentEventConsumer:**
```java
@Component
@RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PAYMENT_QUEUE)
public void handlePaymentEvent(
        PaymentEventPayload payload,
        @Header("amqp_receivedRoutingKey") String routingKey) {
    
    log.info("Received payment event: routingKey={}, paymentId={}", routingKey, payload.getPaymentId());
    
    try {
        notificationService.handlePaymentEvent(payload, routingKey);
    } catch (Exception e) {
        // NEVER rethrow — prevents infinite RabbitMQ requeue loop
        log.error("Failed to process payment notification: {}", e.getMessage());
    }
}
```

---

## 📤 Notification Routing Logic

### Channel Dispatch Matrix

| Notification Type | APP | EMAIL | SMS |
|-------------------|-----|-------|-----|
| BOOKING_CREATED | ✅ | ✅ | ❌ |
| CHECKIN | ✅ | ❌ | ❌ |
| CHECKOUT | ✅ | ✅ | ❌ |
| BOOKING_CANCELLED | ✅ | ✅ | ✅ |
| BOOKING_EXTENDED | ✅ | ❌ | ❌ |
| PAYMENT_COMPLETED | ✅ | ✅ | ❌ |
| PAYMENT_REFUNDED | ✅ | ✅ | ✅ |
| PROMO | ✅ | ✅ | ❌ |

### Dispatch Flow (dispatchAll Method)

```java
private void dispatchAll(NotificationType type, UUID recipientId, UUID relatedId,
                         String relatedType, String title, String message,
                         UserDetailDto user) {
    
    // STEP 1: Always save APP record first (in-app notification bell)
    saveNotification(recipientId, type, title, message,
            NotificationChannel.APP, relatedId, relatedType);
    
    // STEP 2: EMAIL — if routing matrix says yes AND user has email
    if (requiresEmail(type) && user != null && user.getEmail() != null) {
        emailService.sendEmail(user.getEmail(), title, message);
        saveNotification(recipientId, type, title, message,
                NotificationChannel.EMAIL, relatedId, relatedType);
    }
    
    // STEP 3: SMS — if routing matrix says yes AND user has phone
    if (requiresSms(type) && user != null && user.getPhone() != null) {
        smsService.sendSms(user.getPhone(), buildSmsBody(title, message));
        saveNotification(recipientId, type, title, message,
                NotificationChannel.SMS, relatedId, relatedType);
    }
}
```

### Key Design Principles
- **APP Always Saved:** Core record for in-app notification bell — always persisted
- **Graceful Fallback:** If email/SMS fails, APP notification still exists
- **Per-User Preferences:** Email and SMS only sent if user has email/phone on file
- **SMS Body Truncation:** Limited to 160 characters; longer messages truncated with "..."
- **Routing Matrix:** Centralized logic in `requiresEmail()` and `requiresSms()` methods

---

## 🔗 External Services Integration

### Email Service (Resend)

**Configuration:**
```yaml
resend:
  api-key: ${RESEND_API_KEY}                         # API key for authentication
  from-email: ${RESEND_FROM_EMAIL:reply@smartmeeter.online}
  from-name: ${RESEND_FROM_NAME:ParkEase}
```

**Implementation (ResendEmailService):**
```java
@Service
public void sendEmail(String toEmail, String subject, String body) {
    try {
        Resend resend = new Resend(apiKey);
        
        CreateEmailOptions emailRequest = CreateEmailOptions.builder()
                .from(fromName + " <" + fromEmail + ">")
                .to(List.of(toEmail))
                .subject(subject)
                .html(buildHtmlEmail(subject, body))  // Styled HTML template
                .build();
        
        CreateEmailResponse response = resend.emails().send(emailRequest);
        log.info("Email sent via Resend: id={}, to={}", response.getId(), toEmail);
        
    } catch (ResendException e) {
        // Log + swallow — email failure must NOT fail the notification DB save
        log.error("Failed to send email: {}", e.getMessage());
    }
}
```

**HTML Email Template:**
```html
<!DOCTYPE html>
<html>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
    <div style="background-color: #1a73e8; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
        <h1 style="margin: 0; font-size: 24px;">🅿️ ParkEase</h1>
        <p style="margin: 5px 0 0; opacity: 0.9;">Smart Parking Management</p>
    </div>
    <div style="background: #ffffff; border: 1px solid #e0e0e0; padding: 30px; border-radius: 0 0 8px 8px;">
        <h2 style="color: #1a73e8;">{{ subject }}</h2>
        <p style="color: #333; line-height: 1.6;">{{ body }}</p>
        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
        <p style="color: #888; font-size: 12px;">This is an automated message from ParkEase. Please do not reply.</p>
    </div>
</body>
</html>
```

### SMS Service (Twilio)

**Configuration:**
```yaml
twilio:
  account-sid: ${TWILIO_ACCOUNT_SID}
  auth-token: ${TWILIO_AUTH_TOKEN}
  phone-number: ${TWILIO_PHONE_NUMBER}              # E.164 format: +1234567890
  enabled: ${TWILIO_ENABLED:true}                   # Set false in dev to avoid charges
```

**Implementation (TwilioSmsService):**
```java
@Service
public class TwilioSmsService {
    
    @PostConstruct
    public void init() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized. From: {}", fromPhone);
        } else {
            log.warn("Twilio SMS is DISABLED. SMS will be logged only.");
        }
    }
    
    public void sendSms(String toPhone, String body) {
        if (!enabled) {
            log.info("[SMS STUB] To: {} | Body: {}", toPhone, body);
            return;
        }
        
        if (toPhone == null || toPhone.isBlank()) {
            log.warn("Skipping SMS — recipient phone is null or blank");
            return;
        }
        
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhone),      // E.164 format: +91XXXXXXXXXX
                    new PhoneNumber(fromPhone),
                    body
            ).create();
            
            log.info("SMS sent via Twilio: sid={}, to={}", message.getSid(), toPhone);
            
        } catch (ApiException e) {
            // Log + swallow — SMS failure must NOT fail the notification DB save
            log.error("Failed to send SMS: {}", e.getMessage());
        }
    }
}
```

### Auth Service Integration (Feign)

**Purpose:** Fetch user contact details (email, phone, fullName, role) for notification dispatch

**Feign Client:**
```java
@FeignClient(
        name = "auth-service",
        url = "${services.auth.url}",
        configuration = FeignConfig.class
)
public interface AuthServiceClient {
    
    @GetMapping("/api/v1/auth/users/{userId}")
    UserDetailDto getUserById(@PathVariable("userId") UUID userId);
    
    @GetMapping("/api/v1/auth/users")
    List<UserDetailDto> getUsersByRole(@RequestParam("role") String role);
}
```

**Protected Endpoints:**
- `GET /api/v1/auth/users/{userId}` — Fetch single user
- `GET /api/v1/auth/users?role=DRIVER` — Fetch all users by role

**UserDetailDto:**
```java
@Data
public class UserDetailDto {
    private UUID userId;
    private String fullName;
    private String email;                   // Used for EMAIL channel dispatch
    private String phone;                   // Used for SMS channel; may be null
    private String role;                    // DRIVER, MANAGER, ADMIN
    private boolean isActive;
}
```

**Fault-Tolerant Feign Calls:**
```java
private UserDetailDto safeGetUser(UUID userId) {
    try {
        return authServiceClient.getUserById(userId);
    } catch (FeignException.NotFound e) {
        log.warn("User not found in auth-service: userId={}", userId);
        return null;
    } catch (FeignException e) {
        log.error("Auth-service unavailable: {}", e.getMessage());
        return null;
    } catch (Exception e) {
        log.error("Unexpected error fetching user: {}", e.getMessage());
        return null;
    }
}
```

---

## 🔐 Security & Authentication

### JWT Configuration

**CRITICAL:** JWT_SECRET must be **Base64-encoded and identical across ALL services** (auth-service, booking-service, payment-service, notification-service).

**Token Structure (JJWT 0.11.5):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "DRIVER",
  "sub": "user@example.com",
  "iat": 1680000000,
  "exp": 1680086400
}
```

**JWT Validation (JwtUtil):**
```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).get("userId", String.class));
    }
    
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### JWT Authentication Filter (JwtAuthFilter)

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        
        try {
            if (jwtUtil.validateToken(token)) {
                UUID userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                
                List<SimpleGrantedAuthority> authorities = 
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, email, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### Security Configuration (SecurityConfig)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // User endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/notifications/my").hasAnyRole("DRIVER", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/v1/notifications/my/unread").hasAnyRole("DRIVER", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/v1/notifications/my/unread/count").hasAnyRole("DRIVER", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/notifications/*/read").hasAnyRole("DRIVER", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/notifications/my/read-all").hasAnyRole("DRIVER", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/notifications/*").hasAnyRole("DRIVER", "ADMIN")
                
                // Admin endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/notifications/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications/broadcast").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### System JWT Token Provider (for RabbitMQ threads)

**Purpose:** RabbitMQ consumer threads have no HTTP context / RequestContextHolder. This provider generates system-level JWT tokens for Feign calls to auth-service.

```java
@Component
public class SystemTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    public String generateSystemToken() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        
        return Jwts.builder()
                .setSubject("system@parkease.internal")
                .claim("userId", "00000000-0000-0000-0000-000000000000")  // Null UUID
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))  // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
```

### Feign JWT Forwarding (FeignConfig)

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor jwtForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attrs != null) {
                // Normal HTTP thread — forward the incoming Bearer token
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    return;
                }
            }
            
            // RabbitMQ consumer thread — no HTTP context, use system JWT
            String systemToken = systemTokenProvider.generateSystemToken();
            requestTemplate.header("Authorization", "Bearer " + systemToken);
        };
    }
}
```

---

## 📊 Data Models & DTOs

### NotificationResponse DTO
```java
@Data
@Builder
public class NotificationResponse {
    private UUID notificationId;
    private UUID recipientId;
    private String type;          // BOOKING_CREATED, CHECKIN, etc.
    private String title;
    private String message;
    private String channel;       // APP, EMAIL, SMS
    private UUID relatedId;
    private String relatedType;   // BOOKING, PAYMENT
    private boolean isRead;
    private LocalDateTime sentAt;
}
```

### UnreadCountResponse DTO
```java
@Data
@Builder
public class UnreadCountResponse {
    private UUID recipientId;
    private long count;
}
```

### BroadcastNotificationRequest DTO
```java
@Data
public class BroadcastNotificationRequest {
    
    @NotBlank(message = "targetRole is required. Values: DRIVER, MANAGER, ALL")
    private String targetRole;
    
    @NotBlank(message = "title is required.")
    @Size(max = 200)
    private String title;
    
    @NotBlank(message = "message is required.")
    @Size(max = 1000)
    private String message;
}
```

### BookingEventPayload DTO
```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingEventPayload {
    
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private UUID spotId;
    private UUID vehicleId;
    private String vehiclePlate;
    private String vehicleType;       // TWO_WHEELER, FOUR_WHEELER, HEAVY
    private String bookingType;       // PRE_BOOKING, WALK_IN
    private String status;            // RESERVED, ACTIVE, COMPLETED, CANCELLED
    private BigDecimal totalAmount;   // null until COMPLETED
    private BigDecimal pricePerHour;
    private LocalDateTime startTime;
    private LocalDateTime endTime;    // Updated value for BOOKING_EXTENDED events
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime createdAt;
}
```

### PaymentEventPayload DTO
```java
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEventPayload {
    
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private UUID lotId;
    private BigDecimal amount;
    private String status;            // PAID, REFUNDED
    private String mode;              // CARD, UPI, WALLET, CASH
    private String transactionId;
    private String currency;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String description;
    private LocalDateTime createdAt;
}
```

### Enums

**NotificationType:**
```java
public enum NotificationType {
    BOOKING_CREATED,    // New booking confirmed (PRE_BOOKING or WALK_IN)
    CHECKIN,            // Driver checked in to spot
    CHECKOUT,           // Driver checked out, fare calculated
    BOOKING_CANCELLED,  // Booking cancelled (by driver, manager, admin, or auto-expiry)
    BOOKING_EXTENDED,   // Booking duration extended
    PAYMENT_COMPLETED,  // Payment processed successfully
    PAYMENT_REFUNDED,   // Refund issued
    PROMO               // Admin broadcast / promotional message
}
```

**NotificationChannel:**
```java
public enum NotificationChannel {
    APP,    // Stored in DB; driver fetches via REST; has isRead state
    EMAIL,  // Sent via Resend API; stored in DB for audit
    SMS     // Sent via Twilio; stored in DB for audit
}
```

---

## 🗂️ Repository & Database Queries

### NotificationRepository Interface

```java
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    // 1. All APP notifications for a user (notification inbox) — newest first
    List<Notification> findByRecipientIdAndChannelOrderBySentAtDesc(
            UUID recipientId, NotificationChannel channel);
    
    // 2. Unread APP notifications for a user
    List<Notification> findByRecipientIdAndChannelAndIsReadFalseOrderBySentAtDesc(
            UUID recipientId, NotificationChannel channel);
    
    // 3. Unread count — used for notification bell badge
    long countByRecipientIdAndChannelAndIsReadFalse(
            UUID recipientId, NotificationChannel channel);
    
    // 4. Find single notification by UUID (used for mark-as-read + delete)
    Optional<Notification> findByNotificationId(UUID notificationId);
    
    // 5. Find by type — analytics / diagnostics
    List<Notification> findByType(NotificationType type);
    
    // 6. Find by related entity — all notifications triggered by a booking or payment
    List<Notification> findByRelatedId(UUID relatedId);
    
    // 7. Delete by notification UUID
    void deleteByNotificationId(UUID notificationId);
    
    // 8. Bulk mark-as-read for a user's APP notifications
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.recipientId = :recipientId AND n.channel = 'APP' AND n.isRead = false")
    int markAllAsReadForUser(@Param("recipientId") UUID recipientId);
    
    // 9. All notifications — admin view, newest first
    List<Notification> findAllByOrderBySentAtDesc();
}
```

### Query Patterns

| Use Case | Query | Purpose |
|----------|-------|---------|
| User inbox | `findByRecipientIdAndChannelOrderBySentAtDesc(userId, APP)` | Get all notifications for a user |
| Unread badge | `countByRecipientIdAndChannelAndIsReadFalse(userId, APP)` | Count for notification bell |
| Mark single | `findByNotificationId(notificationId)` | Fetch + update + save |
| Mark all | `markAllAsReadForUser(userId)` | Bulk update query |
| Admin audit | `findAllByOrderBySentAtDesc()` | Global notification log |
| Analytics | `findByType(BOOKING_CREATED)` | Count by notification type |
| Tracing | `findByRelatedId(bookingId)` | Find all notifications for a booking |

---

## 📝 Message Building & Formatting

### NotificationMessageBuilder Component

Builds human-readable titles and messages for each notification type.

**Date/Time Formatting:**
```java
private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
```

**Booking Messages:**

| Type | Title | Message |
|------|-------|---------|
| BOOKING_CREATED | "Booking Confirmed 🅿️" | Spot reserved; vehicle plate; time range; grace period reminder |
| CHECKIN | "Checked In ✅" | Vehicle confirmed; check-in time |
| CHECKOUT | "Checkout Complete 🚗" | Duration; check-in/out times; total fare |
| BOOKING_CANCELLED | "Booking Cancelled ❌" | Booking ID; vehicle; refund notice |
| BOOKING_EXTENDED | "Booking Extended ⏱️" | Vehicle; new end time |

**Payment Messages:**

| Type | Title | Message |
|------|-------|---------|
| PAYMENT_COMPLETED | "Payment Successful 💳" | Amount; booking ID; payment mode; transaction ID; receipt reminder |
| PAYMENT_REFUNDED | "Refund Processed 💰" | Amount; booking ID; 3-5 day credit timeline |

**UUID Shortening:**
```java
private String shorten(UUID id) {
    if (id == null) return "N/A";
    String s = id.toString();
    return "..." + s.substring(s.length() - 8);  // Last 8 chars for readability
}
```

**SMS Body Truncation (160 chars):**
```java
private String buildSmsBody(String title, String message) {
    String full = "ParkEase: " + title + ". " + message;
    return full.length() > 160 ? full.substring(0, 157) + "..." : full;
}
```

---

## ⚠️ Error Handling

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Validation errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", 400,
            "error", "Validation Failed",
            "fieldErrors", fieldErrors
        ));
    }
    
    // ResponseStatusException (404, 403, etc.)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex) {
        
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", ex.getStatusCode().value(),
            "error", ex.getReason()
        ));
    }
    
    // Catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", 500,
            "error", "Internal server error"
        ));
    }
}
```

### Common Errors

| Status | Scenario | Message |
|--------|----------|---------|
| 400 | Invalid broadcast request | `"targetRole is required. Values: DRIVER, MANAGER, ALL"` |
| 400 | Missing title/message | `"title is required."` or `"message is required."` |
| 403 | User trying to delete another user's notification | `"You can only delete your own notifications."` |
| 403 | Non-ADMIN calling broadcast | Implicit (SecurityConfig denies access) |
| 404 | Notification not found | `"Notification not found: <id>"` |
| 500 | Unhandled exception | `"Internal server error"` |

### Fault Tolerance Patterns

**Email Failure (does NOT block):**
```java
try {
    emailService.sendEmail(toEmail, title, message);
} catch (ResendException e) {
    log.error("Failed to send email: {}", e.getMessage());
    // Continue — APP notification already saved
}
```

**SMS Failure (does NOT block):**
```java
try {
    smsService.sendSms(phone, body);
} catch (ApiException e) {
    log.error("Failed to send SMS: {}", e.getMessage());
    // Continue — APP notification already saved
}
```

**Auth Service Unavailable (does NOT block):**
```java
try {
    return authServiceClient.getUserById(userId);
} catch (FeignException e) {
    log.error("Auth-service unavailable: {}", e.getMessage());
    return null;  // Continue with null user — APP notification still saved
}
```

**RabbitMQ Event Processing:**
```java
try {
    notificationService.handleBookingEvent(payload, routingKey);
} catch (Exception e) {
    // NEVER rethrow — prevents infinite RabbitMQ requeue loop
    log.error("Failed to process: {}", e.getMessage());
    // Message silently dropped after logging
}
```

---

## 📂 File Structure

```
notification-service/
├── pom.xml                                    # Maven configuration
├── mvnw / mvnw.cmd                           # Maven wrapper scripts
├── notificationservice.md                    # This documentation
│
├── src/main/
│   ├── java/com/parkease/notification/
│   │   ├── NotificationApplication.java              # Spring Boot entry point
│   │   │
│   │   ├── config/
│   │   │   ├── RabbitMQConfig.java                   # RabbitMQ exchanges, queues, bindings
│   │   │   └── SecurityConfig.java                   # Spring Security JWT configuration
│   │   │
│   │   ├── controller/
│   │   │   └── NotificationController.java           # REST API endpoints
│   │   │
│   │   ├── service/
│   │   │   ├── NotificationService.java              # Service interface
│   │   │   └── NotificationServiceImpl.java           # Business logic implementation
│   │   │
│   │   ├── dto/
│   │   │   ├── NotificationResponse.java             # API response DTO
│   │   │   ├── UnreadCountResponse.java              # Unread count DTO
│   │   │   └── BroadcastNotificationRequest.java     # Broadcast request DTO
│   │   │
│   │   ├── entity/
│   │   │   └── Notification.java                     # JPA entity (DB table)
│   │   │
│   │   ├── enums/
│   │   │   ├── NotificationType.java                 # 8 notification types
│   │   │   └── NotificationChannel.java              # APP, EMAIL, SMS
│   │   │
│   │   ├── repository/
│   │   │   └── NotificationRepository.java           # JPA repository with custom queries
│   │   │
│   │   ├── rabbitmq/
│   │   │   ├── BookingEventConsumer.java             # @RabbitListener for booking.*
│   │   │   ├── PaymentEventConsumer.java             # @RabbitListener for payment.*
│   │   │   └── dto/
│   │   │       ├── BookingEventPayload.java          # Booking event structure
│   │   │       └── PaymentEventPayload.java          # Payment event structure
│   │   │
│   │   ├── feign/
│   │   │   ├── AuthServiceClient.java                # OpenFeign client to auth-service
│   │   │   ├── FeignConfig.java                      # JWT forwarding interceptor
│   │   │   └── dto/
│   │   │       └── UserDetailDto.java                # User data from auth-service
│   │   │
│   │   ├── security/
│   │   │   ├── JwtUtil.java                          # Token validation & claims extraction
│   │   │   ├── JwtAuthFilter.java                    # OncePerRequestFilter for JWT
│   │   │   └── SystemTokenProvider.java              # System JWT for RabbitMQ threads
│   │   │
│   │   ├── util/
│   │   │   └── NotificationMessageBuilder.java       # Message title/body builders
│   │   │
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java           # Centralized error handling
│   │   │
│   │   └── ...other files
│   │
│   └── resources/
│       ├── application.yaml                          # Spring Boot configuration
│       ├── static/                                   # Static assets (if any)
│       └── templates/                                # Thymeleaf templates (if any)
│
├── src/test/
│   └── java/com/parkease/notification/
│       └── NotificationApplicationTests.java         # Basic context load test
│
└── target/                                           # Build output directory
    ├── classes/                                      # Compiled .class files
    ├── generated-sources/                            # Generated code (if any)
    └── ...other build artifacts
```

---

## 🎯 Key Design Patterns

### 1. **Fault-Tolerant Event Processing**
```text
RabbitMQ Message → Consumer Thread → Service Method
                                        ↓
                                   APP record saved ✅
                                        ↓
                                   Try: Send Email
                                   Catch: Log error (but don't rethrow)
                                        ↓
                                   Try: Send SMS
                                   Catch: Log error (but don't rethrow)

Result: Email/SMS failures don't block APP notification or cause requeue loops
```

### 2. **Multi-Channel Notification with Routing Matrix**
```text
Event arrives → Check routing matrix → Determine channels (APP, EMAIL, SMS)
                    ↓
                 Save APP record (always)
                    ↓
                 If requires EMAIL: Fetch user email → Send via Resend → Log record
                    ↓
                 If requires SMS: Fetch user phone → Send via Twilio → Log record
```

### 3. **Stateless & Idempotent Event Consumers**
```text
Consumer receives event for bookingId=123 twice (RabbitMQ replay)
    ↓
Both invocations create identical notification records
    ↓
User sees one notification (one record per channel per recipient per event)
    ↓
Safe for at-least-once delivery guarantee
```

### 4. **Context-Aware JWT Management**
```text
Request from REST client → RequestContextHolder available → Forward user's JWT
                                                                    ↓
Event from RabbitMQ consumer → No HTTP context → Generate system JWT (role=ADMIN)
                                    ↓
Feign calls always have valid JWT, regardless of thread context
```

### 5. **Separation of Concerns (3-Layer Architecture)**
```text
Controller    → Request/Response handling, route mapping, role-based access
Service       → Business logic, event handling, multi-channel dispatch
Repository    → Data access, queries, persistence (JPA abstraction)
```

### 6. **Open/Closed for Extension**
```text
NotificationMessageBuilder → Add new notification type?
    → Define new NotificationType enum value
    → Add buildTitle + buildMessage switch cases
    → No change to dispatch logic or database schema
```

---

## 🚀 Building & Running

### Maven Build
```bash
# Clean, compile, test, package
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run specific test class
mvn test -Dtest=NotificationApplicationTests
```

### Running the Service
```bash
# As Spring Boot application (from IDE)
mvn spring-boot:run

# From JAR (after mvn package)
java -jar target/notification-0.0.1-SNAPSHOT.jar

# With environment variables
DB_USER=postgres DB_PASSWORD=secret \
RABBITMQ_HOST=rabbitmq.example.com \
JWT_SECRET=<base64-secret> \
AUTH_SERVICE_URL=http://auth-service:8081 \
RESEND_API_KEY=<key> \
TWILIO_ACCOUNT_SID=<sid> \
TWILIO_AUTH_TOKEN=<token> \
TWILIO_PHONE_NUMBER=+1234567890 \
java -jar target/notification-0.0.1-SNAPSHOT.jar
```

### Docker Build (if Dockerfile exists)
```bash
# Build image
docker build -t parkease-notification:0.0.1 .

# Run container
docker run -d --name notification-service \
  -p 8087:8087 \
  -e DB_USER=postgres \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=<base64-secret> \
  parkease-notification:0.0.1
```

### Health Check
```bash
curl -s http://localhost:8087/actuator/health | jq
# Response: { "status": "UP", ... }

curl -s http://localhost:8087/actuator/info | jq
```

### Swagger API Documentation
```
http://localhost:8087/swagger-ui.html
http://localhost:8087/v3/api-docs
```

---

## 📌 Important Implementation Notes

### 1. **JWT Secret Unity (CRITICAL)**
All services must use identical Base64-encoded JWT_SECRET. If even one byte differs:
- Token validation fails
- Feign calls to auth-service rejected
- System tokens not recognized
- Service startup failure

### 2. **RabbitMQ Declarative Idempotency**
Exchanges and queues declared with identical parameters → Safe to redeclare.  
If parameters differ (durable, autoDelete, etc.) → RabbitMQ returns `406 PRECONDITION_FAILED`.

### 3. **Email/SMS Failure Does NOT Block Notification**
Primary goal: Always save APP notification. Email/SMS are secondary channels.  
If Resend or Twilio unavailable → Log error but continue → APP notification exists.

### 4. **Never Rethrow from RabbitMQ Consumers**
If exception is rethrown → RabbitMQ may requeue message → Infinite loop.  
Always catch, log, and suppress gracefully.

### 5. **Pagination & Performance**
Current queries load all notifications into memory (for small deployments).  
For production with millions of notifications → Add pagination (Pageable, Page<>).

### 6. **Notification History is Permanent**
`hibernat ddl-auto: update` ensures notification records never auto-deleted.  
To remove old records → Manual DELETE query or scheduled cleanup job.

### 7. **Auth Service Fallback**
If auth-service unavailable → No user email/phone → Only APP notification sent.  
System doesn't crash; graceful degradation.

### 8. **Admin Broadcast Async Pattern**
POST /broadcast returns `202 Accepted` (not 200 OK) → Implies async processing.  
Current implementation is synchronous but design allows future async refactor.

---

## 🔑 Summary Table

| Aspect | Details |
|--------|---------|
| **Framework** | Spring Boot 3.5.13, Spring Cloud 2025.0.0 |
| **Database** | PostgreSQL, JPA/Hibernate |
| **Message Broker** | RabbitMQ (topic exchanges, durable queues) |
| **Security** | JWT (JJWT 0.11.5), Spring Security, role-based access |
| **External Services** | Resend (email), Twilio (SMS), Auth Service (user data) |
| **Notification Types** | 8 types (BOOKING_CREATED, CHECKOUT, PAYMENT_COMPLETED, PROMO, etc.) |
| **Channels** | 3 channels (APP, EMAIL, SMS) with routing matrix |
| **REST Endpoints** | 8 endpoints (fetch, mark read, delete, broadcast) |
| **Event Sources** | 2 sources (booking-service, payment-service) |
| **Event Types** | 7 event types from RabbitMQ |
| **Port** | 8087 |
| **Build Tool** | Maven |
| **Java Version** | 17 |

---

**END OF DOCUMENTATION**


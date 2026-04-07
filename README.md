# ParkEase - Smart Parking Management System

A comprehensive microservices-based parking management platform that enables users to discover, reserve, and pay for parking spots in real-time.

## Project Overview

ParkEase is built on a microservices architecture with a Spring Boot ecosystem, providing a scalable, event-driven parking management solution. The system handles everything from vehicle registration through payment processing and notifications.

## Microservices Architecture

### Core Services

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Spring Cloud Gateway that routes requests to appropriate microservices using load-balanced service discovery |
| **Auth Service** | 8081 | Handles user authentication with OAuth2 (Google, GitHub integration) and JWT token issuance |
| **Parking Lot Service** | 8082 | Manages parking lot metadata including location, capacity, operating hours, and pricing rates |
| **Spot Service** | 8083 | Manages individual parking spot availability, pricing, and real-time status with Swagger API |
| **Booking Service** | 8084 | Core orchestration service for parking reservations with auto-expiry scheduling and inter-service communication |
| **Payment Service** | 8085 | Processes parking payments, generates receipts, and validates transactions via RabbitMQ events |
| **Vehicle Service** | 8086 | Manages vehicle registration and information with security controls |
| **Notification Service** | 8087 | Sends email notifications (Resend API) and SMS (Twilio) triggered by RabbitMQ events |
| **Analytics Service** | 8088 | Analyzes parking occupancy trends and generates peak-hours insights with scheduled snapshots |

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| **Eureka Server** | 8761 | Service registry and discovery enabling dynamic service-to-service communication |

## Technology Stack

- **Framework**: Spring Boot 3.x with Spring Cloud
- **Database**: PostgreSQL
- **Message Queue**: RabbitMQ (event-driven async communication)
- **Service Discovery**: Eureka
- **API Gateway**: Spring Cloud Gateway
- **Inter-service Communication**: OpenFeign (REST), RabbitMQ (Events)
- **Security**: JWT, OAuth2
- **External Integrations**: 
  - Resend (Email notifications)
  - Twilio (SMS notifications)
  - OAuth2 Providers (Google, GitHub)

## Architecture Highlights

- **Event-Driven**: RabbitMQ enables asynchronous communication for booking, payment, and notification events
- **Service Discovery**: Eureka prevents hardcoded service URLs and enables dynamic routing
- **Scalable**: Microservices architecture allows independent scaling of services based on demand
- **Secure**: JWT-based authentication across all services with OAuth2 integration
- **Reliable**: Database per service pattern with PostgreSQL for data persistence

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL
- RabbitMQ
- Docker (optional, for containerized deployment)


Or all services can be started together depending on your deployment setup.

## Project Structure

```
ParkEase/
├── analytics-service/          # Parking analytics and reporting
├── apigateway/                 # Request routing and load balancing
├── auth-service/               # User authentication and authorization
├── booking-service/            # Parking spot reservations
├── eurekaserver/              # Service discovery registry
├── notification-service/       # Email and SMS notifications
├── parkinglot-service/        # Parking lot management
├── payment-service/           # Payment processing
├── spot-service/              # Parking spot management
└── vehicle-service/           # Vehicle registration and management
```

## Service Communication

- **Synchronous**: OpenFeign REST calls for immediate inter-service requests (e.g., booking service checking spot availability)
- **Asynchronous**: RabbitMQ events for loose coupling (e.g., payment completion triggers notification)

## Key Features

✅ Real-time parking spot availability
✅ Seamless booking and reservation system
✅ Secure payment processing
✅ Multi-channel notifications (Email, SMS)
✅ Vehicle management and registration
✅ Parking analytics and insights
✅ OAuth2 authentication
✅ Rate limiting and load balancing


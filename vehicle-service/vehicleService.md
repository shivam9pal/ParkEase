# ParkEase Vehicle Service - Complete Documentation

---

## 📋 Project Overview

**Vehicle Service** is a microservice within the **ParkEase** platform that manages vehicle registration, lookup, and vehicle metadata (type, EV status) for drivers. It provides RESTful APIs for driver vehicle management and integrates seamlessly with the **booking-service** for parking spot assignments.

### Key Responsibilities
- Vehicle registration and management for authenticated drivers
- Vehicle type and EV status tracking for spot-type matching
- License plate uniqueness enforcement (per owner)
- Soft-delete vehicle records (never hard-deleted)
- Integration with **auth-service** for JWT validation
- Integration with **booking-service** for spot assignment compatibility

---

## 🏗️ Architecture Overview

### Microservice Stack
- **Framework:** Spring Boot 3.5.13
- **Java Version:** 17
- **Port:** 8086
- **Database:** PostgreSQL
- **Authentication:** JWT (Bearer tokens issued by auth-service)
- **API Documentation:** Swagger/OpenAPI 3.0

### Key Architecture Principles
1. **Stateless Service** - No session management; purely token-based auth
2. **UUID-based Identifiers** - All entities use UUIDs
3. **DTO Layer** - Entities never exposed directly; always through DTOs
4. **Soft Deletes** - Records are marked inactive, never hard-deleted
5. **Cross-service ownerId Reference** - Uses UUID reference (no JPA joins across services)
6. **Single Responsibility** - Focused only on vehicle data; auth handled by auth-service

---

## 🛠️ Technology Stack

### Dependencies
| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot Starter Web | 3.5.13 | REST API framework |
| Spring Boot Starter Security | 3.5.13 | Security configuration & JWT support |
| Spring Boot Starter Data-JPA | 3.5.13 | ORM and database abstraction |
| Spring Boot Starter Validation | 3.5.13 | @Valid input validation |
| Spring Boot Starter Actuator | 3.5.13 | Health checks & metrics |
| PostgreSQL Driver | Latest | Database connectivity |
| JJWT (JWT Library) | 0.11.5 | JWT parsing & validation |
| springdoc-openapi | 2.8.5 | Swagger/OpenAPI documentation |
| Lombok | Latest | Code generation (getters, builders) |

### Build Tool
- **Maven** with Spring Boot parent POM
- **Plugins:** Spring Boot Maven Plugin (build, run)
- **Java Compiler:** 17

---

## 📦 Project Structure

```
vehicle-service/
├── mvnw / mvnw.cmd           (Maven wrapper for CLI)
├── pom.xml                   (Maven config + dependencies)
├── vehicleService.md         (This documentation)
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/parkease/vehicle/
    │   │       ├── VehicleApplication.java          (Spring Boot entry point)
    │   │       ├── config/
    │   │       │   ├── SecurityConfig.java          (JWT + CORS setup)
    │   │       │   └── OpenApiConfig.java           (Swagger configuration)
    │   │       ├── dto/
    │   │       │   ├── RegisterVehicleRequest.java  (Request DTO)
    │   │       │   ├── UpdateVehicleRequest.java    (Update DTO)
    │   │       │   └── VehicleResponse.java         (Response DTO)
    │   │       ├── entity/
    │   │       │   ├── Vehicle.java                 (ORM entity)
    │   │       │   └── VehicleType.java             (Enum: 2W, 4W, Heavy)
    │   │       ├── exception/
    │   │       │   ├── GlobalExceptionHandler.java  (Centralized error handling)
    │   │       │   └── ApiError.java                (Error response DTO)
    │   │       ├── repository/
    │   │       │   └── VehicleRepository.java       (Data access layer)
    │   │       ├── resource/
    │   │       │   └── VehicleResource.java         (REST controller)
    │   │       ├── security/
    │   │       │   ├── JwtUtil.java                 (JWT validation utils)
    │   │       │   └── JwtAuthFilter.java           (JWT auth filter)
    │   │       └── service/
    │   │           ├── VehicleService.java          (Business interface)
    │   │           └── VehicleServiceImpl.java       (Business logic)
    │   └── resources/
    │       ├── application.yaml                     (Config file)
    │       ├── static/                              (CORS headers, templates)
    │       └── templates/
    └── test/
        └── java/.../VehicleApplicationTests.java    (Unit tests)
```

---

## 🗄️ Database Schema

### Table: `vehicles`

| Column | Type | Nullable | Unique | Default | Notes |
|--------|------|----------|--------|---------|-------|
| `vehicle_id` | UUID | NO | PK | UUID generated | Primary key |
| `owner_id` | UUID | NO | NO | - | References auth-service user (FK style) |
| `license_plate` | VARCHAR(20) | NO | UK | - | Unique per owner (UK constraint with owner_id) |
| `make` | VARCHAR(50) | NO | NO | - | Vehicle manufacturer (e.g., Toyota) |
| `model` | VARCHAR(50) | NO | NO | - | Vehicle model (e.g., Innova) |
| `color` | VARCHAR(30) | YES | NO | - | Vehicle color |
| `vehicle_type` | ENUM | NO | NO | - | Enum: TWO_WHEELER, FOUR_WHEELER, HEAVY |
| `is_ev` | BOOLEAN | NO | NO | false | Electric vehicle flag |
| `is_active` | BOOLEAN | NO | NO | true | Soft-delete flag (false = deleted) |
| `registered_at` | TIMESTAMP | NO | NO | NOW() | Registration timestamp |

### Unique Constraints
- **uk_owner_license_plate:** (owner_id, license_plate) — Each driver can't have duplicate plates

### Indexes (Implicit)
- Primary Key Index on `vehicle_id`
- Unique Index on `(owner_id, license_plate)`

---

## 🔑 Data Model

### Entity: `Vehicle.java`

```java
UUID vehicleId;              // Primary key (auto-generated)
UUID ownerId;                // References auth-service user (no JPA join)
String licensePlate;         // Unique per owner
String make;                 // Manufacturer
String model;                // Model name
String color;                // Color (optional)
VehicleType vehicleType;     // Enum: TWO_WHEELER, FOUR_WHEELER, HEAVY
Boolean isEV;                // Electric vehicle flag (for EV charging spots)
Boolean isActive;            // Soft-delete flag
LocalDateTime registeredAt;  // Registration timestamp (set via @PrePersist)
```

### Enum: `VehicleType.java`

```java
TWO_WHEELER   // 2-wheel vehicles (motorcycles, scooters)
FOUR_WHEELER  // 4-wheel vehicles (cars, SUVs)
HEAVY         // Heavy vehicles (trucks, buses)
```

**Used for:** Booking-service spot-type matching

---

## 📨 API Endpoints

### Base URL: `http://localhost:8086/api/v1/vehicles`

### Authentication
- **Required:** All endpoints require a `Authorization: Bearer <JWT_TOKEN>` header
- **Token Source:** Obtained from auth-service POST /api/v1/auth/login
- **Token Validation:** JwtAuthFilter validates token signature & expiry

### 1. Register a Vehicle
```
POST /api/v1/vehicles/register
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request Body:
{
  "licensePlate": "MH-02-AB-1234",
  "make": "Toyota",
  "model": "Innova",
  "color": "Black",
  "vehicleType": "FOUR_WHEELER",
  "isEV": false
}

Response (201 CREATED):
{
  "vehicleId": "550e8400-e29b-41d4-a716-446655440000",
  "ownerId": "550e8400-e29b-41d4-a716-446655440001",
  "licensePlate": "MH-02-AB-1234",
  "make": "Toyota",
  "model": "Innova",
  "color": "Black",
  "vehicleType": "FOUR_WHEELER",
  "isEV": false,
  "registeredAt": "2026-04-03T10:30:45",
  "isActive": true
}

Authorization:
- DRIVER (own ownerId extracted from JWT)
- ADMIN (any user)

Validations:
- License plate cannot be duplicated per owner
- License plate format: ^[A-Z0-9 \\-]+$ (uppercase, digits, spaces, hyphens)
- Required fields: licensePlate, make, model, vehicleType
- Optional fields: color, isEV (defaults to false)
```

### 2. Get Vehicle by ID
```
GET /api/v1/vehicles/{vehicleId}
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
{
  "vehicleId": "550e8400-e29b-41d4-a716-446655440000",
  "ownerId": "550e8400-e29b-41d4-a716-446655440001",
  "licensePlate": "MH-02-AB-1234",
  "make": "Toyota",
  "model": "Innova",
  "color": "Black",
  "vehicleType": "FOUR_WHEELER",
  "isEV": false,
  "registeredAt": "2026-04-03T10:30:45",
  "isActive": true
}

Authorization:
- DRIVER (only if ownerId matches JWT userId)
- ADMIN (any vehicleId)

Used by:
- Frontend: Display vehicle details
- booking-service: Fetch vehicle EV status & type via RestTemplate
```

### 3. Get All Vehicles by Owner
```
GET /api/v1/vehicles/owner/{ownerId}
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
[
  { ...vehicle1... },
  { ...vehicle2... }
]

Authorization:
- DRIVER (only if ownerId matches JWT userId)
- ADMIN (any ownerId)

Note: Only returns active vehicles (isActive = true)
```

### 4. Find Vehicle by License Plate
```
GET /api/v1/vehicles/plate/{licensePlate}
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
{ ...vehicle... }

Authorization:
- DRIVER (only if ownerId matches JWT userId)
- ADMIN (any licensePlate)

Note: Global lookup (not per-owner)
```

### 5. Update Vehicle
```
PUT /api/v1/vehicles/{vehicleId}
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

Request Body (all fields optional):
{
  "make": "Honda",
  "model": "City",
  "color": "White",
  "vehicleType": "FOUR_WHEELER",
  "isEV": false
}

Response (200 OK):
{ ...updated vehicle... }

Authorization:
- DRIVER (only if ownerId matches JWT userId)
- ADMIN (any vehicleId)

Note:
- Only non-null fields are updated
- licensePlate and ownerId cannot be changed (immutable)
- registeredAt is never updated
```

### 6. Delete Vehicle (Soft)
```
DELETE /api/v1/vehicles/{vehicleId}
Authorization: Bearer <JWT_TOKEN>

Response (204 NO CONTENT)

Authorization:
- DRIVER (only if ownerId matches JWT userId)
- ADMIN (any vehicleId)

Note:
- Sets isActive = false (soft delete)
- Record is never hard-deleted from DB
- Subsequent queries exclude inactive vehicles
```

### 7. Get Vehicle Type
```
GET /api/v1/vehicles/{vehicleId}/type
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
"FOUR_WHEELER"

Used by: booking-service to validate spot type compatibility
```

### 8. Check if EV Vehicle
```
GET /api/v1/vehicles/{vehicleId}/isEV
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
true

Used by: booking-service to assign only to EV charging spots if isEV = true
```

### 9. Get All Vehicles (Admin Only)
```
GET /api/v1/vehicles/all
Authorization: Bearer <JWT_TOKEN>

Response (200 OK):
[
  { ...vehicle1... },
  { ...vehicle2... },
  ...
]

Authorization:
- ADMIN only
- DRIVER cannot access

Note: Returns all active vehicles in the system
```

### Error Responses

#### 400 Bad Request
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed — check the 'errors' field for details",
  "errors": [
    "License plate is required",
    "License plate must be between 2 and 20 characters"
  ]
}
```

#### 401 Unauthorized
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required — provide a valid JWT Bearer token"
}
```

#### 403 Forbidden
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to access this resource"
}
```

#### 404 Not Found
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 404,
  "error": "Not Found",
  "message": "Vehicle not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

#### 409 Conflict
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 409,
  "error": "Conflict",
  "message": "License plate 'MH-02-AB-1234' is already registered to your account"
}
```

#### 500 Internal Server Error
```json
{
  "timestamp": "2026-04-03T10:30:45",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please contact support."
}
```

---

## 🔐 Security & Authentication

### JWT Token Structure (Issued by auth-service)
```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "driver@parkease.com",        // Email
  "userId": "550e8400-e29b-41d4-a716-446655440001",  // UUID as string
  "role": "DRIVER",                    // Role: DRIVER, ADMIN, MANAGER
  "iat": 1712145045,                   // Issued at
  "exp": 1712231445                    // Expires in 86400 seconds (24 hours)
}

Signature: HS256 (HMAC-SHA256 with shared secret)
```

### Security Flow

1. **Request arrives with Authorization header**
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

2. **JwtAuthFilter intercepts request**
   - Extracts token from Bearer header
   - Validates signature using JWT_SECRET (must match auth-service)
   - Checks expiration time
   - If invalid/expired, request passes through unauthenticated

3. **Claims are extracted**
   - Email: `jwtUtil.extractEmail(token)`
   - Role: `jwtUtil.extractRole(token)`
   - UserId: `jwtUtil.extractUserId(token)`

4. **SecurityContext is populated**
   - Principal: Email string
   - Authority: ROLE_DRIVER, ROLE_ADMIN, etc.
   - Details: Map containing userId and role

5. **Authorization checks**
   - Method-level: @PreAuthorize("hasRole('ADMIN')")
   - Runtime: enforceOwnerOrAdmin(ownerId) in controller

### Role-Based Access Control (RBAC)

| Endpoint | DRIVER | ADMIN | Public |
|----------|--------|-------|--------|
| POST /register | Own only | Any | ❌ |
| GET /{vehicleId} | Own only | Any | ❌ |
| GET /owner/{ownerId} | Own only | Any | ❌ |
| GET /plate/{plate} | Own only | Any | ❌ |
| PUT /{vehicleId} | Own only | Any | ❌ |
| DELETE /{vehicleId} | Own only | Any | ❌ |
| GET /{vehicleId}/type | Own only | Any | ❌ |
| GET /{vehicleId}/isEV | Own only | Any | ❌ |
| GET /all | ❌ | ✅ | ❌ |
| /swagger-ui/** | ✅ | ✅ | ✅ |
| /v3/api-docs/** | ✅ | ✅ | ✅ |
| /actuator/health | ✅ | ✅ | ✅ |

### Key Security Rules

1. **ownerId is NEVER accepted from request body**
   - Always extracted from JWT claims
   - Prevents privilege escalation (driver can't register vehicle for another driver)

2. **DRIVER role can only access their own data**
   - Verified in controller: `enforceOwnerOrAdmin(ownerId)`
   - Throws 403 Forbidden if mismatch

3. **ADMIN role has unrestricted access**
   - Can read/modify any vehicle
   - Can access /all endpoint

4. **Stateless authentication**
   - No session storage
   - No database lookup on every request
   - Pure token validation

---

## 📋 DTOs (Data Transfer Objects)

### Request DTOs

#### RegisterVehicleRequest.java
```java
@NotBlank String licensePlate;           // Required, 2-20 chars
@NotBlank String make;                   // Required, max 50 chars
@NotBlank String model;                  // Required, max 50 chars
@Size(max=30) String color;              // Optional
@NotNull VehicleType vehicleType;        // Required enum
boolean isEV = false;                    // Optional, defaults to false
```

#### UpdateVehicleRequest.java
```java
@Size(max=50) String make;               // Optional
@Size(max=50) String model;              // Optional
@Size(max=30) String color;              // Optional
VehicleType vehicleType;                 // Optional
Boolean isEV;                            // Optional (wrapped Boolean for null check)
```

### Response DTO

#### VehicleResponse.java
```java
UUID vehicleId;
UUID ownerId;
String licensePlate;
String make;
String model;
String color;
VehicleType vehicleType;
Boolean isEV;
LocalDateTime registeredAt;
Boolean isActive;
```

---

## 🛠️ Business Logic

### Service Layer: VehicleServiceImpl.java

#### registerVehicle(UUID ownerId, RegisterVehicleRequest request)
- Validates license plate uniqueness **per owner**
- Allows same plate for different drivers
- Creates Vehicle entity with ownerId, type, EV flag
- Sets registeredAt via @PrePersist
- Returns VehicleResponse

#### getVehicleById(UUID vehicleId)
- Throws exception if not found
- Called by booking-service via RestTemplate
- Used for spot assignment validation

#### getVehiclesByOwner(UUID ownerId)
- Returns all active vehicles for owner
- Used for driver's vehicle list display

#### getByLicensePlate(String licensePlate)
- Global license plate lookup
- Returns single vehicle if found

#### updateVehicle(UUID vehicleId, UpdateVehicleRequest request)
- Only applies non-null fields
- licensePlate and ownerId are immutable
- registeredAt is never updated

#### deleteVehicle(UUID vehicleId)
- Soft delete: sets isActive = false
- Never hard-deletes from database
- Record remains in DB for audit trail

#### getVehicleType(UUID vehicleId)
- Returns VehicleType enum
- Used by booking-service for spot matching

#### isEVVehicle(UUID vehicleId)
- Returns boolean (true if isEV = true)
- Used by booking-service for EV charging spot assignment

#### getAllVehicles()
- Admin-only method
- Returns all active vehicles in system

---

## ⚙️ Configuration

### application.yaml
```yaml
server:
  port: 8086                           # Service port

spring:
  application:
    name: vehicle-service             # Service name

  datasource:
    url: jdbc:postgresql://localhost:5432/parkease_vehicle
    username: ${DB_USER}              # Environment variable
    password: ${DB_PASSWORD}          # Environment variable
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update                # Auto-migrate schema
    show-sql: true                    # Log SQL queries
    properties:
      hibernate:
        format_sql: true              # Format SQL for readability

jwt:
  secret: ${JWT_SECRET}               # Shared with auth-service (CRITICAL)
  expiry: 86400000                    # 24 hours in milliseconds

management:
  endpoints:
    web:
      exposure:
        include: health, info         # Exposed actuator endpoints
  endpoint:
    health:
      show-details: always            # Show detailed health info

springdoc:
  api-docs:
    path: /v3/api-docs               # OpenAPI definition endpoint
  swagger-ui:
    path: /swagger-ui.html           # Swagger UI path
```

### Environment Variables (Required)
```
DB_USER=postgres                      # PostgreSQL username
DB_PASSWORD=secure_password           # PostgreSQL password
JWT_SECRET=<base64_encoded_secret>    # MUST match auth-service
```

### SecurityConfig.java
```java
- CSRF disabled (stateless REST API)
- CORS enabled (cross-origin requests)
- Stateless session management (no HttpSession)
- HTTP Basic auth disabled
- Form login disabled
- Bearer token validation via JwtAuthFilter
- Public endpoints: /swagger-ui/**, /v3/api-docs/**, /actuator/health
- Protected endpoints: /api/v1/vehicles/**
- Admin-only: /api/v1/vehicles/all
```

---

## 📡 Integration Points

### 1. auth-service Integration
- **Port:** 8081
- **Purpose:** JWT token validation
- **Shared Secret:** JWT_SECRET environment variable (CRITICAL — must be identical)
- **Token Claims Used:**
  - `sub` (email)
  - `userId` (UUID)
  - `role` (DRIVER, ADMIN, MANAGER)
  - `exp` (expiration)

### 2. booking-service Integration
- **Port:** 8087
- **Integration Method:** RestTemplate HTTP calls
- **Endpoints Called:**
  - GET /api/v1/vehicles/{vehicleId} — Fetch vehicle details
  - GET /api/v1/vehicles/{vehicleId}/type — Get vehicle type
  - GET /api/v1/vehicles/{vehicleId}/isEV — Check if EV

### 3. PostgreSQL Database
- **Connection:** JDBC on port 5432
- **Database:** parkease_vehicle
- **Schema Auto-migration:** Hibernate (ddl-auto: update)
- **Credentials:** DB_USER, DB_PASSWORD env variables

---

## 🚀 Running the Service

### Prerequisites
- Java 17+
- PostgreSQL 13+
- Maven (or use provided mvnw)

### Build
```bash
# Using Maven wrapper (Windows)
mvnw.cmd clean package

# Using Maven wrapper (Linux/Mac)
./mvnw clean package

# Using system Maven
mvn clean package
```

### Run
```bash
# Option 1: Maven Spring Boot plugin
mvnw spring-boot:run

# Option 2: Direct Java execution
java -jar target/vehicle-service-0.0.1-SNAPSHOT.jar

# Option 3: With environment variables
set DB_USER=postgres
set DB_PASSWORD=password
set JWT_SECRET=base64_secret
java -jar target/vehicle-service-0.0.1-SNAPSHOT.jar
```

### Health Check
```bash
curl http://localhost:8086/actuator/health

Response (200 OK):
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {...}
    },
    "diskSpace": {
      "status": "UP",
      "details": {...}
    }
  }
}
```

### Swagger/OpenAPI
- **URL:** http://localhost:8086/swagger-ui.html
- **OpenAPI Definition:** http://localhost:8086/v3/api-docs
- **Authentication:** Click "Authorize" button, paste JWT token

---

## 📊 Key Features

### ✅ Multi-Tenant Data Isolation
- Drivers see only their own vehicles
- License plate unique per owner (not global)
- RBAC ensures authorization

### ✅ Soft Deletes
- No hard deletes; all records kept for audit
- isActive flag marks deleted records
- Queries filter by isActive = true

### ✅ EV Vehicle Support
- isEV flag tracks electric vehicles
- booking-service uses this for EV charging spot assignment
- Supports mixed ICE and EV fleets

### ✅ Vehicle Type Classification
- TWO_WHEELER, FOUR_WHEELER, HEAVY
- Used for booking-service spot-type matching
- Type cannot be null

### ✅ JWT-Based Security
- Stateless authentication (no session storage)
- Token issued by auth-service
- Token validation on every request

### ✅ Input Validation
- License plate format validation
- Field length constraints
- Enum type validation
- @Valid annotations on all DTOs

### ✅ Centralized Exception Handling
- GlobalExceptionHandler catches all exceptions
- Returns structured ApiError JSON
- No stack traces exposed to client
- Appropriate HTTP status codes

### ✅ API Documentation
- Swagger/OpenAPI 3.0 annotations
- Endpoint descriptions and parameter docs
- Exception response examples
- Security scheme documented

---

## 🔍 Debugging & Troubleshooting

### Common Issues

#### JWT Token Invalid/Expired
- **Symptom:** 401 Unauthorized on all requests
- **Solution:** 
  - Verify JWT_SECRET matches auth-service exactly
  - Check token expiration time
  - Obtain fresh token from auth-service login

#### License Plate Duplicate Error
- **Root Cause:** Same driver registering same plate twice
- **Expected Behavior:** Should be rejected
- **Note:** Different drivers CAN have same plate

#### Database Connection Failed
- **Symptom:** 500 error on startup or first request
- **Solution:**
  - Verify PostgreSQL is running
  - Check DB_USER, DB_PASSWORD environment variables
  - Verify database exists: `CREATE DATABASE parkease_vehicle;`
  - Check PostgreSQL port (default 5432)

#### CORS Issues
- **Symptom:** Frontend requests blocked
- **Solution:** CORS is enabled in SecurityConfig
  - Check Origin header in request
  - Verify frontend URL is whitelisted

### Logs Location
- **Console:** Printed to stdout (Spring Boot default)
- **Log Level:** Configured in application.yaml
- **SQL Logging:** Enabled with spring.jpa.show-sql: true

### Useful Queries for Debugging

```sql
-- List all vehicles
SELECT * FROM vehicles WHERE is_active = true;

-- Find vehicle by owner
SELECT * FROM vehicles WHERE owner_id = 'uuid-here' AND is_active = true;

-- Check EV vehicles
SELECT * FROM vehicles WHERE is_ev = true AND is_active = true;

-- List vehicles by type
SELECT * FROM vehicles WHERE vehicle_type = 'FOUR_WHEELER';

-- Check duplicate plates per owner
SELECT owner_id, license_plate, COUNT(*) 
FROM vehicles 
WHERE is_active = true 
GROUP BY owner_id, license_plate 
HAVING COUNT(*) > 1;
```

---

## 📝 Code Examples

### Example: Register a Vehicle

**Frontend/Client:**
```javascript
const token = localStorage.getItem('jwtToken');

const response = await fetch('http://localhost:8086/api/v1/vehicles/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    licensePlate: 'MH-02-AB-1234',
    make: 'Toyota',
    model: 'Innova',
    color: 'Black',
    vehicleType: 'FOUR_WHEELER',
    isEV: false
  })
});

const vehicle = await response.json();
console.log('Vehicle registered:', vehicle);
```

### Example: Update a Vehicle

```javascript
const vehicleId = '550e8400-e29b-41d4-a716-446655440000';
const token = localStorage.getItem('jwtToken');

const response = await fetch(
  `http://localhost:8086/api/v1/vehicles/${vehicleId}`,
  {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      color: 'Blue',
      isEV: true
    })
  }
);

const updatedVehicle = await response.json();
console.log('Vehicle updated:', updatedVehicle);
```

### Example: Get All Vehicles for Driver

```javascript
const ownerId = '550e8400-e29b-41d4-a716-446655440001';
const token = localStorage.getItem('jwtToken');

const response = await fetch(
  `http://localhost:8086/api/v1/vehicles/owner/${ownerId}`,
  {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);

const vehicles = await response.json();
console.log('Driver vehicles:', vehicles);
```

---

## 📚 Reference Documentation

- [Spring Boot 3.5.x Reference](https://docs.spring.io/spring-boot/3.5.13/reference/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [springdoc-openapi](https://springdoc.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## 🎯 Version & Status

- **Service Version:** 0.0.1-SNAPSHOT
- **Build Date:** 2026-04-03
- **Java Version:** 17
- **Spring Boot Version:** 3.5.13
- **Status:** Development/Testing

---

## 👥 Auth Rules Summary

### REQUEST FLOW DIAGRAM
```
1. Client → auth-service (login) → JWT Token
2. Client → vehicle-service (with JWT token in Authorization header)
3. JwtAuthFilter validates JWT signature & expiry
4. SecurityContext populated with email, role, userId
5. Controller method executes authorization checks
6. Service layer executes business logic
7. Response returned to client
```

### Authorization Decision Tree
```
Is JWT present and valid? 
  ├── NO → 401 Unauthorized
  └── YES → Continue
    Is endpoint public? (swagger, actuator)
      ├── YES → Allow
      └── NO → Continue
        Is endpoint admin-only? (/all)
          ├── YES → Is role ADMIN?
          │   ├── NO → 403 Forbidden
          │   └── YES → Allow
          └── NO → Continue
            Does data belong to requester? (ownerId check)
              ├── YES (or ADMIN) → Allow
              └── NO → 403 Forbidden
```

---

**End of Documentation**


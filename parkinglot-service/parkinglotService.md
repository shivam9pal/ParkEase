# ParkEase Parking Lot Management Microservice

## Project Overview

**Project Name:** parkinglot-service  
**Version:** 0.0.1-SNAPSHOT  
**Description:** ParkEase Parking Lot Management Microservice — responsible for managing parking lot profiles, geolocation-based search, approval workflows, and real-time spot counting.

**Group ID:** com.parkease  
**Artifact ID:** parkinglot  
**Package Namespace:** com.parkease.parkinglot

---

## Technology Stack

| Component | Version/Technology | Purpose |
|-----------|-------------------|---------|
| **Framework** | Spring Boot | 3.5.13 |
| **Java Version** | 17 | Core language |
| **Build Tool** | Maven | Project building & dependency management |
| **Database** | PostgreSQL | Primary data store |
| **Authentication** | JWT (JJWT 0.11.5) | Token-based security |
| **API Documentation** | OpenAPI 3.0 / Springdoc-UI | 2.8.5 |
| **ORM** | Spring Data JPA + Hibernate | Database abstraction |
| **Validation** | Jakarta Bean Validation | Input validation |
| **Build Tool Wrapper** | Maven Wrapper (mvnw) | Version consistency |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/parkease/parkinglot/
│   │   ├── ParkinglotApplication.java          # Spring Boot entry point
│   │   ├── config/
│   │   │   ├── OpenApiConfig.java              # Swagger/OpenAPI configuration
│   │   │   └── SecurityConfig.java             # Spring Security & CORS setup
│   │   ├── dto/
│   │   │   ├── CreateLotRequest.java           # POST request body
│   │   │   ├── UpdateLotRequest.java           # PUT request body
│   │   │   └── LotResponse.java                # Response DTO
│   │   ├── entity/
│   │   │   └── ParkingLot.java                 # JPA Entity
│   │   ├── exception/
│   │   │   ├── ApiError.java                   # Error response structure
│   │   │   └── GlobalExceptionHandler.java     # Centralized exception handling
│   │   ├── repository/
│   │   │   └── ParkingLotRepository.java       # Spring Data JPA repository
│   │   ├── resource/
│   │   │   └── ParkingLotResource.java         # REST controller
│   │   ├── security/
│   │   │   ├── JwtAuthFilter.java              # Request-level JWT validation
│   │   │   └── JwtUtil.java                    # JWT parsing & claims extraction
│   │   └── service/
│   │       ├── ParkingLotService.java          # Service interface
│   │       └── ParkingLotServiceImpl.java       # Service implementation
│   └── resources/
│       └── application.yaml                     # Configuration file
└── test/
    └── java/com/parkease/parkinglot/
        └── ParkinglotApplicationTests.java      # Basic Spring context test
```

---

## Environment Configuration

**Server Port:** 8082  
**Application Name:** parkinglot-service

### application.yaml

```yaml
server:
  port: 8082

spring:
  application:
    name: parkinglot-service

  datasource:
    url: jdbc:postgresql://localhost:5432/parkease_parkinglot
    username: ${DB_USER}           # Environment variable
    password: ${DB_PASSWORD}       # Environment variable
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: create-drop         # Recreate tables on startup (dev only)
    show-sql: true
    properties:
      hibernate:
        format_sql: true            # Pretty-print SQL logs

jwt:
  secret: ${JWT_SECRET}             # MUST match auth-service & vehicle-service
  expiry: 86400000                  # 24 hours in milliseconds

management:
  endpoints:
    web:
      exposure:
        include: health, info        # Actuator endpoints
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USER` | PostgreSQL username | parkease_user |
| `DB_PASSWORD` | PostgreSQL password | secure_password |
| `JWT_SECRET` | Base64-encoded JWT signing key (256+ bits) | YOUR_BASE64_SECRET |

---

## Database Schema

### Entity: ParkingLot

**Table Name:** `parking_lot`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `lot_id` | UUID | PK, Auto-generated | Unique identifier (UUIDv4) |
| `name` | VARCHAR | NOT NULL | Parking lot name |
| `address` | VARCHAR | NOT NULL | Street address |
| `city` | VARCHAR | NOT NULL | City name |
| `latitude` | DOUBLE PRECISION | NOT NULL | GPS latitude for geolocation |
| `longitude` | DOUBLE PRECISION | NOT NULL | GPS longitude for geolocation |
| `total_spots` | INTEGER | NOT NULL | Total parking capacity |
| `available_spots` | INTEGER | NOT NULL | Currently available spots |
| `manager_id` | UUID | NOT NULL | FK to auth-service user (no JPA join) |
| `is_open` | BOOLEAN | NOT NULL, DEFAULT=true | Operating status |
| `open_time` | TIME | NOT NULL | Daily opening time (HH:MM:SS) |
| `close_time` | TIME | NOT NULL | Daily closing time (HH:MM:SS) |
| `image_url` | VARCHAR | NULLABLE | S3 image URL |
| `is_approved` | BOOLEAN | NOT NULL, DEFAULT=false | Admin approval flag (hidden until approved) |
| `created_at` | TIMESTAMP | NOT NULL, AUTO | Lot creation timestamp |
| `version` | BIGINT | NOT NULL | Optimistic locking version |

**Key Constraints:**
- **Primary Key:** `lot_id` (UUID)
- **Version:** Used for optimistic locking to prevent double-booking race conditions
- **Indexes:** Should exist on `manager_id`, `city`, `latitude`, `longitude`, `is_approved`, `is_open`

---

## Data Transfer Objects (DTOs)

### CreateLotRequest (POST /api/v1/lots)

```java
{
  "name": "Downtown Parking",           // @NotBlank
  "address": "123 Main St",             // @NotBlank
  "city": "New York",                   // @NotBlank
  "latitude": 40.7128,                  // @NotNull
  "longitude": -74.0060,                // @NotNull
  "totalSpots": 150,                    // @NotNull, @Min(1)
  "openTime": "08:00:00",               // @NotNull, LocalTime format
  "closeTime": "22:00:00",              // @NotNull, LocalTime format
  "imageUrl": "https://s3.../image.jpg" // Optional
}
```

**Validation Rules:**
- `name`: Required, non-blank string
- `address`: Required, non-blank string
- `city`: Required, non-blank string
- `latitude`: Required, valid GPS coordinate (-90 to 90)
- `longitude`: Required, valid GPS coordinate (-180 to 180)
- `totalSpots`: Required, minimum 1
- `openTime`: Required, time format HH:MM:SS
- `closeTime`: Required, time format HH:MM:SS
- `imageUrl`: Optional S3 image URL

### UpdateLotRequest (PUT /api/v1/lots/{lotId})

```java
{
  "name": "Updated Name",               // Optional
  "address": "456 New St",              // Optional
  "city": "Boston",                     // Optional
  "latitude": 42.3601,                  // Optional
  "longitude": -71.0589,                // Optional
  "openTime": "09:00:00",               // Optional
  "closeTime": "23:00:00",              // Optional
  "imageUrl": "https://s3.../new.jpg"   // Optional
}
```

**Behavior:** Null fields are ignored; only provided fields are updated.

### LotResponse (API Response)

```java
{
  "lotId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Downtown Parking",
  "address": "123 Main St",
  "city": "New York",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "totalSpots": 150,
  "availableSpots": 45,
  "managerId": "660e8400-e29b-41d4-a716-446655440001",
  "isOpen": true,
  "openTime": "08:00:00",
  "closeTime": "22:00:00",
  "imageUrl": "https://s3.../image.jpg",
  "isApproved": true,
  "createdAt": "2026-04-03T14:30:00"
}
```

---

## REST API Endpoints

### Base URL
```
http://localhost:8082/api/v1/lots
```

---

### 1. **Create Parking Lot** (MANAGER Only)

```
POST /api/v1/lots
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request Body:** [CreateLotRequest](#createlotrequest)

**Response:**
- **201 Created** — Lot created (requires admin approval to show publicly)
- **400 Bad Request** — Validation error
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — User not MANAGER role

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Only users with `MANAGER` role can create lots
- New lots default to `isApproved: false` (hidden until admin approval)
- `availableSpots` initialized to equal `totalSpots`

---

### 2. **Get Parking Lot by ID** (PUBLIC)

```
GET /api/v1/lots/{lotId}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **200 OK** — Lot found
- **404 Not Found** — Lot ID not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- No authentication required
- Returns approved and unapproved lots (for manager/admin access)

---

### 3. **Search Lots by City** (PUBLIC)

```
GET /api/v1/lots/city/{city}
```

**Path Parameters:**
- `city` (String): City name (case-insensitive)

**Response:**
- **200 OK** — Returns list of approved lots in city

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

**Business Rules:**
- No authentication required
- Only returns `isApproved: true` lots
- Case-insensitive city matching

---

### 4. **Geolocation Proximity Search** (PUBLIC)

```
GET /api/v1/lots/nearby?lat={latitude}&lng={longitude}&radius={radiusKm}
```

**Query Parameters:**
- `lat` (double, required): User's latitude
- `lng` (double, required): User's longitude
- `radius` (double, optional, default=5.0): Search radius in kilometers

**Response:**
- **200 OK** — Returns list of nearby approved and open lots, sorted by distance (closest first)

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

**Business Rules:**
- No authentication required
- Only returns lots where `isApproved: true AND isOpen: true`
- Uses **Haversine formula** for distance calculation
- Results ordered by distance (ascending)
- Radius parameter default is 5 km; maximum recommended is 50 km

**Example Request:**
```
GET /api/v1/lots/nearby?lat=40.7128&lng=-74.0060&radius=10
```

---

### 5. **Keyword Search** (PUBLIC)

```
GET /api/v1/lots/search?keyword={searchTerm}
```

**Query Parameters:**
- `keyword` (String, required): Search term matched against name, address, city

**Response:**
- **200 OK** — Returns matching lots (case-insensitive)

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

**Business Rules:**
- No authentication required
- Only returns `isApproved: true` lots
- Matches keywords in: `name`, `address`, `city` (case-insensitive)

**Example Request:**
```
GET /api/v1/lots/search?keyword=downtown
```

---

### 6. **Get All Lots by Manager** (MANAGER/ADMIN)

```
GET /api/v1/lots/manager/{managerId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Response:**
- **200 OK** — Returns all lots for the authenticated manager
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — Non-manager/admin user

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

**Business Rules:**
- Only managers can view their own lots
- Admins can view all lots

---

### 7. **Get All Lots** (ADMIN Only)

```
GET /api/v1/lots/all
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
- **200 OK** — Returns all lots (approved + pending)
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — Non-admin user

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

---

### 8. **Get Pending Lots** (ADMIN Only)

```
GET /api/v1/lots/pending
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
- **200 OK** — Returns all unapproved lots awaiting admin review
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — Non-admin user

**Response Body:**
```json
[
  {LotResponse},
  {LotResponse}
]
```

---

### 9. **Update Parking Lot** (MANAGER/ADMIN)

```
PUT /api/v1/lots/{lotId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Request Body:** [UpdateLotRequest](#updatelotrequestputapiv1lotslotid)

**Response:**
- **200 OK** — Lot updated
- **400 Bad Request** — Validation error
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — User not lot owner (or admin)
- **404 Not Found** — Lot not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Only the lot manager or admin can update
- Null fields in request are ignored (optional updates)
- Cannot update `totalSpots` directly (separate endpoint expected)

---

### 10. **Toggle Lot Open/Close** (MANAGER/ADMIN)

```
PUT /api/v1/lots/{lotId}/toggleOpen
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **200 OK** — Lot open status toggled
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — User not lot owner (or admin)
- **404 Not Found** — Lot not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Only lot manager or admin can toggle
- Flips `isOpen` status (true ↔ false)

---

### 11. **Approve Parking Lot** (ADMIN Only)

```
PUT /api/v1/lots/{lotId}/approve
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **200 OK** — Lot approved (now visible to public searches)
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — Non-admin user
- **404 Not Found** — Lot not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Only admins can approve lots
- Sets `isApproved: true`
- Approved lots become visible in public searches

---

### 12. **Delete Parking Lot** (MANAGER/ADMIN)

```
DELETE /api/v1/lots/{lotId}
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **204 No Content** — Lot deleted
- **401 Unauthorized** — Invalid/missing JWT
- **403 Forbidden** — User not lot owner (unless admin)
- **404 Not Found** — Lot not found

**Business Rules:**
- Managers can only delete their own lots
- Admins can delete any lot

---

### 13. **Decrement Available Spots** (INTERNAL — booking-service)

```
PUT /api/v1/lots/{lotId}/decrement
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **200 OK** — Spot decremented
- **401 Unauthorized** — Invalid/missing JWT
- **409 Conflict** — No available spots left
- **404 Not Found** — Lot not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Called by booking-service when booking is confirmed
- Decrements `availableSpots` by 1
- Throws `IllegalStateException` (409 Conflict) if `availableSpots ≤ 0`
- Uses optimistic locking to prevent race conditions

---

### 14. **Increment Available Spots** (INTERNAL — booking-service)

```
PUT /api/v1/lots/{lotId}/increment
Authorization: Bearer {JWT_TOKEN}
```

**Path Parameters:**
- `lotId` (UUID): Parking lot ID

**Response:**
- **200 OK** — Spot incremented
- **401 Unauthorized** — Invalid/missing JWT
- **409 Conflict** — All spots already available
- **404 Not Found** — Lot not found

**Response Body:** [LotResponse](#lotresponse)

**Business Rules:**
- Called by booking-service when booking is cancelled
- Increments `availableSpots` by 1
- Throws `IllegalStateException` (409 Conflict) if `availableSpots ≥ totalSpots`
- Uses optimistic locking to prevent race conditions

---

## Security Architecture

### Authentication Strategy: JWT (JSON Web Tokens)

**Token Generation:** auth-service (not parkinglot-service)  
**Token Validation:** parkinglot-service, vehicle-service (read-only)

### JWT Claims Structure

```json
{
  "sub": "user@example.com",        // Email (subject)
  "userId": "550e8400-e29b-41d4",   // UUID string
  "role": "MANAGER",                // User role: MANAGER, ADMIN, DRIVER
  "iat": 1234567890,                // Issued at timestamp
  "exp": 1234654290                 // Expiration timestamp
}
```

### JWT Configuration

**Secret:** Base64-encoded key (256+ bits recommended)  
**Expiry:** 86400000 ms (24 hours)  
**Encoding:** HMAC-SHA256 (HS256)

**⚠️ CRITICAL:** JWT secret MUST be identical across:
- auth-service
- parkinglot-service (THIS SERVICE)
- vehicle-service

### Security Filter: JwtAuthFilter

| Component | Purpose |
|-----------|---------|
| `JwtAuthFilter` | OncePerRequestFilter that validates JWT on every request |
| `JwtUtil` | Extracts and validates JWT claims |

**Request Flow:**
1. Client sends `Authorization: Bearer {JWT_TOKEN}` header
2. `JwtAuthFilter` intercepts request
3. Validates token signature and expiration
4. Extracts email, userId, role from claims
5. Creates `UsernamePasswordAuthenticationToken` with authorities
6. Sets SecurityContext for current request
7. If validation fails, request proceeds (may be rejected by `@PreAuthorize`)

**Authority Mapping:**
```
role "MANAGER" → Spring Authority: "ROLE_MANAGER"
role "ADMIN"   → Spring Authority: "ROLE_ADMIN"
role "DRIVER"  → Spring Authority: "ROLE_DRIVER"
```

### Endpoint Access Control

**Public Endpoints (no JWT required):**
- `GET /api/v1/lots/{lotId}`
- `GET /api/v1/lots/city/{city}`
- `GET /api/v1/lots/nearby`
- `GET /api/v1/lots/search`
- `GET /v3/api-docs/**`
- `GET /swagger-ui/**`
- `GET /actuator/health`

**Manager-Only Endpoints:**
- `POST /api/v1/lots` — Create lot
- `PUT /api/v1/lots/{lotId}` — Update lot

**Admin-Only Endpoints:**
- `GET /api/v1/lots/all` — View all lots
- `GET /api/v1/lots/pending` — View pending lots
- `PUT /api/v1/lots/{lotId}/approve` — Approve lot

**Authenticated (any valid JWT):**
- `PUT /api/v1/lots/{lotId}/decrement` — Decrement spots (booking-service)
- `PUT /api/v1/lots/{lotId}/increment` — Increment spots (booking-service)

### CORS Configuration

**Allowed Origins:**
- `http://localhost:3000` (React CRA)
- `http://localhost:5173` (Vite)

**Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS  
**Allowed Headers:** All (`*`)  
**Credentials:** Enabled

**Production Consideration:** Update origins to match actual frontend deployment URLs.

---

## Service Layer

### ParkingLotService Interface

```java
public interface ParkingLotService {
    LotResponse createLot(UUID managerId, CreateLotRequest request);
    LotResponse getLotById(UUID lotId);
    List<LotResponse> getLotsByCity(String city);
    List<LotResponse> getNearbyLots(double lat, double lng, double radiusKm);
    List<LotResponse> getLotsByManager(UUID managerId);
    List<LotResponse> searchLots(String keyword);
    List<LotResponse> getAllLots();
    List<LotResponse> getPendingLots();
    LotResponse updateLot(UUID lotId, UUID managerId, UpdateLotRequest request);
    LotResponse toggleOpen(UUID lotId, UUID managerId);
    LotResponse approveLot(UUID lotId);
    void deleteLot(UUID lotId, UUID requesterId, String requesterRole);
    void decrementAvailable(UUID lotId);   // Booking-service call
    void incrementAvailable(UUID lotId);   // Booking-service call
}
```

### ParkingLotServiceImpl Key Methods

#### createLot(UUID managerId, CreateLotRequest request)

**Behavior:**
- Creates new ParkingLot entity
- Sets `managerId` from request handler
- Initializes `availableSpots = totalSpots`
- Sets `isApproved = false` (requires admin approval)
- Sets `isOpen = true` by default
- Persists to database and returns DTO

**Transaction:** `@Transactional`

---

#### getLotsByCity(String city)

**Behavior:**
- Queries all lots
- Filters by city (case-insensitive)
- Returns only `isApproved: true` lots
- Client-side filtering in practice (stream-based)

**Public:** Yes

---

#### getNearbyLots(double lat, double lng, double radiusKm)

**Behavior:**
- Uses **Haversine formula** native SQL query
- Calculates distance between coordinates
- Returns only `isApproved: true AND isOpen: true` lots
- Orders results by distance (closest first)

**Haversine Formula (PostgreSQL):**
```sql
SELECT * FROM parking_lot
WHERE is_approved = true AND is_open = true
AND (6371 * acos(
    cos(radians(:lat)) * cos(radians(latitude)) *
    cos(radians(longitude) - radians(:lng)) +
    sin(radians(:lat)) * sin(radians(latitude))
)) < :radius
ORDER BY distance ASC
```

**Public:** Yes

**Performance Note:** Native query with geospatial filtering; consider adding database indexes on `latitude`, `longitude`, `is_approved`, `is_open`.

---

#### updateLot(UUID lotId, UUID managerId, UpdateLotRequest request)

**Behavior:**
- Validates lot ownership (`managerId` match)
- Updates only non-null fields from request
- Preserves null fields (partial update pattern)
- Persists changes and returns DTO

**Transaction:** `@Transactional`

**Access Control:** Enforced via `enforceOwnerAccess(lot, managerId)`

---

#### decrementAvailable(UUID lotId) & incrementAvailable(UUID lotId)

**Purpose:** Atomic spot counter operations for booking-service integration

**Decrement Behavior:**
- Validates `availableSpots > 0`
- Decrements by 1
- Throws `IllegalStateException` if no spots available

**Increment Behavior:**
- Validates `availableSpots < totalSpots`
- Increments by 1
- Throws `IllegalStateException` if already at capacity

**Transaction:** `@Transactional`

**Optimistic Locking:** Uses `@Version` field to prevent race conditions

---

## Data Access Layer

### ParkingLotRepository (Spring Data JPA)

```java
@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {
    Optional<ParkingLot> findByLotId(UUID lotId);
    List<ParkingLot> findByCity(String city);
    List<ParkingLot> findByManagerId(UUID managerId);
    List<ParkingLot> findByIsOpen(Boolean isOpen);
    List<ParkingLot> findByAvailableSpotsGreaterThan(int count);
    long countByCity(String city);
    void deleteByLotId(UUID lotId);
    
    @Query(value = "SELECT * FROM parking_lot WHERE ...", nativeQuery = true)
    List<ParkingLot> findNearby(double lat, double lng, double radiusKm);
    
    List<ParkingLot> findByIsApprovedTrue();
    List<ParkingLot> findByIsApprovedFalse();
    List<ParkingLot> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrCityContainingIgnoreCase(
        String name, String address, String city);
}
```

**Key Query Methods:**
- `findByLotId()` — Primary lookup
- `findByManagerId()` — Get manager's lots
- `findByIsApprovedTrue/False()` — Filter approval status
- `findNearby()` — Haversine geolocation search
- Search methods — Support keyword matching

---

## Exception Handling

### GlobalExceptionHandler

Centralized exception handling via `@RestControllerAdvice`

### Handled Exceptions

| Exception | HTTP Status | Use Case |
|-----------|------------|----------|
| `MethodArgumentNotValidException` | **400 Bad Request** | DTO validation errors |
| `SecurityException` | **403 Forbidden** | Ownership/access violations |
| `IllegalStateException` | **409 Conflict** | No available spots |
| `RuntimeException` (not found) | **404 Not Found** | Lot not found |
| `RuntimeException` (generic) | **500 Internal Server Error** | Unexpected errors |

### ApiError Response Structure

```json
{
  "timestamp": "2026-04-03T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed — check the 'errors' field for details",
  "errors": [
    "name: Lot name is required",
    "totalSpots: Total spots must be at least 1"
  ]
}
```

**Fields:**
- `timestamp` — Request timestamp
- `status` — HTTP status code
- `error` — HTTP status name
- `message` — Error description
- `errors` — List of field-level validation errors (if applicable)

---

## Build & Deployment

### Build Tools

**Maven Wrapper:**
```bash
Windows:  mvnw.cmd clean install
Linux:    ./mvnw clean install
```

**Maven Commands:**
- `clean` — Remove build artifacts
- `install` — Build and install to local repository
- `package` — Build JAR
- `spring-boot:run` — Run application locally

### Build Output

**Artifacts Generated:**
- `target/parkinglot-0.0.1-SNAPSHOT.jar` — Executable JAR
- `target/classes/` — Compiled classes
- `target/generated-sources/` — Annotation-processed sources

### Starting the Service

**Via Maven:**
```bash
mvn spring-boot:run
```

**Via JAR:**
```bash
java -jar target/parkinglot-0.0.1-SNAPSHOT.jar
```

**Verify Startup:**
```bash
curl http://localhost:8082/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

---

## Dependencies

### Spring Boot Starters

| Dependency | Version | Purpose |
|-----------|---------|---------|
| spring-boot-starter-web | 3.5.13 | REST API, embedded Tomcat |
| spring-boot-starter-security | 3.5.13 | Security framework, authentication |
| spring-boot-starter-data-jpa | 3.5.13 | Hibernate ORM, data persistence |
| spring-boot-starter-validation | 3.5.13 | Bean validation (Jakarta) |
| spring-boot-starter-actuator | 3.5.13 | Health/metrics endpoints |

### External Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| postgresql | Latest | PostgreSQL JDBC driver |
| jjwt-api | 0.11.5 | JWT token parsing |
| jjwt-impl | 0.11.5 | JWT implementation |
| jjwt-jackson | 0.11.5 | JWT JSON serialization |
| lombok | Parent version | Boilerplate reduction (@Getter, @Builder, etc.) |
| springdoc-openapi-starter-webmvc-ui | 2.8.5 | Swagger/OpenAPI UI integration |

### Test Dependencies

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-test | JUnit 5, Mockito, AssertJ |

---

## API Documentation

### Swagger/OpenAPI Access

**URL:** `http://localhost:8082/swagger-ui.html`

**Endpoints:**
- Swagger UI: `/swagger-ui/**`
- OpenAPI Spec JSON: `/v3/api-docs`

### OpenAPI Configuration

Defined in [OpenApiConfig.java](#openapiconfigjava):

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("ParkEase — Parking Lot Service API")
            .version("v1")
            .description("Parking Lot Management — Profile, geo-proximity search, approval workflow, spot counter management")
            .contact(new Contact()
                .name("ParkEase")
                .email("dev@parkease.com")))
        .servers(List.of(
            new Server().url("http://localhost:8082").description("Development")))
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token from POST /api/v1/auth/login")));
}
```

---

## Database Setup

### PostgreSQL Prerequisites

**Connection Details:**
```
Host: localhost
Port: 5432
Database: parkease_parkinglot
Username: ${DB_USER}
Password: ${DB_PASSWORD}
```

### Initialize Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE parkease_parkinglot;

# Create user and grant privileges
CREATE USER parkease_user WITH PASSWORD 'secure_password';
ALTER ROLE parkease_user SET client_encoding TO 'utf8';
ALTER ROLE parkease_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE parkease_user SET default_transaction_deferrable TO on;
GRANT ALL PRIVILEGES ON DATABASE parkease_parkinglot TO parkease_user;

# Exit psql
\q
```

### Hibernate Auto Schema Creation (Development Only)

In `application.yaml`:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreates tables on startup
```

**Alternatives:**
- `validate` — Verify existing schema only
- `update` — Apply incremental changes
- `create` — Create schema from scratch
- `drop` — Drop schema on shutdown

**Production:** Use database migration tools like Flyway or Liquibase instead.

---

## Interservice Communication

### Integration Points

| Service | Interaction | Method |
|---------|-------------|--------|
| **auth-service** | JWT token issuance | OAuth/token endpoint |
| **booking-service** | Spot counter updates | PUT /api/v1/lots/{id}/decrement, increment |
| **vehicle-service** | Uses same JWT secret | Shares JWT validation |

### Booking-Service Integration

**When booking is created:**
```bash
PUT http://localhost:8082/api/v1/lots/{lotId}/decrement
Authorization: Bearer {JWT_TOKEN}
```

Response:
- **200 OK** if spot successfully decremented
- **409 Conflict** if no available spots left

**When booking is cancelled:**
```bash
PUT http://localhost:8082/api/v1/lots/{lotId}/increment
Authorization: Bearer {JWT_TOKEN}
```

Response:
- **200 OK** if spot successfully incremented
- **409 Conflict** if already at capacity

---

## Performance Considerations

### Database Indexes

**Recommended indexes for optimal query performance:**

```sql
CREATE INDEX idx_parking_lot_manager_id ON parking_lot(manager_id);
CREATE INDEX idx_parking_lot_city ON parking_lot(city);
CREATE INDEX idx_parking_lot_is_approved ON parking_lot(is_approved);
CREATE INDEX idx_parking_lot_is_open ON parking_lot(is_open);
CREATE INDEX idx_parking_lot_geoloc ON parking_lot(latitude, longitude);
CREATE INDEX idx_parking_lot_availability ON parking_lot(available_spots);
```

### Query Optimization

- **Haversine Proximity Search:** Filter by `is_approved` and `is_open` at query level (not in application)
- **Pagination:** Consider adding limit/offset to search results
- **Caching:** Approved lots rarely change; consider Redis caching for public searches

### Concurrency Control

- **Optimistic Locking:** `@Version` field prevents double-booking race conditions
- **Transactional Boundaries:** All write operations use `@Transactional`

---

## Common Development Tasks

### Add a New Endpoint

1. Add method to `ParkingLotService` interface
2. Implement in `ParkingLotServiceImpl`
3. Add controller method to `ParkingLotResource` with `@RequestMapping`
4. Add `@PreAuthorize` for role-based access
5. Add `@Operation` for Swagger documentation
6. Add route to `SecurityConfig` if needed

### Add a New Query

1. Add method to `ParkingLotRepository`
2. Use Spring Data naming conventions or `@Query` annotation
3. Call from `ParkingLotServiceImpl`
4. Filter results in service layer if needed

### Update Database Schema

1. Modify `ParkingLot.java` entity
2. Add `@Column` annotations and constraints
3. Spring Data JPA will regenerate schema on next startup (dev mode)

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Database connection fails | Verify `DB_USER`, `DB_PASSWORD`, PostgreSQL running on localhost:5432 |
| JWT validation error | Check `JWT_SECRET` matches across services; token not expired |
| CORS errors | Add origin to CORS config in `SecurityConfig`; check frontend URL |
| "Parking lot not found" (404) | Verify lot ID exists with `GET /api/v1/lots/{id}` |
| "No available spots" (409) | Verify `availableSpots > 0` before decrement; check booking-service logic |

### Logs

**Enable debug logging in `application.yaml`:**
```yaml
logging:
  level:
    com.parkease: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Git & Version Control

**Repository Structure:**
- Main source code: `src/main/java/`
- Tests: `src/test/java/`
- Configuration: `src/main/resources/`
- Build files: `pom.xml`, `mvnw`, `mvnw.cmd`

**.gitignore Should Include:**
```
target/
*.class
*.jar
.DS_Store
.env
application-local.yaml
```

---

## License & Contact

**Project Name:** ParkEase Parking Lot Management Service  
**Maintainer:** ParkEase Development Team  
**Email:** dev@parkease.com

---

**Documentation Last Updated:** April 3, 2026  
**Spring Boot Version:** 3.5.13  
**Java Version:** 17

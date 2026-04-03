# ParkEase Spot Service - Complete Documentation

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [Directory Structure](#directory-structure)
5. [Core Components](#core-components)
6. [API Endpoints](#api-endpoints)
7. [Database Schema](#database-schema)
8. [Security & Authentication](#security--authentication)
9. [Business Logic & Rules](#business-logic--rules)
10. [Configuration](#configuration)
11. [Exception Handling](#exception-handling)
12. [Build & Deployment](#build--deployment)

---

## 📌 Project Overview

### Purpose
**ParkEase Spot Service** is a Spring Boot microservice responsible for managing individual parking spaces within parking lots. It is part of the larger ParkEase parking management system.

### Key Responsibilities
- **Create & manage parking spots** within lots (single or bulk creation)
- **Track spot availability** through state transitions (AVAILABLE → RESERVED → OCCUPIED → AVAILABLE)
- **Provide spot browsing APIs** for drivers and booking-service
- **Enforce business rules** for spot status transitions and constraints
- **Support filtering & querying** by spot type, vehicle type, floor, and features (EV, handicapped)
- **Enable inter-service communication** with booking-service for spot state management

### Service Port
- **Default Port**: `8083`
- **Base URL**: `http://localhost:8083`

### Service Dependencies
- **Database**: PostgreSQL (parkease_spot database)
- **Auth Service**: JWT tokens from auth-service (port 8081)
- **Booking Service**: Internal calls for spot state transitions
- **Parking Lot Service**: Cross-references (UUID only, no direct joins)

---

## 🛠 Technology Stack

### Core Framework
- **Spring Boot**: v3.5.13
- **Java**: JDK 17
- **Build Tool**: Maven 3.9.x

### Key Dependencies
| Component | Library | Version | Purpose |
|-----------|---------|---------|---------|
| Web Framework | spring-boot-starter-web | 3.5.13 | REST API support |
| Security | spring-boot-starter-security | 3.5.13 | Authentication & authorization |
| JPA/ORM | spring-boot-starter-data-jpa | 3.5.13 | Database access layer |
| Validation | spring-boot-starter-validation | 3.5.13 | Input validation |
| Database | postgresql | Latest | PostgreSQL JDBC driver |
| JWT | io.jsonwebtoken:jjwt | 0.11.5 | JWT token parsing & validation |
| Documentation | springdoc-openapi-starter-webmvc-ui | 2.8.5 | Swagger UI & OpenAPI 3.0 |
| Lombok | org.projectlombok:lombok | Latest | Boilerplate reduction (@Data, @Builder, etc.) |
| Monitoring | spring-boot-starter-actuator | 3.5.13 | Health checks & metrics |

### Development Tools
- **IDE**: IntelliJ IDEA or VS Code
- **Postman/Insomnia**: API testing
- **DBeaver**: Database management

---

## 🏗 Architecture

### Layered Architecture Pattern
```
┌─────────────────────────────────────────────────────┐
│         REST Controller Layer (SpotResource)        │
│  ├─ HTTP request/response handling                  │
│  ├─ Input validation via @Valid annotations         │
│  ├─ Response entity wrapping                        │
│  └─ OpenAPI/Swagger documentation                   │
├─────────────────────────────────────────────────────┤
│         Security Layer (JWT Authentication)         │
│  ├─ JwtAuthFilter: JWT token extraction & validation│
│  ├─ JwtUtil: Token parsing & claim extraction       │
│  ├─ SecurityConfig: Authorization rules & CORS      │
│  └─ Role-based access control (MANAGER, DRIVER)     │
├─────────────────────────────────────────────────────┤
│            Service Layer (Business Logic)           │
│  ├─ SpotService interface: contracts                │
│  ├─ SpotServiceImpl: spot lifecycle & state mgmt     │
│  ├─ Transaction management (@Transactional)         │
│  ├─ Data transformation (entity ↔ DTO)              │
│  └─ Business rule enforcement                       │
├─────────────────────────────────────────────────────┤
│      Repository Layer (Data Access - JPA)           │
│  ├─ SpotRepository: query methods                    │
│  ├─ JpaRepository: CRUD + custom queries             │
│  ├─ Automatic SQL generation                        │
│  └─ Database transaction support                    │
├─────────────────────────────────────────────────────┤
│         Entity Layer (Domain Model)                  │
│  ├─ ParkingSpot: JPA entity with lifecycle hooks    │
│  ├─ SpotStatus, SpotType, VehicleType: enums        │
│  ├─ Database mapping & constraints                  │
│  └─ Business rule enforcement (@PrePersist)         │
├─────────────────────────────────────────────────────┤
│            Data Transfer Objects (DTOs)             │
│  ├─ AddSpotRequest: single spot creation            │
│  ├─ BulkAddSpotRequest: batch spot creation         │
│  ├─ UpdateSpotRequest: partial updates              │
│  ├─ SpotResponse: standardized response format      │
│  └─ Request/response decoupling from entities       │
├─────────────────────────────────────────────────────┤
│      Configuration Layer                            │
│  ├─ OpenApiConfig: Swagger/OpenAPI 3.0 setup        │
│  ├─ SecurityConfig: Spring Security filter chain   │
│  ├─ application.yaml: environment settings          │
│  └─ Database connection pooling                     │
├─────────────────────────────────────────────────────┤
│      Exception Handling Layer                       │
│  ├─ GlobalExceptionHandler: centralized exception   │
│  ├─ ApiError: standardized error response           │
│  ├─ HTTP status code mapping                        │
│  └─ Validation error extraction                     │
├─────────────────────────────────────────────────────┤
│          PostgreSQL Database                        │
│  ├─ parking_spot table with constraints & indexes  │
│  ├─ UUID primary keys                               │
│  ├─ Enum columns (status, spotType, vehicleType)    │
│  └─ Unique & composite indexes for performance      │
└─────────────────────────────────────────────────────┘
```

### Request/Response Flow
```
HTTP Request
    ↓
JwtAuthFilter (extract & validate JWT)
    ↓
SecurityConfig (authorization rules)
    ↓
SpotResource (REST endpoint handler)
    ↓
@Valid annotation (DTO validation)
    ↓
SpotService (business logic)
    ↓
SpotRepository (database query)
    ↓
PostgreSQL Database
    ↓
[Response transforms back through layers]
    ↓
GlobalExceptionHandler (if error occurs)
    ↓
HTTP Response (JSON + status code)
```

---

## 📁 Directory Structure

```
spot-service/
├── src/
│   ├── main/
│   │   ├── java/com/parkease/spot/
│   │   │   ├── SpotApplication.java                 # Main entry point
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java               # Swagger/OpenAPI 3.0 configuration
│   │   │   │   └── SecurityConfig.java              # Spring Security configuration
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── AddSpotRequest.java              # Single spot creation DTO
│   │   │   │   ├── BulkAddSpotRequest.java          # Batch spot creation DTO
│   │   │   │   ├── UpdateSpotRequest.java           # Partial update DTO
│   │   │   │   └── SpotResponse.java                # Standardized response DTO
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── ParkingSpot.java                 # JPA entity (parking_spot table)
│   │   │   │   ├── SpotStatus.java                  # Enum: AVAILABLE, RESERVED, OCCUPIED
│   │   │   │   ├── SpotType.java                    # Enum: COMPACT, STANDARD, LARGE, MOTORBIKE, EV
│   │   │   │   └── VehicleType.java                 # Enum: TWO_WHEELER, FOUR_WHEELER, HEAVY
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── ApiError.java                    # Standardized error response DTO
│   │   │   │   └── GlobalExceptionHandler.java      # Centralized exception handler
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   └── SpotRepository.java              # Data access interface (JpaRepository)
│   │   │   │
│   │   │   ├── resource/
│   │   │   │   └── SpotResource.java                # REST controller (/api/v1/spots)
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthFilter.java               # JWT authentication filter
│   │   │   │   └── JwtUtil.java                     # JWT token parsing & validation
│   │   │   │
│   │   │   └── service/
│   │   │       ├── SpotService.java                 # Service interface (contracts)
│   │   │       └── SpotServiceImpl.java              # Service implementation (business logic)
│   │   │
│   │   └── resources/
│   │       ├── application.yaml                     # Application configuration
│   │       ├── static/                              # Static assets (CSS, JS)
│   │       └── templates/                           # HTML templates (if any)
│   │
│   └── test/
│       └── java/com/parkease/spot/
│           └── SpotApplicationTests.java            # Unit/Integration tests
│
├── target/                                           # Compiled output (Maven build)
│   ├── classes/                                      # Compiled Java classes
│   └── generated-sources/                            # Generated code (Lombok, etc.)
│
├── pom.xml                                          # Maven project configuration
├── mvnw                                             # Maven wrapper (Unix/Linux/Mac)
├── mvnw.cmd                                         # Maven wrapper (Windows)
├── spotservice.md                                   # This documentation file
└── .gitignore                                       # Git ignore patterns
```

---

## 🔧 Core Components

### 1. Entity Layer

#### **ParkingSpot.java** (JPA Entity)
```java
@Entity
@Table(name = "parking_spot", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "spot_number"}),
       indexes = {...})  // Performance indexes
public class ParkingSpot {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private UUID spotId;                    // PK: unique spot identifier
    
    @Column(name = "lot_id")
    private UUID lotId;                     // FK: parking lot reference (cross-service)
    
    @Column(name = "spot_number", nullable = false, unique = false)
    private String spotNumber;              // Human-readable identifier (A-01, B-12)
    
    @Column(name = "floor")
    private Integer floor;                  // 0=Ground, 1=First, -1=Basement
    
    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type")
    private SpotType spotType;              // COMPACT, STANDARD, LARGE, MOTORBIKE, EV
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;        // TWO_WHEELER, FOUR_WHEELER, HEAVY
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SpotStatus status;              // AVAILABLE, RESERVED, OCCUPIED (default: AVAILABLE)
    
    @Column(name = "is_handicapped")
    private Boolean isHandicapped;          // Accessible for disabled drivers (default: false)
    
    @Column(name = "is_ev_charging")
    private Boolean isEVCharging;           // EV charging available (default: false, auto=true for EV)
    
    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private BigDecimal pricePerHour;        // Hourly rate (BigDecimal for monetary precision)
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;       // Creation timestamp (auto-set via @PrePersist)
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (SpotType.EV.equals(this.spotType)) {
            this.isEVCharging = true;       // Business rule: EV spots always have charging
        }
        if (this.status == null) {
            this.status = SpotStatus.AVAILABLE;
        }
    }
}
```

**Key Features**:
- **UUID Primary Key**: Distributed system friendly
- **Unique Constraint**: (lot_id, spot_number) ensures no duplicate spot numbers per lot
- **Performance Indexes**: Fast querying by lot, status, type, vehicle compatibility
- **Monetary Precision**: BigDecimal for pricePerHour (never float/double)
- **Enum Storage**: STRING type for readability in database
- **Cross-Service Reference**: lotId as UUID (no JPA join across service boundary)
- **Lifecycle Hook**: @PrePersist enforces EV charging rule at entity level

---

#### **SpotStatus.java** (Enum)
```java
public enum SpotStatus {
    AVAILABLE,    // Spot is free and can be booked
    RESERVED,     // Booking exists but driver hasn't arrived
    OCCUPIED      // Driver is actively using the spot
}
```

**Valid Transitions**:
```
AVAILABLE ──→ RESERVED   (booking created)
AVAILABLE ──→ OCCUPIED   (walk-in check-in)
RESERVED  ──→ OCCUPIED   (normal check-in)
RESERVED  ──→ AVAILABLE  (booking cancelled)
OCCUPIED  ──→ AVAILABLE  (checkout/exit)
```
Any other transition throws `IllegalStateException` → 409 CONFLICT response.

---

#### **SpotType.java** (Enum)
```java
public enum SpotType {
    COMPACT,      // Small car-sized slot
    STANDARD,     // Regular car-sized slot
    LARGE,        // SUV / large vehicle slot
    MOTORBIKE,    // Two-wheeler dedicated slot
    EV            // Electric vehicle slot (always has charging)
}
```

---

#### **VehicleType.java** (Enum)
```java
public enum VehicleType {
    TWO_WHEELER,   // Motorcycles, scooters, mopeds
    FOUR_WHEELER,  // Cars, SUVs, sedans
    HEAVY          // Trucks, buses, commercial vehicles
}
```
**Note**: Must mirror the VehicleType in vehicle-service exactly for compatibility.

---

### 2. DTO Layer

#### **AddSpotRequest.java** (Single Spot Creation)
```java
@Data
@Builder
public class AddSpotRequest {
    @NotBlank
    private String spotNumber;              // e.g., "A-01", "B-12"
    
    @NotNull
    private Integer floor;                  // 0=Ground, 1=First, -1=Basement
    
    @NotNull
    private SpotType spotType;              // COMPACT, STANDARD, LARGE, MOTORBIKE, EV
    
    @NotNull
    private VehicleType vehicleType;        // TWO_WHEELER, FOUR_WHEELER, HEAVY
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pricePerHour;        // e.g., 5.50, 10.00
    
    private boolean isHandicapped;          // Optional, defaults to false
    
    private boolean isEVCharging;           // Optional, auto-set if spotType=EV
}
```

**Usage**: Create a single parking spot in a lot.
**Validation**: All @NotNull fields are required; pricePerHour must be > 0.

---

#### **BulkAddSpotRequest.java** (Batch Creation)
```java
@Data
@Builder
public class BulkAddSpotRequest {
    @NotNull
    @Min(1)
    private Integer count;                  // Number of spots to create (1-N)
    
    @NotNull
    private SpotType spotType;              // Same type for all spots
    
    @NotNull
    private VehicleType vehicleType;        // Same vehicle type for all spots
    
    @NotNull
    private Integer floor;                  // All spots on same floor
    
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pricePerHour;        // Same price for all spots
    
    private String spotNumberPrefix;        // Optional, e.g., "A" or "Floor-1-Section-A"
                                             // If null → uses spotType.name() (e.g., "EV", "COMPACT")
    
    private boolean isHandicapped;          // Same for all spots
    
    private boolean isEVCharging;           // Same for all spots
}
```

**Auto-Numbering Examples**:
- Prefix="A", Count=5 → `A-01`, `A-02`, `A-03`, `A-04`, `A-05`
- Prefix=null, SpotType=COMPACT, Count=3 → `COMPACT-01`, `COMPACT-02`, `COMPACT-03`

---

#### **UpdateSpotRequest.java** (Partial Update)
```java
@Data
@Builder
public class UpdateSpotRequest {
    private SpotType spotType;              // Optional: change spot category
    private VehicleType vehicleType;        // Optional: change vehicle compatibility
    private BigDecimal pricePerHour;        // Optional: change hourly rate
    private Boolean isHandicapped;          // Optional: toggle accessibility
    private Boolean isEVCharging;           // Optional: toggle EV charging
    private Integer floor;                  // Optional: reassign to different floor
    
    // IMMUTABLE (not included): spotNumber, lotId
}
```

**Behavior**: Only non-null fields are applied. The entity is fetched, updated, and saved.

---

#### **SpotResponse.java** (API Response)
```java
@Data
@Builder
public class SpotResponse {
    private UUID spotId;                    // Unique spot identifier
    private UUID lotId;                     // Parking lot reference
    private String spotNumber;              // Human-readable ID
    private Integer floor;                  // Floor level
    private SpotType spotType;              // Spot category
    private VehicleType vehicleType;        // Vehicle compatibility
    private SpotStatus status;              // Current state
    private Boolean isHandicapped;          // Accessibility flag
    private Boolean isEVCharging;           // EV charging flag
    private BigDecimal pricePerHour;        // Hourly rate (used by booking-service)
    private LocalDateTime createdAt;        // Creation time
}
```

**Key Points**:
- Used by ALL API responses (never expose ParkingSpot entity directly)
- Includes pricePerHour so booking-service can fetch it via GET /api/v1/spots/{spotId}
- Includes createdAt for audit/analytics purposes

---

### 3. Service Layer

#### **SpotService.java** (Interface - Contracts)
```java
public interface SpotService {
    // ── CREATION ──────────────────────────────────────────────
    SpotResponse addSpot(UUID lotId, AddSpotRequest request);
    List<SpotResponse> addBulkSpots(UUID lotId, BulkAddSpotRequest request);
    
    // ── READ ───────────────────────────────────────────────────
    SpotResponse getSpotById(UUID spotId);
    List<SpotResponse> getSpotsByLot(UUID lotId);
    List<SpotResponse> getAvailableSpots(UUID lotId);
    List<SpotResponse> getByTypeAndLot(UUID lotId, SpotType spotType);
    List<SpotResponse> getByVehicleTypeAndLot(UUID lotId, VehicleType vehicleType);
    List<SpotResponse> getByFloorAndLot(UUID lotId, Integer floor);
    List<SpotResponse> getEVSpots(UUID lotId);
    List<SpotResponse> getHandicappedSpots(UUID lotId);
    
    // ── STATUS TRANSITIONS (State Machine) ──────────────────────
    SpotResponse reserveSpot(UUID spotId);      // AVAILABLE → RESERVED
    SpotResponse occupySpot(UUID spotId);       // AVAILABLE/RESERVED → OCCUPIED
    SpotResponse releaseSpot(UUID spotId);      // RESERVED/OCCUPIED → AVAILABLE
    
    // ── UPDATE / DELETE ────────────────────────────────────────
    SpotResponse updateSpot(UUID spotId, UpdateSpotRequest request);
    void deleteSpot(UUID spotId);
    
    // ── AGGREGATES ─────────────────────────────────────────────
    long countAvailable(UUID lotId);
}
```

---

#### **SpotServiceImpl.java** (Implementation - Business Logic)

**Key Features**:

1. **Transactional Methods**:
   - Write operations: `@Transactional` (default: read-write)
   - Read operations: `@Transactional(readOnly = true)` (optimized for queries)

2. **Creation Logic**:
   ```java
   addSpot(UUID lotId, AddSpotRequest request) {
       // Check: spotNumber unique within lot
       if (spotRepository.existsByLotIdAndSpotNumber(lotId, request.getSpotNumber())) {
           throw new IllegalArgumentException("Duplicate spot number");
       }
       
       // Build entity with EV auto-charging rule
       // Save and return as DTO
   }
   
   addBulkSpots(UUID lotId, BulkAddSpotRequest request) {
       // Resolve prefix or use spotType.name()
       // Loop count times, generate spotNumbers: prefix + "-" + zero-padded-index
       // Skip if spot already exists (idempotent re-runs)
       // Save all and return list
   }
   ```

3. **State Transition Logic** (Strict Enforcement):
   ```java
   reserveSpot(UUID spotId) {
       switch (spot.getStatus()) {
           case AVAILABLE → {
               spot.setStatus(RESERVED);
               save(spot);
           }
           case RESERVED, OCCUPIED → throw new IllegalStateException("409");
       }
   }
   
   occupySpot(UUID spotId) {
       switch (spot.getStatus()) {
           case AVAILABLE, RESERVED → {
               spot.setStatus(OCCUPIED);
               save(spot);
           }
           case OCCUPIED → throw new IllegalStateException("409");
       }
   }
   
   releaseSpot(UUID spotId) {
       switch (spot.getStatus()) {
           case RESERVED, OCCUPIED → {
               spot.setStatus(AVAILABLE);
               save(spot);
           }
           case AVAILABLE → throw new IllegalStateException("409");
       }
   }
   ```

4. **Update Logic** (Partial, Null-Safe):
   ```java
   updateSpot(UUID spotId, UpdateSpotRequest request) {
       ParkingSpot spot = findSpotOrThrow(spotId);
       
       if (request.getSpotType() != null) {
           spot.setSpotType(request.getSpotType());
           if (EV.equals(request.getSpotType())) {
               spot.setIsEVCharging(true);  // Re-enforce EV rule
           }
       }
       
       if (request.getPricePerHour() != null) {
           spot.setPricePerHour(request.getPricePerHour());
       }
       
       // ... other non-null fields ...
       
       spotRepository.save(spot);
   }
   ```

5. **Helper Methods**:
   ```java
   private ParkingSpot findSpotOrThrow(UUID spotId) {
       return spotRepository.findBySpotId(spotId)
           .orElseThrow(() → new RuntimeException("Spot not found with id: " + spotId));
       // Caught by GlobalExceptionHandler → 404 NOT FOUND
   }
   
   private SpotResponse toResponse(ParkingSpot spot) {
       // Maps entity to DTO - the only place entities are converted
   }
   ```

---

### 4. Repository Layer

#### **SpotRepository.java** (JPA Data Access)
```java
@Repository
public interface SpotRepository extends JpaRepository<ParkingSpot, UUID> {
    
    // ── Primary Lookups ───────────────────────────────────────
    Optional<ParkingSpot> findBySpotId(UUID spotId);
    
    // ── Lot-Level Queries ──────────────────────────────────────
    List<ParkingSpot> findByLotId(UUID lotId);
    List<ParkingSpot> findByLotIdAndStatus(UUID lotId, SpotStatus status);
    List<ParkingSpot> findByLotIdAndSpotType(UUID lotId, SpotType spotType);
    List<ParkingSpot> findByLotIdAndVehicleType(UUID lotId, VehicleType vehicleType);
    
    // ── Combined Filters ───────────────────────────────────────
    List<ParkingSpot> findByLotIdAndVehicleTypeAndStatus(
        UUID lotId, VehicleType vehicleType, SpotStatus status);
    List<ParkingSpot> findByLotIdAndSpotTypeAndStatus(
        UUID lotId, SpotType spotType, SpotStatus status);
    List<ParkingSpot> findByLotIdAndFloor(UUID lotId, Integer floor);
    List<ParkingSpot> findByLotIdAndIsHandicapped(UUID lotId, Boolean isHandicapped);
    List<ParkingSpot> findByLotIdAndIsEVCharging(UUID lotId, Boolean isEVCharging);
    
    // ── Global Queries ────────────────────────────────────────
    List<ParkingSpot> findByIsEVCharging(Boolean isEVCharging);
    
    // ── Existence / Count ──────────────────────────────────────
    boolean existsByLotIdAndSpotNumber(UUID lotId, String spotNumber);
    long countByLotIdAndStatus(UUID lotId, SpotStatus status);
    
    // ── Delete ─────────────────────────────────────────────────
    void deleteBySpotId(UUID spotId);
}
```

**Auto-Generated SQL**:
Spring Data JPA generates SQL at compile-time based on method names:
- `findByLotIdAndStatus` → `SELECT * FROM parking_spot WHERE lot_id = ? AND status = ?`
- `existsByLotIdAndSpotNumber` → `SELECT COUNT(*) > 0 FROM parking_spot WHERE lot_id = ? AND spot_number = ?`
- `countByLotIdAndStatus` → `SELECT COUNT(*) FROM parking_spot WHERE lot_id = ? AND status = ?`

---

### 5. REST Controller Layer

#### **SpotResource.java** (REST Endpoints)

**Base Path**: `/api/v1/spots`

All endpoints return `SpotResponse` (or `List<SpotResponse>` for collections).

---

## 🔗 API Endpoints

### **1. CREATE ENDPOINTS** (Manager Only)

#### **POST /api/v1/spots** - Add Single Spot
```http
POST /api/v1/spots?lotId=550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "spotNumber": "A-01",
  "floor": 0,
  "spotType": "STANDARD",
  "vehicleType": "FOUR_WHEELER",
  "pricePerHour": 5.50,
  "isHandicapped": false,
  "isEVCharging": false
}
```

**Response**: 201 Created
```json
{
  "spotId": "123e4567-e89b-12d3-a456-426614174000",
  "lotId": "550e8400-e29b-41d4-a716-446655440000",
  "spotNumber": "A-01",
  "floor": 0,
  "spotType": "STANDARD",
  "vehicleType": "FOUR_WHEELER",
  "status": "AVAILABLE",
  "isHandicapped": false,
  "isEVCharging": false,
  "pricePerHour": 5.50,
  "createdAt": "2026-04-03T10:00:00"
}
```

**Errors**:
- `400 Bad Request`: Validation failed or duplicate spotNumber
- `401 Unauthorized`: Missing/invalid JWT
- `403 Forbidden`: User lacks MANAGER role

---

#### **POST /api/v1/spots/bulk** - Bulk Add Spots
```http
POST /api/v1/spots/bulk?lotId=550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "count": 5,
  "spotType": "COMPACT",
  "vehicleType": "FOUR_WHEELER",
  "floor": 1,
  "pricePerHour": 3.50,
  "spotNumberPrefix": "B",
  "isHandicapped": false,
  "isEVCharging": false
}
```

**Response**: 201 Created
```json
[
  {
    "spotId": "123e4567-e89b-12d3-a456-426614174001",
    "spotNumber": "B-01",
    ...
  },
  {
    "spotId": "123e4567-e89b-12d3-a456-426614174002",
    "spotNumber": "B-02",
    ...
  },
  // ... B-03, B-04, B-05 ...
]
```

**Auto-Numbering**:
- If `spotNumberPrefix="B"`, count=5 → generates: B-01, B-02, B-03, B-04, B-05
- If `spotNumberPrefix` is null/blank → uses spotType name: COMPACT-01, COMPACT-02, ...

---

### **2. READ ENDPOINTS** (Public - No JWT Required)

#### **GET /api/v1/spots/{spotId}** - Get Spot By ID
```http
GET /api/v1/spots/123e4567-e89b-12d3-a456-426614174000
```

**Response**: 200 OK
```json
{
  "spotId": "123e4567-e89b-12d3-a456-426614174000",
  "lotId": "550e8400-e29b-41d4-a716-446655440000",
  "spotNumber": "A-01",
  "floor": 0,
  "spotType": "STANDARD",
  "vehicleType": "FOUR_WHEELER",
  "status": "AVAILABLE",
  "isHandicapped": false,
  "isEVCharging": false,
  "pricePerHour": 5.50,
  "createdAt": "2026-04-03T10:00:00"
}
```

**Used By**: Booking-service fetches pricePerHour from this endpoint for fare calculation.

---

#### **GET /api/v1/spots/lot/{lotId}** - Get All Spots in Lot
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000
```

**Response**: 200 OK - Array of all spots in the lot

---

#### **GET /api/v1/spots/lot/{lotId}/available** - Get Available Spots
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/available
```

**Response**: 200 OK - Array of AVAILABLE spots only (primary for driver browsing)

---

#### **GET /api/v1/spots/lot/{lotId}/type/{spotType}** - Filter by Spot Type
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/type/COMPACT
```

**Supported Types**: COMPACT, STANDARD, LARGE, MOTORBIKE, EV

---

#### **GET /api/v1/spots/lot/{lotId}/vehicle/{vehicleType}** - Filter by Vehicle Type
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/vehicle/FOUR_WHEELER
```

**Supported Types**: TWO_WHEELER, FOUR_WHEELER, HEAVY

---

#### **GET /api/v1/spots/lot/{lotId}/floor/{floor}** - Get Spots by Floor
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/floor/0
```

**Floor Values**: 0 (Ground), 1 (First), -1 (Basement 1), etc.

---

#### **GET /api/v1/spots/lot/{lotId}/ev** - Get EV Charging Spots
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/ev
```

**Response**: 200 OK - Array of spots with isEVCharging=true

---

#### **GET /api/v1/spots/lot/{lotId}/handicapped** - Get Handicapped Spots
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/handicapped
```

**Response**: 200 OK - Array of spots with isHandicapped=true

---

#### **GET /api/v1/spots/lot/{lotId}/count** - Count Available Spots
```http
GET /api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/count
```

**Response**: 200 OK
```json
5
```

Returns: long (count of AVAILABLE spots in the lot)

---

### **3. STATUS TRANSITION ENDPOINTS** (JWT Required - Any Valid JWT)

#### **PUT /api/v1/spots/{spotId}/reserve** - Reserve (AVAILABLE → RESERVED)
```http
PUT /api/v1/spots/123e4567-e89b-12d3-a456-426614174000/reserve
Authorization: Bearer <JWT_TOKEN>
```

**Called By**: Booking-service when a new booking is created.

**Allowed Transitions**:
- AVAILABLE → RESERVED ✓

**Rejected Transitions**:
- RESERVED → RESERVED (409 Conflict: "Spot is already RESERVED")
- OCCUPIED → RESERVED (409 Conflict: "Spot is OCCUPIED")

---

#### **PUT /api/v1/spots/{spotId}/occupy** - Occupy (RESERVED/AVAILABLE → OCCUPIED)
```http
PUT /api/v1/spots/123e4567-e89b-12d3-a456-426614174000/occupy
Authorization: Bearer <JWT_TOKEN>
```

**Called By**: Booking-service on driver check-in.

**Allowed Transitions**:
- AVAILABLE → OCCUPIED ✓ (walk-in)
- RESERVED → OCCUPIED ✓ (normal check-in)

**Rejected Transitions**:
- OCCUPIED → OCCUPIED (409 Conflict: "Spot is already OCCUPIED")

---

#### **PUT /api/v1/spots/{spotId}/release** - Release (RESERVED/OCCUPIED → AVAILABLE)
```http
PUT /api/v1/spots/123e4567-e89b-12d3-a456-426614174000/release
Authorization: Bearer <JWT_TOKEN>
```

**Called By**: Booking-service on cancellation or checkout.

**Allowed Transitions**:
- RESERVED → AVAILABLE ✓ (booking cancelled)
- OCCUPIED → AVAILABLE ✓ (driver checkout)

**Rejected Transitions**:
- AVAILABLE → AVAILABLE (409 Conflict: "Spot is already AVAILABLE")

---

### **4. UPDATE ENDPOINT** (Manager Only)

#### **PUT /api/v1/spots/{spotId}** - Update Spot Metadata
```http
PUT /api/v1/spots/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "spotType": "STANDARD",
  "vehicleType": "FOUR_WHEELER",
  "pricePerHour": 6.00,
  "isHandicapped": true,
  "isEVCharging": false,
  "floor": 1
}
```

**Behavior**: All fields are optional. Only non-null fields are applied.

**Immutable Fields** (cannot be changed):
- `spotNumber`
- `lotId`

**Example - Partial Update**:
```json
{
  "pricePerHour": 7.50
}
```
Only price is updated; other fields remain unchanged.

---

### **5. DELETE ENDPOINT** (Manager or Admin Only)

#### **DELETE /api/v1/spots/{spotId}**
```http
DELETE /api/v1/spots/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <JWT_TOKEN>
```

**Response**: 204 No Content

**Errors**:
- `404 Not Found`: Spot doesn't exist
- `403 Forbidden`: User lacks MANAGER or ADMIN role

---

### **6. SYSTEM ENDPOINTS** (Not in SpotResource - Spring Boot Default)

#### **GET /actuator/health** - Health Check
```http
GET /actuator/health
```

**Response**: 200 OK
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

#### **GET /v3/api-docs** - OpenAPI JSON Spec
```http
GET /v3/api-docs
```

Returns the complete OpenAPI 3.0 specification in JSON format (used by Swagger UI).

---

#### **GET /swagger-ui.html** - Swagger UI
Navigate to: `http://localhost:8083/swagger-ui.html`

Interactive API documentation with "Try It Out" functionality.

---

## 🗄 Database Schema

### **PostgreSQL Table: parking_spot**

```sql
CREATE TABLE parking_spot (
    spot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id UUID NOT NULL,
    spot_number VARCHAR(50) NOT NULL,
    floor INTEGER NOT NULL,
    spot_type VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    is_handicapped BOOLEAN NOT NULL DEFAULT FALSE,
    is_ev_charging BOOLEAN NOT NULL DEFAULT FALSE,
    price_per_hour NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: same spot number cannot exist twice per lot
    CONSTRAINT uk_lot_spot_number 
        UNIQUE (lot_id, spot_number),
    
    -- Performance indexes
    INDEX idx_spot_lot_id 
        ON parking_spot(lot_id),
    INDEX idx_spot_lot_status 
        ON parking_spot(lot_id, status),
    INDEX idx_spot_lot_type 
        ON parking_spot(lot_id, spot_type),
    INDEX idx_spot_vehicle_type 
        ON parking_spot(lot_id, vehicle_type)
);
```

### **Column Descriptions**

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| spot_id | UUID | NO | gen_random_uuid() | Primary key, distributed ID |
| lot_id | UUID | NO | - | Cross-service reference to parkinglot-service |
| spot_number | VARCHAR(50) | NO | - | Human-readable ID (e.g., "A-01", "Floor-1-B-12") |
| floor | INTEGER | NO | - | 0=Ground, 1=First, -1=Basement 1, etc. |
| spot_type | VARCHAR(20) | NO | - | Enum: COMPACT, STANDARD, LARGE, MOTORBIKE, EV |
| vehicle_type | VARCHAR(20) | NO | - | Enum: TWO_WHEELER, FOUR_WHEELER, HEAVY |
| status | VARCHAR(20) | NO | 'AVAILABLE' | Enum: AVAILABLE, RESERVED, OCCUPIED |
| is_handicapped | BOOLEAN | NO | FALSE | Accessible for disabled drivers |
| is_ev_charging | BOOLEAN | NO | FALSE | EV charging available (auto=TRUE if type=EV) |
| price_per_hour | NUMERIC(10, 2) | NO | - | Hourly rate (max value: 99,999,999.99) |
| created_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | Audit field (immutable after creation) |

### **Indexes & Constraints**

**Primary Key**: `spot_id` (UUID)
- Unique identifier for each parking spot
- Auto-generated using PostgreSQL's UUID function

**Unique Constraint**: `(lot_id, spot_number)`
- Ensures the same spot number doesn't exist twice within the same lot
- Composite unique index enables natural duplicate checking

**Performance Indexes**:
- `idx_spot_lot_id`: Fast queries filtered by lot
- `idx_spot_lot_status`: Fast queries by lot + status (e.g., available spots)
- `idx_spot_lot_type`: Fast queries by lot + spot type
- `idx_spot_vehicle_type`: Fast queries by lot + vehicle type

### **Query Performance Patterns**

```sql
-- Find all available spots in a lot (uses idx_spot_lot_status)
SELECT * FROM parking_spot 
WHERE lot_id = ? AND status = 'AVAILABLE';

-- Find specific spot (uses PK)
SELECT * FROM parking_spot 
WHERE spot_id = ?;

-- Check for duplicate spot number (uses uk_lot_spot_number)
SELECT COUNT(*) FROM parking_spot 
WHERE lot_id = ? AND spot_number = ?;

-- Count available spots (uses idx_spot_lot_status)
SELECT COUNT(*) FROM parking_spot 
WHERE lot_id = ? AND status = 'AVAILABLE';
```

---

## 🔐 Security & Authentication

### **Authentication Flow**

```
1. Client requests JWT from auth-service (POST /api/v1/auth/login)
   └─ Returns: {"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}

2. Client includes JWT in Authorization header of spot-service requests
   └─ Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

3. JwtAuthFilter (spot-service) intercepts request
   ├─ Extracts token from header
   ├─ Validates signature using JWT_SECRET (shared with auth-service)
   ├─ Parses claims (sub/email, userId, role)
   └─ Populates SecurityContext if valid

4. SecurityConfig enforces authorization rules
   ├─ Check role (DRIVER, MANAGER, ADMIN)
   ├─ Check endpoint access rules
   └─ Grant or deny access

5. If valid: request proceeds to SpotResource
   If invalid: GlobalExceptionHandler returns 401/403 error
```

### **JWT Token Structure** (set by auth-service)

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload** (Claims):
```json
{
  "sub": "user@example.com",          // subject (email)
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "DRIVER",                   // DRIVER | MANAGER | ADMIN
  "iat": 1680700800,                  // issued-at (Unix timestamp)
  "exp": 1680787200                   // expiry (Unix timestamp)
}
```

**Signature**:
```
HMACSHA256(
  base64UrlEncode(header) + "." + 
  base64UrlEncode(payload),
  JWT_SECRET
)
```

### **JWT Validation** (JwtUtil.java)

```java
public boolean isTokenValid(String token, String email) {
    try {
        String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException ex) {
        return false;
    }
}
```

**Checks**:
1. ✓ Signature is valid (signed with JWT_SECRET)
2. ✓ Token is not expired (exp claim > current time)
3. ✓ Email in token matches extracted email
4. ✓ Token structure is well-formed

---

### **Authorization Rules** (SecurityConfig.java)

#### **Public Endpoints** (No JWT Required)
```
GET  /api/v1/spots/{spotId}              → Anyone can fetch spot details
GET  /api/v1/spots/lot/**                → All lot-level browse endpoints
GET  /v3/api-docs/**                     → OpenAPI spec
GET  /swagger-ui/**                      → Swagger UI assets
GET  /actuator/health                    → Health check
```

#### **Authenticated Endpoints** (Any Valid JWT)
```
PUT  /api/v1/spots/{spotId}/reserve      → booking-service internal calls
PUT  /api/v1/spots/{spotId}/occupy       → booking-service internal calls
PUT  /api/v1/spots/{spotId}/release      → booking-service internal calls
```

#### **Manager Only** (JWT + hasRole('MANAGER'))
```
POST /api/v1/spots                       → Create single spot
POST /api/v1/spots/bulk                  → Bulk create spots
PUT  /api/v1/spots/{spotId}              → Update spot metadata
```

#### **Manager or Admin** (JWT + hasAnyRole('MANAGER', 'ADMIN'))
```
DELETE /api/v1/spots/{spotId}            → Delete spot
```

---

### **CORS Configuration**

Allowed Origins (development):
- `http://localhost:3000` (React Create React App)
- `http://localhost:5173` (Vite dev server)

**Update for Production**: Modify `SecurityConfig.corsConfigurationSource()` with production URL.

---

### **Security Best Practices Implemented**

| Practice | Implementation |
|----------|-----------------|
| **HTTPS Only** | Deploy with SSL/TLS certificate (not in dev) |
| **CSRF Disabled** | Stateless REST API using JWT (not vulnerable to CSRF) |
| **CORS Restricted** | Allows only specified origins, not all origins ("*") |
| **Session Stateless** | SessionCreationPolicy.STATELESS — no HttpSession/cookies |
| **Password Hashing** | JWT tokens only (no passwords stored in spot-service) |
| **Claim Validation** | Email + userId + role extracted and validated |
| **Expiration Check** | JwtUtil checks token expiry before allowing access |
| **Role-Based Access** | @PreAuthorize enforces role checks at method level |
| **No Credentials in Logs** | Tokens not logged; only email + role in debug logs |
| **Graceful Degradation** | Invalid JWT → request proceeds to authorization layer which rejects it |

---

## 📊 Business Logic & Rules

### **1. Spot Status State Machine**

**Valid Transitions**:
```
AVAILABLE ──┬──→ RESERVED  (booking created)
            └──→ OCCUPIED  (walk-in direct check-in)

RESERVED  ──┬──→ OCCUPIED  (driver arrived)
            └──→ AVAILABLE (booking cancelled)

OCCUPIED  ──→ AVAILABLE      (driver checkout)
```

**Enforced by**: SpotServiceImpl status transition methods + IllegalStateException on invalid transitions → 409 CONFLICT

---

### **2. Spot Number Uniqueness**

**Rule**: Same spot number cannot exist twice within the same lot.

**Enforced by**: 
- Database unique constraint: `UNIQUE(lot_id, spot_number)`
- SpotServiceImpl duplicate check: `existsByLotIdAndSpotNumber()`
- Throws `IllegalArgumentException` → 400 BAD REQUEST

**Example**:
- Lot A: A-01 (allowed)
- Lot B: A-01 (allowed - different lot)
- Lot A: A-01 again (rejected)

---

### **3. EV Spot Charging Auto-Activation**

**Rule**: If spotType = EV, then isEVCharging is automatically set to true.

**Enforced at Multiple Levels**:

1. **Entity Level** (@PrePersist):
   ```java
   if (SpotType.EV.equals(this.spotType)) {
       this.isEVCharging = true;
   }
   ```

2. **Service Level** (addSpot):
   ```java
   spot.setIsEVCharging(
       SpotType.EV.equals(request.getSpotType()) || request.isEVCharging()
   );
   ```

3. **Service Level** (updateSpot):
   ```java
   if (SpotType.EV.equals(request.getSpotType())) {
       spot.setIsEVCharging(true);
   }
   ```

**Result**: User cannot create EV spot without charging. Request's isEVCharging flag is ignored for EV types.

---

### **4. Price Per Hour Validation**

**Rule**: pricePerHour must be greater than 0 (strictly positive).

**Enforced by**: `@DecimalMin("0.0", inclusive=false)` on DTOs
- Throws `MethodArgumentNotValidException` → 400 BAD REQUEST

**Storage**: BigDecimal with precision=10, scale=2
- Supports values: 0.01 to 99,999,999.99
- Monetary precision (never float/double)

---

### **5. Bulk Spot Creation Auto-Numbering**

**Logic**:
```
spotNumberPrefix provided?
├─ YES → Use it (e.g., "A" → A-01, A-02, A-03)
└─ NO  → Use spotType.name() (e.g., "COMPACT" → COMPACT-01, COMPACT-02)

Format: prefix + "-" + zero-padded-index (2 digits)
Example: A-01, A-02, ..., A-10, A-11, A-12 ... A-99
```

**Idempotent Re-runs**: If a spot number already exists, it's skipped (logged as warning).

---

### **6. Immutable Spot Number & Lot ID**

**Rule**: After creation, spotNumber and lotId cannot be changed.

**Enforcement**:
- UpdateSpotRequest DTO does NOT include spotNumber or lotId fields
- Attempting to change them in the database directly would violate business logic
- Recommendation: Delete + recreate if reassignment needed

---

### **7. Cross-Service Lot Reference**

**Pattern**: 
```
spot-service ONLY stores lotId (UUID)
No JPA @ManyToOne or @JoinColumn to parkinglot-service
parkinglot-service manages parking lot records
spot-service trusts lotId validity at time of creation
```

**Implication**: parkinglot-service could be deleted without spot-service knowing.
**Recommendation**: parkinglot-service should define lot lifecycle policies (e.g., cascade deletes).

---

### **8. Read-Only Transactional Methods**

**Optimization**: All GET operations use `@Transactional(readOnly=true)`
- Signal to database for query optimization
- Prevent accidental writes
- May flush dirty objects (not applicable here)

---

## ⚙️ Configuration

### **application.yaml** - Environment Settings

```yaml
server:
  port: 8083                              # Service port

spring:
  application:
    name: spot-service

  datasource:
    url: jdbc:postgresql://localhost:5432/parkease_spot
    username: ${DB_USER:postgres}         # Env override or default
    password: ${DB_PASSWORD:yourpassword}  # Env override or default
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update                    # Apply incremental schema changes
    show-sql: true                        # Log SQL queries
    properties:
      hibernate:
        format_sql: true                  # Pretty-print SQL

jwt:
  secret: ${JWT_SECRET}                   # MUST match auth-service secret
  expiry: 86400000                        # 24 hours in milliseconds

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
    path: /v3/api-docs                    # OpenAPI JSON endpoint
  swagger-ui:
    path: /swagger-ui.html                # Swagger UI entry
    operations-sorter: method             # Sort endpoints by HTTP method

logging:
  level:
    com.parkease.spot: DEBUG              # Verbose logging for spot-service
    org.springframework.security: WARN    # Less verbose Spring Security logs
    org.hibernate.SQL: DEBUG              # Log SQL statements
```

### **Environment Variables Required**

| Variable | Default | Purpose | Example |
|----------|---------|---------|---------|
| `DB_USER` | postgres | PostgreSQL username | postgres |
| `DB_PASSWORD` | yourpassword | PostgreSQL password | MySecurePass123 |
| `JWT_SECRET` | (none) | Shared JWT secret with auth-service | your-256-bit-secret-key |

---

### **OpenApiConfig** - Swagger/OpenAPI Configuration

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ParkEase — Spot Service API")
                .version("v1")
                .description("Manages parking spots...")
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8083")
                    .description("Local Development")
            ))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            );
    }
}
```

**Accessible At**:
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

---

### **SecurityConfig** - Spring Security Configuration

**Key Features**:
- CSRF disabled (stateless REST API)
- CORS enabled with allowlist (localhost:3000, localhost:5173)
- Stateless session policy (no HttpSession)
- JWT filter before UsernamePasswordAuthenticationFilter
- Role-based authorization rules
- Method-level security (@PreAuthorize)

---

## ❌ Exception Handling

### **GlobalExceptionHandler** - Centralized Exception Management

All exceptions are caught, logged, and transformed into structured ApiError JSON responses.

#### **Error Response Format**

```json
{
  "timestamp": "2026-04-03T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed — check the errors field for details",
  "errors": [
    "spotNumber: Spot number is required (e.g. A-01, B-12)",
    "pricePerHour: Price per hour must be greater than 0"
  ]
}
```

---

### **Exception → HTTP Status Mapping**

| Exception | HTTP Status | Message | Cause |
|-----------|-------------|---------|-------|
| `MethodArgumentNotValidException` | 400 | Validation failed — DTO fields violated | @NotNull/@NotBlank/@Decimal validation |
| `IllegalArgumentException` | 400 | Duplicate spotNumber / bad input | Business rule violation |
| `MethodArgumentTypeMismatchException` | 400 | Invalid value for parameter | Wrong UUID/Enum format in path |
| `RuntimeException` ("not found") | 404 | Spot not found with id: ... | Spot doesn't exist |
| `IllegalStateException` | 409 | Conflict — invalid status transition | AVAILABLE → OCCUPIED (not allowed) |
| `AccessDeniedException` | 403 | You do not have permission | User lacks required role |
| `AuthenticationException` | 401 | Authentication required | Missing/invalid JWT |
| `Exception` (catch-all) | 500 | Unexpected error | Unhandled exception |

---

### **Example Error Response: 409 Conflict**

**Request**: PUT /api/v1/spots/{spotId}/reserve on an already RESERVED spot
**Response**:
```json
{
  "timestamp": "2026-04-03T20:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Spot 550e8400-e29b-41d4-a716-446655440000 is already RESERVED — cannot reserve again",
  "errors": []
}
```

---

### **Example Error Response: 404 Not Found**

**Request**: GET /api/v1/spots/invalid-uuid
**Response**:
```json
{
  "timestamp": "2026-04-03T20:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Spot not found with id: invalid-uuid",
  "errors": []
}
```

---

## 🚀 Build & Deployment

### **Development Setup**

#### **1. Prerequisites**
- JDK 17 or higher
- Maven 3.9+
- PostgreSQL 12+
- Git

#### **2. Clone Repository**
```bash
git clone https://github.com/parkease/spot-service.git
cd spot-service
```

#### **3. Configure Environment**
Create `.env` file in project root:
```env
DB_USER=postgres
DB_PASSWORD=your_secure_password
JWT_SECRET=your-256-bit-secret-key-must-match-auth-service
```

#### **4. Set Environment Variables** (OS-specific)

**Linux/Mac**:
```bash
export DB_USER=postgres
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your-256-bit-secret-key
```

**Windows (PowerShell)**:
```powershell
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "your_secure_password"
$env:JWT_SECRET = "your-256-bit-secret-key"
```

#### **5. Create PostgreSQL Database**
```sql
CREATE DATABASE parkease_spot;
```

#### **6. Build Project**
```bash
./mvnw clean install
```

Or on Windows:
```bash
mvnw.cmd clean install
```

#### **7. Run Application**
```bash
./mvnw spring-boot:run
```

**Log Output** (when started successfully):
```
2026-04-03 10:00:00 INFO  com.parkease.spot.SpotApplication - Starting SpotApplication
2026-04-03 10:00:05 INFO  org.springframework.boot.undertow - Undertow started on port 8083
2026-04-03 10:00:05 INFO  com.parkease.spot.SpotApplication - Started SpotApplication in 5.123 seconds
```

---

### **Testing the Service**

#### **1. Health Check**
```bash
curl http://localhost:8083/actuator/health
# Response: {"status":"UP"}
```

#### **2. Access Swagger UI**
Navigate to: `http://localhost:8083/swagger-ui.html`

#### **3. Get JWT Token** (from auth-service on port 8081)
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@parkease.com","password":"password123"}'

# Response:
# {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}
```

#### **4. Create a Parking Spot**
```bash
curl -X POST "http://localhost:8083/api/v1/spots?lotId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "spotNumber":"A-01",
    "floor":0,
    "spotType":"STANDARD",
    "vehicleType":"FOUR_WHEELER",
    "pricePerHour":5.50,
    "isHandicapped":false,
    "isEVCharging":false
  }'
```

#### **5. Get Available Spots**
```bash
curl http://localhost:8083/api/v1/spots/lot/550e8400-e29b-41d4-a716-446655440000/available
```

---

### **Docker Deployment**

#### **Dockerfile** (if created)
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/spot-0.0.1-SNAPSHOT.jar app.jar

ENV DB_USER=postgres
ENV DB_PASSWORD=password
ENV JWT_SECRET=your-secret

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### **Docker Compose** (if created)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: parkease_spot
    ports:
      - "5432:5432"

  spot-service:
    build: .
    ports:
      - "8083:8083"
    depends_on:
      - postgres
    environment:
      DB_USER: postgres
      DB_PASSWORD: password
      JWT_SECRET: your-secret
```

---

### **Production Deployment Checklist**

- [ ] Update CORS origins to production domain
- [ ] Set secure JWT_SECRET (256-bit minimum)
- [ ] Enable HTTPS/SSL certificates
- [ ] Set `show-sql: false` in application.yaml
- [ ] Set logging level to INFO or WARN
- [ ] Configure database connection pool size
- [ ] Set up database backups
- [ ] Enable monitoring & alerting
- [ ] Configure health check endpoint
- [ ] Test all API endpoints thoroughly
- [ ] Document production URLs & access procedures
- [ ] Set up CI/CD pipeline (GitHub Actions, etc.)

---

## 📝 Summary

**Spot Service** is a core microservice managing parking spot data and lifecycle within the ParkEase system. It provides:

✅ RESTful APIs for spot creation, browsing, and status management
✅ JWT-based authentication & role-based authorization
✅ Strict state machine enforcement for spot status transitions
✅ PostgreSQL persistence with optimized indexing
✅ Comprehensive exception handling & validation
✅ OpenAPI 3.0 / Swagger documentation
✅ Cross-service communication support (booking-service)
✅ Bulk operations for efficient spot creation
✅ Advanced filtering by type, vehicle category, floor, and features

---

## 🔗 Related Services

- **Auth Service** (port 8081): JWT token generation & user management
- **Booking Service** (port 8082): Manages bookings using spot status transitions
- **Parking Lot Service**: Creates and manages parking lots
- **Vehicle Service**: Defines vehicle types & compatibility
- **Analytics Service**: Uses spot data for reporting

---

## 📞 Support & Troubleshooting

**Common Issues**:

1. **JWT_SECRET not found**: Ensure environment variable is set
2. **Database connection error**: Verify PostgreSQL is running and credentials are correct
3. **Port 8083 already in use**: Change port in application.yaml or kill process: `lsof -i :8083`
4. **Swagger UI not loading**: Clear browser cache, verify springdoc dependency is included
5. **Status transition returns 409**: Check current spot status (may not support that transition)

---

**Last Updated**: April 3, 2026
**Version**: 1.0.0
**Maintainer**: ParkEase Development Team

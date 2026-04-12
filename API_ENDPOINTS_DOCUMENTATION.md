# ParkEase REST API Endpoints Documentation

## Overview
- **API Gateway Port:** 8080
- **Base URL:** `http://localhost:8080`
- **Service Discovery:** Eureka Server (port 8761)
- **Total Services:** 8
- **Total Endpoints:** 95+

---

## API Gateway Configuration

The API Gateway (Spring Cloud Gateway) routes requests to backend services based on path predicates:

| Route | Service | URI | Path Predicate |
|-------|---------|-----|-----------------|
| auth-service | auth-service | `lb://auth-service` | `/api/v1/auth/**` |
| parkinglot-service | parkinglot-service | `lb://parkinglot-service` | `/api/v1/lots/**` |
| spot-service | spot-service | `lb://spot-service` | `/api/v1/spots/**` |
| booking-service | booking-service | `lb://booking-service` | `/api/v1/bookings/**` |
| payment-service | payment-service | `lb://payment-service` | `/api/v1/payments/**` |
| vehicle-service | vehicle-service | `lb://vehicle-service` | `/api/v1/vehicles/**` |
| notification-service | notification-service | `lb://notification-service` | `/api/v1/notifications/**` |
| analytics-service | analytics-service | `lb://analytics-service` | `/api/v1/analytics/**` |

---

## 1. AUTH SERVICE

**Base Path:** `/api/v1/auth`  
**Port:** 8083 (direct) | 8080 (via gateway)  
**Controller:** `AuthResource`

### User Registration & Authentication

#### POST /register
- **Authentication:** Public
- **Role:** DRIVER, MANAGER
- **Description:** Register a new user
- **Request Body:**
  ```json
  {
    "name": "string",
    "email": "string",
    "phoneNumber": "string",
    "role": "DRIVER|MANAGER",
    "password": "string"
  }
  ```
- **Response:** `UserProfileResponse` + 201 CREATED
- **Status Codes:** 201, 400

#### POST /login
- **Authentication:** Public
- **Description:** Login and receive JWT token
- **Request Body:**
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```
- **Response:** `AuthResponse` (includes JWT token + refresh token)
- **Status Codes:** 200, 401

#### POST /logout
- **Authentication:** Required (Bearer JWT)
- **Description:** Logout (invalidates token)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** "Logged out successfully" + 200
- **Status Codes:** 200, 401

#### POST /refresh
- **Authentication:** Public
- **Description:** Refresh JWT token using refresh token
- **Request Body:**
  ```json
  {
    "token": "string"
  }
  ```
- **Response:** `AuthResponse` (new JWT)
- **Status Codes:** 200, 401

#### GET /profile
- **Authentication:** Required (Bearer JWT)
- **Description:** Get current logged-in user's profile
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `UserProfileResponse` + 200
- **Status Codes:** 200, 401

#### PUT /profile
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER, ADMIN
- **Description:** Update user profile (name, phone, etc.)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `UpdateProfileRequest`
- **Response:** `UserProfileResponse` + 200
- **Status Codes:** 200, 400, 401

#### PUT /password
- **Authentication:** Required (Bearer JWT)
- **Description:** Change password
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:**
  ```json
  {
    "oldPassword": "string",
    "newPassword": "string"
  }
  ```
- **Response:** "Password changed successfully" + 200
- **Status Codes:** 200, 400, 401, 403

#### DELETE /deactivate
- **Authentication:** Required (Bearer JWT)
- **Description:** Soft-delete account (deactivate)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** "Account deactivated successfully" + 200
- **Status Codes:** 200, 401

### OTP & Password Reset

#### POST /send-otp
- **Authentication:** Public
- **Description:** Send OTP to email for registration or password reset
- **Request Body:**
  ```json
  {
    "email": "string",
    "purpose": "REGISTRATION|FORGOT_PASSWORD"
  }
  ```
- **Response:** "OTP sent successfully" + 200
- **Status Codes:** 200, 400 (rate-limited)

#### POST /verify-otp
- **Authentication:** Public
- **Description:** Verify OTP for registration or password reset
- **Request Body:**
  ```json
  {
    "email": "string",
    "otp": "string",
    "purpose": "REGISTRATION|FORGOT_PASSWORD"
  }
  ```
- **Response:** "OTP verified successfully" + 200
- **Status Codes:** 200, 400 (invalid OTP)

#### POST /forgot-password
- **Authentication:** Public
- **Description:** Convenience endpoint - sends OTP for password reset (aliases /send-otp)
- **Request Body:**
  ```json
  {
    "email": "string"
  }
  ```
- **Response:** "OTP sent successfully" + 200
- **Status Codes:** 200, 400, 429 (rate-limited)

#### POST /reset-password
- **Authentication:** Public
- **Description:** Reset password after OTP verification
- **Request Body:**
  ```json
  {
    "email": "string",
    "newPassword": "string",
    "otp": "string"
  }
  ```
- **Response:** "Password reset successfully" + 200
- **Status Codes:** 200, 400, 401

### Admin Management Endpoints

#### GET /users
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get all users, optionally filtered by role
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `role` (optional): DRIVER | MANAGER | ADMIN
- **Response:** `List<UserProfileResponse>` + 200
- **Status Codes:** 200, 403

#### PUT /users/{userId}/deactivate
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Deactivate user account
- **Path Variables:** `userId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `UserProfileResponse` + 200
- **Status Codes:** 200, 404, 403

#### PUT /users/{userId}/reactivate
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Reactivate user account
- **Path Variables:** `userId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `UserProfileResponse` + 200
- **Status Codes:** 200, 404, 403

#### POST /admin/login
- **Authentication:** Public
- **Description:** Admin login (separate from user login)
- **Request Body:**
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```
- **Response:** `AdminAuthResponse` (includes JWT) + 200
- **Status Codes:** 200, 401

#### POST /admin/create
- **Authentication:** Required (Bearer JWT)
- **Role:** Super Admin only
- **Description:** Create new admin account
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `AdminCreateRequest`
- **Response:** `AdminProfileResponse` + 201
- **Status Codes:** 201, 400, 403

#### DELETE /admin/{adminId}
- **Authentication:** Required (Bearer JWT)
- **Role:** Super Admin only
- **Description:** Soft-delete admin account
- **Path Variables:** `adminId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** "Admin deactivated successfully" + 200
- **Status Codes:** 200, 403, 404

#### GET /admin/all
- **Authentication:** Required (Bearer JWT)
- **Role:** Super Admin only
- **Description:** List all admin accounts
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<AdminProfileResponse>` + 200
- **Status Codes:** 200, 403

---

## 2. VEHICLE SERVICE

**Base Path:** `/api/v1/vehicles`  
**Port:** 8086 (direct) | 8080 (via gateway)  
**Controller:** `VehicleResource`  
**Base Entry Point:** Port 8086

### Vehicle Management

#### POST /register
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Register a new vehicle (ownerId extracted from JWT)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:**
  ```json
  {
    "licensePlate": "string",
    "make": "string",
    "model": "string",
    "color": "string",
    "vehicleType": "TWO_WHEELER|FOUR_WHEELER|HEAVY",
    "isEV": "boolean"
  }
  ```
- **Response:** `VehicleResponse` + 201 CREATED
- **Status Codes:** 201, 400, 401, 409 (duplicate plate)

#### GET /{vehicleId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN (any)
- **Description:** Get vehicle by ID
- **Path Variables:** `vehicleId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `VehicleResponse` + 200
- **Status Codes:** 200, 403, 404, 401

#### GET /owner/{ownerId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN (any)
- **Description:** Get all vehicles for a driver
- **Path Variables:** `ownerId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<VehicleResponse>` + 200
- **Status Codes:** 200, 403, 401

#### GET /plate/{licensePlate}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN (any)
- **Description:** Find vehicle by license plate
- **Path Variables:** `licensePlate` (string)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `VehicleResponse` + 200
- **Status Codes:** 200, 403, 404, 401

#### GET /all
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get all vehicles in system
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<VehicleResponse>` + 200
- **Status Codes:** 200, 403, 401

#### PUT /{vehicleId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own only)
- **Description:** Update vehicle details
- **Path Variables:** `vehicleId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `UpdateVehicleRequest` (partial update)
- **Response:** `VehicleResponse` + 200
- **Status Codes:** 200, 400, 403, 404, 401

#### DELETE /{vehicleId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own only)
- **Description:** Soft-delete vehicle
- **Path Variables:** `vehicleId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 204 NO CONTENT
- **Status Codes:** 204, 403, 404, 401

#### GET /{vehicleId}/type
- **Authentication:** Required (Bearer JWT)
- **Description:** Get vehicle type (used by booking-service)
- **Path Variables:** `vehicleId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `VehicleType` (string enum) + 200
- **Status Codes:** 200, 404, 401

#### GET /{vehicleId}/isEV
- **Authentication:** Required (Bearer JWT)
- **Description:** Check if vehicle is EV (used by booking-service)
- **Path Variables:** `vehicleId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `boolean` + 200
- **Status Codes:** 200, 404, 401

---

## 3. PARKING LOT SERVICE

**Base Path:** `/api/v1/lots`  
**Port:** 8084 (direct) | 8080 (via gateway)  
**Controller:** `ParkingLotResource`

### Lot Management

#### POST /
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER only
- **Description:** Create new parking lot
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `CreateLotRequest`
- **Response:** `LotResponse` + 201 CREATED
- **Status Codes:** 201, 400, 401, 403

#### GET /{lotId}
- **Authentication:** Public
- **Description:** Get parking lot by ID
- **Path Variables:** `lotId` (UUID)
- **Response:** `LotResponse` + 200
- **Status Codes:** 200, 404

#### GET /city/{city}
- **Authentication:** Public
- **Description:** Search lots by city
- **Path Variables:** `city` (string)
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200

#### GET /nearby
- **Authentication:** Public
- **Description:** GPS proximity search (Haversine formula)
- **Query Parameters:**
  - `lat` (required): double
  - `lng` (required): double
  - `radius` (optional): double, default=5.0 km
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200

#### GET /search
- **Authentication:** Public
- **Description:** Keyword search (name, address, city)
- **Query Parameters:**
  - `keyword` (required): string
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200

#### GET /manager/{managerId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own) | ADMIN (any)
- **Description:** Get lots managed by a specific manager
- **Path Variables:** `managerId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200, 403, 401

#### GET /all
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get all lots (approved + pending)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200, 403, 401

#### GET /pending
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get lots pending admin approval
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<LotResponse>` + 200
- **Status Codes:** 200, 403, 401

#### PUT /{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own) | ADMIN
- **Description:** Update lot details
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `UpdateLotRequest`
- **Response:** `LotResponse` + 200
- **Status Codes:** 200, 403, 404, 400, 401

#### PUT /{lotId}/toggleOpen
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own lot only)
- **Description:** Toggle lot open/closed status
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `LotResponse` + 200
- **Status Codes:** 200, 403, 404, 401

#### PUT /{lotId}/approve
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Approve pending lot registration
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `LotResponse` + 200
- **Status Codes:** 200, 403, 404, 401

#### PUT /{lotId}/decrement
- **Authentication:** Required (Bearer JWT)
- **Description:** Decrement available spots (called by booking-service)
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 200
- **Status Codes:** 200, 404, 401

#### PUT /{lotId}/increment
- **Authentication:** Required (Bearer JWT)
- **Description:** Increment available spots (called by booking-service)
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 200
- **Status Codes:** 200, 404, 401

#### DELETE /{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own) | ADMIN
- **Description:** Delete parking lot
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 204 NO CONTENT
- **Status Codes:** 204, 403, 404, 401

---

## 4. SPOT SERVICE

**Base Path:** `/api/v1/spots`  
**Port:** 8085 (direct) | 8080 (via gateway)  
**Controller:** `SpotResource`

### Spot Management (Create)

#### POST /
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER only
- **Description:** Add single spot to lot
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `lotId` (required): UUID
- **Request Body:** `AddSpotRequest`
- **Response:** `SpotResponse` + 201 CREATED
- **Status Codes:** 201, 400, 401, 403

#### POST /bulk
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER only
- **Description:** Bulk add spots (auto-generated sequential numbers)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `lotId` (required): UUID
- **Request Body:** `BulkAddSpotRequest`
- **Response:** `List<SpotResponse>` + 201 CREATED
- **Status Codes:** 201, 400, 401, 403

### Spot Management (Read) - Public

#### GET /{spotId}
- **Authentication:** Public
- **Description:** Get spot by ID
- **Path Variables:** `spotId` (UUID)
- **Response:** `SpotResponse` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}
- **Authentication:** Public
- **Description:** Get all spots in a lot
- **Path Variables:** `lotId` (UUID)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/available
- **Authentication:** Public
- **Description:** Get only AVAILABLE spots in lot
- **Path Variables:** `lotId` (UUID)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/type/{spotType}
- **Authentication:** Public
- **Description:** Get spots filtered by spot type
- **Path Variables:**
  - `lotId` (UUID)
  - `spotType` (enum: COMPACT | STANDARD | LARGE | MOTORBIKE | EV)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/vehicle/{vehicleType}
- **Authentication:** Public
- **Description:** Get spots compatible with vehicle type
- **Path Variables:**
  - `lotId` (UUID)
  - `vehicleType` (enum: TWO_WHEELER | FOUR_WHEELER | HEAVY)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/floor/{floor}
- **Authentication:** Public
- **Description:** Get spots on specific floor
- **Path Variables:**
  - `lotId` (UUID)
  - `floor` (integer: 0=Ground, 1=First, -1=Basement1)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/ev
- **Authentication:** Public
- **Description:** Get EV charging spots in lot
- **Path Variables:** `lotId` (UUID)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/handicapped
- **Authentication:** Public
- **Description:** Get handicapped accessible spots
- **Path Variables:** `lotId` (UUID)
- **Response:** `List<SpotResponse>` + 200
- **Status Codes:** 200, 404

#### GET /lot/{lotId}/count
- **Authentication:** Public
- **Description:** Count AVAILABLE spots
- **Path Variables:** `lotId` (UUID)
- **Response:** `Long` (count) + 200
- **Status Codes:** 200, 404

### Spot Status Transitions (Internal - booking-service)

#### PUT /{spotId}/reserve
- **Authentication:** Required (Bearer JWT)
- **Description:** Reserve spot (AVAILABLE → RESERVED)
- **Path Variables:** `spotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `SpotResponse` + 200
- **Status Codes:** 200, 404, 409 (invalid transition)

#### PUT /{spotId}/occupy
- **Authentication:** Required (Bearer JWT)
- **Description:** Occupy spot (RESERVED/AVAILABLE → OCCUPIED)
- **Path Variables:** `spotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `SpotResponse` + 200
- **Status Codes:** 200, 404, 409

#### PUT /{spotId}/release
- **Authentication:** Required (Bearer JWT)
- **Description:** Release spot (RESERVED/OCCUPIED → AVAILABLE)
- **Path Variables:** `spotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `SpotResponse` + 200
- **Status Codes:** 200, 404, 409

### Spot Management (Update/Delete)

#### PUT /{spotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER only
- **Description:** Update spot metadata (partial)
- **Path Variables:** `spotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `UpdateSpotRequest`
- **Response:** `SpotResponse` + 200
- **Status Codes:** 200, 400, 401, 403, 404

#### DELETE /{spotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER | ADMIN
- **Description:** Delete spot permanently
- **Path Variables:** `spotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 204 NO CONTENT
- **Status Codes:** 204, 401, 403, 404

---

## 5. BOOKING SERVICE

**Base Path:** `/api/v1/bookings`  
**Port:** 8081 (direct) | 8080 (via gateway)  
**Controller:** `BookingResource`

### Booking Creation & Retrieval

#### POST /
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Create new booking (PRE_BOOKING or WALK_IN)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `CreateBookingRequest`
- **Response:** `BookingResponse` + 201 CREATED
- **Status Codes:** 201, 400, 401, 403, 409

#### GET /all
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get all bookings platform-wide
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<BookingResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /my
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Get driver's own bookings
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<BookingResponse>` + 200
- **Status Codes:** 200, 401

#### GET /history
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Get booking history (COMPLETED + CANCELLED)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<BookingResponse>` + 200
- **Status Codes:** 200, 401

#### GET /lot/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER | ADMIN
- **Description:** Get all bookings for lot
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<BookingResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /lot/{lotId}/active
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER | ADMIN
- **Description:** Get active bookings for lot
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<BookingResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /{bookingId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | MANAGER | ADMIN
- **Description:** Get booking by ID
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `BookingResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### GET /{bookingId}/fare
- **Authentication:** Required (Bearer JWT)
- **Description:** Get real-time fare estimate
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `FareCalculationResponse` + 200
- **Status Codes:** 200, 404, 401

### Booking State Transitions

#### PUT /{bookingId}/checkin
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Check in to PRE_BOOKING (RESERVED → ACTIVE)
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `BookingResponse` + 200
- **Status Codes:** 200, 403, 404, 401, 409

#### PUT /{bookingId}/checkout
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER | MANAGER | ADMIN
- **Description:** Check out (ACTIVE → COMPLETED), calculates fare
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `BookingResponse` + 200
- **Status Codes:** 200, 403, 404, 401

#### PUT /{bookingId}/cancel
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | MANAGER (lot-scoped) | ADMIN
- **Description:** Cancel RESERVED or ACTIVE booking
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `BookingResponse` + 200
- **Status Codes:** 200, 403, 404, 401, 409

#### PUT /{bookingId}/extend
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Extend booking end time
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `ExtendBookingRequest`
- **Response:** `BookingResponse` + 200
- **Status Codes:** 200, 400, 403, 404, 401

---

## 6. PAYMENT SERVICE

**Base Path:** `/api/v1/payments`  
**Port:** 8082 (direct) | 8080 (via gateway)  
**Controller:** `PaymentController`

### Payment Initiation & Retrieval

#### POST /initiate
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Initiate payment for completed booking
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `InitiatePaymentRequest`
- **Response:** `PaymentResponse` + 201 CREATED
- **Status Codes:** 201, 400, 401, 403

#### GET /booking/{bookingId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own booking) | ADMIN
- **Description:** Get payment by booking ID
- **Path Variables:** `bookingId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `PaymentResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### GET /user/{userId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN
- **Description:** Get all payments by user
- **Path Variables:** `userId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<PaymentResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /transaction/{transactionId}
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get payment by transaction ID
- **Path Variables:** `transactionId` (string)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `PaymentResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### GET /{paymentId}/status
- **Authentication:** Required (Bearer JWT)
- **Description:** Get payment status
- **Path Variables:** `paymentId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `PaymentStatusResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### GET /history
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER only
- **Description:** Get driver's payment history
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<PaymentResponse>` + 200
- **Status Codes:** 200, 401

### Refunds & Revenue

#### POST /{paymentId}/refund
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER | ADMIN
- **Description:** Process manual refund
- **Path Variables:** `paymentId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `PaymentResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### GET /revenue/lot/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own lot) | ADMIN
- **Description:** Get lot revenue (date range)
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `from` (required): ISO DateTime (e.g., 2026-01-01T00:00:00)
  - `to` (required): ISO DateTime
- **Response:** `RevenueResponse` + 200
- **Status Codes:** 200, 401, 403

#### GET /revenue/lot/{lotId}/daily
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER (own lot) | ADMIN
- **Description:** Get daily revenue breakdown
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `from` (required): ISO DateTime
  - `to` (required): ISO DateTime
- **Response:** `List<DailyRevenueResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /revenue/platform
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get platform-wide revenue
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `from` (required): ISO DateTime
  - `to` (required): ISO DateTime
- **Response:** `RevenueResponse` + 200
- **Status Codes:** 200, 401, 403

### Receipt Download

#### GET /{paymentId}/receipt
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN
- **Description:** Download PDF receipt
- **Path Variables:** `paymentId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** PDF file (application/pdf) + 200
- **Status Codes:** 200, 401, 403, 404

---

## 7. NOTIFICATION SERVICE

**Base Path:** `/api/v1/notifications`  
**Port:** (direct) | 8080 (via gateway)  
**Controller:** `NotificationController`

### User Notifications

#### GET /my
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER
- **Description:** Get all notifications for user
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<NotificationResponse>` + 200
- **Status Codes:** 200, 401

#### GET /my/unread
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER
- **Description:** Get unread notifications
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<NotificationResponse>` + 200
- **Status Codes:** 200, 401

#### GET /my/unread/count
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER
- **Description:** Get unread notification count
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `UnreadCountResponse` + 200
- **Status Codes:** 200, 401

#### PUT /{notificationId}/read
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER (own only)
- **Description:** Mark single notification as read
- **Path Variables:** `notificationId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `NotificationResponse` + 200
- **Status Codes:** 200, 401, 403, 404

#### PUT /my/read-all
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER, MANAGER
- **Description:** Mark all notifications as read
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 204 NO CONTENT
- **Status Codes:** 204, 401

#### DELETE /{notificationId}
- **Authentication:** Required (Bearer JWT)
- **Role:** DRIVER (own) | ADMIN
- **Description:** Delete notification
- **Path Variables:** `notificationId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** Empty body + 204 NO CONTENT
- **Status Codes:** 204, 401, 403, 404

### Admin Notifications

#### GET /all
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get all notifications system-wide
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<NotificationResponse>` + 200
- **Status Codes:** 200, 401, 403

#### POST /broadcast
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Broadcast promo notification to users
- **Headers:** `Authorization: Bearer <JWT>`
- **Request Body:** `BroadcastNotificationRequest`
- **Response:** Empty body + 202 ACCEPTED
- **Status Codes:** 202, 400, 401, 403

---

## 8. ANALYTICS SERVICE

**Base Path:** `/api/v1/analytics`  
**Port:** (direct) | 8080 (via gateway)  
**Controller:** `AnalyticsController`

### Occupancy Analytics

#### GET /occupancy/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get real-time occupancy rate
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `OccupancyRateResponse` + 200
- **Status Codes:** 200, 401, 403

#### GET /occupancy/{lotId}/hourly
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get hourly occupancy breakdown
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<HourlyOccupancyResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /occupancy/{lotId}/peak
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get peak hours analysis
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `topN` (optional): integer, default=5
- **Response:** `List<PeakHourResponse>` + 200
- **Status Codes:** 200, 401, 403

### Revenue Analytics

#### GET /revenue/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get lot revenue (date range)
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `from` (required): ISO DateTime
  - `to` (required): ISO DateTime
- **Response:** `RevenueDto` + 200
- **Status Codes:** 200, 401, 403

#### GET /revenue/{lotId}/daily
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get daily revenue breakdown
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `from` (required): ISO DateTime
  - `to` (required): ISO DateTime
- **Response:** `List<DailyRevenueDto>` + 200
- **Status Codes:** 200, 401, 403

### Other Analytics

#### GET /spot-types/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get spot type utilization
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `List<SpotTypeUtilisationResponse>` + 200
- **Status Codes:** 200, 401, 403

#### GET /avg-duration/{lotId}
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get average parking duration
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `AvgDurationResponse` + 200
- **Status Codes:** 200, 401, 403

#### GET /platform/summary
- **Authentication:** Required (Bearer JWT)
- **Role:** ADMIN only
- **Description:** Get platform-wide summary
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `PlatformSummaryResponse` + 200
- **Status Codes:** 200, 401, 403

#### GET /report/{lotId}/daily
- **Authentication:** Required (Bearer JWT)
- **Role:** MANAGER, ADMIN
- **Description:** Get daily comprehensive report
- **Path Variables:** `lotId` (UUID)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `DailyReportResponse` + 200
- **Status Codes:** 200, 401, 403

---

## Authentication & Authorization

### Bearer Token Format
All protected endpoints require JWT token in header:
```
Authorization: Bearer <JWT_TOKEN>
```

### User Roles
- **DRIVER:** End users who book parking spots
- **MANAGER:** Parking lot operators who manage lots and spots
- **ADMIN:** Platform administrators with elevated privileges
- **SUPER_ADMIN:** Can create and manage admin accounts

### JWT Token Claims
Standard token includes:
- `userId` (UUID)
- `email` (string)
- `role` (string: DRIVER | MANAGER | ADMIN)
- `isSuperAdmin` (boolean)
- `exp` (expiration timestamp)

---

## Common DTO Models

### Request/Response Objects Used

#### VehicleResponse
```json
{
  "vehicleId": "uuid",
  "ownerId": "uuid",
  "licensePlate": "string",
  "make": "string",
  "model": "string",
  "color": "string",
  "vehicleType": "TWO_WHEELER|FOUR_WHEELER|HEAVY",
  "isEV": "boolean",
  "isActive": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

#### LotResponse
```json
{
  "lotId": "uuid",
  "managerId": "uuid",
  "name": "string",
  "address": "string",
  "city": "string",
  "latitude": "double",
  "longitude": "double",
  "totalSpots": "integer",
  "availableSpots": "integer",
  "pricePerHour": "decimal",
  "isOpen": "boolean",
  "isApproved": "boolean",
  "amenities": ["string"]
}
```

#### SpotResponse
```json
{
  "spotId": "uuid",
  "lotId": "uuid",
  "spotNumber": "string",
  "spotType": "COMPACT|STANDARD|LARGE|MOTORBIKE|EV",
  "status": "AVAILABLE|RESERVED|OCCUPIED",
  "floor": "integer",
  "isEVCharging": "boolean",
  "isHandicapped": "boolean",
  "pricePerHour": "decimal"
}
```

#### BookingResponse
```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "spotId": "uuid",
  "lotId": "uuid",
  "vehicleId": "uuid",
  "bookingType": "PRE_BOOKING|WALK_IN",
  "status": "RESERVED|ACTIVE|COMPLETED|CANCELLED",
  "plannedStartTime": "datetime",
  "plannedEndTime": "datetime",
  "checkInTime": "datetime",
  "checkOutTime": "datetime",
  "fare": "decimal"
}
```

#### PaymentResponse
```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "amount": "decimal",
  "status": "PENDING|SUCCESS|FAILED|REFUNDED",
  "transactionId": "string",
  "paymentMethod": "CREDIT_CARD|DEBIT_CARD|UPI|WALLET",
  "createdAt": "datetime"
}
```

#### NotificationResponse
```json
{
  "notificationId": "uuid",
  "userId": "uuid",
  "title": "string",
  "message": "string",
  "type": "BOOKING|PAYMENT|PROMO|SYSTEM",
  "channel": "APP|SMS|EMAIL",
  "isRead": "boolean",
  "createdAt": "datetime"
}
```

---

## Error Handling

### Standard Error Responses

#### 400 Bad Request
```json
{
  "timestamp": "datetime",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: ...",
  "path": "/api/v1/..."
}
```

#### 401 Unauthorized
```json
{
  "message": "Missing or invalid JWT token"
}
```

#### 403 Forbidden
```json
{
  "message": "Insufficient permissions for this resource"
}
```

#### 404 Not Found
```json
{
  "message": "Resource not found"
}
```

#### 409 Conflict
```json
{
  "message": "Conflict: Invalid state transition or duplicate resource"
}
```

---

## Service Communication Map

```
Client → API Gateway (8080)
  ↓
  ├→ Auth Service (8083)
  ├→ Vehicle Service (8086) — calls SpotService
  ├→ Parking Lot Service (8084)
  ├→ Spot Service (8085)
  ├→ Booking Service (8081) — calls VehicleService, SpotService, LotService
  ├→ Payment Service (8082) — listens to booking events
  ├→ Notification Service — listens to booking/payment events
  └→ Analytics Service — listens to booking events

Service Discovery: Eureka (8761)
Message Broker: RabbitMQ (event-driven communication)
```

---

## Summary Statistics

| Metric | Count |
|--------|-------|
| Total Services | 8 |
| Total API Endpoints | 95+ |
| Authentication Required | ~85% |
| Public Endpoints | ~15% |
| Role-Protected Endpoints | ~70% |

### Endpoints by Service
- Auth Service: 23 endpoints
- Vehicle Service: 9 endpoints
- Parking Lot Service: 15 endpoints
- Spot Service: 21 endpoints
- Booking Service: 13 endpoints
- Payment Service: 11 endpoints
- Notification Service: 8 endpoints
- Analytics Service: 9 endpoints

---

**Last Updated:** April 8, 2026  
**API Version:** v1  
**Gateway Base URL:** http://localhost:8080

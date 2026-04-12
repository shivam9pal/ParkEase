# ParkEase Admin API Reference

## Overview
This document contains **complete admin-related endpoints** across all ParkEase microservices. All endpoints that an authenticated admin user can access are documented here with full request/response schemas, error codes, and access control rules.

**Scope:**
- Paths are listed as gateway-relative (`http://localhost:8080`)
- Service-local paths are the same (`/api/v1/...`) behind Spring Cloud Gateway routes
- Access rules enforced via `SecurityConfig` (route-level), controller annotations (`@PreAuthorize`), and service-layer checks
- Super admin-only operations require `isSuperAdmin=true` JWT claim check in AuthResource layer

## Base URLs

| Component | URL |
|---|---|
| API Gateway | `http://localhost:8080` |
| Auth Service | `http://localhost:8081` |
| Parking Lot Service | `http://localhost:8082` |
| Spot Service | `http://localhost:8083` |
| Booking Service | `http://localhost:8084` |
| Payment Service | `http://localhost:8085` |
| Vehicle Service | `http://localhost:8086` |
| Notification Service | `http://localhost:8087` |
| Analytics Service | `http://localhost:8088` |

## Authentication & JWT

### Admin Login Flow
**Endpoint:** `POST /api/v1/auth/admin/login`
- **Access:** PUBLIC
- **Purpose:** Returns admin JWT with all required claims
- **Response:** `AdminAuthResponse` including JWT token and admin profile

### Required Headers for All Authenticated Admin Endpoints
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
Accept: application/json
```

### JWT Token Details
**Admin Token Claims:**
```json
{
  "role": "ADMIN",           // String — admin's persisted role (enum: User.Role)
  "userId": "<adminId>",     // String — admin's UUID
  "isSuperAdmin": true/false, // Boolean — super admin flag (seeded via YML only)
  "sub": "admin@email.com",   // String — admin's email
  "iat": 1712586600,          // Long — issued at timestamp
  "exp": 1712673000           // Long — expiration timestamp (24h from issue)
}
```

**Token Expiry:** 86400000 ms (24 hours)

---

## Access Control Levels

| Level | Required | Description |
|---|---|---|
| `ADMIN` | `role=ADMIN` via `@PreAuthorize` | Standard admin access |
| `SUPER_ADMIN` | `role=ADMIN` + `isSuperAdmin=true` JWT claim | Super admin operations (checked at AuthResource layer) |
| `MANAGER_OR_ADMIN` | `role=MANAGER \| ADMIN` | Both roles allowed |
| `PUBLIC` | None | No authentication required |

---

---

# ADMIN ENDPOINTS BY SERVICE

## 1. AUTH-SERVICE
### Base Path: `/api/v1/auth`

#### 1.1 Admin Login (PUBLIC)
**Endpoint:** `POST /api/v1/auth/admin/login`
- **Access Level:** `PUBLIC`
- **Purpose:** Authenticate admin user and retrieve JWT token
- **Request Body:** `AdminLoginRequest`
  ```json
  {
    "email": "admin@parkease.com",
    "password": "password_string"
  }
  ```
- **Response:** `201` with `AdminAuthResponse`
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "admin": {
      "adminId": "550e8400-e29b-41d4-a716-446655440001",
      "fullName": "Super Admin",
      "email": "admin@parkease.com",
      "role": "ADMIN",
      "isActive": true,
      "isSuperAdmin": true,
      "createdAt": "2026-04-08T10:30:00"
    }
  }
  ```
- **Error Responses:**
  - `400` Bad Request: Validation failure
  - `401` Unauthorized: Invalid email/password or admin account deactivated
  - `500` Internal Server Error: Unmapped runtime error

#### 1.2 Create Admin (SUPER_ADMIN ONLY)
**Endpoint:** `POST /api/v1/auth/admin/create`
- **Access Level:** `SUPER_ADMIN` (requires `isSuperAdmin=true` in JWT)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Create a new admin user (only super admin can create admins)
- **Request Body:** `AdminCreateRequest`
  ```json
  {
    "fullName": "New Admin",
    "email": "newadmin@parkease.com",
    "password": "min-8-chars-password"
  }
  ```
- **Response:** `201 Created` with `AdminProfileResponse`
  ```json
  {
    "adminId": "550e8400-e29b-41d4-a716-446655440002",
    "fullName": "New Admin",
    "email": "newadmin@parkease.com",
    "role": "ADMIN",
    "isActive": true,
    "isSuperAdmin": false,
    "createdAt": "2026-04-08T11:00:00"
  }
  ```
- **Error Responses:**
  - `400` Bad Request: Validation failure or duplicate email
  - `401` Unauthorized: Missing/invalid JWT or `isSuperAdmin=false`
  - `500` Internal Server Error: Database error

#### 1.3 Delete Admin - Soft Delete (SUPER_ADMIN ONLY)
**Endpoint:** `DELETE /api/v1/auth/admin/{adminId}`
- **Access Level:** `SUPER_ADMIN` (requires `isSuperAdmin=true` in JWT)
- **Headers:** `Authorization: Bearer <JWT>`
- **Path Parameters:**
  - `adminId` (UUID): Target admin to deactivate
- **Response:** `200 OK` with plain text message
  ```
  "Admin deactivated successfully"
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT or `isSuperAdmin=false`
  - `404` Not Found: Admin ID does not exist
  - `500` Internal Server Error: Target is super admin or DB error

#### 1.4 List All Admins (SUPER_ADMIN ONLY)
**Endpoint:** `GET /api/v1/auth/admin/all`
- **Access Level:** `SUPER_ADMIN` (requires `isSuperAdmin=true` in JWT)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `200 OK` with `List<AdminProfileResponse>`
  ```json
  [
    {
      "adminId": "550e8400-e29b-41d4-a716-446655440001",
      "fullName": "Super Admin",
      "email": "admin@parkease.com",
      "role": "ADMIN",
      "isActive": true,
      "isSuperAdmin": true,
      "createdAt": "2026-01-01T00:00:00"
    },
    {
      "adminId": "550e8400-e29b-41d4-a716-446655440002",
      "fullName": "Regular Admin",
      "email": "admin2@parkease.com",
      "role": "ADMIN",
      "isActive": true,
      "isSuperAdmin": false,
      "createdAt": "2026-04-08T11:00:00"
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT or `isSuperAdmin=false`
  - `500` Internal Server Error: Database query error

#### 1.5 Get All Users (ADMIN)
**Endpoint:** `GET /api/v1/auth/users?role={role}`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Query Parameters:**
  - `role` (Optional): Filter by user role — `DRIVER | MANAGER | ADMIN`
- **Response:** `200 OK` with `List<UserProfileResponse>`
  ```json
  [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440010",
      "fullName": "John Driver",
      "email": "john@example.com",
      "phone": "+91-9876543210",
      "role": "DRIVER",
      "vehiclePlate": "MH01AB1234",
      "isActive": true,
      "createdAt": "2026-03-01T08:00:00",
      "profilePicUrl": "https://..."
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Invalid role enum or DB error

#### 1.6 Deactivate User (ADMIN)
**Endpoint:** `PUT /api/v1/auth/users/{userId}/deactivate`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Path Parameters:**
  - `userId` (UUID): Target user to deactivate
- **Response:** `200 OK` with `UserProfileResponse` (with `isActive=false`)
  ```json
  {
    "userId": "550e8400-e29b-41d4-a716-446655440010",
    "fullName": "John Driver",
    "email": "john@example.com",
    "phone": "+91-9876543210",
    "role": "DRIVER",
    "vehiclePlate": "MH01AB1234",
    "isActive": false,
    "createdAt": "2026-03-01T08:00:00",
    "profilePicUrl": "https://..."
  }
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT or user already deactivated
  - `403` Forbidden: Non-admin role
  - `404` Not Found: User ID does not exist

#### 1.7 Reactivate User (ADMIN)
**Endpoint:** `PUT /api/v1/auth/users/{userId}/reactivate`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Path Parameters:**
  - `userId` (UUID): Target user to reactivate
- **Response:** `200 OK` with `UserProfileResponse` (with `isActive=true`)
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `404` Not Found: User ID does not exist
  - `500` Internal Server Error: User already active or DB error

---

## 2. PARKING LOT SERVICE
### Base Path: `/api/v1/lots`

#### 2.1 Get All Lots (ADMIN ONLY)
**Endpoint:** `GET /api/v1/lots/all`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `200 OK` with `List<LotResponse>`
  ```json
  [
    {
      "lotId": "550e8400-e29b-41d4-a716-446655440100",
      "name": "Central Parking",
      "address": "123 Main Street",
      "city": "Mumbai",
      "latitude": 19.0760,
      "longitude": 72.8777,
      "totalSpots": 200,
      "availableSpots": 45,
      "managerId": "550e8400-e29b-41d4-a716-446655440050",
      "isOpen": true,
      "openTime": "07:00:00",
      "closeTime": "23:00:00",
      "imageUrl": "https://...",
      "isApproved": true,
      "createdAt": "2026-02-15T10:30:00"
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Database error

#### 2.2 Get Pending Lots (ADMIN ONLY)
**Endpoint:** `GET /api/v1/lots/pending`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Response:** `200 OK` with `List<LotResponse>` (all with `isApproved=false`)
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Database error

#### 2.3 Approve Parking Lot (ADMIN ONLY)
**Endpoint:** `PUT /api/v1/lots/{lotId}/approve`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Path Parameters:**
  - `lotId` (UUID): Target lot to approve
- **Response:** `200 OK` with `LotResponse` (with `isApproved=true`)
  ```json
  {
    "lotId": "550e8400-e29b-41d4-a716-446655440100",
    "name": "Central Parking",
    "address": "123 Main Street",
    "city": "Mumbai",
    "latitude": 19.0760,
    "longitude": 72.8777,
    "totalSpots": 200,
    "availableSpots": 45,
    "managerId": "550e8400-e29b-41d4-a716-446655440050",
    "isOpen": true,
    "openTime": "07:00:00",
    "closeTime": "23:00:00",
    "imageUrl": "https://...",
    "isApproved": true,
    "createdAt": "2026-02-15T10:30:00"
  }
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `404` Not Found: Lot ID does not exist

---

## 3. BOOKING SERVICE
### Base Path: `/api/v1/bookings`

#### 3.1 Get All Bookings (ADMIN ONLY)
**Endpoint:** `GET /api/v1/bookings/all`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Retrieve all bookings across all users and lots
- **Response:** `200 OK` with `List<BookingResponse>`
  ```json
  [
    {
      "bookingId": "550e8400-e29b-41d4-a716-446655440200",
      "userId": "550e8400-e29b-41d4-a716-446655440010",
      "lotId": "550e8400-e29b-41d4-a716-446655440100",
      "spotId": "550e8400-e29b-41d4-a716-446655440150",
      "vehicleId": "550e8400-e29b-41d4-a716-446655440300",
      "vehiclePlate": "MH01AB1234",
      "vehicleType": "FOUR_WHEELER",
      "bookingType": "PRE_BOOKING",
      "status": "ACTIVE",
      "startTime": "2026-04-08T09:00:00",
      "endTime": "2026-04-08T12:00:00",
      "checkInTime": "2026-04-08T09:05:00",
      "checkOutTime": null,
      "pricePerHour": 50.00,
      "totalAmount": 150.00,
      "createdAt": "2026-04-08T08:45:00"
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Database query error

---

## 4. VEHICLE SERVICE
### Base Path: `/api/v1/vehicles`

#### 4.1 Get All Vehicles (ADMIN ONLY)
**Endpoint:** `GET /api/v1/vehicles/all`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Retrieve all vehicles registered in the system
- **Response:** `200 OK` with `List<VehicleResponse>`
  ```json
  [
    {
      "vehicleId": "550e8400-e29b-41d4-a716-446655440300",
      "ownerId": "550e8400-e29b-41d4-a716-446655440010",
      "licensePlate": "MH01AB1234",
      "make": "Honda",
      "model": "City",
      "color": "Silver",
      "vehicleType": "FOUR_WHEELER",
      "isEV": false,
      "registeredAt": "2026-03-01T10:30:00",
      "isActive": true
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Database query error

---

## 5. PAYMENT SERVICE
### Base Path: `/api/v1/payments`

#### 5.1 Get Payment by Transaction ID (ADMIN ONLY)
**Endpoint:** `GET /api/v1/payments/transaction/{transactionId}`
- **Access Level:** `ADMIN` (enforced at SecurityConfig level)
- **Headers:** `Authorization: Bearer <JWT>`
- **Path Parameters:**
  - `transactionId` (String): Payment gateway transaction ID
- **Response:** `200 OK` with `PaymentResponse`
  ```json
  {
    "paymentId": "550e8400-e29b-41d4-a716-446655440400",
    "bookingId": "550e8400-e29b-41d4-a716-446655440200",
    "userId": "550e8400-e29b-41d4-a716-446655440010",
    "lotId": "550e8400-e29b-41d4-a716-446655440100",
    "amount": 150.00,
    "status": "PAID",
    "mode": "UPI",
    "transactionId": "TXN123456789",
    "currency": "INR",
    "paidAt": "2026-04-08T09:30:00",
    "refundedAt": null,
    "description": "Parking fee for Lot Central Parking",
    "createdAt": "2026-04-08T09:30:00"
  }
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `404` Not Found: Transaction ID does not exist
  - `500` Internal Server Error: Database query error

---

## 6. NOTIFICATION SERVICE
### Base Path: `/api/v1/notifications`

#### 6.1 Get All Notifications (ADMIN ONLY)
**Endpoint:** `GET /api/v1/notifications/all`
- **Access Level:** `ADMIN` (no explicit annotation; enforced at SecurityConfig)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Retrieve all notifications across all users and channels
- **Response:** `200 OK` with `List<NotificationResponse>`
  ```json
  [
    {
      "notificationId": "550e8400-e29b-41d4-a716-446655440500",
      "recipientId": "550e8400-e29b-41d4-a716-446655440010",
      "type": "BOOKING_CONFIRMATION",
      "title": "Booking Confirmed",
      "message": "Your booking at Central Parking is confirmed.",
      "channel": "APP",
      "relatedId": "550e8400-e29b-41d4-a716-446655440200",
      "relatedType": "BOOKING",
      "isRead": true,
      "sentAt": "2026-04-08T09:05:00"
    }
  ]
  ```
- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Database query error

#### 6.2 Broadcast Notification (ADMIN ONLY)
**Endpoint:** `POST /api/v1/notifications/broadcast`
- **Access Level:** `ADMIN` (no explicit annotation; enforced at SecurityConfig)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Send a PROMO notification to all users of a target role
- **Request Body:** `BroadcastNotificationRequest`
  ```json
  {
    "targetRole": "DRIVER",
    "title": "Winter Parking Offer",
    "message": "Get 20% discount on all parking fees this weekend!"
  }
  ```
  **Accepted targetRole values:**
  - `DRIVER` — Send to all drivers
  - `MANAGER` — Send to all managers
  - `ALL` — Send to all users (both drivers and managers)

- **Response:** `202 Accepted` (no body)
  ```
  (Empty response body — notification sent asynchronously)
  ```
- **Error Responses:**
  - `400` Bad Request: Validation failure (missing required fields)
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Notification dispatch error

---

## 7. ANALYTICS SERVICE
### Base Path: `/api/v1/analytics`

#### 7.1 Get Platform Summary (ADMIN ONLY)
**Endpoint:** `GET /api/v1/analytics/platform/summary`
- **Access Level:** `ADMIN` (requires `hasRole('ADMIN')`)
- **Headers:** `Authorization: Bearer <JWT>`
- **Purpose:** Retrieve platform-wide analytics and operational metrics
- **Response:** `200 OK` with `PlatformSummaryResponse`
  ```json
  {
    "totalLots": 25,
    "totalSpots": 5000,
    "totalAvailableSpots": 1200,
    "platformOccupancyRate": 76.0,
    "todayRevenue": 450000.00,
    "todayTransactionCount": 1250,
    "platformAvgDurationMinutes": 102,
    "generatedAt": "2026-04-08T10:30:00"
  }
  ```
- **Fields Explanation:**
  - `totalLots` — Total approved parking lots in system
  - `totalSpots` — Sum of all spots across all active lots
  - `totalAvailableSpots` — Sum of currently available spots
  - `platformOccupancyRate` — Percentage of occupied spots (0-100)
  - `todayRevenue` — Total revenue generated today (INR)
  - `todayTransactionCount` — Number of payments processed today
  - `platformAvgDurationMinutes` — Average parking duration across all bookings
  - `generatedAt` — Timestamp when this summary was computed

- **Error Responses:**
  - `401` Unauthorized: Missing/invalid JWT
  - `403` Forbidden: Non-admin role
  - `500` Internal Server Error: Analytics computation error
  - `502` Bad Gateway: Upstream service (payment-service or booking-service) unavailable

---

## Auth Service

### 1) Admin Login
- Method/Path: `POST /api/v1/auth/admin/login`
- Access: `PUBLIC`
- Request body: `AdminLoginRequest`
- Success: `200 OK` with `AdminAuthResponse`
- Main errors:
  - `400` validation failure
  - `500` invalid email/password (current runtime-message mapping path)
  - `401` admin account deactivated

### 2) Create Admin
- Method/Path: `POST /api/v1/auth/admin/create`
- Access: `SUPER_ADMIN`
- Request body: `AdminCreateRequest`
- Success: `201 Created` with `AdminProfileResponse`
- Main errors:
  - `403` requester is not super admin
  - `400` duplicate admin email
  - `400` validation failure

### 3) Delete Admin (soft delete)
- Method/Path: `DELETE /api/v1/auth/admin/{adminId}`
- Access: `SUPER_ADMIN`
- Success: `200 OK`, plain string body: `"Admin deactivated successfully"`
- Main errors:
  - `403` requester not super admin
  - `403` target admin is super admin (cannot be deleted)
  - `404` admin not found

### 4) List Admins
- Method/Path: `GET /api/v1/auth/admin/all`
- Access: `SUPER_ADMIN`
- Success: `200 OK` with `List<AdminProfileResponse>`
- Main errors:
  - `403` requester not super admin

### 5) List Users For Admin Management
- Method/Path: `GET /api/v1/auth/users?role={role}`
- Access: `ADMIN`
- Query param:
  - `role` optional: `DRIVER | MANAGER | ADMIN`
- Success: `200 OK` with `List<UserProfileResponse>`
- Main errors:
  - `403` non-admin
  - `500` invalid `role` string (enum parse failure is not normalized to 400 in current code)

### 6) Deactivate User
- Method/Path: `PUT /api/v1/auth/users/{userId}/deactivate`
- Access: `ADMIN`
- Success: `200 OK` with updated `UserProfileResponse` (`isActive=false`)
- Main errors:
  - `404` user not found
  - `401` user already deactivated (current message-to-status mapping behavior)

### 7) Reactivate User
- Method/Path: `PUT /api/v1/auth/users/{userId}/reactivate`
- Access: `ADMIN`
- Success: `200 OK` with updated `UserProfileResponse` (`isActive=true`)
- Main errors:
  - `404` user not found
  - `500` user already active (current runtime-message mapping path)

### Auth Admin Data Model (Updated)
- `admins` table now persists explicit admin role:
  - Column: `role` (enum `User.Role`)
  - Default: `ADMIN`
- JWT admin role claim now comes from persisted `Admin.role` instead of hardcoded string during token creation.
- Existing admin rows with null role are auto-backfilled to `ADMIN` during `AdminSeeder` startup run.

## Parking Lot Service

### 8) Get All Lots
- Method/Path: `GET /api/v1/lots/all`
- Access: `ADMIN`
- Success: `200 OK` with `List<LotResponse>`

### 9) Get Pending Lots
- Method/Path: `GET /api/v1/lots/pending`
- Access: `ADMIN`
- Success: `200 OK` with `List<LotResponse>`

### 10) Approve Lot
- Method/Path: `PUT /api/v1/lots/{lotId}/approve`
- Access: `ADMIN`
- Success: `200 OK` with `LotResponse` (`isApproved=true`)
- Main errors:
  - `404` lot not found

## Booking Service

### 11) Get All Bookings (platform-wide)
- Method/Path: `GET /api/v1/bookings/all`
- Access: `ADMIN`
- Success: `200 OK` with `List<BookingResponse>`

## Vehicle Service

### 12) Get All Vehicles
- Method/Path: `GET /api/v1/vehicles/all`
- Access: `ADMIN`
- Success: `200 OK` with `List<VehicleResponse>`

## Payment Service

### 13) Get Payment By Transaction ID
- Method/Path: `GET /api/v1/payments/transaction/{transactionId}`
- Access: `ADMIN`
- Success: `200 OK` with `PaymentResponse`
- Main errors:
  - `404` payment not found for transaction id

## Notification Service

### 14) Get All Notifications
- Method/Path: `GET /api/v1/notifications/all`
- Access: `ADMIN`
- Success: `200 OK` with `List<NotificationResponse>`

### 15) Broadcast Notification
- Method/Path: `POST /api/v1/notifications/broadcast`
- Access: `ADMIN`
- Request body: `BroadcastNotificationRequest`
- Success: `202 Accepted` (no body)
- Main errors:
  - `400` validation error

## Analytics Service

### 16) Platform Summary
- Method/Path: `GET /api/v1/analytics/platform/summary`
- Access: `ADMIN`
- Success: `200 OK` with `PlatformSummaryResponse`

---

## Admin-Capable Shared Endpoints

## Parking Lot Service (`MANAGER_OR_ADMIN`, admin can use all below)

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/lots/manager/{managerId}` | `AUTHENTICATED` (admin unrestricted) | Path `managerId` | `200 List<LotResponse>` | `403` (manager cross-access returns empty body), `404` |
| PUT | `/api/v1/lots/{lotId}` | `AUTHENTICATED` (admin can update any lot) | Path `lotId`, body `UpdateLotRequest` | `200 LotResponse` | `404`, `400` |
| PUT | `/api/v1/lots/{lotId}/decrement` | `AUTHENTICATED` | Path `lotId` | `200` empty body | `404`, `409` |
| PUT | `/api/v1/lots/{lotId}/increment` | `AUTHENTICATED` | Path `lotId` | `200` empty body | `404`, `409` |
| DELETE | `/api/v1/lots/{lotId}` | `AUTHENTICATED` (admin can delete any lot) | Path `lotId` | `204 No Content` | `404`, `403` |

## Booking Service

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/bookings/lot/{lotId}` | `MANAGER_OR_ADMIN` | Path `lotId` | `200 List<BookingResponse>` | `404` |
| GET | `/api/v1/bookings/lot/{lotId}/active` | `MANAGER_OR_ADMIN` | Path `lotId` | `200 List<BookingResponse>` | `404` |
| GET | `/api/v1/bookings/{bookingId}` | `AUTHENTICATED` (admin can fetch any) | Path `bookingId` | `200 BookingResponse` | `404`, `403` (driver ownership only) |
| GET | `/api/v1/bookings/{bookingId}/fare` | `AUTHENTICATED` | Path `bookingId` | `200 FareCalculationResponse` | `404`, `409` (not ACTIVE) |
| PUT | `/api/v1/bookings/{bookingId}/checkout` | `AUTHENTICATED` (admin can checkout any via controller logic) | Path `bookingId` | `200 BookingResponse` | `404`, `409`, `403` |
| PUT | `/api/v1/bookings/{bookingId}/cancel` | `AUTHENTICATED` (admin can cancel any) | Path `bookingId` | `200 BookingResponse` | `404`, `409`, `403` |

## Vehicle Service

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/vehicles/{vehicleId}` | `AUTHENTICATED` (admin can fetch any) | Path `vehicleId` | `200 VehicleResponse` | `404`, `403` |
| GET | `/api/v1/vehicles/owner/{ownerId}` | `AUTHENTICATED` (admin can fetch any) | Path `ownerId` | `200 List<VehicleResponse>` | `403` |
| GET | `/api/v1/vehicles/plate/{licensePlate}` | `AUTHENTICATED` (admin can fetch any) | Path `licensePlate` | `200 VehicleResponse` | `404`, `403` |
| GET | `/api/v1/vehicles/{vehicleId}/type` | `AUTHENTICATED` | Path `vehicleId` | `200 VehicleType` | `404` |
| GET | `/api/v1/vehicles/{vehicleId}/isEV` | `AUTHENTICATED` | Path `vehicleId` | `200 boolean` | `404` |

## Spot Service

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| PUT | `/api/v1/spots/{spotId}/reserve` | `AUTHENTICATED` | Path `spotId` | `200 SpotResponse` | `404`, `409` |
| PUT | `/api/v1/spots/{spotId}/occupy` | `AUTHENTICATED` | Path `spotId` | `200 SpotResponse` | `404`, `409` |
| PUT | `/api/v1/spots/{spotId}/release` | `AUTHENTICATED` | Path `spotId` | `200 SpotResponse` | `404`, `409` |
| DELETE | `/api/v1/spots/{spotId}` | `MANAGER_OR_ADMIN` | Path `spotId` | `204 No Content` | `404`, `403` |

## Payment Service

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| POST | `/api/v1/payments/{paymentId}/refund` | `MANAGER_OR_ADMIN` | Path `paymentId` | `200 PaymentResponse` | `404`, `400` (status not PAID), `403` |
| GET | `/api/v1/payments/revenue/lot/{lotId}` | `MANAGER_OR_ADMIN` | Path `lotId`, query `from,to` | `200 RevenueResponse` | `400` |
| GET | `/api/v1/payments/revenue/lot/{lotId}/daily` | `MANAGER_OR_ADMIN` | Path `lotId`, query `from,to` | `200 List<DailyRevenueResponse>` | `400` |
| GET | `/api/v1/payments/revenue/platform` | `MANAGER_OR_ADMIN` | Query `from,to` | `200 RevenueResponse` | `400` |
| GET | `/api/v1/payments/booking/{bookingId}` | `AUTHENTICATED` (admin unrestricted) | Path `bookingId` | `200 PaymentResponse` | `404`, `403` (driver only restriction) |
| GET | `/api/v1/payments/user/{userId}` | `AUTHENTICATED` (admin unrestricted) | Path `userId` | `200 List<PaymentResponse>` | `403` (driver mismatch returns empty body) |
| GET | `/api/v1/payments/{paymentId}/status` | `AUTHENTICATED` (admin unrestricted) | Path `paymentId` | `200 PaymentStatusResponse` | `404`, `403` |
| GET | `/api/v1/payments/{paymentId}/receipt` | `AUTHENTICATED` (admin unrestricted) | Path `paymentId` | `200 application/pdf` | `404`, `400`, `403`, `500` |

## Notification Service

| Method | Path | Access | Request | Success | Common Errors |
|---|---|---|---|---|---|
| DELETE | `/api/v1/notifications/{notificationId}` | `DRIVER_OR_ADMIN` | Path `notificationId` | `204 No Content` | `404`, `403` |

## Analytics Service (`MANAGER_OR_ADMIN`)

| Method | Path | Request | Success | Common Errors |
|---|---|---|---|---|
| GET | `/api/v1/analytics/occupancy/{lotId}` | Path `lotId` | `200 OccupancyRateResponse` | `403`, `404`, `502` |
| GET | `/api/v1/analytics/occupancy/{lotId}/hourly` | Path `lotId` | `200 List<HourlyOccupancyResponse>` | `403`, `404`, `502` |
| GET | `/api/v1/analytics/occupancy/{lotId}/peak` | Path `lotId`, query `topN` optional | `200 List<PeakHourResponse>` | `403`, `404`, `502` |
| GET | `/api/v1/analytics/revenue/{lotId}` | Path `lotId`, query `from,to` | `200 RevenueDto` | `403`, `404`, `502` |
| GET | `/api/v1/analytics/revenue/{lotId}/daily` | Path `lotId`, query `from,to` | `200 List<DailyRevenueDto>` | `403`, `404`, `502` |
| GET | `/api/v1/analytics/spot-types/{lotId}` | Path `lotId` | `200 List<SpotTypeUtilisationResponse>` | `403`, `404` |
| GET | `/api/v1/analytics/avg-duration/{lotId}` | Path `lotId` | `200 AvgDurationResponse` | `403`, `404` |
| GET | `/api/v1/analytics/report/{lotId}/daily` | Path `lotId` | `200 DailyReportResponse` | `403`, `404`, `502` |

---

## Request And Response Schemas

## Auth DTOs

### AdminLoginRequest
```json
{
  "email": "admin@parkease.com",
  "password": "string"
}
```

### AdminCreateRequest
```json
{
  "fullName": "string",
  "email": "new-admin@parkease.com",
  "password": "min-8-chars"
}
```

### AdminProfileResponse
```json
{
  "adminId": "uuid",
  "fullName": "string",
  "email": "string",
  "role": "ADMIN",
  "isActive": true,
  "isSuperAdmin": false,
  "createdAt": "2026-04-08T10:30:00"
}
```

### AdminAuthResponse
```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "admin": {
    "adminId": "uuid",
    "fullName": "string",
    "email": "string",
    "role": "ADMIN",
    "isActive": true,
    "isSuperAdmin": true,
    "createdAt": "2026-04-08T10:30:00"
  }
}
```

### Admin Entity (Persistence Model)
```json
{
  "adminId": "uuid",
  "fullName": "string",
  "email": "string",
  "passwordHash": "bcrypt-hash",
  "role": "ADMIN",
  "isActive": true,
  "isSuperAdmin": false,
  "createdAt": "2026-04-08T10:30:00"
}
```

### UserProfileResponse
```json
{
  "userId": "uuid",
  "fullName": "string",
  "email": "string",
  "phone": "string",
  "role": "DRIVER",
  "vehiclePlate": "string",
  "isActive": true,
  "createdAt": "2026-04-08T10:30:00",
  "profilePicUrl": "string"
}
```

## Parking Lot DTOs

### UpdateLotRequest (all fields optional)
```json
{
  "name": "string",
  "address": "string",
  "city": "string",
  "latitude": 12.34,
  "longitude": 56.78,
  "openTime": "08:00:00",
  "closeTime": "22:00:00",
  "imageUrl": "https://..."
}
```

### LotResponse
```json
{
  "lotId": "uuid",
  "name": "string",
  "address": "string",
  "city": "string",
  "latitude": 12.34,
  "longitude": 56.78,
  "totalSpots": 100,
  "availableSpots": 80,
  "managerId": "uuid",
  "isOpen": true,
  "openTime": "08:00:00",
  "closeTime": "22:00:00",
  "imageUrl": "https://...",
  "isApproved": true,
  "createdAt": "2026-04-08T10:30:00"
}
```

## Booking DTOs

### BookingResponse
```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "lotId": "uuid",
  "spotId": "uuid",
  "vehicleId": "uuid",
  "vehiclePlate": "string",
  "vehicleType": "FOUR_WHEELER",
  "bookingType": "PRE_BOOKING",
  "status": "RESERVED",
  "startTime": "2026-04-08T10:30:00",
  "endTime": "2026-04-08T12:30:00",
  "checkInTime": null,
  "checkOutTime": null,
  "pricePerHour": 50.00,
  "totalAmount": null,
  "createdAt": "2026-04-08T10:00:00"
}
```

### FareCalculationResponse
```json
{
  "bookingId": "uuid",
  "pricePerHour": 50.00,
  "estimatedHours": 1.50,
  "estimatedFare": 75.00,
  "checkInTime": "2026-04-08T09:00:00",
  "calculatedAt": "2026-04-08T10:30:00"
}
```

## Vehicle DTOs

### VehicleResponse
```json
{
  "vehicleId": "uuid",
  "ownerId": "uuid",
  "licensePlate": "KA01AB1234",
  "make": "string",
  "model": "string",
  "color": "string",
  "vehicleType": "FOUR_WHEELER",
  "isEV": false,
  "registeredAt": "2026-04-08T10:30:00",
  "isActive": true
}
```

## Payment DTOs

### PaymentResponse
```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "lotId": "uuid",
  "amount": 120.00,
  "status": "PAID",
  "mode": "UPI",
  "transactionId": "string-or-null",
  "currency": "INR",
  "paidAt": "2026-04-08T10:30:00",
  "refundedAt": null,
  "description": "string",
  "createdAt": "2026-04-08T10:30:00"
}
```

### PaymentStatusResponse
```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "status": "PAID",
  "paidAt": "2026-04-08T10:30:00",
  "refundedAt": null
}
```

### RevenueResponse
```json
{
  "lotId": "uuid-or-null-for-platform",
  "from": "2026-04-01T00:00:00",
  "to": "2026-04-08T23:59:59",
  "totalRevenue": 10000.00,
  "currency": "INR",
  "transactionCount": 120
}
```

### DailyRevenueResponse
```json
{
  "date": "2026-04-08",
  "revenue": 1200.00,
  "transactionCount": 14
}
```

## Notification DTOs

### BroadcastNotificationRequest
```json
{
  "targetRole": "DRIVER",
  "title": "string",
  "message": "string"
}
```

`targetRole` accepted by implementation:
- `DRIVER`
- `MANAGER`
- `ALL` (special-cased in service)

### NotificationResponse
```json
{
  "notificationId": "uuid",
  "recipientId": "uuid",
  "type": "PROMO",
  "title": "string",
  "message": "string",
  "channel": "APP",
  "relatedId": null,
  "relatedType": null,
  "isRead": false,
  "sentAt": "2026-04-08T10:30:00"
}
```

## Analytics DTOs

### OccupancyRateResponse
```json
{
  "lotId": "uuid",
  "occupancyRate": 64.5,
  "availableSpots": 71,
  "totalSpots": 200,
  "computedAt": "2026-04-08T10:30:00"
}
```

### HourlyOccupancyResponse
```json
{
  "hour": 18,
  "averageOccupancyRate": 72.4
}
```

### PeakHourResponse
```json
{
  "hour": 18,
  "averageOccupancyRate": 72.4,
  "label": "18:00 - 19:00"
}
```

### SpotTypeUtilisationResponse
```json
{
  "spotType": "EV",
  "bookingCount": 150,
  "percentage": 23.7
}
```

### AvgDurationResponse
```json
{
  "lotId": "uuid",
  "averageDurationMinutes": 95,
  "averageDurationFormatted": "1h 35m"
}
```

### PlatformSummaryResponse
```json
{
  "totalLots": 20,
  "totalSpots": 3000,
  "totalAvailableSpots": 900,
  "platformOccupancyRate": 70.0,
  "todayRevenue": 125000.00,
  "todayTransactionCount": 1200,
  "platformAvgDurationMinutes": 98,
  "generatedAt": "2026-04-08T10:30:00"
}
```

### DailyReportResponse
```json
{
  "lotId": "uuid",
  "reportDate": "2026-04-08",
  "currentOccupancyRate": 68.3,
  "availableSpots": 63,
  "totalSpots": 200,
  "peakHours": [],
  "todayRevenue": 6500.00,
  "todayTransactionCount": 73,
  "todayBookingsCreated": 80,
  "todayCheckouts": 73,
  "averageParkingDurationMinutes": 102,
  "averageParkingDurationFormatted": "1h 42m",
  "spotTypeUtilisation": [],
  "generatedAt": "2026-04-08T10:30:00"
}
```

### RevenueDto (analytics proxy response)
```json
{
  "lotId": "uuid-or-null",
  "from": "2026-04-01T00:00:00",
  "to": "2026-04-08T23:59:59",
  "totalRevenue": 10000.00,
  "currency": "INR",
  "transactionCount": 120
}
```

### DailyRevenueDto (analytics proxy response)
```json
{
  "date": "2026-04-08",
  "revenue": 1200.00,
  "transactionCount": 14
}
```

---

## Error Response Formats (From Exception Handlers)

## Auth Service (`auth-service`)
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": ["field: message"]
}
```
`errors` appears only for validation failures.

## Parking Lot, Booking, Vehicle, Spot (ApiError style)
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "text",
  "errors": []
}
```
`errors` can be omitted or empty depending on service and exception type.

## Payment Service
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "text"
}
```

## Analytics Service
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Upstream service unavailable"
}
```

## Notification Service
Validation error shape:
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "field": "message"
  }
}
```
ResponseStatusException shape:
```json
{
  "timestamp": "2026-04-08T10:30:00",
  "status": 403,
  "error": "You can only delete your own notifications."
}
```

---

## Status Code Meaning (Admin APIs)

| Status | Meaning in this project |
|---|---|
| `200` | Request succeeded and response body (if any) is valid. |
| `201` | Resource created (for example admin creation). |
| `202` | Accepted for processing (notification broadcast). |
| `204` | Success with no body (delete/read-all patterns). |
| `400` | Invalid payload/params or business-rule conflict mapped as bad request. |
| `401` | Missing/invalid token, expired token, or deactivated-account auth path. |
| `403` | Authenticated but not authorized (role/ownership/super-admin checks). |
| `404` | Target resource not found. |
| `409` | State conflict (invalid status transition, already-full/empty counters). |
| `410` | Gone (expired OTP flow in auth-service runtime mapping). |
| `429` | Too many requests/attempts (OTP throttling flow). |
| `500` | Unmapped runtime path (known in some auth-service messages). |
| `502` | Upstream/dependency failure (for example Feign/downstream service). |

---

## Important Implementation Notes (No Loose Ends)

1. Admin role is now persisted in `admins.role` and defaults to `ADMIN`; token role claim is generated from this stored enum value.
2. `AdminSeeder` now backfills legacy admin rows where `role` is null, then continues normal super-admin seeding logic.
3. Super admin-only operations are enforced in service layer (`create`, `delete`, `list admins`) even after role checks.
4. `GET /api/v1/auth/users?role=...` with invalid role text currently bubbles as `500` due enum parse path.
5. `POST /api/v1/auth/admin/login` invalid credentials currently map to `500` because message text is not covered by auth runtime status mapping.
6. `PUT /api/v1/auth/users/{userId}/reactivate` for already-active user currently maps to `500` for the same reason.
7. `DELETE /api/v1/notifications/{notificationId}` allows admin at security layer, but service still enforces recipient ownership. Admin cannot delete arbitrary user notifications in current implementation.
8. Analytics lot-level endpoints run manager ownership checks; admins bypass ownership and can query any lot.
9. Some controllers return `403` with empty body directly (for example certain ownership checks), bypassing global exception JSON format.
10. In auth-service, JWT auth for protected routes is resolved through `UserDetailsServiceImpl` (users table lookup). Admin JWT tokens issued from `admins` records can fail protected auth-route authentication in current implementation if no matching user principal exists.
11. Some analytics endpoints intentionally return fallback success payloads when downstream services are unavailable (for example empty lists or zeroed summary fields), instead of propagating an error.\n\n---\n\n## SUPER ADMIN JWT CLAIM ENFORCEMENT (NEWLY IMPLEMENTED)\n\n**IMPORTANT SECURITY UPDATE:**\nAll three super admin-only endpoints in AuthResource now enforce the `isSuperAdmin` JWT claim check at the **controller layer** before delegating to service layer. This ensures immediate rejection of non-super-admin requests.\n\n### Endpoints Protected by isSuperAdmin Check:\n\n1. **POST** `/api/v1/auth/admin/create`\n   - Extracts JWT token from Authorization header\n   - Calls `jwtUtil.extractIsSuperAdmin(token)`\n   - If `false` → throws `AccessDeniedException` with message: \"Super Admin privileges required to create admin\"\n   - Returns: `401 Unauthorized`\n\n2. **DELETE** `/api/v1/auth/admin/{adminId}`\n   - Extracts JWT token from Authorization header\n   - Calls `jwtUtil.extractIsSuperAdmin(token)`\n   - If `false` → throws `AccessDeniedException` with message: \"Super Admin privileges required to delete admin\"\n   - Returns: `401 Unauthorized`\n\n3. **GET** `/api/v1/auth/admin/all`\n   - Extracts JWT token from Authorization header\n   - Calls `jwtUtil.extractIsSuperAdmin(token)`\n   - If `false` → throws `AccessDeniedException` with message: \"Super Admin privileges required to list admins\"\n   - Returns: `401 Unauthorized`\n\n### How JWT isSuperAdmin Claim Works:\n\n- **JWT Claim Added By:** `JwtUtil.generateAdminToken()` method\n- **Claim Value:** `isSuperAdmin: true|false` (Boolean)\n- **How It's Set:**\n  - Only `true` for admin user seeded via YML configuration file during application startup\n  - All admins created via `POST /api/v1/auth/admin/create` endpoint are set to `false`\n  - Cannot be changed after admin creation (immutable)\n  - Stored in `Admin.isSuperAdmin` database column (default `false`)\n\n### Architecture Flow:\n\n```\nClient Request\n    ↓\nAuthResource controller method\n    ↓\nExtract JWT token from Authorization header\n    ↓\njwtUtil.extractIsSuperAdmin(token)\n    ↓\n[Decision] if (false) → AccessDeniedException (401)\n[Decision] if (true) → Continue to service layer\n    ↓\nAuthService method executes\n    ↓\nResponse returned to client\n```"

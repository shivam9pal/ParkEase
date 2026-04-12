# ParkEase - Complete API Endpoints Documentation

## Table of Contents
1. [API Gateway Overview](#api-gateway-overview)
2. [Authentication & Headers](#authentication--headers)
3. [Auth Service Endpoints](#auth-service-endpoints)
4. [Vehicle Service Endpoints](#vehicle-service-endpoints)
5. [Parking Lot Service Endpoints](#parking-lot-service-endpoints)
6. [Spot Service Endpoints](#spot-service-endpoints)
7. [Booking Service Endpoints](#booking-service-endpoints)
8. [Payment Service Endpoints](#payment-service-endpoints)
9. [Notification Service Endpoints](#notification-service-endpoints)
10. [Analytics Service Endpoints](#analytics-service-endpoints)
11. [Error Response Codes](#error-response-codes)

---

## API Gateway Overview

### Gateway Configuration
- **Base URL:** `http://localhost:8080`
- **Gateway Type:** Spring Cloud Gateway
- **Service Discovery:** Eureka Server (`http://localhost:8761`)
- **Load Balancing:** Round-robin via load balancer

### Service Routing Map
All requests go through API Gateway and are routed to respective services:

| Service | Port | Base Path | Health Check |
|---------|------|-----------|--------------|
| Auth Service | 8083 | `/auth/**` | `/actuator/health` |
| Booking Service | 8081 | `/booking/**` | `/actuator/health` |
| Payment Service | 8082 | `/payment/**` | `/actuator/health` |
| Parking Lot Service | 8084 | `/parkinglot/**` | `/actuator/health` |
| Spot Service | 8085 | `/spot/**` | `/actuator/health` |
| Vehicle Service | 8086 | `/vehicle/**` | `/actuator/health` |
| Notification Service | - | `/notification/**` | `/actuator/health` |
| Analytics Service | - | `/analytics/**` | `/actuator/health` |

---

## Authentication & Headers

### Common Request Headers
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
X-User-Id: {USER_ID}
X-User-Role: {ROLE}
Accept: application/json
```

### Authorization Roles
- `DRIVER` - Regular parking users
- `MANAGER` - Parking lot managers
- `ADMIN` - System administrators
- `SUPER_ADMIN` - Super administrators

### Authentication Flow
1. **Login/Register** via Auth Service
2. **Receive JWT Token** in response
3. **Include Token** in `Authorization` header for subsequent requests
4. **Token Validation** happens at API Gateway and service level

---

## Auth Service Endpoints

**Service Base URL:** `http://localhost:8080/auth`

### 1. User Registration
```http
POST /auth/register
Content-Type: application/json

Request Body:
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phoneNumber": "+918888888888",
  "password": "SecurePass@123",
  "licenseNumber": "DL2024001",
  "licenseExpiry": "2029-12-31"
}

Response (201 Created):
{
  "id": "user-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phoneNumber": "+918888888888",
  "licenseNumber": "DL2024001",
  "licenseExpiry": "2029-12-31",
  "createdAt": "2026-04-08T10:00:00Z",
  "status": "ACTIVE"
}
```

### 2. User Login
```http
POST /auth/login
Content-Type: application/json

Request Body:
{
  "email": "john@example.com",
  "password": "SecurePass@123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_value",
  "userId": "user-123",
  "email": "john@example.com",
  "role": "DRIVER",
  "expiresIn": 3600
}
```

### 3. Send OTP
```http
POST /auth/send-otp
Content-Type: application/json

Request Body:
{
  "email": "john@example.com"
}

Response (200 OK):
{
  "message": "OTP sent successfully",
  "otpExpiry": 600
}

Error (400 Bad Request):
{
  "error": "User not found",
  "status": 404
}
```

### 4. Verify OTP
```http
POST /auth/verify-otp
Content-Type: application/json

Request Body:
{
  "email": "john@example.com",
  "otp": "123456"
}

Response (200 OK):
{
  "verified": true,
  "message": "OTP verified successfully"
}

Error (400 Bad Request):
{
  "error": "Invalid or expired OTP",
  "status": 400
}
```

### 5. Reset Password
```http
POST /auth/reset-password
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "oldPassword": "OldPass@123",
  "newPassword": "NewPass@123"
}

Response (200 OK):
{
  "message": "Password reset successfully"
}

Error (401 Unauthorized):
{
  "error": "Invalid credentials",
  "status": 401
}
```

### 6. Get User Profile
```http
GET /auth/profile

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "user-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phoneNumber": "+918888888888",
  "licenseNumber": "DL2024001",
  "role": "DRIVER",
  "createdAt": "2026-04-08T10:00:00Z",
  "status": "ACTIVE"
}
```

### 7. Update User Profile
```http
PUT /auth/profile
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+919999999999"
}

Response (200 OK):
{
  "id": "user-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phoneNumber": "+919999999999",
  "updatedAt": "2026-04-08T10:30:00Z"
}
```

### 8. Refresh Token
```http
POST /auth/refresh-token
Content-Type: application/json

Request Body:
{
  "refreshToken": "refresh_token_value"
}

Response (200 OK):
{
  "token": "new_jwt_token",
  "expiresIn": 3600
}
```

### 9. Logout
```http
POST /auth/logout
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "message": "Logged out successfully"
}
```

---

## Vehicle Service Endpoints

**Service Base URL:** `http://localhost:8080/vehicle`

### 1. Register Vehicle
```http
POST /vehicle/register
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "registrationNumber": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "make": "Toyota",
  "model": "Fortuner",
  "color": "Black",
  "seatingCapacity": 7,
  "isElectric": false,
  "registrationYear": 2023
}

Response (201 Created):
{
  "id": "vehicle-001",
  "userId": "user-123",
  "registrationNumber": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "make": "Toyota",
  "model": "Fortuner",
  "color": "Black",
  "seatingCapacity": 7,
  "isElectric": false,
  "registrationYear": 2023,
  "createdAt": "2026-04-08T10:00:00Z",
  "status": "ACTIVE"
}
```

### 2. Get User's Vehicles
```http
GET /vehicle/my-vehicles

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- status: ACTIVE (optional)

Response (200 OK):
[
  {
    "id": "vehicle-001",
    "registrationNumber": "KA-01-AB-1234",
    "vehicleType": "CAR",
    "make": "Toyota",
    "model": "Fortuner",
    "color": "Black",
    "isElectric": false
  },
  {
    "id": "vehicle-002",
    "registrationNumber": "KA-01-AB-5678",
    "vehicleType": "TWO_WHEELER",
    "make": "Honda",
    "model": "Activa",
    "color": "Red",
    "isElectric": false
  }
]
```

### 3. Get Vehicle Details
```http
GET /vehicle/{vehicleId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "vehicle-001",
  "userId": "user-123",
  "registrationNumber": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "make": "Toyota",
  "model": "Fortuner",
  "seatingCapacity": 7,
  "isElectric": false,
  "registrationYear": 2023
}
```

### 4. Update Vehicle
```http
PUT /vehicle/{vehicleId}
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "make": "Toyota",
  "model": "Fortuner",
  "color": "White",
  "isElectric": false
}

Response (200 OK):
{
  "id": "vehicle-001",
  "registrationNumber": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "make": "Toyota",
  "model": "Fortuner",
  "color": "White",
  "isElectric": false,
  "updatedAt": "2026-04-08T10:30:00Z"
}
```

### 5. Delete Vehicle
```http
DELETE /vehicle/{vehicleId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (204 No Content):
```

### 6. Get Vehicles by Type
```http
GET /vehicle/type/{vehicleType}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
[
  {
    "id": "vehicle-001",
    "registrationNumber": "KA-01-AB-1234",
    "vehicleType": "CAR",
    "make": "Toyota",
    "isElectric": false
  }
]
```

### 7. Get Electric Vehicles
```http
GET /vehicle/filter/electric

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
[
  {
    "id": "vehicle-003",
    "registrationNumber": "KA-01-EV-1001",
    "vehicleType": "CAR",
    "make": "Tesla",
    "model": "Model 3",
    "isElectric": true
  }
]
```

---

## Parking Lot Service Endpoints

**Service Base URL:** `http://localhost:8080/parkinglot`

### 1. Create Parking Lot
```http
POST /parkinglot/create
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "name": "Downtown Mall Parking",
  "address": "123 MG Road, Bangalore",
  "city": "Bangalore",
  "state": "Karnataka",
  "zipCode": "560001",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "totalCapacity": 500,
  "pricePerHour": 50,
  "managerId": "user-456"
}

Response (201 Created):
{
  "id": "lot-001",
  "name": "Downtown Mall Parking",
  "address": "123 MG Road, Bangalore",
  "city": "Bangalore",
  "totalCapacity": 500,
  "availableSpots": 500,
  "pricePerHour": 50,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "status": "PENDING_APPROVAL",
  "createdAt": "2026-04-08T10:00:00Z"
}
```

### 2. Get All Parking Lots
```http
GET /parkinglot/all

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- page: 0 (optional)
- size: 10 (optional)
- status: APPROVED (optional)

Response (200 OK):
{
  "content": [
    {
      "id": "lot-001",
      "name": "Downtown Mall Parking",
      "city": "Bangalore",
      "totalCapacity": 500,
      "availableSpots": 320,
      "pricePerHour": 50,
      "status": "APPROVED",
      "occupancyPercentage": 36
    }
  ],
  "totalPages": 5,
  "totalElements": 45,
  "currentPage": 0
}
```

### 3. Search Parking Lots by City
```http
GET /parkinglot/city/{city}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- page: 0 (optional)
- size: 10 (optional)

Response (200 OK):
[
  {
    "id": "lot-001",
    "name": "Downtown Mall Parking",
    "address": "123 MG Road, Bangalore",
    "totalCapacity": 500,
    "availableSpots": 320,
    "pricePerHour": 50
  }
]
```

### 4. Search by GPS Proximity
```http
GET /parkinglot/nearby

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- latitude: 12.9716 (required)
- longitude: 77.5946 (required)
- radiusInKm: 5 (optional, default: 5)

Response (200 OK):
[
  {
    "id": "lot-001",
    "name": "Downtown Mall Parking",
    "address": "123 MG Road, Bangalore",
    "distanceInKm": 0.5,
    "availableSpots": 320,
    "pricePerHour": 50
  }
]
```

### 5. Get Parking Lot Details
```http
GET /parkinglot/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "lot-001",
  "name": "Downtown Mall Parking",
  "address": "123 MG Road, Bangalore",
  "city": "Bangalore",
  "totalCapacity": 500,
  "availableSpots": 320,
  "pricePerHour": 50,
  "managerId": "user-456",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "status": "APPROVED",
  "amenities": ["CCD", "Restroom", "EV Charging"],
  "createdAt": "2026-04-08T10:00:00Z"
}
```

### 6. Update Parking Lot (Manager only)
```http
PUT /parkinglot/{lotId}
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "name": "Downtown Mall Parking",
  "pricePerHour": 60,
  "amenities": ["CCD", "Restroom", "EV Charging", "WiFi"]
}

Response (200 OK):
{
  "id": "lot-001",
  "name": "Downtown Mall Parking",
  "pricePerHour": 60,
  "amenities": ["CCD", "Restroom", "EV Charging", "WiFi"],
  "updatedAt": "2026-04-08T10:30:00Z"
}
```

### 7. Approve Parking Lot (Admin only)
```http
PUT /parkinglot/{lotId}/approve

Headers:
Authorization: Bearer {JWT_TOKEN}
X-User-Role: ADMIN

Response (200 OK):
{
  "id": "lot-001",
  "name": "Downtown Mall Parking",
  "status": "APPROVED",
  "approvedAt": "2026-04-08T10:30:00Z",
  "approvedBy": "admin-123"
}
```

### 8. Delete Parking Lot (Manager only)
```http
DELETE /parkinglot/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (204 No Content):
```

## Auth Service Endpoints (Admin Management)

**Service Base URL:** `http://localhost:8080/auth`

### 1. Admin Login
```http
POST /auth/admin/login
Content-Type: application/json

Request Body:
{
  "email": "admin@parkease.com",
  "password": "Admin@ParkEase123"
}

Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "adminId": "15d0aec7-88f9-490e-b0e8-61972753c94f",
  "fullName": "Super Admin",
  "email": "admin@parkease.com",
  "role": "ADMIN",
  "isSuperAdmin": true,
  "isActive": true
}
```

### 2. Create Admin (Super Admin only)
```http
POST /auth/admin/create
Content-Type: application/json
Authorization: Bearer {SUPER_ADMIN_JWT_TOKEN}

Request Body:
{
  "fullName": "New Admin User",
  "email": "newadmin@parkease.com",
  "password": "SecurePass@123"
}

Response (201 Created):
{
  "adminId": "8517a740-2488-4488-95c8-cfd358a0791d",
  "fullName": "New Admin User",
  "email": "newadmin@parkease.com",
  "role": "ADMIN",
  "isActive": true,
  "isSuperAdmin": false,
  "createdAt": "2026-04-08T19:06:47.161845"
}

Error (403 Forbidden):
{
  "error": "Super Admin privileges required to create admin",
  "status": 403
}
```

### 3. List All Admins (Super Admin only)
```http
GET /auth/admin/all
Authorization: Bearer {SUPER_ADMIN_JWT_TOKEN}

Response (200 OK):
[
  {
    "adminId": "8517a740-2488-4488-95c8-cfd358a0791d",
    "fullName": "Admin User",
    "email": "admin@parkease.com",
    "role": "ADMIN",
    "isActive": true,
    "isSuperAdmin": false,
    "createdAt": "2026-04-08T19:06:47.161845"
  }
]
```

### 4. Deactivate Admin (Super Admin only)
```http
DELETE /auth/admin/{adminId}
Authorization: Bearer {SUPER_ADMIN_JWT_TOKEN}

Response (200 OK):
{
  "message": "Admin deactivated successfully"
}

Error (400 Bad Request):
{
  "error": "Super Admin cannot be deleted",
  "status": 400
}
```

### 5. Reactivate Admin (Super Admin only)
```http
PUT /auth/admin/{adminId}/reactivate
Authorization: Bearer {SUPER_ADMIN_JWT_TOKEN}

Response (200 OK):
{
  "adminId": "8517a740-2488-4488-95c8-cfd358a0791d",
  "fullName": "Admin User",
  "email": "admin@parkease.com",
  "role": "ADMIN",
  "isActive": true,
  "isSuperAdmin": false,
  "createdAt": "2026-04-08T19:06:47.161845"
}

Error (409 Conflict):
{
  "error": "Admin is already active",
  "status": 409
}
```

---

## Spot Service Endpoints

**Service Base URL:** `http://localhost:8080/spot`

### 1. Add Single Spot
```http
POST /spot/add-single
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "parkingLotId": "lot-001",
  "spotNumber": "A-101",
  "spotType": "REGULAR",
  "floor": 1,
  "isAccessible": false,
  "isEVCharging": false
}

Response (201 Created):
{
  "id": "spot-001",
  "parkingLotId": "lot-001",
  "spotNumber": "A-101",
  "spotType": "REGULAR",
  "floor": 1,
  "isAccessible": false,
  "isEVCharging": false,
  "status": "AVAILABLE",
  "createdAt": "2026-04-08T10:00:00Z"
}
```

### 2. Bulk Add Spots
```http
POST /spot/add-bulk
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "parkingLotId": "lot-001",
  "spotCount": 100,
  "floorNumber": 1,
  "spotTypeDistribution": {
    "REGULAR": 70,
    "HANDICAP": 20,
    "COMPACT": 10
  }
}

Response (201 Created):
{
  "message": "100 spots added successfully",
  "parkingLotId": "lot-001",
  "spotsCreated": 100,
  "details": {
    "REGULAR": 70,
    "HANDICAP": 20,
    "COMPACT": 10
  }
}
```

### 3. Get All Spots in Lot
```http
GET /spot/parkinglot/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- page: 0 (optional)
- size: 20 (optional)

Response (200 OK):
{
  "content": [
    {
      "id": "spot-001",
      "spotNumber": "A-101",
      "spotType": "REGULAR",
      "floor": 1,
      "status": "AVAILABLE",
      "isEVCharging": false
    }
  ],
  "totalElements": 500,
  "totalPages": 25
}
```

### 4. Filter Spots by Type
```http
GET /spot/filter/type/{spotType}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- parkingLotId: lot-001 (optional)

Response (200 OK):
[
  {
    "id": "spot-002",
    "spotNumber": "B-101",
    "spotType": "HANDICAP",
    "floor": 1,
    "status": "AVAILABLE"
  }
]
```

### 5. Filter Spots by Vehicle Type
```http
GET /spot/filter/vehicle/{vehicleType}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- parkingLotId: lot-001 (optional)

Response (200 OK):
[
  {
    "id": "spot-001",
    "spotNumber": "A-101",
    "spotType": "REGULAR",
    "availableFor": "CAR"
  }
]
```

### 6. Filter Available Spots by Floor
```http
GET /spot/floor/{floor}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- parkingLotId: lot-001 (required)

Response (200 OK):
[
  {
    "id": "spot-005",
    "spotNumber": "A-105",
    "floor": 1,
    "status": "AVAILABLE"
  }
]
```

### 7. Filter EV Charging Spots
```http
GET /spot/filter/ev-charging

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- parkingLotId: lot-001 (optional)

Response (200 OK):
[
  {
    "id": "spot-010",
    "spotNumber": "C-110",
    "floor": 2,
    "isEVCharging": true,
    "status": "AVAILABLE"
  }
]
```

### 8. Filter Accessible Spots
```http
GET /spot/filter/accessible

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- parkingLotId: lot-001 (optional)

Response (200 OK):
[
  {
    "id": "spot-002",
    "spotNumber": "B-101",
    "floor": 1,
    "isAccessible": true,
    "status": "AVAILABLE"
  }
]
```

### 9. Reserve Spot
```http
PUT /spot/{spotId}/reserve

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "spot-001",
  "spotNumber": "A-101",
  "status": "RESERVED",
  "reservedBy": "user-123",
  "reservedAt": "2026-04-08T10:00:00Z"
}
```

### 10. Occupy Spot
```http
PUT /spot/{spotId}/occupy

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "spot-001",
  "spotNumber": "A-101",
  "status": "OCCUPIED",
  "occupiedBy": "user-123",
  "occupiedAt": "2026-04-08T10:30:00Z"
}
```

### 11. Release Spot
```http
PUT /spot/{spotId}/release

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "spot-001",
  "spotNumber": "A-101",
  "status": "AVAILABLE",
  "releasedAt": "2026-04-08T11:00:00Z"
}
```

### 12. Get Spot Details
```http
GET /spot/{spotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "spot-001",
  "parkingLotId": "lot-001",
  "spotNumber": "A-101",
  "spotType": "REGULAR",
  "floor": 1,
  "status": "OCCUPIED",
  "isAccessible": false,
  "isEVCharging": false,
  "occupiedBy": "user-123",
  "occupiedAt": "2026-04-08T10:30:00Z"
}
```

---

## Booking Service Endpoints

**Service Base URL:** `http://localhost:8080/booking`

### 1. Create Booking
```http
POST /booking/create
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "vehicleId": "vehicle-001",
  "spotId": "spot-001",
  "startTime": "2026-04-08T10:00:00Z",
  "expectedEndTime": "2026-04-08T12:00:00Z"
}

Response (201 Created):
{
  "id": "booking-001",
  "userId": "user-123",
  "vehicleId": "vehicle-001",
  "spotId": "spot-001",
  "spotNumber": "A-101",
  "parkingLotId": "lot-001",
  "startTime": "2026-04-08T10:00:00Z",
  "expectedEndTime": "2026-04-08T12:00:00Z",
  "estimatedFare": 100,
  "status": "CONFIRMED",
  "createdAt": "2026-04-08T09:55:00Z"
}
```

### 2. Check-In (Start Parking)
```http
PUT /booking/{bookingId}/check-in
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "vehicleNumber": "KA-01-AB-1234",
  "odometerReading": 45000
}

Response (200 OK):
{
  "id": "booking-001",
  "status": "CHECKED_IN",
  "checkedInTime": "2026-04-08T10:00:00Z",
  "message": "Check-in successful"
}
```

### 3. Check-Out (End Parking)
```http
PUT /booking/{bookingId}/check-out
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "odometerReading": 45010
}

Response (200 OK):
{
  "id": "booking-001",
  "status": "CHECKED_OUT",
  "checkedOutTime": "2026-04-08T12:00:00Z",
  "actualDuration": "2 hours",
  "finalFare": 100,
  "message": "Check-out successful. Payment pending."
}
```

### 4. Extend Booking
```http
PUT /booking/{bookingId}/extend
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "extensionHours": 2
}

Response (200 OK):
{
  "id": "booking-001",
  "status": "EXTENDED",
  "newExpectedEndTime": "2026-04-08T14:00:00Z",
  "additionalFare": 100,
  "totalFare": 200
}
```

### 5. Cancel Booking
```http
PUT /booking/{bookingId}/cancel
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "reason": "Plans changed"
}

Response (200 OK):
{
  "id": "booking-001",
  "status": "CANCELLED",
  "cancelledAt": "2026-04-08T09:55:00Z",
  "refundAmount": 100,
  "message": "Booking cancelled. Refund will be processed."
}

Error (400 Bad Request):
{
  "error": "Cannot cancel checked-in or checked-out bookings",
  "status": 400
}
```

### 6. Get Booking Details
```http
GET /booking/{bookingId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "booking-001",
  "userId": "user-123",
  "vehicleId": "vehicle-001",
  "spotId": "spot-001",
  "spotNumber": "A-101",
  "parkingLotName": "Downtown Mall Parking",
  "startTime": "2026-04-08T10:00:00Z",
  "expectedEndTime": "2026-04-08T12:00:00Z",
  "status": "CHECKED_OUT",
  "fare": 100,
  "paymentStatus": "PENDING",
  "createdAt": "2026-04-08T09:55:00Z"
}
```

### 7. Get User Bookings History
```http
GET /booking/my-bookings

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- status: CHECKED_OUT (optional)
- page: 0 (optional)
- size: 10 (optional)
- sortBy: createdAt (optional)

Response (200 OK):
{
  "content": [
    {
      "id": "booking-001",
      "spotNumber": "A-101",
      "parkingLotName": "Downtown Mall Parking",
      "startTime": "2026-04-08T10:00:00Z",
      "endTime": "2026-04-08T12:00:00Z",
      "fare": 100,
      "status": "CHECKED_OUT"
    }
  ],
  "totalPages": 5,
  "totalElements": 45
}
```

### 8. Get Active Bookings
```http
GET /booking/active

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
[
  {
    "id": "booking-001",
    "spotNumber": "A-101",
    "parkingLotName": "Downtown Mall Parking",
    "startTime": "2026-04-08T10:00:00Z",
    "expectedEndTime": "2026-04-08T12:00:00Z",
    "status": "CHECKED_IN",
    "fare": 100
  }
]
```

### 9. Calculate Fare
```http
POST /booking/calculate-fare
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "parkingLotId": "lot-001",
  "startTime": "2026-04-08T10:00:00Z",
  "endTime": "2026-04-08T12:00:00Z"
}

Response (200 OK):
{
  "duration": "2 hours",
  "pricePerHour": 50,
  "baseFare": 100,
  "discounts": 0,
  "taxes": 18,
  "totalFare": 118
}
```

---

## Payment Service Endpoints

**Service Base URL:** `http://localhost:8080/payment`

### 1. Initiate Payment
```http
POST /payment/initiate
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "bookingId": "booking-001",
  "amount": 118,
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2026,
    "cvv": "123"
  }
}

Response (200 OK):
{
  "paymentId": "payment-001",
  "bookingId": "booking-001",
  "amount": 118,
  "paymentMethod": "CREDIT_CARD",
  "status": "PROCESSING",
  "transactionId": "TXN-2026-04-08-001",
  "createdAt": "2026-04-08T12:00:00Z"
}
```

### 2. Confirm Payment
```http
POST /payment/{paymentId}/confirm
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "otp": "123456",
  "transactionId": "TXN-2026-04-08-001"
}

Response (200 OK):
{
  "paymentId": "payment-001",
  "bookingId": "booking-001",
  "status": "SUCCESSFUL",
  "amount": 118,
  "transactionId": "TXN-2026-04-08-001",
  "confirmedAt": "2026-04-08T12:00:30Z"
}

Error (400 Bad Request):
{
  "error": "Payment failed. Card declined.",
  "status": "DECLINED",
  "transactionId": "TXN-2026-04-08-001"
}
```

### 3. Get Payment Status
```http
GET /payment/{paymentId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "paymentId": "payment-001",
  "bookingId": "booking-001",
  "amount": 118,
  "status": "SUCCESSFUL",
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "TXN-2026-04-08-001",
  "createdAt": "2026-04-08T12:00:00Z",
  "confirmedAt": "2026-04-08T12:00:30Z"
}
```

### 4. Initiate Refund
```http
POST /payment/{paymentId}/refund
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

Request Body:
{
  "reason": "Booking cancelled",
  "refundAmount": 118
}

Response (200 OK):
{
  "refundId": "refund-001",
  "paymentId": "payment-001",
  "bookingId": "booking-001",
  "amount": 118,
  "status": "PROCESSING",
  "requestedAt": "2026-04-08T12:30:00Z"
}
```

### 5. Get Refund Status
```http
GET /payment/refund/{refundId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "refundId": "refund-001",
  "paymentId": "payment-001",
  "amount": 118,
  "status": "COMPLETED",
  "processedAt": "2026-04-08T12:45:00Z",
  "bankReferenceId": "BANK-REF-12345"
}
```

### 6. Get Payment History
```http
GET /payment/history

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- status: SUCCESSFUL (optional)
- page: 0 (optional)
- size: 10 (optional)

Response (200 OK):
{
  "content": [
    {
      "paymentId": "payment-001",
      "bookingId": "booking-001",
      "amount": 118,
      "status": "SUCCESSFUL",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "TXN-2026-04-08-001",
      "createdAt": "2026-04-08T12:00:00Z"
    }
  ],
  "totalElements": 25,
  "totalPages": 3
}
```

### 7. Download Receipt (PDF)
```http
GET /payment/{paymentId}/receipt

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK - PDF):
[PDF File Binary Content]

Headers:
Content-Type: application/pdf
Content-Disposition: attachment; filename=receipt_payment-001.pdf
```

### 8. Get Transaction Details
```http
GET /payment/transaction/{transactionId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "transactionId": "TXN-2026-04-08-001",
  "bookingId": "booking-001",
  "amount": 118,
  "currency": "INR",
  "status": "SUCCESSFUL",
  "payload": {
    "method": "CREDIT_CARD",
    "last4": "1111",
    "brand": "VISA"
  },
  "createdAt": "2026-04-08T12:00:00Z"
}
```

---

## Notification Service Endpoints

**Service Base URL:** `http://localhost:8080/notification`

### 1. Get User Notifications
```http
GET /notification/my-notifications

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- status: UNREAD (optional)
- page: 0 (optional)
- size: 20 (optional)

Response (200 OK):
{
  "content": [
    {
      "id": "notif-001",
      "title": "Booking Confirmed",
      "message": "Your booking for spot A-101 is confirmed",
      "type": "BOOKING",
      "status": "UNREAD",
      "createdAt": "2026-04-08T10:00:00Z"
    }
  ],
  "unreadCount": 5,
  "totalPages": 1
}
```

### 2. Mark Notification as Read
```http
PUT /notification/{notificationId}/read

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "id": "notif-001",
  "status": "READ",
  "readAt": "2026-04-08T10:05:00Z"
}
```

### 3. Mark All Notifications as Read
```http
PUT /notification/mark-all-read

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "message": "All notifications marked as read",
  "markedCount": 5
}
```

### 4. Delete Notification
```http
DELETE /notification/{notificationId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (204 No Content):
```

### 5. Clear All Notifications
```http
DELETE /notification/clear-all

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "message": "All notifications cleared",
  "deletedCount": 5
}
```

### 6. Get Notification Count
```http
GET /notification/unread-count

Headers:
Authorization: Bearer {JWT_TOKEN}

Response (200 OK):
{
  "unreadCount": 3,
  "totalCount": 45
}
```

### 7. Send Broadcast Notification (Admin only)
```http
POST /notification/broadcast
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
X-User-Role: ADMIN

Request Body:
{
  "title": "System Maintenance",
  "message": "System maintenance scheduled for tonight 2-4 AM",
  "notificationType": "SYSTEM",
  "targetRole": "DRIVER"
}

Response (200 OK):
{
  "broadcastId": "broadcast-001",
  "title": "System Maintenance",
  "recipientsCount": 5000,
  "sentAt": "2026-04-08T10:00:00Z"
}
```

### 8. Send Targeted Notifications (Admin only)
```http
POST /notification/send-targeted
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
X-User-Role: ADMIN

Request Body:
{
  "userIds": ["user-123", "user-456"],
  "title": "Special Offer",
  "message": "Get 20% discount on your next booking",
  "notificationType": "PROMOTION"
}

Response (200 OK):
{
  "sentCount": 2,
  "failedCount": 0,
  "sentAt": "2026-04-08T10:00:00Z"
}
```

---

## Analytics Service Endpoints

**Service Base URL:** `http://localhost:8080/analytics`

### 1. Get Parking Lot Occupancy Rate
```http
GET /analytics/occupancy/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "lotId": "lot-001",
  "lotName": "Downtown Mall Parking",
  "totalCapacity": 500,
  "averageOccupancy": 65,
  "peakOccupancy": 95,
  "lowestOccupancy": 10,
  "occupancyTrend": [
    {
      "date": "2026-04-08",
      "occupancyPercentage": 65,
      "spotsOccupied": 325
    }
  ]
}
```

### 2. Get Hourly Occupancy Breakdown
```http
GET /analytics/occupancy/{lotId}/hourly

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- date: 2026-04-08 (optional)

Response (200 OK):
{
  "lotId": "lot-001",
  "date": "2026-04-08",
  "hourlyData": [
    {
      "hour": 8,
      "occupancyPercentage": 15,
      "spotsOccupied": 75,
      "entries": 20,
      "exits": 5
    },
    {
      "hour": 9,
      "occupancyPercentage": 45,
      "spotsOccupied": 225,
      "entries": 150,
      "exits": 20
    }
  ]
}
```

### 3. Get Peak Hours
```http
GET /analytics/peak-hours/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- days: 7 (optional, default: 7)

Response (200 OK):
{
  "lotId": "lot-001",
  "peakHours": [
    {
      "hour": 10,
      "averageOccupancy": 85,
      "dayOfWeek": "Monday-Friday"
    },
    {
      "hour": 11,
      "averageOccupancy": 88,
      "dayOfWeek": "Monday-Friday"
    }
  ],
  "offPeakHours": [
    {
      "hour": 2,
      "averageOccupancy": 5
    }
  ]
}
```

### 4. Get Daily Revenue
```http
GET /analytics/revenue/daily

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "totalRevenue": 50000,
  "currency": "INR",
  "dailyData": [
    {
      "date": "2026-04-08",
      "revenue": 8500,
      "transactions": 150,
      "averageTransactionValue": 56.67
    }
  ]
}
```

### 5. Get Revenue by Parking Lot
```http
GET /analytics/revenue/by-lot

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "totalRevenue": 150000,
  "lotData": [
    {
      "lotId": "lot-001",
      "lotName": "Downtown Mall Parking",
      "revenue": 45000,
      "bookings": 750,
      "revenuePercentage": 30
    }
  ]
}
```

### 6. Get Platform-wide Revenue Analytics
```http
GET /analytics/revenue/platform

Headers:
Authorization: Bearer {JWT_TOKEN}
X-User-Role: SUPER_ADMIN

Query Parameters:
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "totalRevenue": 500000,
  "totalBookings": 10000,
  "totalTransactions": 9500,
  "averageBookingValue": 50,
  "currency": "INR",
  "topLots": [
    {
      "lotName": "Downtown Mall Parking",
      "revenue": 50000,
      "percentage": 10
    }
  ]
}
```

### 7. Get Parking Duration Statistics
```http
GET /analytics/parking-duration

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- lotId: lot-001 (optional)
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "averageDuration": "2.5 hours",
  "medianDuration": "2 hours",
  "minDuration": "15 minutes",
  "maxDuration": "24 hours",
  "durationBuckets": [
    {
      "duration": "0-1 hour",
      "count": 2000,
      "percentage": 20
    },
    {
      "duration": "1-3 hours",
      "count": 5000,
      "percentage": 50
    }
  ]
}
```

### 8. Get Lot Utilization Report
```http
GET /analytics/utilization/{lotId}

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- startDate: 2026-04-01 (optional)
- endDate: 2026-04-08 (optional)

Response (200 OK):
{
  "lotId": "lot-001",
  "lotName": "Downtown Mall Parking",
  "totalCapacity": 500,
  "totalTimeSlots": 168,
  "utilizationPercentage": 65,
  "totalBookings": 750,
  "totalRevenue": 45000,
  "recommendations": [
    "Consider dynamic pricing during peak hours",
    "Optimize EV charging spots allocation"
  ]
}
```

### 9. Get Vehicle Type Distribution
```http
GET /analytics/vehicle-distribution

Headers:
Authorization: Bearer {JWT_TOKEN}

Query Parameters:
- lotId: lot-001 (optional)
- startDate: 2026-04-01 (optional)

Response (200 OK):
{
  "totalBookings": 750,
  "distribution": [
    {
      "vehicleType": "CAR",
      "count": 450,
      "percentage": 60,
      "averageStay": "2.5 hours"
    },
    {
      "vehicleType": "TWO_WHEELER",
      "count": 200,
      "percentage": 26,
      "averageStay": "1 hour"
    },
    {
      "vehicleType": "TRUCK",
      "count": 100,
      "percentage": 14,
      "averageStay": "3 hours"
    }
  ]
}
```

---

## Error Response Codes

### Standard HTTP Status Codes

| Code | Status | Description | Example |
|------|--------|-------------|---------|
| 200 | OK | Successful request | `{ "message": "Success" }` |
| 201 | Created | Resource created successfully | `{ "id": "123", ...}` |
| 204 | No Content | Successful request with no response body | [Empty response] |
| 400 | Bad Request | Invalid request parameters | `{ "error": "Invalid email format", "status": 400 }` |
| 401 | Unauthorized | Missing or invalid authentication | `{ "error": "Unauthorized", "status": 401 }` |
| 403 | Forbidden | User lacks required permissions | `{ "error": "Access denied", "status": 403 }` |
| 404 | Not Found | Resource not found | `{ "error": "Booking not found", "status": 404 }` |
| 409 | Conflict | Resource conflict (e.g., spot already occupied) | `{ "error": "Spot already occupied", "status": 409 }` |
| 422 | Unprocessable Entity | Request validation failed | `{ "error": "Invalid data", "status": 422 }` |
| 500 | Internal Server Error | Server error | `{ "error": "Internal server error", "status": 500 }` |
| 503 | Service Unavailable | Service temporarily unavailable | `{ "error": "Service unavailable", "status": 503 }` |

### Common Error Response Format

```json
{
  "error": "Error message",
  "status": 400,
  "timestamp": "2026-04-08T10:00:00Z",
  "path": "/booking/create",
  "details": {
    "field": "startTime",
    "message": "Must be in future"
  }
}
```

### Authentication Errors

**Missing Token:**
```json
{
  "error": "Missing authorization token",
  "status": 401,
  "message": "Authorization header is required"
}
```

**Invalid Token:**
```json
{
  "error": "Invalid token",
  "status": 401,
  "message": "Token has expired or is invalid"
}
```

**Insufficient Permissions:**
```json
{
  "error": "Forbidden",
  "status": 403,
  "message": "User role 'DRIVER' is not authorized for this action"
}
```

---

## How to Use This Documentation

### 1. For Client Integration
- Identify the service you need to interact with
- Find the relevant endpoint section
- Note the HTTP method and path
- Include required headers (Authorization, Content-Type)
- Follow the request body format
- Handle the response and error codes appropriately

### 2. For API Gateway Routing
All requests follow this pattern:
```
http://localhost:8080/{service-path}/{endpoint}
```

Example:
```
POST http://localhost:8080/booking/create
Authorization: Bearer {your-jwt-token}
```

### 3. For Authentication
1. Register or Login via Auth Service
2. Obtain JWT token from response
3. Include token in Authorization header for all subsequent requests
4. Token format: `Bearer {JWT_TOKEN}`

### 4. For Error Handling
- Check HTTP status code first
- Parse error response body for specific error message
- Implement retry logic for 5xx errors
- Handle 401 errors by re-authenticating
- Handle 403 errors by checking user permissions

---

## Notes

- All timestamps are in UTC (ISO 8601 format)
- All monetary amounts are in INR unless specified otherwise
- Pagination starts from page 0
- Default page size is 10 unless specified
- Most endpoints require JWT authentication
- Role-based access control is enforced at service level
- API Gateway validates requests before routing
- Services communicate via REST and message queues
- Circuit breaker pattern is implemented for resilience

---

**Last Updated:** April 8, 2026  
**Documentation Version:** 1.0

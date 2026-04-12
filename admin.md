# ParkEase Admin Documentation

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Admin Service Architecture](#admin-service-architecture)
4. [API Gateway Entry Points](#api-gateway-entry-points)
5. [Auth Service Admin Endpoints](#auth-service-admin-endpoints)
6. [Parking Lot Service Admin Endpoints](#parking-lot-service-admin-endpoints)
7. [Booking Service Admin Endpoints](#booking-service-admin-endpoints)
8. [Payment Service Admin Endpoints](#payment-service-admin-endpoints)
9. [Vehicle Service Admin Endpoints](#vehicle-service-admin-endpoints)
10. [Spot Service Admin Endpoints](#spot-service-admin-endpoints)
11. [Notification Service Admin Endpoints](#notification-service-admin-endpoints)
12. [Analytics Service Admin Endpoints](#analytics-service-admin-endpoints)
13. [Complete Request/Response Examples](#complete-requestresponse-examples)
14. [Service Communication & Data Models](#service-communication--data-models)
15. [Frontend Development Guide](#frontend-development-guide)

---

## System Overview

**ParkEase** is a microservices-based parking management platform with the following architecture:

### Core Components
- **API Gateway** - Routes all requests to appropriate services
- **Auth Service** - Handles authentication and admin management
- **Eureka Server** - Service discovery for internal communication
- **7 Business Services** - Parking Lots, Bookings, Payments, Vehicles, Spots, Notifications, Analytics

### Admin System Features
- Role-based access control (RBAC) with 4 roles
- JWT-based stateless authentication
- Dual authentication enforcement (Route + Method level)
- Separate admin database with BCrypt hashing
- System-level tokens for inter-service communication

---

## Authentication & Authorization

### Role Hierarchy

**Roles in System:** `ADMIN`, `DRIVER`, `MANAGER`

**Admin System Uses Boolean Flag:**

```
ADMIN Role (in JWT)
  ├── isSuperAdmin=true (Seeded admin)
  │   ├── Create/Delete other admins
  │   ├── Access all admin endpoints
  │   └── Can manage the entire platform
  │
  └── isSuperAdmin=false (Created via API)
      ├── Access all admin endpoints
      ├── Cannot create/delete other admins
      └── Same platform access as Super Admin

DRIVER Role
  └── User-level access only

MANAGER Role
  └── Lot-specific access (can manage own parking lot)
```

**Key Distinction:**
- `isSuperAdmin` is a **boolean database flag**, NOT a role
- All admins have role `"ADMIN"` in JWT token
- Only ONE Super Admin can exist (seeded from configuration)
- Other admins are created by Super Admin with `isSuperAdmin=false`

### JWT Token Structure

**Token Type:** HS256 (HMAC with SHA-256)
**Expiry:** 24 hours (86400000 ms)
**Algorithm:** JWT with symmetric key

**Admin Token:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "admin@parkease.com",
  "role": "ADMIN",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@parkease.com",
  "isSuperAdmin": true,
  "iat": 1712551200,
  "exp": 1712637600
}
```

**User Token (for comparison):**
```json
{
  "role": "DRIVER" or "MANAGER",
  "userId": "user_uuid",
  "email": "user@example.com",
  "iat": 1712551200,
  "exp": 1712637600
  // NO isSuperAdmin claim for users
}
```

### Required Headers for All Admin API Calls

```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
X-Service-ID: admin-portal         # Optional but recommended
```

**Note:** Token must be obtained from `/auth/admin/login` endpoint and will be valid for 24 hours.

### Authentication Flow

```
1. Admin sends credentials to /auth/admin/login endpoint
   ↓
2. Auth Service queries ADMINS table by email (BCrypt validation)
   ↓
3. Validates: Account exists, isActive=true, password matches
   ↓
4. Checks isSuperAdmin flag in database
   ↓
5. Generates JWT token with role="ADMIN" and isSuperAdmin claim
   ↓
6. Returns token (valid for 24 hours)
   ↓
7. Admin includes token in Authorization header: Bearer <token>
   ↓
8. API Gateway validates token signature
   ↓
9. Each service validates token independently via JWT filter
   ↓
10. Authorization checks:
    - Route-level: hasRole("ADMIN") from Spring Security
    - Service-level: isSuperAdmin boolean checked for sensitive operations
```

---

## Admin Service Architecture

### Service-to-Service Communication

```
Admin Portal (Frontend)
        ↓
    API Gateway (localhost:8080)
        ↓
    ┌───────────────────────────────────────┐
    │                                       │
Auth Service       Other Services      Eureka Server
(Auth)             (via Discovery)      (Discovery)
    └───────────────────────────────────────┘
```

### Eureka Service Discovery

| Service | Eureka Name | Port | Base URL |
|---------|------------|------|----------|
| Auth Service | auth-service | 8001 | http://auth-service:8001 |
| Booking Service | booking-service | 8002 | http://booking-service:8002 |
| Parking Lot Service | parkinglot-service | 8003 | http://parkinglot-service:8003 |
| Payment Service | payment-service | 8004 | http://payment-service:8004 |
| Vehicle Service | vehicle-service | 8005 | http://vehicle-service:8005 |
| Spot Service | spot-service | 8006 | http://spot-service:8006 |
| Notification Service | notification-service | 8007 | http://notification-service:8007 |
| Analytics Service | analytics-service | 8008 | http://analytics-service:8008 |
| Eureka Server | eureka-server | 8761 | http://eureka-server:8761 |
| API Gateway | apigateway | 8080 | http://gateway:8080 |

---

## API Gateway Entry Points

### Gateway Configuration

**Base Gateway URL:** `http://localhost:8080`

### Gateway Routes for Admin

All admin requests route through the gateway to respective services:

```yaml
Gateway Routes:
  /auth/** → auth-service:8001/auth/**
  /parking-lots/** → parkinglot-service:8003/parking-lots/**
  /bookings/** → booking-service:8002/bookings/**
  /payments/** → payment-service:8004/payments/**
  /vehicles/** → vehicle-service:8005/vehicles/**
  /spots/** → spot-service:8006/spots/**
  /notifications/** → notification-service:8007/notifications/**
  /analytics/** → analytics-service:8008/analytics/**
```

### Gateway Headers Pass-Through

The gateway automatically forwards these headers:
- `Authorization: Bearer <token>`
- `Content-Type: application/json`
- `X-Request-ID` (if present)
- `X-Forwarded-For`
- Custom headers starting with `X-`

---

## Auth Service Admin Endpoints

**Service URL:** `http://localhost:8001/auth`  
**Gateway URL:** `http://localhost:8080/auth`

### 1. Admin Login

**Endpoint:** `POST /auth/admin/login`  
**Access:** Public (no auth required)  
**Description:** Authenticate admin user and receive JWT token

**Request:**
```json
{
  "email": "admin@parkease.com",
  "password": "SecurePassword123!"
}
```

**Request Headers:**
```
Content-Type: application/json
Accept: application/json
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "admin": {
    "adminId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "admin@parkease.com",
    "fullName": "Super Admin",
    "isSuperAdmin": true,
    "isActive": true
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Invalid email or password",
  "status": 401,
  "timestamp": "2024-04-08T10:30:00Z"
}
```

---

### 2. Create New Admin (SUPER_ADMIN Only)

**Endpoint:** `POST /auth/admin/create`  
**Access:** ROLE_ADMIN (Spring Security) + isSuperAdmin=true (service level)  
**Description:** Create a new admin account (Super Admin exclusive)

**Request:**
```json
{
  "email": "newadmin@parkease.com",
  "password": "AdminPassword123!",
  "fullName": "New Admin User"
}
```

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN_WITH_isSuperAdmin_true>
Accept: application/json
```

**Response (201 Created):**
```json
{
  "adminId": "660e8400-e29b-41d4-a716-446655440001",
  "email": "newadmin@parkease.com",
  "fullName": "New Admin User",
  "isSuperAdmin": false,
  "isActive": true,
  "createdAt": "2024-04-08T10:30:00Z"
}
```

**Note:** API automatically sets `isSuperAdmin=false` for all newly created admins.

**Error Responses:**

403 Forbidden (Insufficient Permissions - Not Super Admin):
```json
{
  "error": "Access denied. Only super admins can create admins",
  "status": 403,
  "message": "This operation requires isSuperAdmin=true"
}
```

409 Conflict (Email Already Exists):
```json
{
  "error": "Admin with this email already exists",
  "status": 409
}
```

---

### 3. Delete Admin (SUPER_ADMIN Only)

**Endpoint:** `DELETE /auth/admin/{adminId}`  
**Access:** ROLE_ADMIN (Spring Security) + isSuperAdmin=true (service level)  
**Description:** Delete an admin account

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN_WITH_isSuperAdmin_true>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "adminId": "660e8400-e29b-41d4-a716-446655440001",
  "email": "newadmin@parkease.com",
  "fullName": "New Admin User",
  "isActive": false,
  "deletedAt": "2024-04-08T10:35:00Z",
  "message": "Admin account deleted successfully (soft deleted)"
}
```

**Error Responses:**

404 Not Found:
```json
{
  "error": "Admin not found",
  "status": 404
}
```

---

### 4. Get All Admins

**Endpoint:** `GET /auth/admin/all`  
**Access:** ROLE_ADMIN (Spring Security) + isSuperAdmin=true (service level)  
**Description:** Retrieve list of all admin accounts (Super Admin only)

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=20
&isSuperAdmin=true  (optional filter)
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "adminId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "admin@parkease.com",
      "fullName": "Super Admin",
      "role": "ADMIN",
      "isSuperAdmin": true,
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00Z"
    },
    {
      "adminId": "660e8400-e29b-41d4-a716-446655440001",
      "email": "newadmin@parkease.com",
      "fullName": "Regular Admin",
      "role": "ADMIN",
      "isSuperAdmin": false,
      "isActive": true,
      "createdAt": "2024-04-08T10:30:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "size": 20
}
```

---

## User Management Admin Endpoints

**Service URL:** `http://localhost:8001/auth`  
**Gateway URL:** `http://localhost:8080/auth`

### 1. Get All Users (with optional role filter)

**Endpoint:** `GET /auth/users`  
**Access:** ROLE_ADMIN  
**Description:** Retrieve all active DRIVER/MANAGER/ADMIN users, optionally filtered by role

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?role=DRIVER    (optional: DRIVER, MANAGER, ADMIN)
```

**Response (200 OK):**
```json
[
  {
    "userId": "550e8400-e29b-41d4-a716-446655440100",
    "fullName": "John Driver",
    "email": "john@parkease.com",
    "phone": "+1-555-1000",
    "role": "DRIVER",
    "vehiclePlate": "ABC-1234",
    "isActive": true,
    "createdAt": "2024-02-15T08:00:00Z",
    "profilePicUrl": "https://example.com/pic.jpg"
  },
  {
    "userId": "660e8400-e29b-41d4-a716-446655440101",
    "fullName": "Sarah Manager",
    "email": "sarah@parkease.com",
    "phone": "+1-555-2000",
    "role": "MANAGER",
    "vehiclePlate": null,
    "isActive": true,
    "createdAt": "2024-01-10T10:00:00Z",
    "profilePicUrl": null
  }
]
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Unauthorized",
  "status": 401,
  "message": "Full authentication is required to access this resource"
}
```

**Error Response (403 Forbidden):**
```json
{
  "error": "Access Denied",
  "status": 403,
  "message": "Access is denied. Admin role required."
}
```

---

### 2. Deactivate User Account

**Endpoint:** `PUT /auth/users/{userId}/deactivate`  
**Access:** ROLE_ADMIN  
**Description:** Deactivate (soft delete) a user account and send notification email

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440100",
  "fullName": "John Driver",
  "email": "john@parkease.com",
  "phone": "+1-555-1000",
  "role": "DRIVER",
  "vehiclePlate": "ABC-1234",
  "isActive": false,
  "createdAt": "2024-02-15T08:00:00Z",
  "profilePicUrl": "https://example.com/pic.jpg"
}
```

**Side Effects:**
- ✅ User account marked as inactive (soft delete - data retained)
- ✅ User receives deactivation confirmation email
- ✅ User cannot login or book parking spots
- ✅ Existing reservations remain for audit purposes

**Email Sent:**
```
Subject: Account Deactivation Confirmation
Content: 
  - Personalized greeting with user's full name
  - Confirmation that account has been deactivated
  - Information about lost access to bookings and features
  - Option to reactivate anytime
  - ParkEase Team signature
  - Professional styled HTML email
```

**Error Responses:**

404 Not Found:
```json
{
  "error": "Not Found",
  "status": 404,
  "message": "User not found with id: 550e8400-e29b-41d4-a716-446655440100"
}
```

400 Bad Request (Already deactivated):
```json
{
  "error": "Bad Request",
  "status": 400,
  "message": "User is already deactivated"
}
```

---

### 3. Reactivate User Account

**Endpoint:** `PUT /auth/users/{userId}/reactivate`  
**Access:** ROLE_ADMIN  
**Description:** Reactivate a previously deactivated user account and send welcome-back email

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440100",
  "fullName": "John Driver",
  "email": "john@parkease.com",
  "phone": "+1-555-1000",
  "role": "DRIVER",
  "vehiclePlate": "ABC-1234",
  "isActive": true,
  "createdAt": "2024-02-15T08:00:00Z",
  "profilePicUrl": "https://example.com/pic.jpg"
}
```

**Side Effects:**
- ✅ User account marked as active
- ✅ User receives reactivation welcome-back email
- ✅ User can now login and book parking spots
- ✅ Full account access restored

**Email Sent:**
```
Subject: Your ParkEase Account Is Active Again
Content:
  - Welcome back message with user's full name
  - Confirmation that account has been reactivated
  -Information about full access to bookings and features
  - Encouragement to start booking parking spots
  - ParkEase Team signature
  - Professional styled HTML email
```

**Error Responses:**

404 Not Found:
```json
{
  "error": "Not Found",
  "status": 404,
  "message": "User not found with id: 550e8400-e29b-41d4-a716-446655440100"
}
```

400 Bad Request (Already active):
```json
{
  "error": "Bad Request",
  "status": 400,
  "message": "User is already active"
}
```

---

## Email Notification System

### User Account Status Emails

**Two automated emails are sent by auth-service:**

#### 1. Deactivation Email
- **Trigger:** When admin calls PUT `/auth/users/{userId}/deactivate`
- **Recipient:** User's email address
- **Subject:** "Account Deactivation Confirmation"
- **Content:** Explains account has been deactivated, lists consequences, offers reactivation option
- **Delivery:** Synchronous via Resend API (non-blocking if fails - logged only)

#### 2. Reactivation Email
- **Trigger:** When admin calls PUT `/auth/users/{userId}/reactivate`
- **Recipient:** User's email address
- **Subject:** "Your ParkEase Account Is Active Again"
- **Content:** Welcomes user back, confirms full access restored, encourages booking
- **Delivery:** Synchronous via Resend API (non-blocking if fails - logged only)

### Email Template Features
- ✅ Personalized with user's full name
- ✅ HTML formatted with professional styling
- ✅ Responsive design for mobile/desktop
- ✅ ParkEase branding and footer
- ✅ Clear call-to-action messages
- ✅ Blue gradient accent colors

### Email Configuration
Email service uses:
- **Provider:** Resend API
- **Configuration Keys:**
  - `resend.api-key` - API key for Resend
  - `resend.from-email` - Sender email address
  - `resend.from-name` - Sender display name
- **Error Handling:** Non-critical (logged warning, doesn't block transaction)

---

## Parking Lot Service Admin Endpoints

**Service URL:** `http://localhost:8003/parking-lots`  
**Gateway URL:** `http://localhost:8080/parking-lots`

### 1. Get All Parking Lots

**Endpoint:** `GET /parking-lots/admin/all`  
**Access:** ROLE_ADMIN  
**Description:** Retrieve all parking lots with admin details

**Request:**
```
No body required
```

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=50
&status=ACTIVE  (optional: ACTIVE, INACTIVE, PENDING_APPROVAL)
&sortBy=createdAt
&sortDirection=DESC
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "lotName": "Downtown Parking Complex",
      "location": "123 Main Street, City, State",
      "totalSpots": 500,
      "availableSpots": 350,
      "occupiedSpots": 150,
      "managerId": 5,
      "managerName": "John Manager",
      "managerEmail": "manager@parkease.com",
      "pricePerHour": 5.99,
      "pricePerDay": 45.00,
      "monthlySubscription": 300.00,
      "status": "ACTIVE",
      "isApproved": true,
      "createdAt": "2024-01-15T08:00:00Z",
      "updatedAt": "2024-04-08T10:00:00Z",
      "phone": "+1-555-1234",
      "email": "downtown@parkease.com",
      "openingTime": "06:00",
      "closingTime": "23:00",
      "features": ["CCTV", "EV_CHARGING", "COVERED", "24_7_SUPPORT"],
      "averageRating": 4.5,
      "totalReviews": 234
    }
  ],
  "totalElements": 25,
  "totalPages": 1,
  "currentPage": 0,
  "size": 50
}
```

---

### 2. Get Pending Approval Loads

**Endpoint:** `GET /parking-lots/admin/pending`  
**Access:** ROLE_ADMIN  
**Description:** Get all parking lots awaiting admin approval

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 8,
      "lotName": "New Shopping Mall Parking",
      "location": "456 Commerce Ave",
      "totalSpots": 300,
      "managerId": 12,
      "managerName": "Sarah Owner",
      "pricePerHour": 4.99,
      "status": "PENDING_APPROVAL",
      "isApproved": false,
      "submittedAt": "2024-04-07T15:30:00Z",
      "documents": {
        "businessLicense": "license_url",
        "proofOfOwnership": "proof_url",
        "insuranceCertificate": "insurance_url"
      }
    }
  ],
  "totalElements": 3,
  "totalPages": 1
}
```

---

### 3. Approve Parking Lot

**Endpoint:** `PUT /parking-lots/admin/{lotId}/approve`  
**Access:** ROLE_ADMIN  
**Description:** Approve a pending parking lot for operation

**Request:**
```json
{
  "approvalNotes": "Documentation verified. Ready for operation.",
  "approvedFeatures": ["CCTV", "EV_CHARGING"],
  "effectiveDate": "2024-04-09T00:00:00Z"
}
```

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 8,
  "lotName": "New Shopping Mall Parking",
  "status": "ACTIVE",
  "isApproved": true,
  "approvedAt": "2024-04-08T11:00:00Z",
  "approvedBy": {
    "id": 1,
    "email": "admin@parkease.com"
  },
  "approvalNotes": "Documentation verified. Ready for operation."
}
```

---

### 4. Update Parking Lot

**Endpoint:** `PUT /parking-lots/admin/{lotId}`  
**Access:** ROLE_ADMIN  
**Description:** Update parking lot details

**Request:**
```json
{
  "lotName": "Downtown Parking Complex - Updated",
  "totalSpots": 550,
  "pricePerHour": 6.99,
  "pricePerDay": 50.00,
  "monthlySubscription": 350.00,
  "openingTime": "05:00",
  "closingTime": "24:00",
  "features": ["CCTV", "EV_CHARGING", "COVERED", "24_7_SUPPORT", "VALET_SERVICE"],
  "phone": "+1-555-5678",
  "email": "downtown.updated@parkease.com"
}
```

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 1,
  "lotName": "Downtown Parking Complex - Updated",
  "status": "ACTIVE",
  "totalSpots": 550,
  "pricePerHour": 6.99,
  "updatedAt": "2024-04-08T11:15:00Z",
  "updatedBy": "admin@parkease.com"
}
```

---

### 5. Delete Parking Lot

**Endpoint:** `DELETE /parking-lots/admin/{lotId}`  
**Access:** ROLE_ADMIN  
**Description:** Delete a parking lot and all associated data

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 1,
  "lotName": "Downtown Parking Complex",
  "status": "DELETED",
  "deletedAt": "2024-04-08T11:30:00Z",
  "message": "Parking lot deleted successfully. Associated bookings have been cancelled."
}
```

---

## Booking Service Admin Endpoints

**Service URL:** `http://localhost:8002/bookings`  
**Gateway URL:** `http://localhost:8080/bookings`

### 1. Get All Bookings (Platform-wide)

**Endpoint:** `GET /bookings/admin/all`  
**Access:** ROLE_ADMIN  
**Description:** Retrieve all bookings across all parking lots

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=50
&status=ACTIVE  (ACTIVE, COMPLETED, CANCELLED, EXPIRED)
&startDate=2024-04-01T00:00:00Z
&endDate=2024-04-30T23:59:59Z
&sortBy=createdAt
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 101,
      "bookingReference": "BK-2024-001",
      "userId": 50,
      "userEmail": "driver@parkease.com",
      "parkingLotId": 1,
      "lotName": "Downtown Parking Complex",
      "spotId": 45,
      "spotNumber": "A-45",
      "vehicleId": 30,
      "vehicleNumber": "ABC-1234",
      "checkInTime": "2024-04-08T09:00:00Z",
      "checkOutTime": "2024-04-08T17:30:00Z",
      "duration": "8.5 hours",
      "pricePerHour": 5.99,
      "totalCost": 50.92,
      "status": "ACTIVE",
      "paymentStatus": "PAID",
      "createdAt": "2024-04-08T08:30:00Z"
    }
  ],
  "totalElements": 1250,
  "totalPages": 25,
  "currentPage": 0,
  "size": 50
}
```

---

### 2. Get Bookings by Parking Lot

**Endpoint:** `GET /bookings/admin/lot/{lotId}`  
**Access:** ROLE_ADMIN  
**Description:** Get all bookings for a specific parking lot

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=50
&status=ACTIVE
&date=2024-04-08  (optional: filter by specific date)
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 101,
      "bookingReference": "BK-2024-001",
      "spotNumber": "A-45",
      "vehicleNumber": "ABC-1234",
      "userEmail": "driver@parkease.com",
      "checkInTime": "2024-04-08T09:00:00Z",
      "checkOutTime": "2024-04-08T17:30:00Z",
      "status": "ACTIVE",
      "totalCost": 50.92
    }
  ],
  "totalElements": 45,
  "totalPages": 1
}
```

---

## Payment Service Admin Endpoints

**Service URL:** `http://localhost:8004/payments`  
**Gateway URL:** `http://localhost:8080/payments`

### 1. Get All Transactions

**Endpoint:** `GET /payments/admin/transactions`  
**Access:** ROLE_ADMIN  
**Description:** Get all payment transactions

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=50
&status=COMPLETED  (PENDING, COMPLETED, FAILED, REFUNDED)
&startDate=2024-04-01T00:00:00Z
&endDate=2024-04-30T23:59:59Z
&paymentMethod=CREDIT_CARD  (CREDIT_CARD, DEBIT_CARD, UPI, WALLET)
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 501,
      "transactionId": "TXN-2024-5001",
      "bookingId": 101,
      "bookingReference": "BK-2024-001",
      "userId": 50,
      "userEmail": "driver@parkease.com",
      "amount": 50.92,
      "currency": "USD",
      "paymentMethod": "CREDIT_CARD",
      "cardLast4": "4242",
      "status": "COMPLETED",
      "gatewayReference": "stripe_txn_123456",
      "processedAt": "2024-04-08T09:05:00Z",
      "createdAt": "2024-04-08T09:00:00Z"
    }
  ],
  "totalElements": 5000,
  "totalPages": 100,
  "totalRevenue": 45250.00
}
```

---

### 2. Process Refund

**Endpoint:** `POST /payments/admin/refund`  
**Access:** ROLE_ADMIN  
**Description:** Process refund for a completed payment

**Request:**
```json
{
  "transactionId": "TXN-2024-5001",
  "refundAmount": 50.92,
  "reason": "Customer cancellation due to venue closure",
  "notes": "Full refund approved"
}
```

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 502,
  "originalTransactionId": "TXN-2024-5001",
  "refundId": "REF-2024-5001",
  "refundAmount": 50.92,
  "status": "COMPLETED",
  "reason": "Customer cancellation due to venue closure",
  "processedAt": "2024-04-08T11:45:00Z",
  "processedBy": "admin@parkease.com"
}
```

---

### 3. Get Revenue Analytics

**Endpoint:** `GET /payments/admin/revenue`  
**Access:** ROLE_ADMIN  
**Description:** Get revenue analytics and dashboard data

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?period=MONTHLY  (DAILY, WEEKLY, MONTHLY, YEARLY)
&startDate=2024-01-01T00:00:00Z
&endDate=2024-04-30T23:59:59Z
&groupBy=PARKING_LOT  (PARKING_LOT, PAYMENT_METHOD, VEHICLE_TYPE)
```

**Response (200 OK):**
```json
{
  "summaryMetrics": {
    "totalRevenue": 450000.00,
    "totalTransactions": 5000,
    "averageTransactionValue": 90.00,
    "completedTransactions": 4950,
    "failedTransactions": 50,
    "successRate": 99.0
  },
  "revenueByPeriod": [
    {
      "period": "2024-04-01",
      "revenue": 15000.00,
      "transactionCount": 150
    }
  ],
  "revenueByParkingLot": [
    {
      "lotId": 1,
      "lotName": "Downtown Parking Complex",
      "revenue": 45000.00,
      "transactionCount": 500
    }
  ],
  "paymentMethodBreakdown": {
    "CREDIT_CARD": {
      "amount": 250000.00,
      "percentage": 55.6,
      "count": 2500
    },
    "DEBIT_CARD": {
      "amount": 150000.00,
      "percentage": 33.3,
      "count": 1500
    }
  }
}
```

---

## Vehicle Service Admin Endpoints

**Service URL:** `http://localhost:8005/vehicles`  
**Gateway URL:** `http://localhost:8080/vehicles`

### 1. Get All Vehicles (Admin View)

**Endpoint:** `GET /vehicles/admin/all`  
**Access:** ROLE_ADMIN  
**Description:** Get all registered vehicles on the platform

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?page=0
&size=50
&status=ACTIVE  (ACTIVE, INACTIVE, SUSPENDED)
&vehicleType=CAR  (CAR, MOTORCYCLE, TRUCK, BUS)
&verificationStatus=VERIFIED  (VERIFIED, PENDING, REJECTED)
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 30,
      "vehicleNumber": "ABC-1234",
      "vehicleType": "CAR",
      "make": "Honda",
      "model": "Civic",
      "color": "Silver",
      "registrationNumber": "REG-2024-001",
      "userId": 50,
      "userEmail": "driver@parkease.com",
      "verificationStatus": "VERIFIED",
      "verifiedAt": "2024-03-15T10:00:00Z",
      "status": "ACTIVE",
      "totalBookings": 45,
      "createdAt": "2024-03-01T08:00:00Z"
    }
  ],
  "totalElements": 3500,
  "totalPages": 70,
  "currentPage": 0
}
```

---

## Spot Service Admin Endpoints

**Service URL:** `http://localhost:8006/spots`  
**Gateway URL:** `http://localhost:8080/spots`

### 1. Delete Parking Spot

**Endpoint:** `DELETE /spots/admin/{spotId}`  
**Access:** ROLE_ADMIN  
**Description:** Remove a parking spot from circulation

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 45,
  "spotNumber": "A-45",
  "parkingLotId": 1,
  "status": "DELETED",
  "deletedAt": "2024-04-08T12:00:00Z",
  "message": "Spot deleted successfully"
}
```

---

## Notification Service Admin Endpoints

**Service URL:** `http://localhost:8007/notifications`  
**Gateway URL:** `http://localhost:8080/notifications`

### 1. Get All Notifications

**Endpoint:** `GET /notifications/admin/all`  
**Access:** ROLE_ADMIN  
**Description:** Get all system notifications

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1001,
      "type": "BOOKING_CONFIRMATION",
      "recipient": "driver@parkease.com",
      "recipientId": 50,
      "subject": "Booking Confirmed",
      "message": "Your parking spot has been confirmed",
      "status": "SENT",
      "sentAt": "2024-04-08T09:30:00Z",
      "createdAt": "2024-04-08T09:25:00Z"
    }
  ],
  "totalElements": 10000,
  "totalPages": 200
}
```

---

### 2. Broadcast Notification

**Endpoint:** `POST /notifications/admin/broadcast`  
**Access:** ROLE_ADMIN  
**Description:** Send broadcast notification to all users

**Request:**
```json
{
  "title": "Platform Maintenance",
  "message": "The system will be under maintenance on April 15, 2024 from 2:00 AM to 4:00 AM UTC",
  "notificationType": "SYSTEM_ALERT",
  "priority": "HIGH",
  "targetAudience": "ALL_USERS",  (ALL_USERS, ACTIVE_USERS, SPECIFIC_USERS)
  "scheduledFor": "2024-04-08T14:00:00Z"
}
```

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 1002,
  "broadcastId": "BC-2024-001",
  "status": "SCHEDULED",
  "recipientCount": 15000,
  "title": "Platform Maintenance",
  "scheduledFor": "2024-04-08T14:00:00Z",
  "createdBy": "admin@parkease.com"
}
```

---

### 3. Delete Notification

**Endpoint:** `DELETE /notifications/admin/{notificationId}`  
**Access:** ROLE_ADMIN  
**Description:** Delete a specific notification record

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "id": 1001,
  "status": "DELETED",
  "deletedAt": "2024-04-08T12:30:00Z"
}
```

---

## Analytics Service Admin Endpoints

**Service URL:** `http://localhost:8008/analytics`  
**Gateway URL:** `http://localhost:8080/analytics`

### 1. Get Platform Analytics Dashboard

**Endpoint:** `GET /analytics/admin/dashboard`  
**Access:** ROLE_ADMIN  
**Description:** Get comprehensive platform analytics

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Query Parameters:**
```
?startDate=2024-01-01T00:00:00Z
&endDate=2024-04-30T23:59:59Z
&metrics=BOOKINGS,REVENUE,USERS,SPOTS
```

**Response (200 OK):**
```json
{
  "dateRange": {
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-04-30T23:59:59Z"
  },
  "keyMetrics": {
    "totalUsers": 8500,
    "activeUsers": 5200,
    "totalBookings": 15000,
    "totalRevenue": 450000.00,
    "totalParkingLots": 25,
    "totalSpots": 5000,
    "occupancyRate": 68.5,
    "averageRating": 4.6
  },
  "occupancyTrend": [
    {
      "date": "2024-04-08",
      "occupancyPercentage": 72.5,
      "totalSpots": 5000,
      "occupiedSpots": 3625
    }
  ],
  "bookingTrend": [
    {
      "date": "2024-04-08",
      "bookings": 450,
      "revenue": 15000.00,
      "averageSessionTime": "4.2 hours"
    }
  ],
  "topParkingLots": [
    {
      "lotId": 1,
      "lotName": "Downtown Parking Complex",
      "bookings": 3000,
      "revenue": 45000.00,
      "occupancyRate": 85.0
    }
  ],
  "topVehicleTypes": {
    "CAR": {
      "count": 10000,
      "percentage": 66.7
    },
    "MOTORCYCLE": {
      "count": 3000,
      "percentage": 20.0
    }
  }
}
```

---

### 2. Get Parking Lot Analytics

**Endpoint:** `GET /analytics/admin/parking-lot/{lotId}`  
**Access:** ROLE_ADMIN  
**Description:** Get detailed analytics for a specific parking lot

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

**Response (200 OK):**
```json
{
  "lotId": 1,
  "lotName": "Downtown Parking Complex",
  "dateRange": "2024-04-01 to 2024-04-08",
  "metrics": {
    "totalSpots": 500,
    "averageOccupancy": 78.5,
    "peakOccupancy": 95.0,
    "lowestOccupancy": 45.0,
    "totalBookings": 1200,
    "totalRevenue": 45000.00,
    "averageSessionTime": "4.5 hours",
    "customerSatisfaction": 4.7
  },
  "hourlyOccupancy": [
    {
      "hour": "09:00",
      "occupancyPercentage": 65.0,
      "occupiedSpots": 325
    }
  ]
}
```

---

## Complete Request/Response Examples

### Example 1: Admin Login Flow

**Step 1: Login Request**
```bash
curl -X POST http://localhost:8080/auth/admin/login \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "admin@parkease.com",
    "password": "SecurePassword123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX1NVUEVSX0FETUluIiwiaXNTdXBlckFkbWluIjp0cnVlLCJleHAiOjE3MTI1NTQ4MDB9.signature",
  "userId": 1,
  "role": "ROLE_SUPER_ADMIN",
  "isSuperAdmin": true,
  "expiresIn": 3600
}
```

**Step 2: Access Protected Endpoint with Token**
```bash
curl -X GET http://localhost:8080/parking-lots/admin/all?page=0&size=20 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept: application/json"
```

---

### Example 2: Create Admin (Super Admin Only)

```bash
curl -X POST http://localhost:8080/auth/admin/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN_WITH_isSuperAdmin_true>" \
  -H "Accept: application/json" \
  -d '{
    "email": "newadmin@parkease.com",
    "password": "NewAdminPassword123!",
    "fullName": "New Admin User"
  }'
```

**Note:** The API automatically sets `isSuperAdmin=false` for newly created admins. Only the initially seeded admin has `isSuperAdmin=true`.

**Response:**
```json
{
  "id": 2,
  "email": "newadmin@parkease.com",
  "username": "newadmin",
  "role": "ROLE_ADMIN",
  "isSuperAdmin": false,
  "createdAt": "2024-04-08T10:30:00Z",
  "status": "ACTIVE"
}
```

---

### Example 3: Process Refund Flow

```bash
curl -X POST http://localhost:8080/payments/admin/refund \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Accept: application/json" \
  -d '{
    "transactionId": "TXN-2024-5001",
    "refundAmount": 50.92,
    "reason": "Customer cancellation",
    "notes": "Full refund approved"
  }'
```

**Timeline:**
1. Admin initiates refund request
2. Payment Service validates transaction
3. Refund processed through payment gateway
4. User notified via email/notification
5. Analytics updated
6. Booking status updated to CANCELLED

---

## Service Communication & Data Models

### Inter-Service Communication Architecture

```
Admin Portal
    ↓
API Gateway (localhost:8080)
    ├── Routes /auth/* → Auth Service
    ├── Routes /parking-lots/* → Parking Lot Service
    ├── Routes /bookings/* → Booking Service
    ├── Routes /payments/* → Payment Service
    ├── Routes /vehicles/* → Vehicle Service
    ├── Routes /spots/* → Spot Service
    ├── Routes /notifications/* → Notification Service
    └── Routes /analytics/* → Analytics Service
    ↓
Service Discovery (Eureka)
    ├── auth-service:8001
    ├── booking-service:8002
    ├── parkinglot-service:8003
    ├── payment-service:8004
    ├── vehicle-service:8005
    ├── spot-service:8006
    ├── notification-service:8007
    └── analytics-service:8008
```

### Database Schema: ADMINS Table

```sql
CREATE TABLE admins (
  admin_id CHAR(36) PRIMARY KEY,         -- UUID
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,   -- BCrypt hashed, NO OAuth2
  is_active BOOLEAN NOT NULL DEFAULT true,
  is_super_admin BOOLEAN NOT NULL DEFAULT false,  -- Soft delete via isActive
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email),
  INDEX idx_is_super_admin (is_super_admin),
  INDEX idx_is_active (is_active)
);
```

**Important Notes:**
- `admin_id` is UUID (not auto-increment)
- NO `role` column - all admins have implicit role "ADMIN"
- `is_super_admin` is the only authorization differentiator
- `is_active` used for soft-delete (not permanent deletion)
- Only ONE admin can have `is_super_admin=true` (initially seeded)

### Admin Model (Database Entity)

```json
{
  "adminId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "Super Admin",
  "email": "admin@parkease.com",
  "isSuperAdmin": true,
  "isActive": true,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Note:** There is NO role field on Admin entity. Role is implicitly "ADMIN" for all admins.

### JWT Token Claims

**Admin JWT Payload:**
```json
{
  "sub": "admin@parkease.com",
  "role": "ADMIN",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "admin@parkease.com",
  "isSuperAdmin": true,
  "iat": 1712551200,
  "exp": 1712637600
}
```

**User JWT Payload (for comparison):**
```json
{
  "sub": "user@example.com",
  "role": "DRIVER" or "MANAGER",
  "userId": "user_uuid",
  "email": "user@example.com",
  "iat": 1712551200,
  "exp": 1712637600
}
```

**Key Differences:**
- Admin role is always `"ADMIN"` (users have "DRIVER" or "MANAGER")
- Admin JWT includes `isSuperAdmin` claim (users don't have this)
- Token expiry: 24 hours (86400000 ms)

### Common Response Wrapper

All API responses follow this structure:

**Success Response:**
```json
{
  "status": 200,
  "message": "Request processed successfully",
  "data": { /* actual response data */ },
  "timestamp": "2024-04-08T10:30:00Z"
}
```

**Error Response:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "errors": [
    {
      "field": "email",
      "message": "Email format is invalid"
    }
  ],
  "timestamp": "2024-04-08T10:30:00Z",
  "path": "/auth/admin/login"
}
```

---

## Frontend Development Guide

### Frontend Architecture Overview

```
Admin Dashboard
├── Authentication Module
│   ├── Login Page
│   ├── Token Management
│   └── Session Handling
├── Admin Management Module
│   ├── Admin List
│   ├── Create Admin
│   └── Delete Admin
├── Parking Lot Management
│   ├── All Lots Dashboard
│   ├── Pending Approvals
│   ├── Lot Details Editor
│   └── Lot Deletion
├── Booking Management
│   ├── All Bookings List
│   ├── Lot-wise Bookings
│   └── Booking Details
├── Payment Management
│   ├── Transaction History
│   ├── Refund Processing
│   └── Revenue Analytics
├── Vehicle Management
│   └── Vehicle Registry
├── Notification Management
│   ├── Notification History
│   ├── Broadcast Notifications
│   └── Notification Settings
├── Analytics Dashboard
│   ├── Platform Metrics
│   ├── Lot Analytics
│   └── Trend Analysis
└── Settings Module
    ├── Profile Management
    └── Security Settings
```

### Key Frontend Requirements

1. **Authentication**
   - Implement JWT token storage (localStorage/sessionStorage)
   - Add token refresh mechanism (before 1-hour expiry)
   - Implement logout and session cleanup
   - Redirect to login on token expiry (401 response)

2. **Authorization**
   - Check `isSuperAdmin` flag for admin creation features
   - Role-based UI rendering (show/hide features based on role)
   - Implement role guard before API calls

3. **API Integration**
   - Use centralized API client with Authorization header
   - Implement request/response interceptors
   - Add error handling for network failures
   - Use proper HTTP methods (GET, POST, PUT, DELETE)

4. **Data Display**
   - Implement pagination for all list endpoints
   - Add filters and sorting capabilities
   - Use loading states for async operations
   - Add success/error toast notifications

5. **Form Validation**
   - Validate on client-side before submission
   - Match backend validation rules
   - Display field-level error messages

### Example: Frontend Token Manager

```javascript
// Token Management Service
class TokenManager {
  static storeToken(token, expiresIn) {
    localStorage.setItem('admin_token', token);
    localStorage.setItem('token_expiry', Date.now() + (expiresIn * 1000));
  }

  static getToken() {
    const token = localStorage.getItem('admin_token');
    const expiry = localStorage.getItem('token_expiry');
    
    if (!token || Date.now() > expiry) {
      this.clearToken();
      return null;
    }
    return token;
  }

  static clearToken() {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('token_expiry');
    localStorage.removeItem('admin_user');
  }

  static isTokenValid() {
    return this.getToken() !== null;
  }
}
```

### Example: API Client with Authorization

```javascript
// API Client
class AdminAPIClient {
  static async call(endpoint, options = {}) {
    const token = TokenManager.getToken();
    
    if (!token) {
      throw new Error('No authentication token');
    }

    const config = {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json',
        ...options.headers
      },
      ...options
    };

    try {
      const response = await fetch(`http://localhost:8080${endpoint}`, config);
      
      if (response.status === 401) {
        TokenManager.clearToken();
        window.location.href = '/login';
        throw new Error('Session expired');
      }

      return await response.json();
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  }

  static get(endpoint, options = {}) {
    return this.call(endpoint, { method: 'GET', ...options });
  }

  static post(endpoint, data, options = {}) {
    return this.call(endpoint, { 
      method: 'POST', 
      body: JSON.stringify(data),
      ...options 
    });
  }

  static put(endpoint, data, options = {}) {
    return this.call(endpoint, { 
      method: 'PUT', 
      body: JSON.stringify(data),
      ...options 
    });
  }

  static delete(endpoint, options = {}) {
    return this.call(endpoint, { method: 'DELETE', ...options });
  }
}
```

### Example: Login Component Flow

```
1. User enters email and password
2. Frontend validates input
3. Calls POST /auth/admin/login
4. Receives JWT token and user info
5. Stores token via TokenManager
6. Redirects to admin dashboard
7. All subsequent requests include token in Authorization header
```

### Error Handling Checklist

| Status | Handle | Action |
|--------|--------|--------|
| 200-299 | Success | Process response, show success message |
| 400 | Bad Request | Show field validation errors |
| 401 | Unauthorized | Clear token, redirect to login |
| 403 | Forbidden | Show "Access Denied" message |
| 404 | Not Found | Show "Resource not found" message |
| 409 | Conflict | Show "Resource already exists" message |
| 500 | Server Error | Show generic error, enable retry |

---

## Summary: Admin System Capabilities

### Admin Access Levels

**Super Admin Access (isSuperAdmin=true):**
- Create new admin accounts (new admins get isSuperAdmin=false)
- Delete admin accounts (soft-delete via isActive flag)
- View all admins
- All platform admin operations

**Regular Admin Access (isSuperAdmin=false):**
- Cannot create or delete other admins
- All other admin operations:
  - Login to admin portal
  - View all parking lots
  - Approve pending parking lots
  - Update parking lot details
  - Delete parking lots
  - View all bookings (platform-wide)
  - View all vehicles
  - Process refunds
  - View revenue analytics
  - Get platform analytics
  - Send broadcast notifications
  - View all notifications
  - Delete notification records

**Access Control Rule:**
- Route-level: All endpoints check `hasRole("ADMIN")` in Spring Security
- Service-level: Admin creation/deletion/list endpoints check `isSuperAdmin==true` in code

### Security Features

✓ JWT-based authentication (1-hour expiry)  ✓ BCrypt password hashing
✓ Role-based access control (RBAC)  
✓ Dual authorization (Route + Method level)  
✓ Stateless microservices architecture  
✓ Service-to-service JWT validation  
✓ Eureka service discovery  
✓ API Gateway rate limiting (recommended)  
✓ HTTPS support required in production  

### Data Protection

✓ Admin credentials isolated in separate ADMINS table
✓ User credentials in USERS table (BCrypt hashed)
✓ JWT tokens do not contain sensitive data
✓ Audit logging for sensitive operations (recommended)
✓ Transaction history retained for compliance

---

## Quick Reference: Gateway Routes

```
Base URL: http://localhost:8080

Auth Service - Admin Management:
  POST   /auth/admin/login          - Public
  POST   /auth/admin/create         - SUPER_ADMIN
  DELETE /auth/admin/{id}           - SUPER_ADMIN
  GET    /auth/admin/all            - SUPER_ADMIN

Auth Service - User Management:
  GET    /auth/users                - ADMIN
  PUT    /auth/users/{userId}/deactivate - ADMIN
  PUT    /auth/users/{userId}/reactivate - ADMIN

Parking Lots:
  GET    /parking-lots/admin/all    - ADMIN
  GET    /parking-lots/admin/pending - ADMIN
  PUT    /parking-lots/admin/{id}/approve - ADMIN
  PUT    /parking-lots/admin/{id}   - ADMIN
  DELETE /parking-lots/admin/{id}   - ADMIN

Bookings:
  GET    /bookings/admin/all        - ADMIN
  GET    /bookings/admin/lot/{lotId} - ADMIN

Payments:
  GET    /payments/admin/transactions - ADMIN
  POST   /payments/admin/refund     - ADMIN
  GET    /payments/admin/revenue    - ADMIN

Vehicles:
  GET    /vehicles/admin/all        - ADMIN

Spots:
  DELETE /spots/admin/{spotId}      - ADMIN

Notifications:
  GET    /notifications/admin/all   - ADMIN
  POST   /notifications/admin/broadcast - ADMIN
  DELETE /notifications/admin/{id}  - ADMIN

Analytics:
  GET    /analytics/admin/dashboard - ADMIN
  GET    /analytics/admin/parking-lot/{id} - ADMIN
```

---

**This documentation provides complete information for building a fully-functional Admin Frontend with proper authentication, authorization, and all necessary API integrations.**

---

## Auth System Architecture Summary

### Role System
- **Roles**: ADMIN, DRIVER, MANAGER (defined in User.Role enum)
- **isSuperAdmin**: Boolean flag on Admin entity (NOT a role)
- **JWT Claims**: Admins have `role="ADMIN"` + `isSuperAdmin=true/false`

### Admin Types
1. **Super Admin** (isSuperAdmin=true)
   - Seeded from application.yml configuration
   - Only ONE can exist at any time
   - Can create and delete other admins
   - Default credentials: admin@parkease.com / Admin@ParkEase123

2. **Regular Admin** (isSuperAdmin=false)
   - Created by Super Admin via API
   - Same platform access as Super Admin
   - Cannot manage other admins
   - Cannot be Super Admin

### Database
- **ADMINS table**: Separate from USERS table
- **Password**: BCrypt only (NO OAuth2)
- **Authentication**: `/auth/admin/login` endpoint only
- **Authorization**: Spring Security + service-level isSuperAdmin check

### Token Details
- **Type**: HS256 (symmetric, server-side secret)
- **Expiry**: 24 hours
- **Claims**: role, userId, email, isSuperAdmin, iat, exp
- **Refresh**: No refresh token mechanism (24h expiry)

# ParkEase Auth Service - Complete Documentation

## Project Overview

**Auth Service** is a Spring Boot-based authentication microservice for the ParkEase platform. It provides user registration, login, JWT-based authentication, OAuth2 social login (Google & GitHub), profile management, and account management features.

- **Framework**: Spring Boot 3.5.13
- **Java Version**: 17
- **Database**: PostgreSQL
- **Server Port**: 8081
- **Service Identifier**: `auth-service`
- **Artifact ID**: `auth`
- **Group ID**: `com.parkease`

---

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/parkease/auth/
│   │   │   ├── AuthApplication.java                 (Spring Boot entry point)
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java              (Swagger/OpenAPI configuration)
│   │   │   │   └── SecurityConfig.java             (Spring Security configuration)
│   │   │   ├── dto/                                (Data Transfer Objects)
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── ChangePasswordRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── UpdateProfileRequest.java
│   │   │   │   └── UserProfileResponse.java
│   │   │   ├── entity/
│   │   │   │   └── User.java                       (JPA Entity)
│   │   │   ├── exception/
│   │   │   │   ├── ApiError.java
│   │   │   │   └── GlobalExceptionHandler.java     (Exception handling)
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java             (JPA Repository)
│   │   │   ├── resource/
│   │   │   │   └── AuthResource.java               (REST Controller)
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthFilter.java              (JWT validation filter)
│   │   │   │   ├── JwtUtil.java                    (JWT generation/validation)
│   │   │   │   ├── UserDetailsServiceImpl.java      (Spring Security user service)
│   │   │   │   └── oauth2/
│   │   │   │       ├── GithubOAuth2UserInfo.java   (GitHub attributes mapper)
│   │   │   │       ├── GoogleOAuth2UserInfo.java   (Google attributes mapper)
│   │   │   │       ├── OAuth2SuccessHandler.java   (OAuth2 login success handler)
│   │   │   │       ├── OAuth2UserInfo.java         (OAuth2 attributes base class)
│   │   │   │       ├── OAuth2UserInfoFactory.java  (Factory for OAuth2 providers)
│   │   │   │       ├── OAuth2UserPrincipal.java    (Spring Security principal)
│   │   │   │       └── OAuth2UserServiceImpl.java   (OAuth2 user service)
│   │   │   └── service/
│   │   │       ├── AuthService.java                (Service interface)
│   │   │       └── AuthServiceImpl.java             (Service implementation)
│   │   └── resources/
│   │       ├── application.yaml                    (Configuration file)
│   │       ├── static/                             (Static resources)
│   │       └── templates/                          (HTML templates)
│   └── test/
│       └── java/com/parkease/auth/
│           └── AuthApplicationTests.java
├── pom.xml                                         (Maven configuration)
├── mvnw, mvnw.cmd                                  (Maven wrapper)
└── target/                                         (Build artifacts)
```

---

## Key Dependencies

### Spring Boot Starters
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-security` - Authentication & authorization
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-oauth2-client` - OAuth2 login support
- `spring-boot-starter-actuator` - Health & metrics endpoints

### Database & ORM
- `postgresql` - PostgreSQL driver
- `spring-boot-starter-data-jpa` - Hibernate ORM

### Security & JWT
- `jjwt-api` (0.11.5) - JWT API
- `jjwt-impl` (0.11.5) - JWT implementation
- `jjwt-jackson` (0.11.5) - JWT JSON support

### Utilities
- `lombok` - Code generation (getters, setters, builders)
- `springdoc-openapi-starter-webmvc-ui` (2.8.5) - Swagger UI integration

---

## Database Configuration

### Connection Details
```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/parkease_auth
    username: postgres
    password: ****************************
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: true
```

### User Table Schema
- **Table Name**: `users`
- **ID**: UUID (Primary Key, auto-generated)
- **Columns**:
  - `user_id`: UUID (Primary Key)
  - `full_name`: VARCHAR (NOT NULL)
  - `email`: VARCHAR (UNIQUE, NOT NULL)
  - `password_hash`: VARCHAR (nullable for OAuth2 users)
  - `phone`: VARCHAR (nullable)
  - `role`: ENUM (DRIVER, MANAGER, ADMIN) - Default: DRIVER
  - `vehicle_plate`: VARCHAR (nullable)
  - `is_active`: BOOLEAN (Default: true)
  - `created_at`: TIMESTAMP (auto-generated)
  - `profile_pic_url`: VARCHAR (nullable)

---

## User Entity (`User.java`)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;
    
    private String fullName;
    private String email;
    private String passwordHash;      // null for OAuth2-only users
    private String phone;
    
    @Enumerated(EnumType.STRING)
    private Role role;                // DRIVER, MANAGER, ADMIN
    
    private String vehiclePlate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String profilePicUrl;
    
    enum Role { DRIVER, MANAGER, ADMIN }
}
```

---

## JWT Configuration

### Configuration (application.yaml)
```yaml
jwt:
  secret: bXlfc3VwZXJfc2VjcmV0X2tleV9mb3JfcGFya2Vhc2VfajEyMzQ1Njc4OTAxMjM0NTY=
  expiry: 86400000  # 24 hours in milliseconds
```

### JWT Claims
- **Subject**: User email
- **Custom Claims**:
  - `role`: User role (DRIVER, MANAGER, or ADMIN)
  - `userId`: User UUID as string
- **Algorithm**: HS256
- **Expiration**: 24 hours from generation

### JWT Utilities (`JwtUtil.java`)
**Methods**:
- `generateToken(email, role, userId)` - Creates JWT token
- `isTokenValid(token, email)` - Validates token signature and email match
- `isTokenExpired(token)` - Checks token expiration
- `extractEmail(token)` - Extracts email from token
- `extractRole(token)` - Extracts role from token
- `extractUserId(token)` - Extracts userId from token
- `extractExpiration(token)` - Extracts expiration date
- `getJwtExpiry()` - Returns configured expiry time

---

## Authentication Flow

### 1. Registration Flow
**Endpoint**: `POST /api/v1/auth/register`
**Request**:
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "phone": "+1234567890",
  "role": "DRIVER",
  "vehiclePlate": "ABC-1234"
}
```
**Response** (201 Created):
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "fullName": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "role": "DRIVER",
  "vehiclePlate": "ABC-1234",
  "isActive": true,
  "createdAt": "2026-04-03T10:30:00",
  "profilePicUrl": null
}
```
**Validations**:
- Email must be unique
- Email must be valid format
- Password minimum 8 characters
- Full name 2-100 characters

### 2. Login Flow (Email/Password)
**Endpoint**: `POST /api/v1/auth/login`
**Request**:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123"
}
```
**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890",
    "role": "DRIVER",
    "vehiclePlate": "ABC-1234",
    "isActive": true,
    "createdAt": "2026-04-03T10:30:00",
    "profilePicUrl": null
  }
}
```
**Flow**:
1. Authenticate using Spring Security AuthenticationManager
2. Check if account is active
3. Generate JWT token with email, role, and userId
4. Return token and user profile

### 3. OAuth2 Login Flow (Google / GitHub)
**Step 1**: Frontend redirects user to Google/GitHub
```
GET /oauth2/authorization/google
GET /oauth2/authorization/github
```

**Step 2**: OAuth2 provider redirects back with authorization code
```
GET /login/oauth2/code/google?code=...&state=...
GET /login/oauth2/code/github?code=...&state=...
```

**Step 3**: Spring Security handles the authorization code flow
- Calls `OAuth2UserServiceImpl.loadUser()`
- Fetches user info from provider
- Maps attributes using `OAuth2UserInfoFactory`
- Creates or updates user in database

**Step 4**: `OAuth2SuccessHandler` generates JWT and returns JSON response
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": null,
    "role": "DRIVER",
    "vehiclePlate": null,
    "isActive": true,
    "createdAt": "2026-04-03T11:00:00",
    "profilePicUrl": "https://..."
  }
}
```

**OAuth2 Providers Configuration**:
```yaml
spring.security.oauth2.client.registration:
  google:
    client-id: ****************************
    client-secret: ****************************
    scope: [email, profile]
    
  github:
    client-id: YOUR_GITHUB_CLIENT_ID
    client-secret: YOUR_GITHUB_CLIENT_SECRET
    scope: [user:email, read:user]
```

### 4. Token Refresh Flow
**Endpoint**: `POST /api/v1/auth/refresh`
**Request**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
**Response**: New `AuthResponse` with fresh token
**Conditions**:
- Token must not be expired yet
- User must exist and be active

### 5. Logout Flow
**Endpoint**: `POST /api/v1/auth/logout`
**Headers**: `Authorization: Bearer {token}`
**Response**: Success message
**Note**: Currently stateless (JWT tokens are client-side). For production: implement Redis token blacklist.

---

## REST Endpoints

### Authentication Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/api/v1/auth/register` | ❌ | Register new user |
| POST | `/api/v1/auth/login` | ❌ | Login with email/password |
| POST | `/api/v1/auth/refresh` | ❌ | Refresh JWT token |
| POST | `/api/v1/auth/logout` | ✅ | Logout (client discards token) |
| GET | `/oauth2/authorization/google` | ❌ | Redirect to Google login |
| GET | `/oauth2/authorization/github` | ❌ | Redirect to GitHub login |
| GET | `/login/oauth2/code/google` | ❌ | Google OAuth2 callback |
| GET | `/login/oauth2/code/github` | ❌ | GitHub OAuth2 callback |

### User Profile Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| GET | `/api/v1/auth/profile` | ✅ | Get current user profile |
| PUT | `/api/v1/auth/profile` | ✅ | Update user profile |
| PUT | `/api/v1/auth/password` | ✅ | Change password |
| DELETE | `/api/v1/auth/deactivate` | ✅ | Deactivate account (soft delete) |

### Documentation Endpoints

| Endpoint | Description |
|----------|-------------|
| `/v3/api-docs` | OpenAPI specification |
| `/swagger-ui.html` | Swagger UI |

---

## Security Configuration (`SecurityConfig.java`)

### CORS Configuration
**Allowed Origins**:
- `http://localhost:3000` (React CRA)
- `http://localhost:5173` (Vite)

**Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS
**Allowed Headers**: All (`*`)
**Credentials**: Allowed

### Public Endpoints (No Authentication Required)
```
/api/v1/auth/register
/api/v1/auth/login
/api/v1/auth/refresh
/oauth2/**
/login/oauth2/**
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
```

### Session Management
- **Policy**: `SessionCreationPolicy.IF_REQUIRED`
- Allows brief sessions for OAuth2 authorization code flow
- JWT routes remain stateless

### Security Filters
1. **JwtAuthFilter** - Validates JWT in Authorization header before controller execution
2. **CSRF** - Disabled (stateless API)
3. **UsernamePasswordAuthenticationFilter** - Intercepted by custom JwtAuthFilter

### Password Encoding
- **Algorithm**: BCrypt
- **Used for**: User password hashing and validation

---

## Data Transfer Objects (DTOs)

### RegisterRequest
```java
@Data
public class RegisterRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8)
    private String password;
    
    private String phone;
    private User.Role role = DRIVER;
    private String vehiclePlate;
}
```

### LoginRequest
```java
@Data
public class LoginRequest {
    @NotBlank @Email
    private String email;
    
    @NotBlank
    private String password;
}
```

### RefreshTokenRequest
```java
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
```

### UpdateProfileRequest
```java
@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 100)
    private String fullName;
    
    private String phone;
    private String vehiclePlate;
    private String profilePicUrl;
}
```

### ChangePasswordRequest
```java
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank
    @Size(min = 8)
    private String newPassword;
}
```

### AuthResponse
```java
@Data
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserProfileResponse user;
}
```

### UserProfileResponse
```java
@Data
public class UserProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String vehiclePlate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String profilePicUrl;
}
```

---

## Service Layer (`AuthService` & `AuthServiceImpl`)

**Interface**: `AuthService.java`

**Implementation**: `AuthServiceImpl.java`

### Methods

#### 1. `UserProfileResponse register(RegisterRequest request)`
- **Purpose**: Register a new user
- **Validation**: Check email uniqueness
- **Password**: Encoded with BCrypt
- **Default Role**: DRIVER (if not specified)
- **Throws**: RuntimeException if email already exists

#### 2. `AuthResponse login(LoginRequest request)`
- **Purpose**: Authenticate user and issue JWT
- **Auth**: Uses AuthenticationManager with email/password
- **Checks**: Account must be active
- **Returns**: JWT token + user profile
- **Throws**: RuntimeException if user not found or account deactivated

#### 3. `void logout(String token)`
- **Purpose**: Logout user
- **Current**: Stateless (client discards token)
- **Future**: Add token to Redis blacklist for production

#### 4. `boolean validateToken(String token)`
- **Purpose**: Validate JWT token
- **Checks**: 
  - Token signature valid
  - Email matches
  - User exists in database
  - User is active
- **Returns**: true if all checks pass

#### 5. `AuthResponse refreshToken(String token)`
- **Purpose**: Issue new JWT token
- **Validation**: Token must not be expired
- **Returns**: New JWT token + user profile
- **Throws**: RuntimeException if token expired

#### 6. `UserProfileResponse getUserByEmail(String email)`
- **Purpose**: Fetch user by email
- **Returns**: User profile
- **Throws**: RuntimeException if not found

#### 7. `UserProfileResponse getUserById(UUID userId)`
- **Purpose**: Fetch user by ID
- **Returns**: User profile
- **Throws**: RuntimeException if not found

#### 8. `UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request)`
- **Purpose**: Update user profile (partial update)
- **Updatable Fields**: fullName, phone, vehiclePlate, profilePicUrl
- **Returns**: Updated user profile
- **Transactional**: true

#### 9. `void changePassword(UUID userId, ChangePasswordRequest request)`
- **Purpose**: Change user password
- **Validation**: Current password must match
- **Process**: Encode new password with BCrypt
- **Throws**: RuntimeException if current password incorrect
- **Transactional**: true

#### 10. `void deactivateAccount(UUID userId)`
- **Purpose**: Soft delete account
- **Process**: Set `isActive = false`
- **Effect**: User cannot login or access protected resources
- **Transactional**: true

---

## OAuth2 Implementation

### OAuth2UserInfo (Abstract Base Class)
**Purpose**: Normalize OAuth2 provider attributes into common interface

**Abstract Methods** (implemented by subclasses):
- `getId()` - Provider-specific user ID
- `getName()` - User's display name
- `getEmail()` - User's email
- `getImageUrl()` - User's avatar URL

### GoogleOAuth2UserInfo
**Maps**:
- `sub` → ID
- `name` → Name
- `email` → Email
- `picture` → Image URL

**Provider Attributes Returned by Google**:
```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "email": "john@example.com",
  "picture": "https://lh3.googleusercontent.com/..."
}
```

### GithubOAuth2UserInfo
**Maps**:
- `id` → ID
- `name` or `login` → Name (fallback to login if name blank)
- `email` → Email
- `avatar_url` → Image URL

**Provider Attributes Returned by GitHub**:
```json
{
  "id": 1234567,
  "login": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "avatar_url": "https://avatars.githubusercontent.com/..."
}
```

### OAuth2UserInfoFactory
**Purpose**: Factory pattern to instantiate correct OAuth2UserInfo based on provider

**Logic**:
```java
public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map attributes) {
    return switch (registrationId.toLowerCase()) {
        case "google" -> new GoogleOAuth2UserInfo(attributes);
        case "github" -> new GithubOAuth2UserInfo(attributes);
        default -> throw new RuntimeException("OAuth2 provider not supported: " + registrationId);
    };
}
```

### OAuth2UserServiceImpl
**Purpose**: Handle OAuth2 user loading and creation/update in database

**Process**:
1. Delegate to default OAuth2 service to fetch user info from provider
2. Normalize attributes using `OAuth2UserInfoFactory`
3. Validate email is present
4. Find existing user by email or create new:
   - **New User**: Default role = DRIVER, password = null, isActive = true
   - **Existing User**: Update name and profile picture

**Result**: Return `OAuth2UserPrincipal` wrapping user + attributes

### OAuth2UserPrincipal
**Purpose**: Spring Security principal for OAuth2 authenticated users

**Properties**:
- `user` - Our User entity
- `attributes` - Raw OAuth2 provider attributes

**Implements OAuth2User**:
- `getAttributes()` - Provider attributes
- `getAuthorities()` - Returns ROLE_{role} authority
- `getName()` - Returns user email

### OAuth2SuccessHandler
**Purpose**: Handle successful OAuth2 authentication

**Process**:
1. Extract `OAuth2UserPrincipal` from authentication
2. Generate JWT token (same as email/password login)
3. Build `AuthResponse` with JWT + user profile
4. Write JSON response directly (no redirect)

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": { ... }
}
```

---

## JwtAuthFilter

**Purpose**: Extract and validate JWT from every request

**Process**:
1. Check for Authorization header with "Bearer " prefix
2. Extract JWT from header
3. Extract email from JWT
4. Load UserDetails from database
5. Validate token signature and email match
6. Create `UsernamePasswordAuthenticationToken` and set in SecurityContext
7. Allow request to proceed

**Error Handling**: Invalid tokens logged as exceptions, request continues unauthenticated (intercepted by endpoint auth checks)

---

## UserDetailsServiceImpl

**Purpose**: Load user details for Spring Security authentication

**Implementation**:
- Loads User from database by email
- Throws `UsernameNotFoundException` if not found
- Returns Spring Security `UserDetails` with:
  - Username: email
  - Password: password hash
  - Enabled: user's isActive status
  - Authorities: ROLE_{role}

---

## Exception Handling (`GlobalExceptionHandler`)

### MethodArgumentNotValidException
- **Status**: 400 Bad Request
- **Returns**: List of validation errors per field

### BadCredentialsException
- **Status**: 401 Unauthorized
- **Message**: "Invalid email or password"

### RuntimeException
- **Status**: Determined by message content:
  - Contains "not found" → 404 Not Found
  - Contains "already registered" or "incorrect" → 400 Bad Request
  - Contains "deactivated" or "expired" → 401 Unauthorized
  - Default → 500 Internal Server Error

**Response Format**:
```json
{
  "timestamp": "2026-04-03T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered: john@example.com",
  "errors": ["field: message", "field: message"]
}
```

---

## OpenAPI / Swagger Configuration (`OpenApiConfig.java`)

### API Information
- **Title**: ParkEase Auth Service API
- **Version**: v1
- **Description**: Authentication & User Management — Security gateway for the ParkEase platform
- **Contact**: ParkEase (dev@parkease.com)

### Server Configuration
- **Base URL**: http://localhost:8081 (Development)

### Security Scheme
- **Name**: bearerAuth
- **Type**: HTTP Bearer
- **Format**: JWT
- **Description**: "Enter JWT token obtained from POST /api/v1/auth/login"

### Swagger UI Access
- **URL**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/v3/api-docs

---

## UserRepository

**Purpose**: Data access for User entity

**Built-in Methods** (JpaRepository):
- `save(User)` - Create/update user
- `findById(UUID)` - Find by UUID
- `delete(User)` - Delete user
- `findAll()` - Get all users

**Custom Query Methods**:
- `Optional<User> findByEmail(String email)` - Find user by email
- `Optional<User> findByUserId(UUID userId)` - Find by userId (alias for findById)
- `boolean existsByEmail(String email)` - Check email exists
- `List<User> findAllByRole(User.Role role)` - Find all users by role
- `Optional<User> findByVehiclePlate(String vehiclePlate)` - Find by vehicle plate
- `Optional<User> findByPhone(String phone)` - Find by phone number
- `void deleteByUserId(UUID userId)` - Delete by userId

---

## REST Controller (`AuthResource`)

**Base Path**: `/api/v1/auth`

**Base Path Prefix**: All endpoints are under `/api/v1/auth`

### Endpoint Details

#### 1. Registration
```
POST /api/v1/auth/register
Status: 201 Created
Body: RegisterRequest
Response: UserProfileResponse
Public: Yes
```

#### 2. Login
```
POST /api/v1/auth/login
Status: 200 OK
Body: LoginRequest
Response: AuthResponse
Public: Yes
```

#### 3. Logout
```
POST /api/v1/auth/logout
Status: 200 OK
Headers: Authorization: Bearer {token}
Response: "Logged out successfully"
Public: No (Requires token)
```

#### 4. Refresh Token
```
POST /api/v1/auth/refresh
Status: 200 OK
Body: RefreshTokenRequest
Response: AuthResponse
Public: Yes
```

#### 5. Get Profile
```
GET /api/v1/auth/profile
Status: 200 OK
Headers: @AuthenticationPrincipal UserDetails userDetails
Response: UserProfileResponse
Public: No (Requires token)
```

#### 6. Update Profile
```
PUT /api/v1/auth/profile
Status: 200 OK
Headers: Authorization: Bearer {token}
Body: UpdateProfileRequest
Response: UserProfileResponse
Public: No (Requires token)
```

#### 7. Change Password
```
PUT /api/v1/auth/password
Status: 200 OK
Headers: Authorization: Bearer {token}
Body: ChangePasswordRequest
Response: "Password changed successfully"
Public: No (Requires token)
```

#### 8. Deactivate Account
```
DELETE /api/v1/auth/deactivate
Status: 200 OK
Headers: Authorization: Bearer {token}
Response: "Account deactivated successfully"
Public: No (Requires token)
```

---

## How to Build & Run

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ running on localhost:5432
- Database: `parkease_auth`

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Access Points
- **API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **API Docs**: http://localhost:8081/v3/api-docs

---

## Key Technologies & Libraries

| Technology | Purpose | Version |
|------------|---------|---------|
| Spring Boot | Framework | 3.5.13 |
| Spring Security | Authentication | Latest |
| Spring Data JPA | ORM | Latest |
| JWT (JJWT) | Token generation/validation | 0.11.5 |
| PostgreSQL | Database | N/A |
| Lombok | Code generation | Latest |
| Swagger/OpenAPI | API documentation | 2.8.5 |
| BCrypt | Password hashing | Latest |
| OAuth2 | Social login | Latest |

---

## Security Considerations

1. **Password Storage**: Encrypted with BCrypt
2. **JWT Tokens**: HS256 signed, 24-hour expiration
3. **CORS**: Configured for localhost development
4. **OAuth2**: Supports Google & GitHub (extensible)
5. **Account Deactivation**: Soft delete via `isActive` flag
6. **Session Management**: Stateless JWT (no server-side sessions)
7. **Token Blacklist**: Not implemented (recommended for production)

---

## Future Enhancements

1. Implement Redis token blacklist for logout
2. Add refresh token rotation
3. Implement email verification
4. Add two-factor authentication (2FA)
5. Support additional OAuth2 providers
6. Implement role-based access control (RBAC) middleware
7. Add audit logging
8. Implement rate limiting

---

## Error Scenarios & Troubleshooting

### Common Issues

**Issue**: "Email already registered"
- **Status**: 400 Bad Request
- **Cause**: Email exists in database
- **Solution**: Use different email or login with existing account

**Issue**: "User not found"
- **Status**: 404 Not Found
- **Cause**: Email doesn't exist in database
- **Solution**: Register account first

**Issue**: "Invalid email or password"
- **Status**: 401 Unauthorized
- **Cause**: Wrong password or email
- **Solution**: Verify credentials

**Issue**: "Account is deactivated"
- **Status**: 401 Unauthorized
- **Cause**: User has been deactivated
- **Solution**: Contact admin to reactivate

**Issue**: "Token is expired and cannot be refreshed"
- **Status**: 401 Unauthorized
- **Cause**: Token expired
- **Solution**: Login again to get new token

---

## Contact & Support

**Service Name**: auth-service
**Port**: 8081
**Database**: PostgreSQL (parkease_auth)
**Team**: ParkEase Development
**Email**: dev@parkease.com

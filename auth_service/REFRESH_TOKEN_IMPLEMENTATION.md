# Auth Service - Refresh Token Implementation Summary

## Overview
Successfully implemented the POST `/auth/refresh` endpoint to refresh access tokens without requiring user re-login.

## Problem Fixed
**Error:** "No EntityManager with actual transaction available for current thread - cannot reliably process 'remove' call"

**Solution:** 
- Changed from `jakarta.transaction.Transactional` to `org.springframework.transaction.annotation.Transactional`
- Replaced custom delete queries with find-then-delete pattern
- Properly annotated all methods that perform database operations with `@Transactional`

## Files Created/Modified

### 1. New Entity: RefreshToken.java
**Location:** `src/main/java/com/himusharier/auth/model/RefreshToken.java`

**Purpose:** Entity to store refresh tokens in the database

**Fields:**
- `id` (UUID) - Primary key
- `token` (String) - Unique refresh token string
- `userId` (UUID) - Associated user ID
- `expiryDate` (Instant) - Token expiration timestamp
- `createdAt` (Instant) - Creation timestamp

**Key Methods:**
- `isExpired()` - Checks if the token has expired

**Table Name:** `em_refresh_token`

### 2. New Repository: RefreshTokenRepository.java
**Location:** `src/main/java/com/himusharier/auth/repository/RefreshTokenRepository.java`

**Methods:**
- `findByToken(String token)` - Find refresh token by token string
- `findByUserId(UUID userId)` - Find refresh token by user ID

### 3. New Service: RefreshTokenService.java
**Location:** `src/main/java/com/himusharier/auth/service/RefreshTokenService.java`

**Configuration:**
- `app.jwt.refreshExpiration` - Refresh token expiration time (default: 7 days = 604800000ms)

**Key Methods:**
- `createRefreshToken(UUID userId)` - Creates a new refresh token (deletes existing one for the user)
- `findByToken(String token)` - Finds refresh token by token string
- `verifyExpiration(RefreshToken token)` - Verifies token hasn't expired, deletes if expired
- `deleteByUserId(UUID userId)` - Deletes refresh token for a user
- `deleteByToken(String token)` - Deletes specific refresh token

**Transaction Management:**
All methods that modify data are annotated with `@Transactional` to ensure proper database transaction handling.

### 4. New Exception: RefreshTokenException.java
**Location:** `src/main/java/com/himusharier/auth/exception/RefreshTokenException.java`

**Purpose:** Custom exception for refresh token related errors

### 5. New DTO: RefreshTokenRequest.java
**Location:** `src/main/java/com/himusharier/auth/dto/request/RefreshTokenRequest.java`

**Fields:**
- `refreshToken` (String, required) - The refresh token to use

### 6. New DTO: TokenRefreshResponse.java
**Location:** `src/main/java/com/himusharier/auth/dto/response/TokenRefreshResponse.java`

**Fields:**
- `accessToken` (String) - New access token
- `refreshToken` (String) - New refresh token
- `tokenType` (String) - Token type (default: "Bearer")

### 7. Updated: JwtTokenProvider.java
**Location:** `src/main/java/com/himusharier/auth/config/JwtTokenProvider.java`

**New Method:**
- `createTokenFromAuth(Auth auth)` - Creates JWT token from Auth entity (used in refresh flow)

### 8. Updated: AuthController.java
**Location:** `src/main/java/com/himusharier/auth/controller/AuthController.java`

**Changes:**
- Added `RefreshTokenService` dependency injection
- Updated `POST /api/v1/login` endpoint to include refresh token in response
- Added new endpoint: `POST /api/v1/auth/refresh`
- Updated `POST /api/v1/auth/logout` to delete refresh tokens

**New Endpoint Details:**
- **URL:** `/api/v1/auth/refresh`
- **Method:** POST
- **Request Body:**
  ```json
  {
    "refreshToken": "your-refresh-token-here"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Token refreshed successfully!",
    "data": {
      "accessToken": "new-access-token",
      "refreshToken": "new-refresh-token",
      "tokenType": "Bearer"
    }
  }
  ```
- **Error Response (403 Forbidden):**
  ```json
  {
    "success": false,
    "message": "Refresh token has expired. Please login again."
  }
  ```

### 9. Updated: JwtAuthService.java
**Location:** `src/main/java/com/himusharier/auth/service/JwtAuthService.java`

**New Method:**
- `getAuthByUserId(UUID userId)` - Retrieves Auth entity by user ID

### 10. Updated: ApplicationExceptionHandler.java
**Location:** `src/main/java/com/himusharier/auth/advice/ApplicationExceptionHandler.java`

**New Handler:**
- `handleRefreshTokenException()` - Handles RefreshTokenException with 403 status

## How It Works

### Login Flow (Updated)
1. User logs in with credentials
2. Server validates credentials
3. Server generates:
   - Access token (short-lived, e.g., 1 hour)
   - Refresh token (long-lived, e.g., 7 days)
4. Both tokens are returned to the client
5. Client stores both tokens

**Login Response:**
```json
{
  "success": true,
  "message": "Authentication successful!",
  "data": {
    "id": "user-uuid",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### Refresh Token Flow
1. Client's access token expires
2. Client sends refresh token to `/api/v1/auth/refresh`
3. Server validates refresh token:
   - Checks if token exists in database
   - Checks if token is not expired
   - Verifies user still exists
4. Server generates new tokens:
   - New access token
   - New refresh token (old one is deleted)
5. Both new tokens are returned to client
6. Client updates stored tokens

### Logout Flow (Updated)
1. Client sends logout request with access token
2. Server:
   - Adds access token to blacklist
   - Deletes refresh token from database
   - Clears security context
3. Both tokens are now invalidated

## Configuration

Add to `application.properties`:
```properties
# Access token expiration (1 hour = 3600000ms)
app.jwt.expiration=3600000

# Refresh token expiration (7 days = 604800000ms)
app.jwt.refreshExpiration=604800000
```

## Testing the Endpoint

### Using cURL:

**1. Login:**
```bash
curl -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**2. Refresh Token:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN_HERE"
  }'
```

### Using Postman:

**Refresh Token Request:**
1. Method: POST
2. URL: `http://localhost:8080/api/v1/auth/refresh`
3. Headers:
   - `Content-Type`: `application/json`
4. Body (raw JSON):
   ```json
   {
     "refreshToken": "your-refresh-token-from-login"
   }
   ```

### Expected Flow:
1. Login to get access token and refresh token
2. Use access token to access protected resources
3. When access token expires, use refresh token to get new tokens
4. Continue using new access token
5. Logout when done (invalidates both tokens)

## Security Features

### Token Rotation
- Each refresh generates a new refresh token
- Old refresh token is automatically deleted
- Prevents refresh token reuse attacks

### One Token Per User
- Only one refresh token per user at a time
- New login invalidates previous refresh token
- Prevents token proliferation

### Automatic Cleanup
- Expired refresh tokens are deleted when accessed
- Prevents database bloat

### Blacklist Integration
- Logout invalidates both access and refresh tokens
- Prevents token reuse after logout

## Database Schema

The `em_refresh_token` table will be automatically created with the following structure:

```sql
CREATE TABLE em_refresh_token (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_refresh_token_token ON em_refresh_token(token);
CREATE INDEX idx_refresh_token_user_id ON em_refresh_token(user_id);
```

## Production Considerations

### Current Implementation Features:
✅ Token rotation on each refresh
✅ Automatic expiration checking
✅ One token per user policy
✅ Database-backed storage
✅ Proper transaction management
✅ Integration with logout

### Recommended Enhancements for Production:

1. **Add Token Revocation Endpoint:**
```java
@PostMapping("/auth/revoke")
public ResponseEntity<?> revokeRefreshToken(@RequestBody RefreshTokenRequest request) {
    refreshTokenService.deleteByToken(request.refreshToken());
    return ResponseEntity.ok(new ApiResponse<>(true, "Token revoked successfully!"));
}
```

2. **Add Scheduled Cleanup:**
```java
@Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
public void removeExpiredTokens() {
    List<RefreshToken> expiredTokens = refreshTokenRepository
        .findAllByExpiryDateBefore(Instant.now());
    refreshTokenRepository.deleteAll(expiredTokens);
}
```

3. **Add Device Tracking:**
- Store device information with refresh token
- Allow multiple refresh tokens per user (one per device)
- Enable users to view and revoke tokens per device

4. **Add Rate Limiting:**
- Limit refresh token requests per user
- Prevent brute force attacks

5. **Add Audit Logging:**
- Log all refresh token operations
- Track suspicious activity

## API Endpoints Summary

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/v1/register` | POST | Register new user | No |
| `/api/v1/login` | POST | Login and get tokens | No |
| `/api/v1/validate-token` | GET | Validate access token | Yes |
| `/api/v1/auth/refresh` | POST | Refresh access token | No (uses refresh token) |
| `/api/v1/auth/logout` | POST | Logout and invalidate tokens | Yes |

## Transaction Management Fix

**Problem:** 
The application was throwing "No EntityManager with actual transaction available for current thread" error.

**Root Cause:**
- Using `jakarta.transaction.Transactional` instead of Spring's transaction annotation
- Custom delete methods in repository without proper transaction context

**Solution:**
1. Changed to `org.springframework.transaction.annotation.Transactional`
2. Replaced custom delete queries (`deleteByUserId`, `deleteByToken`) with find-then-delete pattern:
   ```java
   @Transactional
   public void deleteByUserId(UUID userId) {
       Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(userId);
       refreshToken.ifPresent(refreshTokenRepository::delete);
   }
   ```
3. Added `@Transactional` to all methods that perform database modifications

## Status
✅ Implementation Complete
✅ Transaction management fixed
✅ Refresh token endpoint implemented
✅ Login endpoint updated to return refresh tokens
✅ Logout endpoint updated to delete refresh tokens
✅ Proper exception handling added
✅ Database entity and repository created
✅ All services properly transactional


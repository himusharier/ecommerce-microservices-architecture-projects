# Database-Backed Token Blacklist Implementation

## Problem Solved ✅
**Issue:** After logout, the `/validate-token` endpoint was still accepting the logged-out token because the blacklist was using in-memory storage which:
- Was not persistent across server restarts
- Didn't work in distributed/clustered environments
- Lost data on application restart

**Solution:** Migrated to a **database-backed token blacklist** that persists blacklisted tokens in the database.

---

## Implementation Details

### 1. New Entity: BlacklistedToken.java
**Location:** `src/main/java/com/himusharier/auth/model/BlacklistedToken.java`

**Purpose:** Entity to store blacklisted (logged-out) tokens in the database

**Fields:**
- `id` (UUID) - Primary key
- `token` (String, max 1000 chars) - The blacklisted JWT token
- `userId` (UUID) - User who logged out
- `blacklistedAt` (Instant) - When token was blacklisted
- `expiresAt` (Instant) - When the token naturally expires

**Table Name:** `em_blacklisted_token`

**Automatic Cleanup:** Token includes `isExpired()` method to check if token has expired

### 2. New Repository: BlacklistedTokenRepository.java
**Location:** `src/main/java/com/himusharier/auth/repository/BlacklistedTokenRepository.java`

**Methods:**
- `findByToken(String token)` - Find blacklisted token by token string
- `existsByToken(String token)` - Quick check if token is blacklisted
- `findAllByExpiresAtBefore(Instant expiryDate)` - Find expired tokens for cleanup

### 3. Updated Service: TokenBlacklistService.java
**Location:** `src/main/java/com/himusharier/auth/service/TokenBlacklistService.java`

**Changed from:** In-memory HashSet storage  
**Changed to:** Database-backed storage using BlacklistedTokenRepository

**Key Methods:**
- `blacklistToken(String token, UUID userId)` - Adds token to database with expiration time
- `isTokenBlacklisted(String token)` - Checks database if token is blacklisted
- `cleanupExpiredTokens()` - Removes expired tokens from database

**Important Features:**
- Automatically extracts token expiration from JWT claims
- Stores token with its natural expiration time
- Uses `@Transactional` for proper database transaction management

### 4. Updated Controller: AuthController.java
**Location:** `src/main/java/com/himusharier/auth/controller/AuthController.java`

**Changes:**
- Updated `/logout` endpoint to pass `userId` when blacklisting tokens
- Updated `/validate-token` endpoint to check database blacklist

---

## How It Works Now

### Logout Flow
1. User sends logout request with access token
2. Server extracts user details from token
3. Server saves token to `em_blacklisted_token` table with:
   - Token string
   - User ID
   - Blacklisted timestamp
   - Token expiration time (extracted from JWT)
4. Server deletes refresh token from database
5. Server clears security context
6. Token is now permanently blacklisted in the database

### Token Validation Flow
1. User sends request to `/validate-token` with access token
2. Server validates JWT signature and expiration
3. **Server checks database** if token is blacklisted
4. If blacklisted: Returns 401 Unauthorized with message "Token has been invalidated"
5. If not blacklisted: Returns user information

### Authentication Filter Flow
1. Every authenticated request passes through `JwtAuthenticationFilter`
2. Filter validates JWT token
3. **Filter checks database** if token is blacklisted
4. If blacklisted: Throws authentication exception
5. If valid: Allows request to proceed

---

## Database Schema

The following table will be automatically created by JPA:

```sql
CREATE TABLE em_blacklisted_token (
    id UUID PRIMARY KEY,
    token VARCHAR(1000) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- Recommended indexes for performance
CREATE INDEX idx_blacklisted_token_token ON em_blacklisted_token(token);
CREATE INDEX idx_blacklisted_token_expires_at ON em_blacklisted_token(expires_at);
CREATE INDEX idx_blacklisted_token_user_id ON em_blacklisted_token(user_id);
```

---

## Benefits of Database-Backed Blacklist

✅ **Persistent:** Survives server restarts  
✅ **Scalable:** Works in distributed/clustered environments  
✅ **Auditable:** Can track who logged out and when  
✅ **Efficient:** Uses database indexes for fast lookups  
✅ **Automatic Expiry:** Tokens include expiration timestamp  
✅ **Cleanup Ready:** Can clean up expired tokens periodically

---

## Testing

### Test the Complete Flow:

**1. Register/Login:**
```bash
POST /api/v1/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful!",
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "access_token": "eyJhbGc...",
    "tokenType": "Bearer",
    "refresh_token": "550e8400-e29b-41d4-..."
  }
}
```

**2. Validate Token (should work):**
```bash
GET /api/v1/validate-token
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response:**
```json
{
  "success": true,
  "message": "Authorization successful!",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "userRole": "CUSTOMER",
    "createdAt": "2025-10-22T..."
  }
}
```

**3. Logout:**
```bash
POST /api/v1/logout
Authorization: Bearer YOUR_ACCESS_TOKEN
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful!"
}
```

**4. Try to Validate Same Token (should fail now):**
```bash
GET /api/v1/validate-token
Authorization: Bearer SAME_ACCESS_TOKEN
```

**Response:**
```json
{
  "success": false,
  "message": "Token has been invalidated. Please login again."
}
```

**5. Try to Access Protected Endpoint (should fail):**
```bash
GET /api/v1/protected-resource
Authorization: Bearer SAME_ACCESS_TOKEN
```

**Expected:** 401 Unauthorized - Token is blacklisted

---

## Optional: Scheduled Cleanup Job

To automatically remove expired tokens from the database, add this scheduled task:

```java
package com.himusharier.auth.config;

import com.himusharier.auth.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        tokenBlacklistService.cleanupExpiredTokens();
    }
}
```

**Don't forget to enable scheduling in your main application class:**
```java
@SpringBootApplication
@EnableScheduling  // Add this annotation
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

---

## Migration from In-Memory to Database

### What Changed:

**Before (In-Memory):**
```java
private final Set<String> blacklistedTokens = new HashSet<>();

public void blacklistToken(String token) {
    blacklistedTokens.add(token);
}

public boolean isTokenBlacklisted(String token) {
    return blacklistedTokens.contains(token);
}
```

**After (Database):**
```java
@Transactional
public void blacklistToken(String token, UUID userId) {
    Claims claims = jwtTokenProvider.getClaimsFromToken(token);
    Date expiration = claims.getExpiration();
    
    BlacklistedToken blacklistedToken = BlacklistedToken.builder()
            .token(token)
            .userId(userId)
            .expiresAt(expiration.toInstant())
            .build();
    
    blacklistedTokenRepository.save(blacklistedToken);
}

public boolean isTokenBlacklisted(String token) {
    return blacklistedTokenRepository.existsByToken(token);
}
```

---

## Performance Considerations

### Database Queries:
- **Logout:** 1 INSERT query to blacklist table
- **Validate Token:** 1 SELECT query (with index, very fast)
- **Cleanup:** Batch DELETE of expired tokens

### Optimization Tips:
1. **Add Database Index on token column** (unique constraint already creates one)
2. **Consider token hash:** Store SHA-256 hash of token instead of full token for faster lookups
3. **Cache frequently checked tokens:** Use Redis cache layer for even faster blacklist checks
4. **Regular cleanup:** Schedule cleanup job to keep table size manageable

---

## Production Recommendations

### 1. Add Token Hashing (Optional but Recommended)
```java
private String hashToken(String token) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hash);
}
```

### 2. Add Caching Layer (Redis)
```java
@Cacheable(value = "blacklistedTokens", key = "#token")
public boolean isTokenBlacklisted(String token) {
    return blacklistedTokenRepository.existsByToken(token);
}
```

### 3. Monitor Blacklist Table Size
- Set up alerts if table grows too large
- Ensure cleanup job is running properly
- Consider partitioning table by date

### 4. Add Metrics
```java
@Autowired
private MeterRegistry meterRegistry;

public void blacklistToken(String token, UUID userId) {
    // ... existing code ...
    meterRegistry.counter("auth.tokens.blacklisted").increment();
}
```

---

## Summary

✅ **Token blacklist now uses database storage**  
✅ **Logout properly invalidates tokens permanently**  
✅ **Validate-token endpoint checks database blacklist**  
✅ **JwtAuthenticationFilter checks database blacklist**  
✅ **Tokens include expiration for automatic cleanup**  
✅ **Works in distributed environments**  
✅ **Persists across server restarts**  

The logout feature now works correctly with database persistence. Tokens are permanently blacklisted and the `/validate-token` endpoint properly rejects logged-out tokens! 🎉


# JWT Token Expiration Configuration

## Configuration Summary ✅

Successfully configured JWT token expiration times for both access tokens and refresh tokens.

---

## Token Expiration Settings

### Access Token (Short-lived)
- **Duration:** 15 minutes
- **Milliseconds:** 900000
- **Property:** `app.jwt.expiration=900000`
- **Used for:** API authentication
- **Purpose:** Short expiration for security - if token is compromised, it expires quickly

### Refresh Token (Long-lived)
- **Duration:** 7 days
- **Milliseconds:** 604800000
- **Property:** `app.jwt.refreshExpiration=604800000`
- **Used for:** Getting new access tokens
- **Purpose:** Allows users to stay logged in without re-entering credentials

---

## Files Updated

### 1. application-local.properties ✅
```properties
# JWT Configuration:
# Access token expiration: 15 minutes (900000 milliseconds)
app.jwt.expiration=900000
# Refresh token expiration: 7 days (604800000 milliseconds)
app.jwt.refreshExpiration=604800000
app.jwt.secret=f09dc49c3ade7e1aa7bfa11244850cb0ba18b002c5e5d02f2840e8d13d2967b1
```

### 2. application-remote.properties ✅
```properties
# JWT Configuration:
# Access token expiration: 15 minutes (900000 milliseconds)
app.jwt.expiration=900000
# Refresh token expiration: 7 days (604800000 milliseconds)
app.jwt.refreshExpiration=604800000
app.jwt.secret=f09dc49c3ade7e1aa7bfa11244850cb0ba18b002c5e5d02f2840e8d13d2967b1
```

### 3. RefreshTokenService.java ✅
Already configured to use the property:
```java
@Value("${app.jwt.refreshExpiration:604800000}") // Default: 7 days in milliseconds
private long refreshTokenDurationMs;
```

### 4. JwtTokenProvider.java ✅
Already configured to use the property:
```java
@Value("${app.jwt.expiration}")
private int jwtExpirationMs;
```

---

## How It Works

### Login Flow
1. User logs in with credentials
2. Server generates:
   - **Access Token:** Valid for 15 minutes
   - **Refresh Token:** Valid for 7 days
3. Both tokens returned to client

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful!",
  "data": {
    "id": "user-uuid",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "access_token": "eyJhbGc...", // Expires in 15 minutes
    "tokenType": "Bearer",
    "refresh_token": "550e8400-e29b..." // Expires in 7 days
  }
}
```

### Token Usage Flow

**Timeline:**

| Time | Access Token | Refresh Token | Action |
|------|-------------|---------------|---------|
| 0 min | ✅ Valid | ✅ Valid | User can access APIs |
| 14 min | ✅ Valid | ✅ Valid | User can access APIs |
| 15 min | ❌ Expired | ✅ Valid | Access token expires |
| 16 min | ❌ Expired | ✅ Valid | Use refresh token to get new access token |
| 17 min | ✅ New Valid | ✅ New Valid | New tokens issued, continue using APIs |
| 7 days | ❌ Expired | ❌ Expired | Both expired, user must login again |

### Refresh Flow (After 15 minutes)
1. Client notices access token expired
2. Client sends refresh token to `/api/v1/auth/refresh`
3. Server validates refresh token
4. Server generates new tokens:
   - **New Access Token:** Valid for another 15 minutes
   - **New Refresh Token:** Valid for another 7 days
5. Client updates stored tokens

**Request:**
```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully!",
  "data": {
    "accessToken": "eyJhbGc...", // New token, expires in 15 minutes
    "refreshToken": "660e9500-f39c-...", // New token, expires in 7 days
    "tokenType": "Bearer"
  }
}
```

---

## Token Lifespan Examples

### Scenario 1: Active User
- **00:00** - User logs in → Gets 15-min access token + 7-day refresh token
- **00:15** - Access token expires → Use refresh token
- **00:15** - Get new 15-min access token + new 7-day refresh token
- **00:30** - Access token expires → Use refresh token again
- **00:30** - Get new tokens...
- *User can stay logged in for 7 days by refreshing every 15 minutes*

### Scenario 2: Inactive User
- **Day 1** - User logs in and uses app
- **Day 2-6** - User doesn't use app
- **Day 7** - User opens app → Refresh token still valid → Refresh tokens
- **Day 8** - User doesn't use app → Refresh token expires
- **Day 9** - User opens app → Must login again

### Scenario 3: Security Breach
- **00:00** - User's access token is stolen
- **00:15** - Stolen token expires automatically
- **00:16** - Attacker can't use expired token
- *Short 15-min window limits damage from compromised access token*

---

## Security Benefits

### Short Access Token Expiration (15 minutes)
✅ **Limits exposure window** - If token is compromised, attacker has only 15 minutes  
✅ **Reduces attack surface** - Tokens in logs/cache expire quickly  
✅ **Follows security best practices** - OAuth 2.0 recommends short-lived access tokens  
✅ **Easy to revoke** - Just wait 15 minutes for natural expiration

### Long Refresh Token Expiration (7 days)
✅ **Better user experience** - Users stay logged in without constant re-authentication  
✅ **Token rotation** - New refresh token issued on each refresh  
✅ **Database-backed** - Can be revoked immediately on logout  
✅ **One token per user** - Previous refresh token deleted when new one issued

---

## Client Implementation Guide

### Frontend Token Management

```javascript
class AuthService {
  constructor() {
    this.accessToken = localStorage.getItem('access_token');
    this.refreshToken = localStorage.getItem('refresh_token');
  }

  // Check if access token is expired or will expire soon
  isAccessTokenExpired() {
    if (!this.accessToken) return true;
    
    const payload = JSON.parse(atob(this.accessToken.split('.')[1]));
    const expiry = payload.exp * 1000; // Convert to milliseconds
    const now = Date.now();
    
    // Refresh if token expires in less than 1 minute
    return (expiry - now) < 60000;
  }

  // Refresh access token
  async refreshAccessToken() {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: this.refreshToken })
    });

    if (response.ok) {
      const data = await response.json();
      this.accessToken = data.data.accessToken;
      this.refreshToken = data.data.refreshToken;
      
      localStorage.setItem('access_token', this.accessToken);
      localStorage.setItem('refresh_token', this.refreshToken);
      
      return this.accessToken;
    } else {
      // Refresh token expired or invalid - redirect to login
      this.logout();
      window.location.href = '/login';
    }
  }

  // Make authenticated API request
  async apiCall(url, options = {}) {
    // Check if token needs refresh
    if (this.isAccessTokenExpired()) {
      await this.refreshAccessToken();
    }

    // Make request with current access token
    options.headers = {
      ...options.headers,
      'Authorization': `Bearer ${this.accessToken}`
    };

    const response = await fetch(url, options);

    // If 401, try to refresh and retry once
    if (response.status === 401) {
      await this.refreshAccessToken();
      options.headers['Authorization'] = `Bearer ${this.accessToken}`;
      return fetch(url, options);
    }

    return response;
  }

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }
}
```

---

## Testing

### Test Access Token Expiration (15 minutes)

**1. Login and get tokens:**
```bash
POST /api/v1/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

**2. Use access token immediately (should work):**
```bash
GET /api/v1/validate-token
Authorization: Bearer YOUR_ACCESS_TOKEN
```
✅ Returns user info

**3. Wait 16 minutes, try same access token (should fail):**
```bash
GET /api/v1/validate-token
Authorization: Bearer SAME_ACCESS_TOKEN
```
❌ Returns "Authorization failed" or token expired error

**4. Use refresh token to get new access token:**
```bash
POST /api/v1/auth/refresh
{
  "refreshToken": "YOUR_REFRESH_TOKEN"
}
```
✅ Returns new access token and new refresh token

### Test Refresh Token Expiration (7 days)

**1. Login and save refresh token**

**2. Wait 8 days**

**3. Try to refresh:**
```bash
POST /api/v1/auth/refresh
{
  "refreshToken": "OLD_REFRESH_TOKEN"
}
```
❌ Returns "Refresh token has expired. Please login again."

---

## Monitoring & Metrics

### What to Monitor

1. **Token Refresh Rate**
   - Normal: Users refresh every ~15 minutes while active
   - Abnormal: Excessive refresh requests may indicate token theft

2. **Failed Refresh Attempts**
   - Track expired refresh tokens
   - Alert on unusual patterns

3. **Blacklisted Token Access Attempts**
   - Users trying to use logged-out tokens
   - May indicate compromised credentials

4. **Token Expiration Distribution**
   - Are tokens expiring at expected intervals?
   - Are refresh tokens being used before expiration?

---

## Production Checklist

✅ **Access token expiration:** 15 minutes (900000ms)  
✅ **Refresh token expiration:** 7 days (604800000ms)  
✅ **Token rotation enabled:** New refresh token on each refresh  
✅ **Database blacklist:** Tokens can be revoked on logout  
✅ **Automatic cleanup:** Expired blacklisted tokens can be cleaned  
✅ **Secure secret:** Long, random JWT secret configured  
✅ **Properties documented:** Clear comments in config files  

---

## Summary

The JWT token system is now configured with:

- **Access tokens expire in 15 minutes** - Provides security by limiting exposure window
- **Refresh tokens expire in 7 days** - Provides good UX by keeping users logged in
- **Token rotation** - New refresh token issued on each refresh for security
- **Database persistence** - Tokens can be immediately revoked on logout

This is a secure, production-ready configuration following OAuth 2.0 best practices! 🎉


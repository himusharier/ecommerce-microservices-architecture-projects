# Multi-Device Login Implementation

## Overview
This implementation supports **simultaneous login from multiple devices** for the same user. Each device gets its own refresh token, and users can manage their active sessions.

## Key Features

### 1. **Multi-Device Support**
- Users can log in from multiple devices simultaneously (e.g., phone, laptop, tablet)
- Each device maintains its own session with a unique refresh token
- No interference between devices - logging in on one device doesn't log out others

### 2. **Token Expiration**
- **Access Token (JWT)**: 15 minutes (900000 milliseconds)
- **Refresh Token**: 7 days (604800000 milliseconds)

Configuration in `application-local.properties`:
```properties
app.jwt.expiration=900000
app.jwt.refreshExpiration=604800000
```

### 3. **Device Tracking**
Each refresh token stores:
- `deviceInfo`: Device type (e.g., "Android Mobile", "Windows Desktop")
- `ipAddress`: Client IP address
- `userAgent`: Full user agent string
- `createdAt`: Session creation timestamp

## API Endpoints

### 1. **Login** - POST `/api/v1/login`
Creates a new session for the device.

**Request:**
```json
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
    "id": "user-uuid",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "access_token": "jwt-token",
    "tokenType": "Bearer",
    "refresh_token": "refresh-token-uuid",
    "device_info": "Windows Desktop"
  }
}
```

### 2. **Refresh Token** - POST `/api/v1/refresh`
Refreshes the access token for the current device.

**Request:**
```json
{
  "refreshToken": "refresh-token-uuid"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully!",
  "data": {
    "accessToken": "new-jwt-token",
    "refreshToken": "new-refresh-token-uuid"
  }
}
```

### 3. **Logout (Single Device)** - POST `/api/v1/logout`
Logs out from the current device only.

**Headers:**
```
Authorization: Bearer {access-token}
```

**Request Body (optional):**
```json
{
  "refreshToken": "refresh-token-uuid"
}
```

**Behavior:**
- If `refreshToken` is provided: Logs out from **that specific device only**
- If `refreshToken` is NOT provided: Logs out from **all devices**

**Response:**
```json
{
  "success": true,
  "message": "Logout successful!"
}
```

### 4. **Get Active Devices** - GET `/api/v1/devices`
Lists all active sessions (devices) for the current user.

**Headers:**
```
Authorization: Bearer {access-token}
X-Refresh-Token: {current-refresh-token} (optional, to mark current device)
```

**Response:**
```json
{
  "success": true,
  "message": "Active devices retrieved successfully!",
  "data": [
    {
      "id": "device-uuid-1",
      "deviceInfo": "Windows Desktop",
      "ipAddress": "192.168.1.100",
      "lastUsed": "2025-11-13T10:30:00Z",
      "current": true
    },
    {
      "id": "device-uuid-2",
      "deviceInfo": "Android Mobile",
      "ipAddress": "192.168.1.101",
      "lastUsed": "2025-11-13T09:15:00Z",
      "current": false
    }
  ]
}
```

### 5. **Logout from Specific Device** - DELETE `/api/v1/devices/{deviceId}`
Logs out from a specific device by its ID.

**Headers:**
```
Authorization: Bearer {access-token}
```

**Response:**
```json
{
  "success": true,
  "message": "Device logged out successfully!"
}
```

### 6. **Logout from All Other Devices** - POST `/api/v1/logout-all-except-current`
Logs out from all devices except the current one.

**Headers:**
```
Authorization: Bearer {access-token}
```

**Request:**
```json
{
  "refreshToken": "current-device-refresh-token"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out from all other devices successfully!"
}
```

## Database Schema Changes

### RefreshToken Table (`em_refresh_token`)
```sql
CREATE TABLE em_refresh_token (
    id UUID PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    device_info VARCHAR(500),
    ip_address VARCHAR(100),
    user_agent VARCHAR(1000)
);
```

### BlacklistedToken Table (`em_blacklisted_token`)
```sql
CREATE TABLE em_blacklisted_token (
    id UUID PRIMARY KEY,
    token VARCHAR(1000) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
```

## How It Works

### Login Flow
1. User logs in from a device
2. System extracts device information (device type, IP, user agent)
3. Creates a new refresh token with device info (doesn't delete existing tokens)
4. Returns access token + refresh token to the client
5. User is now logged in on this device without affecting other sessions

### Logout Flow

#### Single Device Logout (with refresh token):
1. User sends logout request with refresh token
2. System blacklists the access token
3. System deletes ONLY the specified refresh token
4. User is logged out from that device only
5. Other devices remain logged in

#### Logout from All Devices (without refresh token):
1. User sends logout request without refresh token
2. System blacklists the access token
3. System deletes ALL refresh tokens for the user
4. User is logged out from all devices

### Token Validation Flow
1. Client sends request with access token
2. System checks if token is valid (not expired, correct signature)
3. System checks if token is blacklisted
4. If blacklisted: Returns 401 Unauthorized
5. If valid: Processes the request

### Refresh Token Flow
1. Client's access token expires (after 15 minutes)
2. Client sends refresh token to get new access token
3. System validates refresh token (not expired, exists in database)
4. System creates new access token
5. System creates new refresh token (rotates the token)
6. System deletes old refresh token
7. Returns new tokens to client

## Security Considerations

1. **Token Rotation**: Refresh tokens are rotated on each refresh to prevent reuse
2. **Blacklisting**: Access tokens are blacklisted on logout to prevent use
3. **Device Tracking**: Each session is tracked with device and IP information
4. **Expiration**: Short-lived access tokens (15 min) with longer refresh tokens (7 days)
5. **Database Storage**: Refresh tokens stored in database, not just in-memory

## Usage Examples

### Frontend Implementation (JavaScript/TypeScript)

```typescript
// Store tokens
localStorage.setItem('access_token', response.data.access_token);
localStorage.setItem('refresh_token', response.data.refresh_token);

// Logout from current device only
async function logoutCurrentDevice() {
  const refreshToken = localStorage.getItem('refresh_token');
  await axios.post('/api/v1/logout', { refreshToken }, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
}

// Logout from all devices
async function logoutAllDevices() {
  await axios.post('/api/v1/logout', null, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
}

// Get active devices
async function getDevices() {
  const response = await axios.get('/api/v1/devices', {
    headers: { 
      'Authorization': `Bearer ${accessToken}`,
      'X-Refresh-Token': refreshToken
    }
  });
  return response.data.data;
}

// Logout from specific device
async function logoutDevice(deviceId) {
  await axios.delete(`/api/v1/devices/${deviceId}`, {
    headers: { 'Authorization': `Bearer ${accessToken}` }
  });
}
```

## Testing Scenarios

### Scenario 1: Multi-Device Login
1. Login from Device A → Gets tokens A
2. Login from Device B → Gets tokens B
3. Both devices work independently
4. Validate token on Device A → ✅ Success
5. Validate token on Device B → ✅ Success

### Scenario 2: Single Device Logout
1. Login from Device A and Device B
2. Logout from Device A (with refresh token A)
3. Validate token on Device A → ❌ Unauthorized (token blacklisted)
4. Validate token on Device B → ✅ Success (still works)

### Scenario 3: Logout from All Devices
1. Login from Device A and Device B
2. Logout from Device A (without refresh token)
3. Validate token on Device A → ❌ Unauthorized
4. Validate token on Device B → ❌ Unauthorized (refresh token deleted)

### Scenario 4: Token Refresh
1. Login from Device A
2. Wait 15 minutes (access token expires)
3. Validate token → ❌ Expired
4. Refresh token → Gets new access + refresh tokens
5. Validate new token → ✅ Success

## Files Modified

1. **RefreshToken.java** - Added device tracking fields
2. **RefreshTokenRepository.java** - Added methods for multi-device support
3. **RefreshTokenService.java** - Removed auto-delete logic, added device parameters
4. **AuthController.java** - Updated all endpoints for multi-device support
5. **DeviceExtractor.java** - New utility to extract device information
6. **LogoutRequest.java** - New DTO for logout requests
7. **DeviceResponse.java** - New DTO for device information
8. **application-local.properties** - Token expiration configuration
9. **pom.xml** - Fixed Java version from 21 to 17

## Summary

This implementation provides a complete multi-device authentication system with:
- ✅ Multiple simultaneous logins per user
- ✅ Device-specific logout
- ✅ Logout from all devices
- ✅ Device management (view and remove)
- ✅ Token blacklisting via database
- ✅ 15-minute access tokens
- ✅ 7-day refresh tokens
- ✅ Device tracking (type, IP, user agent)
- ✅ Secure token rotation

The system is production-ready and follows security best practices for modern authentication systems.


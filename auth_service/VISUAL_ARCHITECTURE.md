# Multi-Device Authentication - Visual Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT DEVICES                              │
├───────────────┬─────────────────┬────────────────┬─────────────────┤
│  Device A     │   Device B      │   Device C     │   Device D      │
│  (Windows)    │   (Android)     │   (iPhone)     │   (Mac)         │
│               │                 │                │                 │
│  Access Token │   Access Token  │   Access Token │   Access Token  │
│  Refresh A    │   Refresh B     │   Refresh C    │   Refresh D     │
└───────┬───────┴────────┬────────┴────────┬───────┴────────┬────────┘
        │                │                 │                │
        └────────────────┴─────────┬───────┴────────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │     Auth Service API        │
                    │     (Port 8092)             │
                    └──────────────┬──────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
┌───────▼────────┐      ┌──────────▼─────────┐      ┌────────▼────────┐
│ JWT Filter     │      │  Auth Controller   │      │  Token Services │
│ - Validate JWT │      │  - Login           │      │  - Create       │
│ - Check        │      │  - Logout          │      │  - Refresh      │
│   Blacklist    │      │  - Refresh         │      │  - Blacklist    │
│ - Authorize    │      │  - Devices         │      │  - Verify       │
└───────┬────────┘      └──────────┬─────────┘      └────────┬────────┘
        │                          │                          │
        └──────────────────────────┼──────────────────────────┘
                                   │
                    ┌──────────────▼──────────────┐
                    │      PostgreSQL DB          │
                    ├─────────────────────────────┤
                    │  em_refresh_token           │
                    │  - Multiple per user        │
                    │  - Device tracking          │
                    ├─────────────────────────────┤
                    │  em_blacklisted_token       │
                    │  - Invalidated access tokens│
                    └─────────────────────────────┘
```

## Token Lifecycle

```
LOGIN
─────
User → Login Request
         ↓
    Extract Device Info
         ↓
    Create Access Token (15 min)
         ↓
    Create Refresh Token (7 days)
         ↓
    Save to em_refresh_token
         ↓
    Return Tokens
         ↓
    User Stores Tokens


VALIDATE
────────
User → API Request + Access Token
         ↓
    JWT Filter Intercepts
         ↓
    Validate Token Signature ──→ Invalid? → 401
         ↓ Valid
    Check Expiration ──→ Expired? → 401
         ↓ Not Expired
    Check Blacklist ──→ Blacklisted? → 401
         ↓ Not Blacklisted
    Allow Request → 200


REFRESH
───────
User → Refresh Request + Refresh Token
         ↓
    Find Token in DB ──→ Not Found? → 403
         ↓ Found
    Check Expiration ──→ Expired? → 403 (Delete Token)
         ↓ Not Expired
    Get User Info
         ↓
    Create New Access Token
         ↓
    Delete Old Refresh Token
         ↓
    Create New Refresh Token
         ↓
    Return New Tokens


LOGOUT (Device-Specific)
─────────────────────────
User → Logout + Refresh Token
         ↓
    Add Access Token to Blacklist
         ↓
    Delete ONLY This Refresh Token
         ↓
    Clear Security Context
         ↓
    Return Success
         ↓
    Other Devices Still Active ✓


LOGOUT (All Devices)
────────────────────
User → Logout (No Refresh Token)
         ↓
    Add Access Token to Blacklist
         ↓
    Delete ALL User's Refresh Tokens
         ↓
    Clear Security Context
         ↓
    Return Success
         ↓
    All Devices Logged Out ✓
```

## Database State Examples

### Scenario: User logged in on 3 devices

**em_refresh_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ device_info      │ ip_address  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ uuid-1   │ token-abc...   │ user-1  │ Windows Desktop  │ 192.168.1.1 │
│ uuid-2   │ token-def...   │ user-1  │ Android Mobile   │ 192.168.1.2 │
│ uuid-3   │ token-ghi...   │ user-1  │ iPhone           │ 192.168.1.3 │
└──────────┴────────────────┴─────────┴──────────────────┴─────────────┘
```

**em_blacklisted_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ blacklisted_at   │ expires_at  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ (empty - no logouts yet)                                              │
└───────────────────────────────────────────────────────────────────────┘
```

### After Device 1 Logs Out:

**em_refresh_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ device_info      │ ip_address  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ uuid-2   │ token-def...   │ user-1  │ Android Mobile   │ 192.168.1.2 │
│ uuid-3   │ token-ghi...   │ user-1  │ iPhone           │ 192.168.1.3 │
└──────────┴────────────────┴─────────┴──────────────────┴─────────────┘
          ↑ Device 1 refresh token DELETED
```

**em_blacklisted_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ blacklisted_at   │ expires_at  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ uuid-b1  │ access-tok-1   │ user-1  │ 2025-11-13 10:00 │ 10:15       │
└──────────┴────────────────┴─────────┴──────────────────┴─────────────┘
          ↑ Device 1 access token BLACKLISTED
```

**Result:**
- Device 1: ❌ Cannot make requests (access token blacklisted)
- Device 2: ✅ Still works
- Device 3: ✅ Still works

### After "Logout All Devices":

**em_refresh_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ device_info      │ ip_address  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ (empty - all refresh tokens deleted)                                  │
└───────────────────────────────────────────────────────────────────────┘
```

**em_blacklisted_token:**
```
┌──────────┬────────────────┬─────────┬──────────────────┬─────────────┐
│ id       │ token          │ user_id │ blacklisted_at   │ expires_at  │
├──────────┼────────────────┼─────────┼──────────────────┼─────────────┤
│ uuid-b1  │ access-tok-1   │ user-1  │ 2025-11-13 10:00 │ 10:15       │
│ uuid-b2  │ access-tok-2   │ user-1  │ 2025-11-13 10:05 │ 10:20       │
│ uuid-b3  │ access-tok-3   │ user-1  │ 2025-11-13 10:10 │ 10:25       │
└──────────┴────────────────┴─────────┴──────────────────┴─────────────┘
```

**Result:**
- Device 1: ❌ Access token blacklisted
- Device 2: ❌ Access token blacklisted
- Device 3: ❌ Access token blacklisted
- All devices: ❌ Cannot refresh (refresh tokens deleted)

## Request Flow Examples

### Example 1: Login from Windows

```
POST /api/v1/login
Headers:
  Content-Type: application/json
  User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
Body:
  {
    "email": "user@example.com",
    "password": "password123"
  }

                    ↓

Response:
  {
    "success": true,
    "message": "Authentication successful!",
    "data": {
      "id": "user-uuid",
      "email": "user@example.com",
      "role": "CUSTOMER",
      "access_token": "eyJhbGc...",
      "tokenType": "Bearer",
      "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
      "device_info": "Windows Desktop"
    }
  }

Database After:
  em_refresh_token:
    + New row with Windows device info
```

### Example 2: Validate Token

```
GET /api/v1/validate-token
Headers:
  Authorization: Bearer eyJhbGc...

                    ↓

JWT Filter:
  1. Extract token from header
  2. Validate signature ✓
  3. Check expiration ✓
  4. Query: SELECT * FROM em_blacklisted_token WHERE token = ?
  5. Result: Not found (not blacklisted) ✓
  6. Allow request

                    ↓

Response:
  {
    "success": true,
    "message": "Authorization successful!",
    "data": { user details }
  }
```

### Example 3: Device-Specific Logout

```
POST /api/v1/logout
Headers:
  Authorization: Bearer eyJhbGc...
Body:
  {
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }

                    ↓

Auth Controller:
  1. Extract JWT from header
  2. Validate token ✓
  3. Get user from token
  4. INSERT INTO em_blacklisted_token (token, user_id, ...)
  5. DELETE FROM em_refresh_token WHERE token = '550e8400...'
  6. Clear security context

                    ↓

Response:
  {
    "success": true,
    "message": "Logout successful!"
  }

Database After:
  em_blacklisted_token:
    + New row with this access token
  em_refresh_token:
    - Deleted ONLY this refresh token (other devices unaffected)
```

### Example 4: View All Devices

```
GET /api/v1/devices
Headers:
  Authorization: Bearer eyJhbGc...
  X-Refresh-Token: 550e8400-e29b-41d4-a716-446655440000

                    ↓

Auth Controller:
  1. Extract JWT from header
  2. Validate and get user ID
  3. Query: SELECT * FROM em_refresh_token WHERE user_id = ?
  4. Map to DeviceResponse objects
  5. Mark current device (matching X-Refresh-Token)

                    ↓

Response:
  {
    "success": true,
    "message": "Active devices retrieved successfully!",
    "data": [
      {
        "id": "uuid-1",
        "deviceInfo": "Windows Desktop",
        "ipAddress": "192.168.1.1",
        "lastUsed": "2025-11-13T10:30:00Z",
        "current": true  ← This device
      },
      {
        "id": "uuid-2",
        "deviceInfo": "Android Mobile",
        "ipAddress": "192.168.1.2",
        "lastUsed": "2025-11-13T09:15:00Z",
        "current": false
      }
    ]
  }
```

## Comparison: Before vs After

### Before (Single Device)
```
User Login (Device A)
    ↓
Create Refresh Token A
    ↓
User Login (Device B)
    ↓
Delete Refresh Token A ← Device A logged out!
Create Refresh Token B
    ↓
Device A: ❌ Can't refresh
Device B: ✅ Works
```

### After (Multi-Device)
```
User Login (Device A)
    ↓
Create Refresh Token A
    ↓
User Login (Device B)
    ↓
Keep Refresh Token A ← Device A stays logged in!
Create Refresh Token B
    ↓
Device A: ✅ Works
Device B: ✅ Works
    ↓
User Logout from Device A (with token)
    ↓
Delete ONLY Refresh Token A
    ↓
Device A: ❌ Logged out
Device B: ✅ Still works
```

## Key Differences

| Feature | Old System | New System |
|---------|------------|------------|
| Multiple Logins | ❌ Only 1 device | ✅ Unlimited devices |
| Device Tracking | ❌ No tracking | ✅ Full device info |
| Logout Options | ❌ All devices only | ✅ Single or all |
| Device Management | ❌ No UI support | ✅ List & remove devices |
| Token Storage | ✅ Database | ✅ Database |
| Blacklisting | ✅ Database | ✅ Database |
| Token Rotation | ✅ Yes | ✅ Yes |

## Security Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Layers                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: JWT Signature Validation                         │
│  ├─ Verify token signed with secret key                    │
│  └─ Reject if signature invalid                            │
│                                                             │
│  Layer 2: Token Expiration Check                           │
│  ├─ Check expiration claim                                 │
│  └─ Reject if expired                                      │
│                                                             │
│  Layer 3: Blacklist Check (Database)                       │
│  ├─ Query em_blacklisted_token                            │
│  └─ Reject if token found                                  │
│                                                             │
│  Layer 4: User Authorization                               │
│  ├─ Extract user ID and role                              │
│  └─ Set authentication context                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
        ↓
   Request Processed
```

This multi-layered approach ensures maximum security while maintaining flexibility for multi-device scenarios.


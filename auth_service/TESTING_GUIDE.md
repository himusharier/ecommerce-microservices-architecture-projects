# Quick Testing Guide - Multi-Device Authentication

## Prerequisites
1. PostgreSQL database running
2. Database `ecommerce_microservices_architecture_project` created
3. Application started: `mvn spring-boot:run`

## Test Scenarios

### Scenario 1: Basic Login and Logout

#### Step 1: Register a User
```bash
POST http://localhost:8092/api/v1/register
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123",
  "userRole": "CUSTOMER"
}
```

#### Step 2: Login from Device 1
```bash
POST http://localhost:8092/api/v1/login
Content-Type: application/json
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)

{
  "email": "test@example.com",
  "password": "password123"
}
```

**Save from response:**
- `access_token` → Save as ACCESS_TOKEN_1
- `refresh_token` → Save as REFRESH_TOKEN_1

#### Step 3: Validate Token (Should Work)
```bash
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_1}
```
**Expected:** ✅ Success (200 OK)

#### Step 4: Logout from Device 1
```bash
POST http://localhost:8092/api/v1/logout
Authorization: Bearer {ACCESS_TOKEN_1}
Content-Type: application/json

{
  "refreshToken": "{REFRESH_TOKEN_1}"
}
```

#### Step 5: Validate Token After Logout (Should Fail)
```bash
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_1}
```
**Expected:** ❌ 401 Unauthorized - "Token has been invalidated"

---

### Scenario 2: Multi-Device Login

#### Step 1: Login from Device 1 (Windows Desktop)
```bash
POST http://localhost:8092/api/v1/login
Content-Type: application/json
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)

{
  "email": "test@example.com",
  "password": "password123"
}
```
**Save:** ACCESS_TOKEN_1, REFRESH_TOKEN_1

#### Step 2: Login from Device 2 (Android Mobile)
```bash
POST http://localhost:8092/api/v1/login
Content-Type: application/json
User-Agent: Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36

{
  "email": "test@example.com",
  "password": "password123"
}
```
**Save:** ACCESS_TOKEN_2, REFRESH_TOKEN_2

#### Step 3: Validate Both Tokens (Both Should Work)
```bash
# Device 1
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_1}

# Device 2
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_2}
```
**Expected:** ✅ Both succeed

#### Step 4: View All Active Devices
```bash
GET http://localhost:8092/api/v1/devices
Authorization: Bearer {ACCESS_TOKEN_1}
X-Refresh-Token: {REFRESH_TOKEN_1}
```
**Expected:** List showing 2 devices (Windows Desktop, Android Mobile)

#### Step 5: Logout from Device 1 Only
```bash
POST http://localhost:8092/api/v1/logout
Authorization: Bearer {ACCESS_TOKEN_1}
Content-Type: application/json

{
  "refreshToken": "{REFRESH_TOKEN_1}"
}
```

#### Step 6: Check Both Devices
```bash
# Device 1 - Should FAIL
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_1}
# Expected: ❌ 401 Unauthorized

# Device 2 - Should STILL WORK
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_2}
# Expected: ✅ 200 OK
```

---

### Scenario 3: Logout from All Devices

#### Step 1: Login from Multiple Devices
Login from Device 1, Device 2, Device 3 (repeat Scenario 2, Step 1-2)

#### Step 2: Logout from All Devices
```bash
POST http://localhost:8092/api/v1/logout
Authorization: Bearer {ACCESS_TOKEN_1}
Content-Type: application/json
```
**Note:** No refresh token in body = logout from all devices

#### Step 3: Verify All Devices Logged Out
```bash
# Try all access tokens
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_1/2/3}
```
**Expected:** ❌ All return 401 Unauthorized

---

### Scenario 4: Token Refresh

#### Step 1: Login
```bash
POST http://localhost:8092/api/v1/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

#### Step 2: Wait 15 Minutes (or modify expiration to 1 minute for testing)
For quick testing, temporarily change in application-local.properties:
```properties
app.jwt.expiration=60000  # 1 minute
```

#### Step 3: Try Validate Token (Should Fail - Expired)
```bash
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN}
```
**Expected:** ❌ 401 Unauthorized

#### Step 4: Refresh Token
```bash
POST http://localhost:8092/api/v1/refresh
Content-Type: application/json

{
  "refreshToken": "{REFRESH_TOKEN}"
}
```
**Expected:** ✅ New access_token and refresh_token

#### Step 5: Use New Token
```bash
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {NEW_ACCESS_TOKEN}
```
**Expected:** ✅ 200 OK

---

### Scenario 5: Device Management

#### Step 1: Login from 3 Devices
Login from different devices and save all tokens

#### Step 2: Get All Devices
```bash
GET http://localhost:8092/api/v1/devices
Authorization: Bearer {ACCESS_TOKEN_1}
X-Refresh-Token: {REFRESH_TOKEN_1}
```
**Expected:** List of 3 devices with IDs

#### Step 3: Logout Specific Device by ID
```bash
DELETE http://localhost:8092/api/v1/devices/{DEVICE_2_ID}
Authorization: Bearer {ACCESS_TOKEN_1}
```

#### Step 4: Verify Device 2 Logged Out
```bash
GET http://localhost:8092/api/v1/validate-token
Authorization: Bearer {ACCESS_TOKEN_2}
```
**Expected:** ❌ 401 (when token expires, can't refresh because refresh token is deleted)

#### Step 5: Logout All Except Current
```bash
POST http://localhost:8092/api/v1/logout-all-except-current
Authorization: Bearer {ACCESS_TOKEN_1}
Content-Type: application/json

{
  "refreshToken": "{REFRESH_TOKEN_1}"
}
```

#### Step 6: Verify
- Device 1: ✅ Still works
- Device 2, 3: ❌ Can't refresh (refresh tokens deleted)

---

## Database Verification

### Check Refresh Tokens
```sql
SELECT * FROM em_refresh_token;
```
**Should show:** Multiple entries per user (multi-device)

### Check Blacklisted Tokens
```sql
SELECT * FROM em_blacklisted_token;
```
**Should show:** Tokens added after logout

---

## Common Issues & Solutions

### Issue 1: Token Still Valid After Logout
**Cause:** Blacklist not being checked in JwtAuthenticationFilter
**Solution:** Verify `JwtAuthenticationFilter` checks blacklist:
```java
if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
    // Reject request
}
```

### Issue 2: EntityManager Transaction Error
**Cause:** Missing `@Transactional` annotation
**Solution:** All delete methods must have `@Transactional`

### Issue 3: Can't Login from Multiple Devices
**Cause:** Old logic deletes existing tokens
**Solution:** Verify `createRefreshToken()` doesn't delete existing tokens

### Issue 4: Token Expired Immediately
**Cause:** Wrong expiration config (milliseconds vs seconds)
**Solution:** Verify `app.jwt.expiration=900000` (15 minutes in ms)

---

## Postman Collection

Import this as JSON:

```json
{
  "info": {
    "name": "Auth Service - Multi-Device",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Register",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"email\":\"test@example.com\",\"password\":\"password123\",\"userRole\":\"CUSTOMER\"}"
        },
        "url": "http://localhost:8092/api/v1/register"
      }
    },
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\"email\":\"test@example.com\",\"password\":\"password123\"}"
        },
        "url": "http://localhost:8092/api/v1/login"
      }
    },
    {
      "name": "Validate Token",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer {{access_token}}"}],
        "url": "http://localhost:8092/api/v1/validate-token"
      }
    },
    {
      "name": "Logout",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Authorization", "value": "Bearer {{access_token}}"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"refreshToken\":\"{{refresh_token}}\"}"
        },
        "url": "http://localhost:8092/api/v1/logout"
      }
    },
    {
      "name": "Get Devices",
      "request": {
        "method": "GET",
        "header": [
          {"key": "Authorization", "value": "Bearer {{access_token}}"},
          {"key": "X-Refresh-Token", "value": "{{refresh_token}}"}
        ],
        "url": "http://localhost:8092/api/v1/devices"
      }
    }
  ]
}
```

Save tokens as Postman variables for easier testing!


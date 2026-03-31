# Session Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8096  
**Database**: N/A (Redis only)

---

## 📋 Overview

Session Service xử lý **xác thực phiên đăng nhập và JWT token management**. Là cổng đăng nhập chính của game — validate credentials với user-service, phát JWT token, theo dõi trạng thái online của người chơi.

### Core Features
- ✅ Login flow: xác thực user → phát JWT
- ✅ JWT token validation & introspection
- ✅ Rate limiting (chống brute force)
- ✅ User online status tracking (Redis)
- ✅ Time sync endpoint (không cần auth)
- ✅ Heartbeat để duy trì session
- ✅ Logout và thu hồi session

---

## 🎯 Login Flow

```
Client ──► POST /api/session/login {username, password}
                │
                ▼
        session-service
                │
                ├──► user-service (Feign): validate credentials
                │          │
                │    ◄── userId nếu hợp lệ
                │
                ├── Tạo JWT token (userId, roleId, expiry)
                ├── Lưu session vào Redis: key=session:{token}
                │
                ◄── Trả về { token, userId, expiresIn }
```

---

## 🗄️ Storage (Redis)

```
# Session token
session:{jwtToken} → { userId, roleId, createdAt, expiresAt }
TTL: 24 giờ

# Rate limiting
ratelimit:login:{ip} → attemptCount
TTL: 15 phút

# Online status
online:{userId} → { roleId, serverId, lastSeen }
TTL: 120 giây (renew bằng heartbeat)
```

---

## 🔌 API Endpoints

```
POST  /api/session/login           - Đăng nhập, lấy JWT token
POST  /api/session/refresh         - Refresh access token
POST  /api/session/heartbeat       - Renew session (giữ online status)
POST  /api/session/logout          - Đăng xuất, xóa session
GET   /api/session/time            - Server time sync
POST  /internal/session/introspect - Internal: validate JWT (dùng bởi gateway)
```

---

## 📦 API Examples

### Login
```bash
curl -X POST http://localhost:8096/api/session/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player123",
    "password": "securepass",
    "platform": "android",
    "deviceId": "device_abc123"
  }'
# Response: { "token": "eyJ...", "userId": 12345, "expiresIn": 86400 }
```

### Heartbeat (giữ session sống)
```bash
curl -X POST http://localhost:8096/api/session/heartbeat \
  -H "Authorization: Bearer {jwt-token}"
```

### Logout
```bash
curl -X POST http://localhost:8096/api/session/logout \
  -H "Authorization: Bearer {jwt-token}"
```

### Internal: Validate Token (dùng bởi Gateway)
```bash
curl -X POST http://localhost:8096/internal/session/introspect \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJ..."}'
# Response: { "valid": true, "userId": 12345, "roleId": 67890 }
```

---

## 🔧 Business Logic

### Rate Limiting
- Mỗi IP: tối đa **5 lần login thất bại** trong 15 phút
- Vượt quá → trả 429 Too Many Requests
- Dùng Redis sliding window counter

### Token Expiry
- JWT mặc định: **24 giờ**
- Refresh bằng heartbeat (gia hạn thêm 24h)
- Force expire khi logout hoặc ban user

### Online Status
- Heartbeat mỗi 30 giây từ WebSocket Server
- Nếu không có heartbeat trong 120 giây → đánh dấu offline

---

## 🚀 Running

```bash
cd GameServer/session-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
- **user-service** (Feign): Validate username/password

### Được gọi bởi
- **gateway-service**: Validate JWT mỗi request
- **webSocket-server**: Validate token khi WebSocket connect

---

## 📊 Statistics

```
Storage:         Redis only (không có MySQL)
Entities:        N/A
Controllers:     2 (SessionController, InternalSessionController)
Services:        2 (SessionService, RateLimitService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~500 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


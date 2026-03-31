# User Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8110  
**Database**: `game_user`

---

## 📋 Overview

User Service quản lý **tài khoản người dùng** (accounts) — đăng ký, thông tin tài khoản, và xác thực nội bộ cho các services khác. Tách biệt với role-service (quản lý nhân vật) — một user có thể có nhiều roles.

### Core Features
- ✅ Đăng ký tài khoản mới
- ✅ Quản lý thông tin user account
- ✅ Internal auth API cho các services khác
- ✅ Tích hợp với session-service cho login flow

---

## 🎯 Flow Hoạt Động

```
Client ──► POST /api/auth/register ──► user-service ──► Tạo user_db record
                                              │
Client ──► POST /login (session-service) ──► user-service.internal/validate
                                              │
                                       ◄── userId + credentials
```

---

## 🗄️ Database Schema

### user_account
```sql
CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    platform VARCHAR(20),         -- ios, android, pc
    platform_user_id VARCHAR(100),
    Status**: ✅ Production Ready (Updated 2026-03-22)
    created_at DATETIME NOT NULL,
    last_login_at DATETIME
);
```

---

## 🔌 API Endpoints

```
POST  /api/auth/register                - Đăng ký tài khoản
GET   /api/users/{userId}               - Lấy thông tin user
POST  /api/users/register               - Đăng ký (alternative)
GET   /internal/users/{userId}/active   - Kiểm tra user active (internal)
POST  /internal/auth/verify-password    - Xác thực password (internal)
```

---

## 📦 API Examples

### Đăng Ký Tài Khoản
```bash
curl -X POST http://localhost:8110/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player123",
    "password": "securepass",
    "email": "player@game.com",
    "platform": "android"
  }'
```

### Lấy Thông Tin User
```bash
curl http://localhost:8110/api/users/12345
```

### Internal Auth Validation (from session-service)
```bash
curl -X POST http://localhost:8110/internal \
  -H "Content-Type: application/json" \
  -d '{"username": "player123", "password": "securepass"}'
```

---

## 🔧 Business Logic

### Đăng Ký
- Validate username unique
- Hash password (BCrypt)
- Tạo user record
- Trả về userId

### Platform Support
- iOS, Android, PC
- Third-party login (Google, Apple, WeChat)

---

## 🚀 Running

```bash
cd GameServer/user-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **session-service**: Validate credentials khi login

### WebSocket Handler
- Không có WebSocket handler trực tiếp

---

## 📊 Statistics

```
Entities:        1 class (UserAccount)
Repositories:    1 interface
Controllers:     2 (AuthController, UserController)
Services:        1 class
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


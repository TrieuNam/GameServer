# Gateway Service

**Version**: 1.0.0  
**Phase**: P0 (Core Infrastructure)  
**Port**: 8080  
**Database**: N/A (Stateless)

---

## 📋 Overview

Gateway Service là **entry point duy nhất** cho tất cả HTTP/REST requests từ client. Xử lý authentication, routing, CORS, và WebSocket proxy. Tất cả requests từ client (mobile/web) đều đi qua Gateway trước khi đến các business services.

### Core Features
- ✅ API Gateway — single entry point
- ✅ JWT authentication filter
- ✅ Dynamic routing qua Eureka load balancer
- ✅ CORS configuration
- ✅ WebSocket proxy (`/websocket-server/ws/**`)
- ✅ Rate limiting
- ✅ Auth whitelist cho public endpoints

---

## 🎯 Flow Hoạt Động

```
Client (Mobile/Web)
        │
        ▼
[Gateway :8080]
        │
        ├── JWT Filter ──► Validate token → Extract userId/roleId
        │
        ├──► /session-service/**  → session-service:8096
        ├──► /role-service/**     → role-service:8410
        ├──► /bag-service/**      → bag-service:8230
        ├──► /shop-service/**     → shop-service:8260
        ├──► /websocket-server/ws/** → webSocket-server:8094 (WebSocket)
        └──► ... (tất cả ~50 services)
```

---

## 🔌 Routing Rules

| Path Pattern | Target Service |
|---|---|
| `/session-service/**` | session-service:8096 |
| `/user-service/**` | user-service:8110 |
| `/role-service/**` | role-service:8410 |
| `/wallet-service/**` | wallet-service:8210 |
| `/bag-service/**` | bag-service:8230 |
| `/websocket-server/ws/**` | webSocket-server:8094 (WS) |
| `/{service-name}/**` | Tự động route qua Eureka |

---

## 🔒 Auth Whitelist (Không cần JWT)

```
/actuator/**
/session-service/api/session/login
/config-service/api/config/**
/localization-service/api/i18n/**
```

---

## 📦 API Examples

### Login (Public — không cần JWT)
```bash
curl -X POST http://localhost:8080/session-service/api/session/login \
  -H "Content-Type: application/json" \
  -d '{"username": "player123", "password": "pass123"}'
```

### Get Role Info (Cần JWT)
```bash
curl http://localhost:8080/role-service/api/role/{roleId} \
  -H "Authorization: Bearer {jwt-token}"
```

### WebSocket Connection
```
ws://localhost:8080/websocket-server/ws/game?token={jwt-token}
```

---

## ⚙️ Configuration

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true          # Auto-discover services từ Eureka
          lower-case-service-id: true

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400             # 24 giờ
```

---

## 🔧 Business Logic

### JWT Authentication Flow
1. Client gửi request với `Authorization: Bearer {token}`
2. Gateway filter extract và validate JWT
3. Nếu hợp lệ → forward request với userId/roleId headers
4. Nếu không hợp lệ → 401 Unauthorized

### Load Balancing
- Sử dụng Eureka để discover service instances
- Round-robin load balancing giữa các instances
- Circuit breaker (Resilience4j) khi service down

---

## 🚀 Running

```bash
cd GameServer/gateway-service
mvn clean install
mvn spring-boot:run
```

> ⚠️ Khởi động **SAU eureka-server và config-service**

---

## 🔗 Integration Points

### Upstream (nhận requests từ)
- Mobile clients
- Web clients
- Admin tools

### Downstream (forward requests đến)
- Tất cả ~57 business services qua Eureka

---

## 📊 Statistics

```
Filters:         2 (JWT Auth, Logging)
Routes:          Auto-discover (~57 services)
Protocol:        HTTP/1.1, WebSocket
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~300 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


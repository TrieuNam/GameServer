# ServerInfo Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8095  
**Database**: `game_serverinfo`

---

## 📋 Overview

ServerInfo Service quản lý **thông tin server game** — danh sách servers, trạng thái (online/maintenance), thời gian hợp nhất server. Client dùng thông tin này để chọn server trước khi login.

### Core Features
- ✅ CRUD thông tin server
- ✅ Redis cache server info
- ✅ Cung cấp danh sách server cho client khi login
- ✅ Trạng thái server: online, maintenance, full
- ✅ Real start time và combine time tracking

---

## 🎯 Flow Hoạt Động

```
[Client mở game lần đầu]
        │
        ▼
GET /api/server-info
        │
        ▼
serverInfo-service
├── Load từ Redis cache
├── Nếu không có → Query serverinfo_db
└── Trả về danh sách servers: { id, name, status, playerCount, ... }

Client chọn server → login vào đúng server đó
```

---

## 🗄️ Database Schema

### server_info
```sql
CREATE TABLE server_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    server_id INT UNIQUE NOT NULL,
    server_name VARCHAR(100) NOT NULL,
    status INT DEFAULT 1,          -- 1=Online, 2=Maintenance, 3=Full
    host VARCHAR(100),
    port INT,
    player_count INT DEFAULT 0,
    max_players INT DEFAULT 5000,
    region VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
GET   /api/server-info                       - Lấy danh sách tất cả servers
GET   /api/server-info/real-start-time       - Lấy thời gian server thực sự bắt đầu
GET   /api/server-info/combine-time          - Lấy thời gian hợp nhất server
POST  /api/server-info                       - Cập nhật thông tin server (admin)
```

---

## 📦 API Examples

### Lấy Danh Sách Server
```bash
curl http://localhost:8095/api/server-info
# Response:
# [
#   { "serverId": 1, "name": "Server 1", "status": 1, "playerCount": 1250 },
#   { "serverId": 2, "name": "Server 2", "status": 3, "playerCount": 4998 }
# ]
```

### Kiểm Tra Thời Gian Khai Server
```bash
curl http://localhost:8095/api/server-info/real-start-time
```

---

## 🔧 Business Logic

### Server Status
- **1 = Online**: Hoạt động bình thường
- **2 = Maintenance**: Đang bảo trì, không thể login
- **3 = Full**: Đạt max players, có thể queue hoặc chặn login

### Caching
- Server list được cache trong Redis
- TTL: 60 giây
- Invalidate khi có update từ admin

---

## 🚀 Running

```bash
cd GameServer/serverInfo-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **Client**: Màn hình chọn server khi mở game
- **admin-service**: Cập nhật trạng thái server

---

## 📊 Statistics

```
Entities:        1 class (ServerInfo)
Repositories:    1 interface
Controllers:     1 class (ServerInfoController)
Services:        1 class (ServerInfoService)
Cache:           Redis
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~350 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


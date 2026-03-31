# GlobalServer Service

**Version**: 1.0.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8100  
**Database**: `game_globalserver`

---

## 📋 Overview

GlobalServer Service quản lý **dữ liệu global và cross-server** — đồng bộ trạng thái giữa các server, quản lý thông tin server nào player đang ở, và điều phối các sự kiện cross-server.

### Core Features
- ✅ Cross-server data synchronization
- ✅ Global state management
- ✅ Server registry (register/heartbeat)
- ✅ Player online status tracking (cross-server)
- ✅ Player location (player đang ở server nào)
- ✅ Kafka integration

---

## 🎯 Flow Hoạt Động

```
[Server mới khởi động]
service_instance ──► POST /api/global/server/register { serverId, host, port }
                            │
                    globalserver-service
                    └── Ghi vào global server registry

[Player login vào server]
webSocket-server ──► POST /api/global/player/online { roleId, serverId }
                            │
                    Lưu: player → serverId mapping

[Cross-server feature cần biết player ở đâu]
GET /api/global/player/{roleId}/server
└── Trả về serverId player đang kết nối
```

---

## 🗄️ Database Schema

### global_server
```sql
CREATE TABLE global_server (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    server_id INT UNIQUE NOT NULL,
    server_type VARCHAR(50),           -- "game", "battle", "ws"
    host VARCHAR(100),
    port INT,
    status INT DEFAULT 1,
    last_heartbeat DATETIME,
    registered_at DATETIME NOT NULL
);
```

### global_player_location
```sql
CREATE TABLE global_player_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) UNIQUE NOT NULL,
    server_id INT,
    logged_in_at DATETIME,
    logged_out_at DATETIME
);
```

---

## 🔌 API Endpoints

```
GET   /api/global/status                    - Trạng thái global
POST  /api/global/server/register           - Đăng ký server
POST  /api/global/server/{serverId}/heartbeat - Server heartbeat
GET   /api/global/servers                   - Danh sách tất cả servers
POST  /api/global/player/online             - Player online (login)
POST  /api/global/player/offline            - Player offline (logout)
GET   /api/global/player/{roleId}/server    - Server của player
```

---

## 📦 API Examples

### Register Server
```bash
curl -X POST http://localhost:8100/api/global/server/register \
  -H "Content-Type: application/json" \
  -d '{"serverId": 1, "serverType": "game", "host": "10.0.0.1", "port": 8094}'
```

### Track Player Location
```bash
curl -X POST http://localhost:8100/api/global/player/online \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "serverId": 1}'
```

---

## 🚀 Running

```bash
cd GameServer/globalserver-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **webSocket-server**: Track player online/offline status
- **arena-service**: Cross-server arena matching

---

## 📊 Statistics

```
Entities:        2 classes
Repositories:    2 interfaces
Controllers:     1 class (GlobalController)
Services:        1 class (GlobalServerService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


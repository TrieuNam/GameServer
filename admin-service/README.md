# Admin Service

**Version**: 1.0.0  
**Phase**: Special (Admin & Support)  
**Port**: 9091  
**Database**: game_admin

---

## 📋 Overview

Admin Service là **cổng quản trị server game** — dashboard quản lý, theo dõi trạng thái services, quản lý người chơi, và xem thống kê. Chạy trên port 9091 (tách biệt với game services).

### Core Features
- ✅ Dashboard quản lý server
- ✅ Process/service management (start/stop/restart)
- ✅ Port monitoring
- ✅ Player management
- ✅ Thống kê tổng hợp
- ✅ Cấu hình runtime
- ✅ Web UI (Control Panel & Process Manager)

---

## 🎯 Admin Dashboard Flow

```
[Admin mở trình duyệt]
http://localhost:9091/control-panel.html
        │
        ▼
[Dashboard hiển thị]
├── Trạng thái tất cả ~57 services (UP/DOWN)
├── Server metrics (CPU, Memory, Player count)
├── Recent events (boss kills, player joins)
└── Quick actions (restart service, ban player, broadcast)

[Admin quản lý service]
POST /api/services/{serviceName}/restart
├── Gọi lệnh restart service
└── Monitor service status

[Admin ban player]
→ Gọi gm-service /api/gm/user/{userId}/ban
```

---

## 🔌 API Endpoints

```
# Process Management
GET   /api/processes/list         - Danh sách Java processes đang chạy
GET   /api/processes/ports        - Ports đang sử dụng
POST  /api/processes/kill/{pid}   - Kill process
POST  /api/processes/kill-all     - Kill tất cả

# Service Management
GET   /api/services               - Danh sách tất cả services
GET   /api/services/{serviceName} - Thông tin service
GET   /api/services/phase/{phase} - Services theo phase
GET   /api/services/{serviceName}/status   - Status service
POST  /api/services/{serviceName}/start    - Start service
POST  /api/services/{serviceName}/stop     - Stop service
POST  /api/services/{serviceName}/restart  - Restart service
GET   /api/services/{serviceName}/logs     - Xem logs
POST  /api/services/start-all              - Start tất cả
POST  /api/services/stop-all               - Stop tất cả
POST  /api/services/phase/{phase}/start    - Start theo phase
PUT   /api/services/{serviceName}          - Cập nhật config service

# Web UI
GET   /control-panel.html         - Control Panel UI
GET   /process-manager.html       - Process Manager UI
```

---

## 📦 API Examples

### Xem Tất Cả Services
```bash
curl http://localhost:9091/api/services
```

### Restart Một Service
```bash
curl -X POST http://localhost:9091/api/services/role-service/restart \
  -H "Authorization: Bearer {admin-token}"
```

### Start Tất Cả Theo Phase
```bash
curl -X POST http://localhost:9091/api/services/phase/P1/start \
  -H "Authorization: Bearer {admin-token}"
```

### Xem Logs Service
```bash
curl http://localhost:9091/api/services/role-service/logs?lines=100
```

---

## 🔧 Security

- Chạy trên port **9091** — không exposed ra internet, chỉ internal access
- Basic Auth hoặc JWT token cho tất cả API calls
- Rate limiting cho các destructive operations (kill, restart)
- Audit log cho tất cả admin actions

---

## 🚀 Running

```bash
cd GameServer/admin-service
mvn clean install
mvn spring-boot:run
```

> ⚠️ Chỉ truy cập từ internal network / VPN

---

## 📊 Statistics

```
Entities:        1 class (AdminLog)
Repositories:    1 interface
Controllers:     2 (ProcessController, ServiceController)
Services:        2 (ProcessService, ServiceManagementService)
Web UI:          2 HTML pages
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~800 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


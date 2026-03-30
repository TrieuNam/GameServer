# Report Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8098  
**Database**: `game_report`

---

## 📋 Overview

Report Service **ghi nhận và quản lý các sự kiện quan trọng** trong game — boss kill tracking, thông báo hệ thống (notices), và event reporting. Kafka consumer nhận events từ nhiều services để aggregate và display.

### Core Features
- ✅ Lưu sự kiện báo cáo (ReportEvent)
- ✅ Quản lý thông báo/thông tin (Notice/Announcement)
- ✅ Boss kill tracking (BossService)
- ✅ Kafka event consumer
- ✅ Hiển thị kill count theo user

---

## 🎯 Flow Hoạt Động

```
[Player giết Boss]
battleserver-service / world-service
        │
        ▼ Kafka event
report-service (Kafka Consumer)
├── Lưu vào boss_kill_record
├── Cập nhật user kill count
└── Tạo thông báo hệ thống: "Player X đã tiêu diệt Boss Y!"
                    │
                    ▼
            notice_service → broadcast cho tất cả players
```

---

## 🗄️ Database Schema

### report_event
```sql
CREATE TABLE report_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,    -- "boss_kill", "pvp_win", "server_first", v.v.
    role_id VARCHAR(50),
    role_name VARCHAR(50),
    target_id VARCHAR(50),              -- boss_id, item_id, v.v.
    target_name VARCHAR(100),
    extra_data JSON,
    created_at DATETIME NOT NULL
);
```

### notice
```sql
CREATE TABLE notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type INT NOT NULL,              -- 1=System, 2=Event, 3=Maintenance
    title VARCHAR(200),
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    start_at DATETIME,
    end_at DATETIME,
    created_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
# Boss Service
GET   /api/boss/{userId}/kill-count    - Kill count của user

# Notice Service
POST  /api/notice/create               - Tạo thông báo
GET   /api/notice/list                 - Lấy danh sách thông báo

# Report
POST  /api/report                      - Ghi report event (param: data base64)
GET   /api/report                      - Lấy report event (param: data base64)
```

---

## 📦 API Examples

### Lấy Kill Count
```bash
curl http://localhost:8098/api/boss/player123/kill-count
```

### Tạo Thông Báo Hệ Thống
```bash
curl -X POST http://localhost:8098/api/notice/create \
  -H "Content-Type: application/json" \
  -d '{
    "type": 1,
    "title": "Sự Kiện Đặc Biệt",
    "content": "Server khai mở sự kiện đặc biệt trong 7 ngày!",
    "startAt": "2026-03-16T00:00:00",
    "endAt": "2026-03-23T23:59:59"
  }'
```

---

## 🔧 Business Logic

### Boss Kill Events
- Kafka consumer nhận từ `boss-kill-events` topic
- Lưu record vào DB
- Broadcast thông báo toàn server qua notification-service

### Notice Types
- **1 = System**: Thông báo hệ thống (maintenance, update)
- **2 = Event**: Thông báo sự kiện đang diễn ra
- **3 = Achievement**: Thông báo thành tích (server first, boss kill)

---

## 🚀 Running

```bash
cd GameServer/report-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Kafka Consumer
- **boss-kill-events**: Từ world-service, gameworld-service
- **combat-events**: Từ battleserver-service

---

## 📊 Statistics

```
Entities:        2 classes (ReportEvent, Notice)
Repositories:    2 interfaces
Controllers:     3 (BossController, NoticeController, ReportController)
Services:        2 (BossService, ReportService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~500 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


# Notification Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8520  
**Database**: game_notification

---

## 📋 Overview

Notification Service quản lý **hệ thống thông báo push và in-app** — gửi thông báo đến người chơi (level up, event bắt đầu, boss spawn, kết quả arena, v.v.) và quản lý trạng thái đọc/chưa đọc.

### Core Features
- ✅ Push notifications (in-app và external)
- ✅ In-app notification management
- ✅ Trạng thái đọc/chưa đọc
- ✅ Broadcast đến tất cả players
- ✅ Targeted notifications (1 player)
- ✅ Unread count badge

---

## 🎯 Flow Thông Báo

```
[World Boss spawn]
world-service ──► POST /api/notification/send
                          │
                  notification-service
                  ├── Ghi vào game_notification DB
                  ├── Gửi push notification đến devices
                  └── WebSocket push (nếu player online)

[Player mở app — kiểm tra thông báo]
GET /api/notification/player/{playerId}/unread
└── Trả về danh sách chưa đọc với badge count
```

---

## 🗄️ Database Schema

### notification
```sql
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(50),            -- NULL = broadcast to all
    type VARCHAR(50) NOT NULL,        -- "system", "event", "battle", "friend"
    title VARCHAR(200),
    content TEXT NOT NULL,
    extra_data JSON,
    is_read BOOLEAN DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL,
    expires_at DATETIME,
    INDEX idx_player_read (player_id, is_read)
);
```

---

## 🔌 API Endpoints

```
POST  /api/notification/send                      - Gửi thông báo
GET   /api/notification/player/{playerId}         - Tất cả thông báo của player
GET   /api/notification/player/{playerId}/unread  - Thông báo chưa đọc
PUT   /api/notification/{notificationId}/read     - Đánh dấu đã đọc
```

---

## 📦 API Examples

### Gửi Thông Báo Toàn Server
```bash
curl -X POST http://localhost:8520/api/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "type": "event",
    "title": "World Boss Xuất Hiện!",
    "content": "Boss Dragon King đã xuất hiện tại bản đồ Trung Nguyên. Hãy tham chiến ngay!",
    "expiresAt": "2026-03-16T22:00:00"
  }'
```

### Gửi Thông Báo Cá Nhân
```bash
curl -X POST http://localhost:8520/api/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "type": "battle",
    "title": "Bạn bị tấn công!",
    "content": "DragonSlayer đã tấn công lãnh địa của bạn."
  }'
```

### Đánh Dấu Đã Đọc
```bash
curl -X PUT http://localhost:8520/api/notification/789/read
```

---

## 🔧 Business Logic

### Notification Types
| Type | Mô tả |
|------|-------|
| `system` | Thông báo hệ thống |
| `event` | Sự kiện game |
| `battle` | Kết quả chiến đấu |
| `friend` | Kết bạn, tin nhắn |
| `guild` | Thông báo bang hội |
| `mail` | Thư mới |

### Auto-Expiry
- Notifications expire sau 30 ngày
- Scheduled cleanup job xóa notifications cũ

---

## 🚀 Running

```bash
cd GameServer/notification-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **world-service**: Boss spawn/die events
- **arena-service**: Battle results
- **friend-service**: Friend requests
- **guild-service**: Guild events
- **mail-service**: Mail received

---

## 📊 Statistics

```
Entities:        1 class (Notification)
Repositories:    1 interface
Controllers:     1 class (NotificationController)
Services:        1 class (NotificationService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


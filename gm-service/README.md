# GM Service

**Version**: 1.0.0  
**Phase**: Special (Admin & Support)  
**Port**: 9093  
**Database**: game_gm

---

## 📋 Overview

GM Service là **Game Master Tool** — công cụ toàn năng cho Game Masters và admins để quản lý người chơi trong game: tặng/xóa items, thêm/trừ currency, ban/unban users, broadcast thông báo, và xem audit logs.

### Core Features
- ✅ **Item Management**: Tặng/xóa items, xem inventory
- ✅ **Currency Management**: Thêm/trừ gold/diamond/coin, xem wallet
- ✅ **VIP Management**: Cập nhật VIP level
- ✅ **User Management**: Ban/unban (tạm thời hoặc vĩnh viễn), xem thông tin
- ✅ **Broadcast**: Gửi thông báo hệ thống đến tất cả players
- ✅ **Audit Logging**: Ghi log tất cả hành động GM, xem lịch sử đầy đủ

---

## 🎯 Flow GM Actions

```
[GM tặng items cho player]
POST /api/gm/item/give
{
  gmId: "gm001",
  playerId: "player123",
  items: [{ itemId: "item_001", quantity: 99 }],
  reason: "Compensation for server downtime"
}
        │
        ▼
gm-service
├── Validate GM quyền hạn
├──► bag-service: add items
├── Ghi audit log: "gm001 gave item_001 x99 to player123 — reason: ..."
└── Gửi notification cho player

[GM ban user]
POST /api/gm/user/{userId}/ban
{
  banType: "temporary",
  duration: 86400,   // 1 ngày
  reason: "Cheating"
}
        │
        ▼
├──► moderation-service: create ban record
└── Ghi audit log
```

---

## 🗄️ Database Schema

### gm_audit_log
```sql
CREATE TABLE gm_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    gm_id VARCHAR(50) NOT NULL,
    gm_name VARCHAR(50),
    action_type VARCHAR(50) NOT NULL,    -- "give_item", "add_currency", "ban_user"
    target_player_id VARCHAR(50),
    target_user_id VARCHAR(50),
    action_data JSON,                     -- Details của action
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    INDEX idx_gm_id (gm_id),
    INDEX idx_target (target_player_id),
    INDEX idx_created (created_at)
);
```

---

## 🔌 API Endpoints

```
# Item Management
POST  /api/gm/item/give                  - Tặng items cho player
POST  /api/gm/item/remove                - Xóa items khỏi inventory

# Currency Management
POST  /api/gm/currency/add               - Thêm tiền cho player
POST  /api/gm/currency/deduct            - Trừ tiền của player

# User Management
GET   /api/gm/player/{playerId}          - Xem thông tin nhân vật
GET   /api/gm/user/{userId}              - Xem thông tin user
GET   /api/gm/user/{userId}/roles        - Các nhân vật của user
POST  /api/gm/vip/update                 - Cập nhật VIP level
POST  /api/gm/user/{userId}/ban          - Ban user
POST  /api/gm/user/{userId}/unban        - Unban user

# Communication
POST  /api/gm/broadcast                  - Broadcast thông báo

# Audit Logs
GET   /api/gm/logs/gm/{gmId}             - Lịch sử actions của GM
GET   /api/gm/logs/player/{playerId}     - Actions trên player
GET   /api/gm/logs/recent                - Actions gần đây
```

---

## 📦 API Examples

### Tặng Items
```bash
curl -X POST http://localhost:9093/api/gm/item/give \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {gm-token}" \
  -d '{
    "gmId": "gm_admin_001",
    "playerId": "player123",
    "items": [
      {"itemId": "item_hp_potion", "quantity": 100},
      {"itemId": "item_diamond_pack", "quantity": 1}
    ],
    "reason": "Server compensation 2026-03-16"
  }'
```

### Thêm Diamond
```bash
curl -X POST http://localhost:9093/api/gm/currency/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {gm-token}" \
  -d '{
    "gmId": "gm_admin_001",
    "playerId": "player123",
    "currencyType": 2,
    "amount": 500,
    "reason": "Event reward"
  }'
```

### Ban User
```bash
curl -X POST http://localhost:9093/api/gm/user/user789/ban \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {gm-token}" \
  -d '{
    "gmId": "gm_admin_001",
    "banType": "permanent",
    "reason": "Multi-account cheating"
  }'
```

### Broadcast Thông Báo
```bash
curl -X POST http://localhost:9093/api/gm/broadcast \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {gm-token}" \
  -d '{
    "gmId": "gm_admin_001",
    "message": "[System] Server sẽ bảo trì lúc 02:00 ngày mai. Vui lòng offline trước 01:50.",
    "type": "maintenance"
  }'
```

### Xem Audit Log
```bash
curl http://localhost:9093/api/gm/logs/gm/gm_admin_001?limit=50
```

---

## 🔧 Business Logic

### GM Roles & Permissions
- **SuperAdmin**: Tất cả permissions
- **GM**: item, currency, vip, broadcast
- **Moderator**: ban/unban, xem logs

### Audit Logging
- Mọi action đều ghi log với: gmId, timestamp, target, action, reason
- Logs không thể xóa (immutable)
- Retention: 1 năm

---

## 🚀 Running

```bash
cd GameServer/gm-service
mvn clean install
mvn spring-boot:run
```

> ⚠️ Chỉ truy cập từ internal network / VPN. Port 9093 không exposed ra internet.

---

## 🔗 Integration Points

### Gọi (Feign)
- **bag-service**: Grant/remove items
- **wallet-service**: Add/deduct currency
- **moderation-service**: Ban/unban execution
- **notification-service**: Broadcast messages

---

## 📊 Statistics

```
Entities:        1 class (GmAuditLog)
Repositories:    1 interface
Controllers:     1 class (GmController)
Services:        1 class (GmService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


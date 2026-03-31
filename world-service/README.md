# World Service

**Version**: 1.0.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8370  
**Database**: game_world

---

## 📋 Overview

World Service quản lý **sự kiện thế giới và World Boss** — khác với gameworld-service (quản lý scene/monster detail), World Service tập trung vào các **sự kiện lớn toàn server**: World Boss spawning, world events (thời tiết, bonus), và trạng thái thế giới.

### Core Features
- ✅ World Boss management (WorldBoss)
- ✅ World event system (WorldEvent) — bonus EXP, drop rate, v.v.
- ✅ World state tracking
- ✅ Scene management ở cấp độ macro
- ✅ Boss damage tracking (multiple players)

---

## 🎯 Flow World Boss

```
[World Boss Spawn — scheduled]
world-service (internal trigger)
        │
        ▼
WorldBossService.spawnBoss(bossId)
├── Tạo boss instance trong game_world
├──► notification-service: broadcast cho tất cả players
│         "World Boss [Dragon King] đã xuất hiện tại [vị trí]!"
│
[Players tấn công Boss]
POST /api/world/bosses/{bossId}/damage { roleId, damage }
├── Cộng damage vào boss HP
├── Ghi contribution của player
├── Khi boss HP = 0 → boss dead
│
[Boss die]
├── Tính top contributors
├──► mail-service: gửi reward cho contributors
└──► report-service: ghi kill event
```

---

## 🗄️ Database Schema

### world_boss
```sql
CREATE TABLE world_boss (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    boss_id VARCHAR(50) NOT NULL,
    boss_name VARCHAR(100),
    max_hp BIGINT NOT NULL,
    current_hp BIGINT NOT NULL,
    status INT DEFAULT 0,          -- 0=alive, 1=dead
    spawned_at DATETIME NOT NULL,
    killed_at DATETIME,
    scene_id VARCHAR(50)
);
```

### world_boss_contribution
```sql
CREATE TABLE world_boss_contribution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    boss_instance_id BIGINT NOT NULL,
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50),
    damage_dealt BIGINT DEFAULT 0,
    is_rewarded BOOLEAN DEFAULT FALSE
);
```

### world_event
```sql
CREATE TABLE world_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,   -- "exp_bonus", "drop_rate_up", "pvp_event"
    multiplier DECIMAL(3,1) DEFAULT 1.5,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT FALSE
);
```

---

## 🔌 API Endpoints

```
GET   /api/world/state              - Trạng thái thế giới hiện tại
GET   /api/world/time               - Server time
PUT   /api/world/state              - Cập nhật world state
GET   /api/world/events             - Sự kiện đang active
GET   /api/world/events/upcoming    - Sự kiện sắp diễn ra
POST  /api/world/events             - Tạo world event (admin)
PUT   /api/world/events/{eventId}/activate   - Kích hoạt event
PUT   /api/world/events/{eventId}/deactivate - Tắt event
GET   /api/world/bosses             - Danh sách World Boss
GET   /api/world/bosses/recent      - Boss mới bị giết gần đây
POST  /api/world/bosses/spawn       - Spawn World Boss (admin/scheduled)
POST  /api/world/bosses/{bossId}/damage - Ghi damage cho Boss
POST  /api/world/enter              - Player vào world/scene
POST  /api/world/leave              - Player rời world/scene
POST  /api/world/move               - Cập nhật vị trí player trong scene
POST  /api/world/pickup             - Nhặt item trong scene
POST  /api/world/interact           - Tương tác với NPC
GET   /api/world/scene/{sceneId}    - Lấy thông tin scene (playerCount, ...)
```

---

## 📦 API Examples

### Lấy Trạng Thái Thế Giới
```bash
curl http://localhost:8370/api/world/state
# Response: { "activeBossCount": 2, "activeEvents": ["exp_2x"], "worldTime": "..." }
```

### Ghi Damage World Boss
```bash
curl -X POST http://localhost:8370/api/world/bosses/boss_dragon_001/damage \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "damage": 50000}'
```

### Tạo World Event
```bash
curl -X POST http://localhost:8370/api/world/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "exp_bonus",
    "multiplier": 2.0,
    "startAt": "2026-03-16T20:00:00",
    "endAt": "2026-03-16T22:00:00"
  }'
```

---

## 🚀 Running

```bash
cd GameServer/world-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
- **notification-service**: Broadcast boss spawn/event
- **mail-service**: Gửi reward cho boss contributors
- **report-service**: Ghi kill events

---

## 📊 Statistics

```
Entities:        3 classes (WorldBoss, WorldBossContribution, WorldEvent)
Repositories:    3 interfaces
Controllers:     1 class (WorldController)
Services:        2 (WorldBossService, WorldEventService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~700 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


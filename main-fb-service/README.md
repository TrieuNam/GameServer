# Main-FB Service

**Version**: 1.0.0  
**Phase**: P4 (Optional Features)  
**Port**: 8128 · **gRPC**: 9128  
**Database**: game_mainfb

---

## 📋 Overview

Main-FB Service quản lý **hệ thống đánh Boss Chính** (Main Boss Fight) — tính năng cao cấp cho phép player chiến đấu với các boss chính trong game theo chapters và stages. Hỗ trợ sweep (quét nhanh) cho stages đã clear.

### Core Features
- ✅ Boss fight management theo chapter/stage
- ✅ Stamina system (tiêu thụ stamina-item khi vào boss)
- ✅ Progress tracking (chapter, stage)
- ✅ Daily task integration
- ✅ Sweep mode (quét nhanh)
- ✅ Chapter reward claiming
- ✅ gRPC server (port 9128)

---

## 🎯 Flow Đánh Boss

```
[Player vào Boss Fight]
POST /api/mainfb/enter { roleId, stageId }
        │
        ▼
mainfb-service
├── Check stamina: bag-service.consume(stamina-item-id: 50001, 1)
├── Load boss config từ config-service
├──► battleserver-service (gRPC): RunBattle
│          │
│    ◄── Battle result { winner, damage, ... }
│
├── Cập nhật progress (chapter/stage)
├── Nếu stage cleared → unlock stage tiếp theo
└── Grant rewards

[Sweep (quét nhanh)]
POST /api/mainfb/sweep { roleId, stageId, sweepCount }
├── Điều kiện: stage đã clear trước đó với 3 sao
├── Tiêu stamina x sweepCount
└── Tính reward x sweepCount → grant
```

---

## 🗄️ Database Schema

### mainfb_progress
```sql
CREATE TABLE mainfb_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL UNIQUE,
    current_chapter INT DEFAULT 1,
    current_stage INT DEFAULT 1,
    max_chapter INT DEFAULT 1,
    max_stage INT DEFAULT 0,          -- Highest stage cleared
    stars_data JSON,                  -- Stars per stage: { "1_1": 3, "1_2": 2 }
    claimed_chapters JSON,            -- Bit flags for claimed chapter rewards
    updated_at DATETIME NOT NULL
);
```

### mainfb_task
```sql
CREATE TABLE mainfb_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    task_type INT NOT NULL,
    progress INT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    period_key VARCHAR(20),
    UNIQUE KEY uk_role_task_period (role_id, task_type, period_key)
);
```

---

## 🔌 API Endpoints

```
GET   /api/mainfb/progress/{playerId}   - Tiến độ boss fight
GET   /api/mainfb/task/{playerId}       - Daily tasks boss
POST  /api/mainfb/enter                 - Vào boss fight
POST  /api/mainfb/finish                - Kết thúc trận (sau battle)
POST  /api/mainfb/sweep                 - Sweep (quét nhanh)
POST  /api/mainfb/chapter/{stage}/claim - Nhận reward chương
```

---

## 📦 API Examples

### Vào Boss Fight
```bash
curl -X POST http://localhost:8128/api/mainfb/enter \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "stageId": "3_5"}'
```

### Sweep 5 Lần
```bash
curl -X POST http://localhost:8128/api/mainfb/sweep \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "stageId": "2_10", "sweepCount": 5}'
```

---

## 🔧 Business Logic

### Stamina System
- Mỗi lần vào boss tiêu 1 Stamina item (itemId: 50001)
- Stamina tối đa: 5 (cộng thêm theo VIP)
- Hồi 1 stamina mỗi 30 phút, hoặc mua bằng diamond

### Chapter Clear Reward
- Khi clear hết stage trong chapter → unlock chapter reward
- Reward chỉ nhận 1 lần (bit flag tracking)

---

## 🚀 Running

```bash
cd GameServer/main-fb-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### gRPC Server (port 9128)
- webSocket-server gọi trực tiếp

### Phụ thuộc
- **battleserver-service** (gRPC): Run battle simulation
- **bag-service**: Consume stamina, grant rewards
- **config-service**: Boss configs, stage configs

Main-fb-service chuẩn hoá gọi config qua `GET /api/config/file?path=...`.

---

## 📊 Statistics

```
Entities:        2 classes (MainFbProgress, MainFbTask)
Repositories:    2 interfaces
Controllers:     1 class (MainFbController)
Services:        1 class (MainFbService)
gRPC:            MainFbGrpcImpl
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


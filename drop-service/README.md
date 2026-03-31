# Drop Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8250  
**Database**: N/A (Stateless)

---

## 📋 Overview

Drop Service là **hệ thống rơi đồ (loot drops)** — tính toán và trả về danh sách items khi player giết monster/boss. Stateless service: không có DB riêng, đọc toàn bộ drop config từ config-service.

### Core Features
- ✅ Tính toán drop tables theo xác suất từ config
- ✅ Phát rewards theo xác suất (weighted random)
- ✅ Support nhiều loại drop: monster, boss, chest, event
- ✅ Đọc file drop XML từ `config-service` qua `GET /api/config/file?path=...` (ETag-aware)
- ✅ Simulate drop (testing endpoint)
- ✅ Internal API cho gameworld-service, battleserver-service

---

## 🎯 Flow Hoạt Động

```
[Player giết monster]
battleserver-service / gameworld-service
        │
        ▼ REST (internal)
POST /internal/drop/roll { dropTableId, roleId, ... }
        │
        ▼
drop-service
├── Load drop table từ config-service (cached)
├── Roll weighted random cho từng item slot
├── Apply luck bonus (nếu có)
│
◄── Trả về list items { itemId, quantity }
        │
        ▼
battleserver-service ──► bag-service: grant items to player
```

---

## 🔌 API Endpoints

```
POST  /internal/drop/roll       - Roll drop từ drop table (dùng bởi gameworld, battle)
GET   /internal/drop/tables     - Xem toàn bộ drop tables (debug)
POST  /internal/drop/simulate   - Simulate drop nhiều lần (testing)
```

---

## 📦 API Examples

### Roll Drop
```bash
curl -X POST http://localhost:8250/internal/drop/roll \
  -H "Content-Type: application/json" \
  -d '{
    "dropTableId": "monster_001_drop",
    "roleId": "player123",
    "killCount": 1,
    "luckBonus": 10
  }'
# Response: [{ "itemId": "item_sword_001", "quantity": 1 }, ...]
```

### Simulate Drop (1000 lần để test)
```bash
curl -X POST http://localhost:8250/internal/drop/simulate \
  -H "Content-Type: application/json" \
  -d '{"dropTableId": "boss_dragon_drop", "count": 1000}'
```

---

## 🔧 Business Logic

### Drop Table Structure (từ config)
```json
{
  "id": "monster_001_drop",
  "slots": [
    {
      "dropRate": 100,
      "items": [{"itemId": "gold", "minQty": 10, "maxQty": 50}]
    },
    {
      "dropRate": 30,
      "items": [
        {"itemId": "item_hp_potion", "weight": 70, "qty": 1},
        {"itemId": "item_sword_001", "weight": 30, "qty": 1}
      ]
    }
  ]
}
```

### Weighted Random
- `dropRate`: % xác suất slot này có drop (0-100)
- Trong slot: weighted random giữa các items
- Luck bonus: cộng thêm % dropRate

### Config-Service Integration
- Drop table được tải theo path template: `app.config.dropPathTemplate` (mặc định `gameworld/drop/%s.xml`)
- Request gửi `If-None-Match` để tận dụng `304 Not Modified`
- `app.config.knownDropIds` là danh sách id dùng cho endpoint liệt kê/debug (không còn phụ thuộc endpoint `/config/list/*` cũ)

---

## 🚀 Running

```bash
cd GameServer/drop-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
- **config-service**: Load drop tables

### Được gọi bởi
- **gameworld-service**: Khi monster die
- **battleserver-service**: Sau combat
- **box-service**: Khi mở hộp quà

---

## 📊 Statistics

```
Entities:        N/A (Stateless)
Config:          Loaded from config-service
Controllers:     1 class (DropController)
Services:        1 class (DropService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~300 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


# Bag Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8230 · **gRPC**: 9230  
**Database**: `game_bag`

---

## 📋 Overview

Bag Service quản lý **túi đồ (inventory)** của nhân vật — thêm, xóa, sử dụng, bán items. Là service trung tâm cho item flow trong game và phát Kafka events khi inventory thay đổi để WebSocket Server push real-time về client.

> Hiện trạng runtime: dữ liệu bag đang dùng 1 bảng `bag_items` (chưa tách vật lý theo `BAG_NORMAL/BAG_EQUIP/BAG_MATERIALS`).

### Core Features
- ✅ Thêm/xóa/dùng/bán items (public + internal APIs)
- ✅ Event deduplication (BagEventDedup) — tránh double-grant
- ✅ Recycle progress tracking
- ✅ Kafka producer (`BagChangedEvent` → webSocket-server push)
- ✅ gRPC server (port 9230)
- ✅ Internal API cho các services (box-service, equip-service, mail-service, v.v.)
- ✅ Grant items từ nhiều sources (mail, quest, drop, shop, box, decompose, v.v.)

---

## 🎯 Flow Hoạt Động

```
[Player nhận item từ quest]
task-service ──► bag-service (gRPC/Feign): /internal/add
                        │
                        ▼
              BagDomainService.addItems()
              ├── Dedup check (BagEventDedup): đã grant chưa?
              ├── Thêm item vào bag_db
              │
              ▼ Kafka
BagChangedEvent { roleId, items } ──► webSocket-server
                                           │
                                    ◄── Push về client real-time

[Player dùng item]
POST /api/bag/{roleId}/items/use
        │
        ▼
BagDomainService.useItem()
├── Kiểm tra item tồn tại và đủ quantity
├── Xóa/giảm item
├── Apply effect (HP potion → gọi role-service, v.v.)
└── Gửi BagChangedEvent
```

---

## 🗄️ Database Schema

### bag_items
```sql
CREATE TABLE bag_items (
    id         VARCHAR(36) NOT NULL,
    role_id    BIGINT      NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    item_id    INT         NOT NULL,
    num        BIGINT      NOT NULL,
    bind       TINYINT(1)  NOT NULL DEFAULT 0,
    expire_at  TIMESTAMP NULL DEFAULT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_item_bind_exp (role_id, item_id, bind, expire_at)
);
```

### bag_event_dedup
```sql
CREATE TABLE bag_event_dedup (
    event_id   VARCHAR(100) PRIMARY KEY,
    created_at TIMESTAMP(3) NOT NULL
);
```

### recycle_progress
```sql
CREATE TABLE recycle_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL UNIQUE,
    level INT NOT NULL DEFAULT 0,
    exp BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6)
);
```

---

## 🔌 API Endpoints

```
# Public API
GET   /api/bag/{roleId}/items          - Lấy danh sách items
POST  /api/bag/{roleId}/items/use      - Dùng item
POST  /api/bag/{roleId}/items/sell     - Bán item
POST  /api/bag/{roleId}/recycle        - Recycle items

# Internal API (cho services khác)
POST  /api/bag/grant                   - Grant items (dùng bởi task, mail, v.v.)
POST  /api/bag/internal/add            - Internal add items
POST  /api/bag/internal/consume        - Internal consume items
```

---

## 📦 API Examples

### Lấy Danh Sách Items
```bash
curl http://localhost:8230/api/bag/player123/items
```

### Dùng Item
```bash
curl -X POST http://localhost:8230/api/bag/player123/items/use \
  -H "Content-Type: application/json" \
  -d '{"itemId": "item_hp_potion_001", "quantity": 1}'
```

### Grant Items (Internal — từ reward service)
```bash
curl -X POST http://localhost:8230/api/bag/grant \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "quest_001_reward_player123",
    "roleId": "player123",
    "items": [
      {"itemId": "item_001", "quantity": 5},
      {"itemId": "item_sword_002", "quantity": 1}
    ]
  }'
```

---

## 🔧 Business Logic

### Event Deduplication
- Mỗi grant phải kèm `eventId` unique
- BagEventDedup kiểm tra xem `eventId` đã xử lý chưa
- Nếu rồi → skip (idempotent)
- Đảm bảo không bao giờ double-grant dù retry

### Item Stack
- Items được stack theo khóa: `role_id + item_id + bind + expire_at + quality + bag_type`
- `quality` và `bag_type` đã được persist trong `bag_items`

### Kafka Events
- Sau mỗi thay đổi inventory trực tiếp (`grant/use/sell/recycle`) → gửi `BagChangedEvent`
- Grant qua topic `bag.grant` được xử lý idempotent và publish `BagChangedEvent` tại consumer
- webSocket-server consume `BagChangedEvent` và push về client

---

## 🚀 Running

```bash
cd GameServer/bag-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|----------|
| **wallet-service** | (Feign) | Khi bán item → cộng gold |

### Được gọi bởi
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **box-service** | `POST /api/bag/internal/consume` | Trừ item hộp (`40004`) khi mở hộp |
| **box-service** | `POST /api/bag/internal/add` | Thêm bonus item, thời trang, fixed reward |
| **equip-service** | `POST /api/bag/internal/consume` | Trừ item khi trang bị / Fumo |
| **equip-service** | `POST /api/bag/internal/add` | Trả đồ cũ về bag khi tháo slot |
| **equip-service** | `GET /api/bag/{roleId}/items` | Lấy danh sách bag cho `wearable-items` |
| **mail-service**, **task-service**, **shop-service**, **drop-service**, **arena-service** | `POST /api/bag/grant` hoặc `POST /api/bag/internal/add` | Grant items từ nhiều sources |

### Kafka Producer
- **BagChangedEvent**: Real-time inventory update về webSocket-server → client

### gRPC Server (port 9230)
- webSocket-server, shop-service gọi trực tiếp


---

## 📊 Statistics

```
Entities:        3 classes (BagItem, BagEventDedup, RecycleProgress)
Repositories:    3 interfaces
Controllers:     2 (BagController, InternalBagController)
Domain Service:  BagDomainService
Kafka Producer:  BagChangedEvent
gRPC:            BagServiceGrpcImpl
DB tables:       3 (bag_items, bag_event_dedup, recycle_progress)
Feign clients:   1 (WalletFeign)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~900 lines
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22

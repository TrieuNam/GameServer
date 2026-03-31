# Gift Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8270  
**Database**: N/A (Stateless)

---

## 📋 Overview

Gift Service quản lý **hệ thống quà tặng và gói phần thưởng** — open gift packages, daily rewards, và các reward bundles. Stateless service: không lưu trạng thái, chỉ xử lý logic mở quà và forward rewards đến bag/wallet.

### Core Features
- ✅ Open gift packages (mở gói quà)
- ✅ Sử dụng gift items đặc biệt
- ✅ Reward bundle management
- ✅ Tích hợp với scheduler cho daily gifts
- ✅ Internal API để trao quà từ các events/services khác

---

## 🎯 Flow Trao Quà

```
[Daily login reward — scheduler-service trigger]
scheduler-service ──► gift-service: /api/gift/open
                              │
                              ▼
                    gift-service
                    ├── Load gift config (loại quà, nội dung)
                    ├──► bag-service: grant items
                    ├──► wallet-service: grant currency
                    └── Return reward list

[Player dùng gift item trong bag]
POST /api/gift/use { roleId, giftItemId }
        │
        ▼
gift-service.useGiftItem()
└── Tính phần thưởng → forward đến bag/wallet
```

---

## 🔌 API Endpoints

```
GET   /api/gift/{giftItemId}/info   - Thông tin gói quà
POST  /api/gift/open                - Mở gói quà
POST  /api/gift/use                 - Dùng gift item
```

---

## 📦 API Examples

### Xem Thông Tin Gói Quà
```bash
curl http://localhost:8270/api/gift/gift_daily_login_001/info
```

### Mở Gói Quà
```bash
curl -X POST http://localhost:8270/api/gift/open \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "giftItemId": "gift_daily_login_001"
  }'
# Response:
# { "rewards": [{"type": "gold", "amount": 1000}, {"itemId": "hp_potion", "qty": 5}] }
```

---

## 🔧 Business Logic

### Gift Config (từ config-service)
Gift-service đọc cấu hình từ `GET /api/config/file?path=gameworld/item/gift.json` (hỗ trợ `If-None-Match` / `ETag`).

```json
{
  "id": "gift_daily_login_001",
  "name": "Daily Login Reward Day 1",
  "rewards": [
    {"type": "currency", "itemId": 1, "amount": 1000},
    {"type": "item", "itemId": "hp_potion", "quantity": 5}
  ]
}
```

### Daily Gift Reset
- `scheduler-service` gọi reset endpoint mỗi ngày 00:00
- Reset cho phép player nhận daily gift tiếp theo

---

## 🚀 Running

```bash
cd GameServer/gift-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
- **bag-service**: Grant items
- **wallet-service**: Grant currency

### Được gọi bởi
- **scheduler-service**: Daily reset
- **task-service**: Task completion rewards
- **webSocket-server**: Direct player requests

---

## 📊 Statistics

```
Entities:        N/A (Stateless)
Controllers:     1 class (GiftController)
Services:        1 class (GiftService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~300 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


# Wallet Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8210  
**Database**: `game_wallet`

---

## 📋 Overview

Wallet Service quản lý **ví tiền trong game** (gold, diamond, coin, v.v.) cho mỗi nhân vật. Mọi giao dịch tiền tệ trong game đều đi qua đây — mua đồ, nhận thưởng, nạp tiền. Cung cấp cả public API và internal API cho các services khác.

### Core Features
- ✅ CRUD ví tiền cho mỗi role
- ✅ Thêm/trừ từng loại currency
- ✅ Lịch sử giao dịch (WalletLedger)
- ✅ Bulk add (trao thưởng hàng loạt)
- ✅ Transfer giữa players
- ✅ Internal API cho bag-service, shop-service, v.v.
- ✅ gRPC server

---

## 🎯 Flow Hoạt Động

```
[Player mua đồ trong shop]
shop-service ──► wallet-service (Feign): /internal/wallet/{roleId}/consume
                        │
                        ▼
              Check balance đủ không?
              ├── Đủ → Trừ balance, ghi ledger → 200 OK
              └── Không đủ → 400 Insufficient funds

[Trao thưởng level up]
role-service ──► wallet-service (Feign): /internal/wallet/batch-add
                        │
                        ▼
              Cộng từng currency vào wallet, ghi ledger
```

---

## 🎮 Currency Types

| Loại | ID | Mô tả | Cách nhận |
|------|----|-------|-----------|
| **Gold** | 1 | Tiền vàng (phổ thông) | Quest, drop, daily |
| **Diamond** | 2 | Kim cương (premium) | Nạp tiền, sự kiện |
| **Coin** | 3 | Xu đặc biệt | Đấu trường, PvP |
| **Energy** | 4 | Năng lượng | Hồi phục theo thời gian |
| **Reputation** | 5 | Danh vọng | Guild activities |

---

## 🗄️ Database Schema

### wallet_account
```sql
CREATE TABLE wallet_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL UNIQUE,
    gold BIGINT DEFAULT 0,
    diamond BIGINT DEFAULT 0,
    coin BIGINT DEFAULT 0,
    energy INT DEFAULT 100,
    reputation BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### wallet_ledger
```sql
CREATE TABLE wallet_ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    item_id INT NOT NULL,             -- Currency type ID
    amount BIGINT NOT NULL,           -- Positive=add, Negative=consume
    balance_after BIGINT NOT NULL,
    reason VARCHAR(100),              -- "shop_buy", "quest_reward", v.v.
    source_service VARCHAR(50),
    created_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
# Public API
GET   /api/wallet/{playerId}                - Lấy toàn bộ ví tiền
GET   /api/wallet/{playerId}/{itemId}       - Lấy số dư từng currency
POST  /api/wallet/{playerId}/add            - Thêm tiền
POST  /api/wallet/{playerId}/consume        - Trừ tiền
POST  /api/wallet/{playerId}/transfer       - Chuyển tiền
POST  /api/wallet/bulk-add                  - Thêm tiền hàng loạt

# Internal API (cho services khác)
GET   /internal/wallet/{roleId}             - Lấy wallet theo itemIds (internal)
GET   /internal/wallet/info                 - Lấy toàn bộ ví tiền (internal, param: roleId)
POST  /internal/wallet/batch-add            - Batch thêm tiền
POST  /internal/wallet/batch-cost           - Batch trừ tiền
```

---

## 📦 API Examples

### Lấy Ví Tiền
```bash
curl http://localhost:8210/api/wallet/player123
# Response:
# { "roleId": "player123", "gold": 50000, "diamond": 500, "coin": 200 }
```

### Thêm Tiền (Reward)
```bash
curl -X POST http://localhost:8210/api/wallet/player123/add \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": 1,
    "amount": 10000,
    "reason": "daily_login_reward"
  }'
```

### Trừ Tiền (Mua Đồ)
```bash
curl -X POST http://localhost:8210/api/wallet/player123/consume \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": 2,
    "amount": 100,
    "reason": "shop_buy_item_001"
  }'
```

### Batch Add (Trao Thưởng Nhiều Currency)
```bash
curl -X POST http://localhost:8210/api/wallet/bulk-add \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "items": [
      {"itemId": 1, "amount": 5000},
      {"itemId": 2, "amount": 100}
    ]
  }'
```

---

## 🔧 Business Logic

### Transaction Safety
- Tất cả add/consume trong 1 DB transaction
- Optimistic locking để tránh race condition
- Balance không được âm (kiểm tra trước khi consume)

### Ledger
- Mọi thay đổi balance đều ghi vào `wallet_ledger`
- Dùng để audit, debug, và hoàn tiền khi cần

---

## 🚀 Running

```bash
cd GameServer/wallet-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **item-service** | `GET /api/item/meta?itemId=` | Validate item metadata |
| **config-service** | (fallback) | Cấu hình tiền tệ |

### Được gọi bởi
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **bag-service** | `POST /internal/wallet/{roleId}/add` | Cộng tiền khi bán item |
| **shop-service** | `POST /internal/wallet/{roleId}/consume` | Trừ tiền khi mua đồ |
| **mail-service** | `POST /internal/wallet/{roleId}/add` | Cộng reward gold/gem |
| **arena-service** | `POST /internal/wallet/{roleId}/add` | Cộng phần thưởng đấu trường |
| **task-service** | `POST /internal/wallet/{roleId}/add` | Cộng reward task |
| **box-service** | `POST /internal/wallet/{roleId}/consume` | Trừ tiền khi roll equip |
| **equip-service** | (fallback) Lấy tài chính |  |
| **webSocket-server** | gRPC (9210) | Query wallet info |

---

## 📊 Statistics

```
Entities:        2 classes (WalletAccount, WalletLedger)
Repositories:    2 interfaces
Controllers:     2 (WalletController, InternalWalletController)
Services:        1 class (WalletService)
gRPC:            WalletServiceGrpcImpl
Feign clients:   1 (ItemServiceFeign)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~800 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)  
**Last Updated**: 2026-03-22


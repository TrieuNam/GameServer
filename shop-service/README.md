# Shop Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8260 · **gRPC**: 9260  
**Database**: `game_shop`

---

## 📋 Overview

Shop Service quản lý **hệ thống cửa hàng trong game** — mua bán items, trang phục, hộp bí ẩn và nhiều loại shop. Hỗ trợ giới hạn mua hàng theo ngày/tuần, cache config, và tích hợp với bag/wallet để thực hiện giao dịch.

### Core Features
- ✅ Mua bán items qua nhiều loại shop
- ✅ Shop config cache (ShopConfigCache)
- ✅ Giới hạn mua hàng theo ngày/tuần (ShopLimit)
- ✅ Shop thường, shop trang phục, shop bí ẩn
- ✅ gRPC server (port 9260)
- ✅ Tích hợp wallet để trừ tiền, bag để phát item

---

## 🎯 Flow Mua Hàng

```
Client ──► POST /api/shop/buy { itemId, quantity, shopType }
                │
                ▼
        shop-service
        ├── Load config từ ShopConfigCache (lấy từ config-service)
        ├── Check ShopLimit: đã mua quá giới hạn chưa?
        ├── Tính giá: price = shopItem.price * quantity
        │
        ├──► wallet-service (Feign): consume currency
        │
        ├──► bag-service (gRPC): add items
        │
        └── Ghi ShopLimit record
```

---

## 🎮 Loại Shop

| Loại | ID | Mô tả | Currency |
|------|----|-------|----------|
| **Common** | 1 | Shop thường — items phổ thông | Gold |
| **Cloth** | 2 | Shop trang phục | Diamond |
| **Mystery** | 3 | Shop bí ẩn — items random | Diamond |

---

## 🗄️ Database Schema

### shop_limit
```sql
CREATE TABLE shop_limit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    shop_type INT NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    purchased_count INT DEFAULT 0,
    period_key VARCHAR(20),       -- "2026-03-16" (daily) hoặc "2026-W11" (weekly)
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_item_period (role_id, item_id, period_key)
);
```

---

## 🔌 API Endpoints

```
GET   /api/shop/info              - Thông tin shop
POST  /api/shop/list/common       - Danh sách shop thường
POST  /api/shop/list/cloth        - Danh sách shop trang phục
GET   /api/shop/list/mystery      - Danh sách shop bí ẩn
POST  /api/shop/buy               - Mua item
```

---

## 📦 API Examples

### Lấy Danh Sách Shop Thường
```bash
curl http://localhost:8260/api/shop/list/common
```

### Mua Item
```bash
curl -X POST http://localhost:8260/api/shop/buy \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "shopType": 1,
    "itemId": "shop_item_001",
    "quantity": 5
  }'
```

---

## 🔧 Business Logic

### Shop Limit
- Mỗi item có `dailyLimit` và `weeklyLimit` trong config
- ShopLimit record theo `period_key` (ngày/tuần)
- Scheduler reset daily/weekly limit qua `/api/shop` reset endpoint
- `scheduler-service` trigger reset mỗi ngày/tuần

### Config Cache (ShopConfigCache)
- Load từ config-service khi khởi động
- Refresh định kỳ hoặc khi có invalidate signal
- Cache giữ toàn bộ shop catalog (items, prices, limits)
- Endpoint dùng: `GET /api/config/file?path=gameworld/logicconfig/*.json` (+ `If-None-Match` / `ETag`)

---

## 🚀 Running

```bash
cd GameServer/shop-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc (gRPC/Feign)
| Service | Endpoint/Method | Mục đích |
|---------|-----------------|---------|
| **bag-service** | `POST /api/bag/internal/add` | Phát item khi mua |
| **wallet-service** | (Feign) | Trừ currency (gold/diamond) |
| **item-service** | `GET /api/item/meta?itemId=` | Validate item metadata |
| **role-service** | `GET /api/role/{roleId}` | Check level requirement |
| **config-service** | `GET /api/config/file?path=gameworld/logicconfig/shop_cfg.json` | Load shop configuration (ETag cached) |

### Được gọi bởi
| Caller | Mục đích |
|--------|---------|
| **webSocket-server** | gRPC (9260): Mua hàng, hiển thị shop |
| **box-service** | (fallback) Lấy item metadata qua item-service |

---

## 📊 Statistics

```
Entities:        1 class (ShopLimit)
Repositories:    1 interface
Controllers:     1 class (ShopController)
Services:        1 class (ShopService)
Config Cache:    ShopConfigCache
gRPC:            ShopServiceGrpcImpl
Feign clients:   3 (BagServiceFeign, WalletServiceFeign, ItemServiceFeign)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~700 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)  
**Last Updated**: 2026-03-22

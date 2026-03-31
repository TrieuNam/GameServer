# Item Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8220  
**Database**: N/A (Stateless — metadata only)

---

## 📋 Overview

Item Service quản lý **metadata (định nghĩa) của tất cả items** trong game — không phải inventory của player. Là service tra cứu thông tin item: tên, loại, thuộc tính, giá, stack size, v.v. Stateless service vì data đọc từ config.

### Core Features
- ✅ CRUD item definitions (metadata)
- ✅ Batch query nhiều items cùng lúc
- ✅ Phân loại item theo type
- ✅ Validate item tồn tại
- ✅ Recycle value lookup
- ✅ Internal API cho bag, shop, drop sử dụng

---

## 🎯 Vai Trò

```
Câu hỏi: "item_sword_001 tên gì, thuộc tính gì, giá bán bao nhiêu?"

item-service ──► Trả về ItemMetaDTO { id, name, type, attrs, sellPrice, ... }

[Được gọi bởi]
bag-service    ──► Khi thêm item: lấy tên/type để lưu
shop-service   ──► Khi hiển thị shop: lấy thông tin item
drop-service   ──► Validate item IDs trong drop tables
equip-service  ──► Lấy base attributes của equipment
```

---

## 🎮 Item Types

| Loại | ID | Mô tả |
|------|----|-------|
| **Material** | 1 | Nguyên liệu (crafting, upgrade) |
| **Consumable** | 2 | Tiêu thụ được (HP potion, EXP scroll) |
| **Equipment** | 3 | Trang bị có attributes |
| **Currency** | 4 | Tiền tệ đặc biệt |
| **Key** | 5 | Chìa khóa mở hộp/dungeon |
| **Special** | 6 | Items đặc biệt (event, seasonal) |

---

## 🔌 API Endpoints

```
GET   /api/item/meta               - Lấy metadata 1 item (query param: itemId)
GET   /api/item/meta/batch         - Lấy metadata nhiều items cùng lúc
GET   /api/item/type               - Lấy items theo loại
GET   /api/item/validate           - Validate item ID và count (query params: itemId, count)
GET   /internal/item/meta/raw      - Raw metadata (internal)
POST  /api/item/{roleId}/recycle   - Tính giá trị recycle
```

---

## 📦 API Examples

### Lấy Metadata Item
```bash
curl "http://localhost:8220/api/item/meta?itemId=8500"
# Response:
# { "itemId": 8500, "itemType": "equip", "quality": 1,
#   "exp": 5, "sellPrice": 1, "pileLimit": 1,
#   "invalidTime": 0, "isSpecial": false }
```

### Batch Query
```bash
curl "http://localhost:8220/api/item/meta/batch?itemId=8500&itemId=8501&itemId=40002"
```

### Validate Items
```bash
curl "http://localhost:8220/api/item/validate?itemId=8500&count=1"
# Response: { "ok": true, "message": "OK" }
```

---

## 🔧 Business Logic

### Data Source
- Item definitions được load từ `config-service`
- Nguồn thật là các file gộp dưới `gameworld/item/*.json`, không phải mỗi item một file riêng
- Ví dụ: item `8500` nằm trong `gameworld/item/equipment.json`
- Cache in-memory theo item và theo catalog file để tận dụng ETag/304

### Item Attributes
```json
{
  "itemId": 8500,
  "itemType": "equip",
  "quality": 1,
  "exp": 5,
  "sellPrice": 1,
  "pileLimit": 1,
  "invalidTime": 0,
  "isSpecial": false
}
```

---

## 🚀 Running

```bash
cd GameServer/item-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **bag-service** | `GET /api/item/meta?itemId=` | Validate item khi add vào bag |
| **box-service** | `GET /api/item/meta/batch?itemId=` | Filter virtual items trong pending equip |
| **shop-service** | `GET /api/item/meta/batch?ids=` | Hiển thị item trong shop |
| **drop-service** | `POST /api/item/validate` | Validate item IDs trong drop tables |
| **equip-service** | `GET /api/item/meta?itemId=` | Lấy metadata equip (primary source trước fallback config) |
| **crafting-service** | `GET /api/item/meta?itemId=` | Tính output crafting |
| **shizhuang-service** | `GET /api/item/meta?itemId=` | Lấy thông tin quần áo |
| **wallet-service** | `GET /api/item/meta?itemId=` | Tính giá tiền tệ |
| **task-service** | `POST /api/item/validate` | Validate reward items |

---

## 📊 Statistics

```
Entities:        N/A (Config-based)
Controllers:     1 class (ItemController, ItemInternalController)
Services:        1 class (ItemService)
Cache:           In-memory ConcurrentHashMap (2-tier: L1 = 1min, L2 = reload on demand)
Config source:   config-service (gameworld/item/*.json)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~450 lines
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22

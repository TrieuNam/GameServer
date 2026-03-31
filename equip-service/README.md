# Equip Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8240 (HTTP) · **gRPC**: 9240  
**Database**: `game_equip`

---

## 📋 Tổng quan

Equip Service quản lý **trang bị (equipment)** của nhân vật — trang bị/tháo đồ, nâng cấp, Fumo enchantment, và cung cấp internal APIs cho **box-service** (wear-from-box, compute-sell, decompose).

### Core Features
- ✅ Trang bị / tháo trang bị (slot-based, 1 item/slot)
- ✅ Snapshot stats khi trang bị (HP/ATK/DEF/SPD + 2 attr phụ)
- ✅ Hệ thống Fumo/enchantment (level + exp, tốn item từ bag)
- ✅ Internal APIs phục vụ box-service: `wear-from-box`, `compute-sell`, `decompose`
- ✅ Meta lookup qua item-service + fallback `equipment.json` từ config-service
- ✅ gRPC server port 9240 (GetEquipment, EquipItem, UnequipItem, UpgradeEquipment, GetEquipmentStats, BatchGetEquipment)

---

## 🎯 Flow Trang Bị

### Flow thường (từ bag)
```
POST /api/equip/wear/{roleId}/{itemId}
        │
        ▼
EquipService.wear() → equip()
├── getOneMeta(itemId)            → item-service / fallback equipment.json
├── extractEquipType(meta)        → kiểm tra có trang bị được không
├── bagFeign.consume()            → trừ item khỏi bag
├── findByRoleIdAndEquipType()    → lấy slot hiện tại
├── nếu có đồ cũ → bagFeign.add() → trả đồ cũ về bag
├── snapshotStatsFromMeta()       → lưu HP/ATK/DEF/SPD vào slot
└── slotRepo.save()               → cập nhật DB
```

### Flow từ box-service (pending equip)
```
POST /internal/equip/wear-from-box { roleId, item: { itemId, equipType, quality, equipLevel } }
        │
        ▼
EquipService.wearFromBox()
├── KHÔNG đụng bag (box-service quản lý)
├── Ghi item mới vào slot
└── Trả về { replaced: {itemId, equipType, quality, equipLevel} } (nếu có đồ cũ)
       → box-service dùng replaced để tạo pending mới
```

### Flow Decompose (từ box-service)
```
POST /internal/equip/decompose { roleId, item: { itemId, quality, equipLevel } }
        │
        ▼
EquipService.decompose()          → KHÔNG thực thi add/consume
├── getOneMeta(itemId)
├── itemOut = meta.decompose_item_id || props.decomposeItemId
├── num    = numBase + numPerLevel × (equipLevel - 1)
├── exp    = meta.decompose_exp || fallbackSellExp(quality, equipLevel)
└── return { itemId, num, exp }  → box-service thực thi add vào bag
```

---

## 🗄️ Database Schema — `game_equip`

### `equip_slot` — Trang bị đang mặc của nhân vật
```sql
CREATE TABLE equip_slot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    equip_type  INT    NOT NULL,      -- loại slot (map từ equipment.json: part 0-11)
    item_id     INT    NOT NULL,      -- 0 nếu slot trống
    hp          INT    DEFAULT 0,     -- snapshot lúc trang bị
    attack      INT    DEFAULT 0,
    defend      INT    DEFAULT 0,
    speed       INT    DEFAULT 0,
    attr_type1  INT    DEFAULT 0,     -- thuộc tính phụ 1
    attr_value1 INT    DEFAULT 0,
    attr_type2  INT    DEFAULT 0,     -- thuộc tính phụ 2
    attr_value2 INT    DEFAULT 0,
    version     BIGINT NOT NULL DEFAULT 0,   -- optimistic lock
    updated_at  BIGINT NOT NULL,
    UNIQUE KEY uk_role_type (role_id, equip_type),
    KEY idx_role (role_id)
);
```

### `equip_fumo` — Fumo enchantment theo slot
```sql
CREATE TABLE equip_fumo (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    equip_type  INT    NOT NULL,
    level       INT    NOT NULL,      -- fumo level (0 → fumoMaxLevel=50)
    exp         INT    NOT NULL,      -- exp hiện tại trong level
    end_time    BIGINT NOT NULL,      -- epoch giây hết hiệu lực (0 = không có)
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  BIGINT NOT NULL,
    UNIQUE KEY uk_role_type (role_id, equip_type)
);
```

---

## 🔌 API Endpoints

### Public — `/api/equip`

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/equip/{roleId}` | Danh sách **sparse** các slot đang có trang bị (không fill sẵn slot trống) |
| `GET` | `/api/equip/{roleId}/wearable-items` | Item trong bag có thể trang bị được |
| `POST` | `/api/equip/equip` | Trang bị item (body: `EquipReq`) |
| `POST` | `/api/equip/unequip` | Tháo trang bị (body: `UnequipReq`) |
| `POST` | `/api/equip/wear/{roleId}/{itemId}` | Trang bị theo itemId, optional `?bagType=` |
| `GET` | `/api/equip/fumo/{roleId}` | Danh sách Fumo của tất cả slots |
| `GET` | `/api/equip/fumo/{roleId}/{equipType}` | Fumo của 1 slot cụ thể |
| `POST` | `/api/equip/fumo/add-exp` | Thêm exp Fumo (tiêu `costItems` từ bag) |
| `POST` | `/api/equip/fumo/activate` | Kích hoạt Fumo (đặt `endTimeEpochSec`) |
| `POST` | `/api/equip/fumo/reset` | Reset Fumo về level 0 |

### Internal — `/internal/equip` (service-to-service)

| Method | Path | Caller | Mô tả |
|--------|------|--------|-------|
| `GET` | `/internal/equip/{roleId}` | — | Giống `/api/equip/{roleId}` |
| `POST` | `/internal/equip/equip` | — | Giống `/api/equip/equip` |
| `POST` | `/internal/equip/unequip` | — | Giống `/api/equip/unequip` |
| `POST` | `/internal/equip/wear-from-box` | **box-service** | Mặc pending equip, trả về replaced |
| `POST` | `/internal/equip/compute-sell` | **box-service** | Tính coin + exp khi bán equip |
| `POST` | `/internal/equip/decompose` | **box-service** | Tính item + exp khi phân giải |
| `POST` | `/internal/equip/item-kind` | **box-service** | Kiểm tra itemId có phải equip không |
| `POST` | `/internal/equip/resolve-item-id` | **box-service** | Resolve itemId theo equipType/quality/level |
| `POST` | `/internal/equip/item-meta` | **box-service** | Lấy meta (equipType/quality/level) |

### gRPC — port 9240

| Method | Request | Mô tả |
|--------|---------|-------|
| `GetEquipment` | `{ roleId, includeStats }` | Lấy trang bị + TotalStats tuỳ chọn |
| `EquipItem` | `{ roleId, itemId, slotId }` | Trang bị item, trả về newFightPower |
| `UnequipItem` | `{ roleId, slotId }` | Tháo trang bị, trả về newStats |
| `UpgradeEquipment` | `{ roleId, slotId }` | Nâng cấp stat +10% (min +1) |
| `GetEquipmentStats` | `{ roleId }` | Tổng stats + avgLevel/avgQuality |
| `BatchGetEquipment` | `{ roleIds[], includeStats }` | Batch lấy trang bị nhiều role |

---

## 📦 API Examples

### Lấy trang bị đang mặc
```bash
curl http://localhost:8240/api/equip/1001
```

### Trang bị item từ bag
```bash
curl -X POST "http://localhost:8240/api/equip/wear/1001/50001"
```

### Phân giải equip (box-service gọi)
```bash
curl -X POST http://localhost:8240/internal/equip/decompose \
  -H "Content-Type: application/json" \
  -d '{"roleId":"1001","item":{"itemId":50001,"quality":3,"equipLevel":10}}'
# Response: { "itemId": 40003, "num": 5, "exp": 200 }
```

### Tính tiền bán equip (box-service gọi)
```bash
curl -X POST http://localhost:8240/internal/equip/compute-sell \
  -H "Content-Type: application/json" \
  -d '{"item":{"itemId":50001,"quality":3,"equipLevel":10},"businessmanPermyriad":500}'
# Response: { "coin": 1500, "exp": 100 }
```

### Thêm Fumo exp
```bash
curl -X POST http://localhost:8240/api/equip/fumo/add-exp \
  -H "Content-Type: application/json" \
  -d '{"roleId":"1001","equipType":1,"addExp":500,"costItems":{"40003":2}}'
```

---

## 🔧 Business Logic

### 1. Equipment Slots (`equip_type`)
- Map từ `equipment.json → part` field (0–11, từ config-service)
- Mỗi slot: 1 item tại 1 thời điểm (UNIQUE KEY `role_id + equip_type`)
- Khi trang bị item mới vào slot đã có → đồ cũ tự động trả về bag
- `GET /api/equip/{roleId}` hiện trả về **các slot đang có row/equip trong DB**; slot trống không được pad thành mảng 12 phần tử

### 2. Meta Lookup — Dual Source
```
getOneMeta(itemId):
  1) itemMetaFeign.meta(itemId)         → item-service (primary)
  2) equipmentConfigCache.find(itemId)  → config-service equipment.json (fallback)
     parse: { equipType=part, hp=hp_max, attack=att_max, defend=def_max, speed=speed_max,
              attrType1=frist_att, attrType2=second_att }
```
> Fallback đảm bảo flow equip không bị block khi item-service tạm thời lỗi.

### 3. Stat Snapshot (khi trang bị)
```
hp      ← meta["hp"]
attack  ← meta["attack"] || meta["att"]
defend  ← meta["defend"] || meta["def"]
speed   ← meta["speed"]  || meta["spd"]
attrType1/Value1  ← meta["attrType1"] || meta["fristAtt"]
attrType2/Value2  ← meta["attrType2"] || meta["secondAtt"]
```

### 4. Decompose Logic
```
itemOut = meta.decompose_item_id  || props.decomposeItemId   (fallback 0)
numBase = meta.decompose_num_base || props.decomposeNumBase  (fallback 0)
numPerL = meta.decompose_num_per_level || props.decomposeNumPerLevel
exp     = meta.decompose_exp      || fallbackSellExp(quality, equipLevel)

num = numBase + numPerL × max(0, equipLevel - 1)
```
> box-service nhân `num` và `exp` lên 5 nếu mở x5 và `isNew=true`.

### 5. Compute Sell Logic
```
baseCoin = meta["sell_price"] || meta["price_sell"] || meta["price"] || fallbackSellCoin()
baseExp  = meta["sell_exp"]   || meta["exp_sell"]   || fallbackSellExp()

nếu businessmanPermyriad > 0:
  baseCoin = round(baseCoin × (1 + permyriad / 10000))
```

### 6. Fumo System
Config mặc định (`EquipProperties`):

| Property | Default | Mô tả |
|----------|---------|-------|
| `fumoMaxLevel` | 50 | Level Fumo tối đa |
| `fumoBaseExp` | 100 | EXP cần để lên level 1 |
| `fumoGrowExp` | 50 | Tăng thêm mỗi level: level N cần `100 + 50×(N-1)` EXP |

- `add-exp`: tiêu `costItems` từ bag → cộng exp → tự động level up nếu đủ
- `activate`: set `endTimeEpochSec` (thời hạn hiệu lực Fumo)
- `reset`: về level=0, exp=0, endTime=0

### 7. gRPC Fight Power Formula
```
fightPower = HP/10 + ATK×3 + DEF×2
```
`UpgradeEquipment` boost: mỗi stat +10% (tối thiểu +1) trực tiếp trên snapshot `equip_slot`.

---

## ⚙️ Config Files (từ config-service)

| File | Path | Cache class | Mô tả |
|------|------|-------------|-------|
| `equipment.json` | `gameworld/item/equipment.json` | `EquipmentConfigCache` | Metadata trang bị: part, hp_max, att_max, def_max, speed_max, frist_att, second_att |

**Cấu trúc `equipment.json`:**
```json
{
  "wuqi":   [{ "id": 50001, "part": 1, "hp_max": 0, "att_max": 500, "def_max": 0, "speed_max": 0 }],
  "toukui": [{ "id": 50100, "part": 2, "hp_max": 300, "att_max": 0, "def_max": 200, "speed_max": 0 }]
}
```
> Key top-level là tên loại trang bị (wuqi=vũ khí, toukui=mũ, …).  
> `EquipmentConfigCache` flatten tất cả → `Map<itemId, EquipRow>`, ETag-cached.

**Config path** (`application.yml`):
```yaml
equip:
  config:
    equipment-path: ${EQUIP_CONFIG_PATH:gameworld/item/equipment.json}
```

---

## 🔗 Integration Points

### Phụ thuộc (gọi ra)
| Service | Feign class | Endpoint | Mục đích |
|---------|-------------|----------|----------|
| **bag-service** | `BagInternalFeign` | `POST /api/bag/internal/consume` | Trừ item khi trang bị / tiêu item Fumo |
| **bag-service** | `BagInternalFeign` | `POST /api/bag/internal/add` | Trả đồ cũ về bag khi thay slot |
| **bag-service** | `BagPublicFeign` | `GET /api/bag/{roleId}/items` | Lấy danh sách bag cho `wearable-items` |
| **item-service** | `ItemMetaFeign` | `GET /api/item/meta?itemId=` | Lấy metadata item (primary) |
| **config-service** | `ConfigFeign` + `EquipmentConfigCache` | `GET /api/config/file?path=gameworld/item/equipment.json` | Metadata fallback (ETag cached) |

### Được gọi bởi
| Caller | Cách gọi | Endpoints dùng |
|--------|----------|----------------|
| **box-service** | REST Feign (`EquipFeign`) | `/internal/equip/wear-from-box`, `/decompose`, `/compute-sell`, `/item-kind`, `/resolve-item-id`, `/item-meta` |
| **webSocket-server** | gRPC port 9240 | `GetEquipment`, `EquipItem`, `UnequipItem`, `UpgradeEquipment`, `GetEquipmentStats`, `BatchGetEquipment` |

---

## 🚀 Chạy local

```bash
cd GameServer/equip-service
mvn clean package -DskipTests
java -jar target/equip-service-1.0.0.jar --spring.profiles.active=local
```

> ⚠️ Khởi động **sau** eureka-server, config-service, bag-service, item-service

---

## 📊 Thống kê code

```
Entities:       2  (EquipSlotEntity, EquipFumoEntity)
Repositories:   2  (EquipSlotRepository, EquipFumoRepository)
Controllers:    2  (EquipController — 10 endpoints, InternalEquipController — 9 endpoints)
Services:       2  (EquipService, EquipFumoService)
Config caches:  1  (EquipmentConfigCache — equipment.json)
gRPC impl:      1  (EquipmentServiceGrpcImpl — 6 methods)
Feign clients:  3  (BagInternalFeign, BagPublicFeign, ItemMetaFeign)
DB tables:      2  (equip_slot, equip_fumo)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EquipService:     ~400 lines
EquipFumoService: ~120 lines
gRPC impl:        ~450 lines
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22  
**Analyzed by**: gameserver-skill-memory agent

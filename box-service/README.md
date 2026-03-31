# Box Service

**Version**: 1.0.0  
**Port**: 8290  
**Database**: `game_box`  
**Profile mặc định**: `local`

---

## 📋 Tổng quan

Box Service quản lý toàn bộ **hệ thống mở hộp (Khai Hộp)** trong game:
- Mở hộp → roll trang bị ngẫu nhiên (pending equip)
- Trang bị / bán / phân giải equip đang chờ
- Nâng cấp hộp (box level) + tăng tốc
- Thời trang (shizhuang) theo quota ngày
- Phần thưởng cột mốc cố định (fixed milestone)
- Hệ thống may mắn tích lũy (Khai Hộp Đại Giải)
- Wabao (宝盒) — bản đồ kho báu

---

## 🎮 Flow Mở Hộp

```
Client ──► POST /api/box/open { roleId, count }
                │
                ▼
        BoxService.open()
        ├── getOrCreate(roleId)        → tạo BoxState nếu chưa có
        ├── maybeCompleteLevelUp()     → kiểm tra nâng cấp hộp xong chưa
        ├── maybeDailyReset()          → reset quota ngày
        ├── Kiểm tra pending equip     → nếu còn pending, trả về luôn (không mở tiếp)
        ├── Kiểm tra cooldown (lastOpenEpoch)
        ├── bag-service.consume()      → trừ item hộp (itemId=40004)
        │
        ├── [Nhánh A] Fixed milestone? → fixed item, nếu là equip → pending
        ├── [Nhánh B] Còn quota thời trang? → add thẳng fashion item
        └── [Nhánh C] Random equip     → roll quality + level + part → pending
                │
                ▼
        Client nhận OpenResp { pending, bonusItems, openBoxTotal }
        Client chọn: wear / sell / decompose
```

---

## 🗄️ Database Schema

### `box_state` — Trạng thái hộp mỗi nhân vật
```sql
CREATE TABLE box_state (
    role_id            BIGINT      NOT NULL,
    box_level          INT         NOT NULL DEFAULT 1,       -- cấp hộp hiện tại
    box_buy_times      INT         NOT NULL DEFAULT 0,       -- số lần mua
    level_up_end_epoch BIGINT      NOT NULL DEFAULT 0,       -- epoch kết thúc nâng cấp
    level_fetch_flag   INT         NOT NULL DEFAULT 0,       -- bitflag đã nhận reward cấp
    open_box_total     INT         NOT NULL DEFAULT 0,       -- tổng số hộp đã mở
    last_open_is_five  TINYINT     NOT NULL DEFAULT 0,       -- lần mở gần nhất có mở 5 không
    pending_json       TEXT        NULL,                     -- equip đang chờ xử lý (JSON)
    shi_zhuang_num     INT         NOT NULL DEFAULT 0,       -- số thời trang đã nhận hôm nay
    arena_item_num     INT         NOT NULL DEFAULT 0,       -- số vé arena đã nhận
    daily_ymd          VARCHAR(16) NULL,                     -- ngày reset (yyyy-MM-dd UTC)
    last_open_epoch    BIGINT      NOT NULL DEFAULT 0,       -- epoch giây lần mở cuối (throttle)
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
);
```

### `luck_state` — Trạng thái may mắn tích lũy (Khai Hộp Đại Giải)
```sql
CREATE TABLE luck_state (
    role_id           BIGINT NOT NULL,
    start_epoch       BIGINT NOT NULL DEFAULT 0,   -- thời gian bắt đầu sự kiện
    end_epoch         BIGINT NOT NULL DEFAULT 0,   -- thời gian kết thúc sự kiện
    receive_bitmap    BIGINT NOT NULL DEFAULT 0,   -- bitflag các phần thưởng đã nhận
    snapshot_open_cnt INT    NOT NULL DEFAULT 0,   -- số hộp mở tại thời điểm bắt đầu
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id)
);
```

### `box_setting` — Cài đặt tự động của người chơi
```sql
CREATE TABLE box_setting (
    role_id               BIGINT NOT NULL,
    equip_eqality         INT    NOT NULL DEFAULT 0,
    open_five_mark        INT    NOT NULL DEFAULT 0,
    equip_cap_mark        INT    NOT NULL DEFAULT 1,  -- mặc định bật
    equip_sell_mark       INT    NOT NULL DEFAULT 0,
    condition_first1      INT    NOT NULL DEFAULT 0,
    condition_first2      INT    NOT NULL DEFAULT 0,
    condition_second1     INT    NOT NULL DEFAULT 0,
    condition_second2     INT    NOT NULL DEFAULT 0,
    condition_first_mark  INT    NOT NULL DEFAULT 0,
    condition_second_mark INT    NOT NULL DEFAULT 0,
    retain_mark           INT    NOT NULL DEFAULT 0,
    challenge_mark        INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (role_id)
);
```

---

## 🔌 API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET`  | `/api/box/info?roleId=` | Thông tin trạng thái hộp |
| `POST` | `/api/box/open` | Mở hộp (`count` = 1 hoặc 5) |
| `POST` | `/api/box/wear` | Trang bị equip đang pending |
| `POST` | `/api/box/sell` | Bán equip đang pending |
| `POST` | `/api/box/decompose?roleId=` | Phân giải equip → nhận vật liệu + EXP |
| `POST` | `/api/box/buy` | Tăng `boxBuyTimes` |
| `POST` | `/api/box/level-up` | Bắt đầu nâng cấp hộp (timer 60s) |
| `POST` | `/api/box/quicken` | Tăng tốc nâng cấp (dùng item `accelerate_id=40002`) |
| `POST` | `/api/box/level-reward` | Nhận thưởng theo cấp hộp |
| `GET`  | `/api/box/luck/info?roleId=` | Thông tin may mắn tích lũy |
| `POST` | `/api/box/luck/receive` | Nhận phần thưởng may mắn |
| `GET`  | `/api/box/setting?roleId=` | Lấy cài đặt |
| `POST` | `/api/box/setting` | Lưu cài đặt |
| `GET`  | `/api/box/equipInfo?roleId=` | Thông tin equip trong hộp |
| `GET`  | `/api/box/wabao/map?roleId=` | SC 1643 — Bản đồ Wabao |
| `GET`  | `/api/box/wabao/integrity?roleId=` | SC 1645 — Tính toàn vẹn |
| `GET`  | `/api/box/wabao/collection?roleId=` | SC 1646 — Bộ sưu tập |
| `GET`  | `/api/box/wabao/tool?roleId=` | SC 1647 — Công cụ |
| `GET`  | `/api/box/wabao/task?roleId=` | SC 1648 — Nhiệm vụ |
| `GET`  | `/api/box/wabao/book?roleId=` | SC 1651 — Sách kho báu |

---

## 📦 API Examples

### Mở 1 hộp
```bash
curl -X POST http://localhost:8290/api/box/open \
  -H "Content-Type: application/json" \
  -d '{"roleId": "1001", "count": 1, "roleLevel": 60}'
```

### Mở 5 hộp (x5 mode)
```bash
curl -X POST http://localhost:8290/api/box/open \
  -H "Content-Type: application/json" \
  -d '{"roleId": "1001", "count": 5, "roleLevel": 60}'
```

### Trang bị equip đang pending
```bash
curl -X POST http://localhost:8290/api/box/wear \
  -H "Content-Type: application/json" \
  -d '{"roleId": "1001"}'
```

### Phân giải equip
```bash
curl -X POST "http://localhost:8290/api/box/decompose?roleId=1001"
```

---

## 🔧 Business Logic

### 1. Pending Equip
- Mỗi lần mở hộp ra equip → equip lưu vào `pending_json` (không vào bag ngay)
- Client nhận `pending` trong response → hiển thị popup cho người dùng chọn:
  - **Wear** → thêm vào `BAG_EQUIP`
  - **Sell** → xóa pending (coin xử lý phía client/wallet)
  - **Decompose** → nhờ `equip-service` tính vật liệu + EXP → cộng vào bag/role
- Nếu đang có pending → gọi `open` lại sẽ trả về pending cũ, **không mở mới**

### 2. Phần thưởng cột mốc cố định (`fixed_reward`)
Mở đủ N hộp → tự động nhận item đặc biệt (32 entries, `box_oder` = thứ tự tích lũy):

| Lần mở (box_oder) | Item ID | Ghi chú |
|-------------------|---------|---------|
| 1 | 1000 | |
| 2 | 8500 | |
| 3 | 16000 | |
| 4 | 23500 | |
| 5 | 3500 | |
| 6 | 1000 | |
| 7 | 6000 | |
| 8 | 8500 | |
| 9 | 11000 | |
| 10 | 13500 | |
| 11 | 16000 | |
| 12 | 18500 | |
| 13 | 21000 | |
| 14 | 23500 | |
| 15 | 26000 | |
| 16 | 28500 | |
| 17 | 1000 | |
| 18 | 11000 | |
| 19 | 13500 | |
| 20 | 28501 | |
| 21 | 18500 | |
| 22 | 21000 | |
| 23 | 26000 | |
| 24 | 13501 | |
| 25 | 11001 | |
| 31 | 40420 | ✨ Milestone đặc biệt |
| 51 | 40410 | ✨ |
| 350 | 40412 | ✨ |
| 800 | 40407 | ✨ |
| 1500 | 40417 | ✨ |
| 3000 | 40401 | ✨ |
| 5000 | 40413 | ✨ Milestone tối cao |

### 3. Roll Equipment
```
playerLevel → random_level table  → equipLevel
boxLevel    → random_color table  → quality (1–8) theo rate
random      → pickEquipPart()     → part (0–9)
(part, quality, equipLevel) → EquipmentIndex.resolve() → itemId
```

### 4. Thời trang (Shizhuang)
- Config: `max_shizhuang=3`, `get_shizhuang=1`
- Nếu hôm nay `shiZhuangNum < 3` → roll fashion từ `shizhuang_rate` → add thẳng vào bag
- Reset mỗi ngày theo `daily_ymd`

### 5. Nâng cấp hộp
- `POST /level-up` → bắt đầu timer (`level_up_end_epoch = now + 60s`)
- `POST /quicken` → tiêu item `accelerate_id=40002`, rút ngắn thời gian
- Khi timer hết → `boxLevel++` (tự động qua `maybeCompleteLevelUp()`)
- `boxLevel` ảnh hưởng đến `random_color` → quality weight thay đổi

### 6. Khai Hộp Đại Giải (Lucky Jackpot — `kaixiangdaji.json`)
- Sự kiện tích lũy kéo dài **7 ngày** (`time=7`, `is_open=1`)
- **2 loại điều kiện** nhận thưởng (`type_box_num`):
  - `type_box_num=1` — tích lũy **số hộp mở** trong kỳ (7 tier: 100→5000 hộp → item 40002 × 4–100)
  - `type_box_num=2` — tích lũy **cấp hộp đạt được** trong kỳ (8 tier: cấp 2–9 → item 40001 × 10–160)
- `receive_bitmap` theo dõi tier nào đã nhận (15 bit tương ứng 15 tier)

### 7. x5 Mode
- Mở 5 hộp cùng lúc (`count=5`)
- Nếu decompose equip nhận từ lần mở x5: **vật liệu và EXP nhân 5**
- Cờ `last_open_is_five` lưu trong `box_state`

---

## ⚙️ Config Files (từ config-service)

| File | Path trong config-service | Mô tả | Cache class |
|------|--------------------------|-------|-------------|
| `unpack.json` | `gameworld/logicconfig/unpack.json` | Config mở hộp chính | `UnpackConfigCache` |
| `kaixiangdaji.json` | `gameworld/logicconfig/kaixiangdaji.json` | Config sự kiện Jackpot | `LuckUnpackConfigCache` |

### Cấu trúc `unpack.json` — 7989 dòng, 9 sections

> 📖 Xem chi tiết đầy đủ tại: [`config-service/README.md` → §📦 Unpack Config](../config-service/README.md)

| Section | Entries | Mô tả |
|---------|---------|-------|
| `random_level` | 1195 | Xác suất roll equip level theo player level × random_level → rate (/10000) |
| `random_color` | 30 | Config theo cấp hộp: quality weight (`equipment_color_1/2`), giá mua thêm lần (`price`), thời gian tăng tốc (`up_time_minute`), bonus reward |
| `other` | 1 object | Tham số toàn cục: `unpack_item_id=40004`, `accelerate_id=40002`, `currency_type=40000`, `box_num_max=2000`, `max_shizhuang=3`, `max_challenge=50` |
| `color_att` | 100 | Nhóm thuộc tính theo chất lượng màu: `att_group`, `att_type`, `att_num_min/max`, `rate` |
| `att_describe` | 25 | Mô tả thuộc tính (hiện để trống `[]`) |
| `auto_unpack` | 21 | Cài đặt tự mở (hiện để trống `[]`) |
| `shizhuang_rate` | 48 | Tỷ lệ drop thời trang: `seq`, `item_id`, `rate` |
| `fixed_reward` | 32 | Milestone cố định: `box_oder` → `item_id` (mở lần thứ N nhận item) |
| `getway` | 4 | Cách nhận gói (hiện để trống `[]`) |

**JSON mẫu (`other` + `random_color` + `fixed_reward`):**
```json
{
  "other": {
    "additional_attribute_num": "2",
    "box_num_max": "2000",
    "currency_type": "40000",
    "accelerate_id": "40002",
    "unpack_item_id": "40004",
    "challenge": "40090",
    "get_rate": "300",
    "get_num": "15",
    "down_rate": "10",
    "max_challenge": "50",
    "get_shizhuang": "1",
    "max_shizhuang": "3"
  },
  "random_level": [
    { "level": "1", "random_level": "1", "rate": "9500" },
    { "level": "1", "random_level": "2", "rate": "400"  }
  ],
  "random_color": [
    {
      "box_level": "1",
      "equipment_color_1": "8900100010000000",
      "equipment_color_2": "7087251939400000",
      "up_buy_num": "1",
      "price": "150",
      "up_time_minute": "5",
      "reward": [{ "item_id": "40001", "num": "10" }]
    }
  ],
  "shizhuang_rate": [
    { "seq": "1", "item_id": "40400", "rate": "400" }
  ],
  "fixed_reward": [
    { "box_oder": "1",  "item_id": "1000"  },
    { "box_oder": "31", "item_id": "40420" }
  ]
}
```

### Cấu trúc `kaixiangdaji.json` — 2 sections

| Section | Entries | Mô tả |
|---------|---------|-------|
| `reward` | 15 | Tier phần thưởng tích lũy (`type_box_num` + `type_num` → `reward_item`) |
| `other` | 1 object | `time=7` (ngày kéo dài), `is_open=1` |

**15 Tier reward (type_box_num=1 → tích số hộp; type_box_num=2 → tích cấp hộp):**

| type | type_box_num | type_num | item_id | num | Điều kiện |
|------|-------------|----------|---------|-----|-----------|
| 0 | 1 | 100 | 40002 | 4 | Mở ≥ 100 hộp trong kỳ |
| 1 | 1 | 200 | 40002 | 8 | Mở ≥ 200 hộp |
| 2 | 1 | 400 | 40002 | 16 | Mở ≥ 400 hộp |
| 3 | 1 | 800 | 40002 | 32 | Mở ≥ 800 hộp |
| 4 | 1 | 1600 | 40002 | 48 | Mở ≥ 1600 hộp |
| 5 | 1 | 3200 | 40002 | 64 | Mở ≥ 3200 hộp |
| 6 | 1 | 5000 | 40002 | 100 | Mở ≥ 5000 hộp |
| 7 | 2 | 2 | 40001 | 10 | Nâng hộp lên cấp 2 |
| 8 | 2 | 3 | 40001 | 20 | Nâng hộp lên cấp 3 |
| 9 | 2 | 4 | 40001 | 30 | Nâng hộp lên cấp 4 |
| 10 | 2 | 5 | 40001 | 40 | Nâng hộp lên cấp 5 |
| 11 | 2 | 6 | 40001 | 60 | Nâng hộp lên cấp 6 |
| 12 | 2 | 7 | 40001 | 80 | Nâng hộp lên cấp 7 |
| 13 | 2 | 8 | 40001 | 120 | Nâng hộp lên cấp 8 |
| 14 | 2 | 9 | 40001 | 160 | Nâng hộp lên cấp 9 |

**JSON mẫu:**
```json
{
  "other": { "time": "7", "is_open": "1" },
  "reward": [
    { "type": "0", "type_box_num": "1", "type_num": "100",  "reward_item": { "item_id": "40002", "num": "4"   } },
    { "type": "1", "type_box_num": "1", "type_num": "200",  "reward_item": { "item_id": "40002", "num": "8"   } },
    { "type": "6", "type_box_num": "1", "type_num": "5000", "reward_item": { "item_id": "40002", "num": "100" } },
    { "type": "7", "type_box_num": "2", "type_num": "2",    "reward_item": { "item_id": "40001", "num": "10"  } },
    { "type": "14","type_box_num": "2", "type_num": "9",    "reward_item": { "item_id": "40001", "num": "160" } }
  ]
}
```

> ⚠️ **Lưu ý tên file**: Hai file này **không có hậu tố `_cfg`**.  
> Tên đúng: `unpack.json` và `kaixiangdaji.json`  
> Tên sai (không tồn tại): ~~`unpack_cfg.json`~~ và ~~`kaixiang_cfg.json`~~

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Dùng để |
|---------|---------|
| **bag-service** | Tiêu item hộp (`consume`), thêm item vào túi (`add`) |
| **item-service** | Lọc virtual items trước khi add vào bag |
| **equip-service** | Tính vật liệu + EXP khi decompose |
| **role-service** | Cộng EXP sau decompose, lấy player level |
| **config-service** | Load `unpack.json`, `kaixiangdaji.json` qua ETag cache (TTL 30s) |

### Được gọi bởi
- **webSocket-server** → `BoxHandler.pushAll()` (bootstrap khi login)
- **webSocket-server** → `WaBaoHandler.pushAll()` (bootstrap wabao khi login)

---

## 🚀 Chạy local

```bash
cd GameServer/box-service
mvn clean package -DskipTests
java -jar target/box-service-1.0.0.jar --spring.profiles.active=local
```

---

## 🐛 Bugs đã fix (2026-03-22)

| Bug | Nguyên nhân | Fix |
|-----|-------------|-----|
| `Duplicate entry for key 'box_state.PRIMARY'` | Race condition: 2 request cùng `getOrCreate()` | Catch `DataIntegrityViolationException` → retry `findById()` |
| `Bad config: other.unpack_item_id` | `application.yml` trỏ tới `unpack_cfg.json` (không tồn tại) | Sửa path → `unpack.json`; thêm fallback `box.unpack-item-id=40004` |
| Config path kaixiang sai | Trỏ tới `kaixiang_cfg.json` (không tồn tại) | Sửa path → `kaixiangdaji.json` |

---

## 📊 Thống kê code

```
Entities:        3  (BoxState, LuckState, BoxSetting)
Repositories:    3  (BoxStateRepository, LuckStateRepository, BoxSettingRepository)
Controllers:     1  (BoxController — 21 endpoints)
Services:        3  (BoxService, BoxInfoServiceImpl, BoxEquipService)
Config caches:   2  (UnpackConfigCache, LuckUnpackConfigCache)
DB tables:       3  (box_state, luck_state, box_setting)
Config files:    2  (unpack.json, kaixiangdaji.json)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BoxService:      ~1100 lines
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22

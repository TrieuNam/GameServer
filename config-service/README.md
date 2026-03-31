# Config Service

**Version**: 1.0.0  
**Phase**: P0 (Core Infrastructure)  
**Port**: 8888  
**Database**: N/A (File-based)

---

## 📋 Overview

Config Service là **Centralized Configuration Management** — phục vụ toàn bộ file cấu hình game cho tất cả services. Hỗ trợ ETag caching, hot-reload không cần restart service.

### Core Features
- ✅ Serve config files JSON/XML cho toàn hệ thống
- ✅ ETag caching với TTL 60 giây
- ✅ Hỗ trợ classpath và filesystem mode
- ✅ Clear cache API (bảo vệ bằng token)
- ✅ Hot-reload config không cần restart
- ✅ Batch config fetch

---

## 🗂️ Cấu Trúc Thư Mục Config

```
config/
├── config/                        ← Cấu hình server-level
│   ├── local.json                 ← Cấu hình môi trường dev/local
│   ├── cross.json                 ← Cấu hình cross-server (tiểu/trung/đại)
│   ├── dev-query-h02.json         ← Cấu hình query dev
│   ├── openserver.xml             ← Thời gian khai server và hợp server
│   └── .json                      ← (placeholder)
│
├── gameworld/                     ← Config logic game
│   ├── skill/
│   │   ├── single_skill.json      ← Cấu hình active skill (skill_eff + skill_cfg)
│   │   └── passive_skill.json     ← Cấu hình passive skill (passive_cfg, 11943 dòng)
│   ├── monster/
│   │   └── monster.json           ← Thông số quái vật
│   ├── drop/
│   │   └── 2000-2534.xml          ← Bảng drop theo drop_id (253 file XML)
│   ├── item/
│   │   ├── equipment.json         ← Trang bị nhân vật
│   │   ├── equipment_shilian.json ← Trang bị thí luyện
│   │   ├── equipment_angle_cfg.json ← Cấu hình góc trang bị
│   │   ├── gemstone.json          ← Đá quý
│   │   ├── gemstone_drawing.json  ← Vẽ đá quý
│   │   ├── pet_item.json          ← Item thú cưng
│   │   ├── pet_weapon_item.json   ← Vũ khí thú cưng
│   │   ├── scroll_item.json       ← Cuộn sách
│   │   ├── title_item.json        ← Danh hiệu
│   │   ├── inscription_item.json  ← Khắc văn
│   │   ├── harness_item.json      ← Dây cương
│   │   ├── model_item.json        ← Item ngoại hình
│   │   ├── gift.json              ← Gói quà
│   │   ├── other.json             ← Item khác
│   │   ├── block_item.json        ← Item chặn
│   │   ├── debris.json            ← Mảnh vỡ
│   │   ├── expense.json           ← Chi tiêu
│   │   └── wabao_cfg.json         ← Cấu hình wabao
│   ├── globalconfig/
│   │   ├── fault_isolation.json   ← Cách ly lỗi
│   │   ├── hotfixfile.json        ← Hotfix config
│   │   ├── keyconfig.json         ← Key config
│   │   ├── otherconfig.json       ← Config khác
│   │   └── other_config_sample.json
│   ├── logicconfig/               ← Logic gameplay
│   │   ├── roleexp.json           ← Bảng EXP theo cấp (cấp 1-200+)
│   │   ├── shop_cfg.json          ← Cấu hình cửa hàng (1228 entries)
│   │   ├── task_cfg.json          ← Cấu hình nhiệm vụ (11090 dòng)
│   │   ├── arena.json             ← Đấu trường (challenge, score, rank)
│   │   ├── guild.json             ← Bang hội (guild_boss, skill, member)
│   │   ├── escort.json            ← Áp tiêu (ship types, rewards)
│   │   ├── mount.json             ← Tọa kỵ (14739 dòng)
│   │   ├── starmap.json           ← Bản đồ sao (role_star, new_superstar, 4 routes)
│   │   ├── territory.json         ← Lãnh địa (territory items, refresh)
│   │   ├── angel.json             ← Thiên thần (26304 dòng)
│   │   ├── pet.json               ← Thú cưng
│   │   ├── scroll.json            ← Cuộn sách
│   │   ├── inscription.json       ← Khắc văn
│   │   ├── inscription_tower.json ← Tháp khắc văn
│   │   ├── gem_cfg.json           ← Đá quý
│   │   ├── lingzhu.json           ← Linh châu
│   │   ├── knights.json           ← Hiệp sĩ
│   │   ├── orb.json               ← Cầu năng lượng
│   │   ├── shenqi.json            ← Thần khí
│   │   ├── shenyiwu.json          ← Thần dị vật
│   │   ├── fumo.json              ← Phù mô
│   │   ├── maoxian.json           ← Mạo hiểm
│   │   ├── gumo_pagoda.json       ← Tháp cổ ma
│   │   ├── shilian_pagoda.json    ← Tháp thí luyện
│   │   ├── duobao.json            ← Đa bảo
│   │   ├── df_arena.json          ← Đấu trường df
│   │   ├── bag_cfg.json           ← Cấu hình túi đồ
│   │   ├── block.json             ← Chặn
│   │   ├── limit_core.json        ← Giới hạn core
│   │   ├── function_guide.json    ← Hướng dẫn chức năng
│   │   ├── funopen.json           ← Mở chức năng
│   │   ├── score_cfg.json         ← Cấu hình điểm
│   │   ├── roleexp.json           ← Kinh nghiệm nhân vật
│   │   ├── role_name.json         ← Tên nhân vật
│   │   ├── language_cfg.json      ← Ngôn ngữ
│   │   ├── server_mail.json       ← Thư hệ thống
│   │   ├── sundries.json          ← Đồ linh tinh
│   │   ├── item_retrieve.json     ← Thu hồi item
│   │   ├── unpack.json            ← Mở gói
│   │   ├── model_clothes.json     ← Quần áo model
│   │   ├── cloth_shop.json        ← Shop quần áo
│   │   ├── pet_cloth.json         ← Quần áo thú cưng
│   │   ├── pet_cloth_game.json    ← Game quần áo thú cưng
│   │   ├── qiriqiandao.json       ← 7 ngày đến đảo
│   │   ├── jishishangdian.json    ← Cửa hàng thời hạn
│   │   ├── kaixiangdaji.json      ← Khai thương đại cát
│   │   ├── chongzhireward_spid.json ← Phần thưởng nạp tiền
│   │   ├── shop_shenmi.json       ← Cửa hàng bí ẩn
│   │   ├── titile_cfg.json        ← Cấu hình danh hiệu
│   │   ├── xinfutehui.json        ← Ưu đãi tân phúc
│   │   ├── ad_cfg.json            ← Cấu hình quảng cáo
│   │   ├── agent_adapt.json       ← Thích ứng đại lý
│   │   ├── randactivity/          ← Hoạt động ngẫu nhiên
│   │   │   ├── activity_main.json         ← Cấu hình hoạt động chính
│   │   │   ├── richanglibao.json          ← Lì xì hàng ngày
│   │   │   ├── zhoumohaoli.json           ← Phúc lợi cuối tuần
│   │   │   ├── month_card.json            ← Thẻ tháng
│   │   │   ├── leichong.json              ← Tích nạp
│   │   │   ├── lianchongzengli.json       ← Nạp liên tục
│   │   │   ├── shouchong.json             ← Nạp đầu
│   │   │   ├── shouchongzhuanshu.json     ← Nạp đầu chuyên thuộc
│   │   │   ├── zhoumolianchong.json       ← Nạp liên tục cuối tuần
│   │   │   ├── zhoumoleichong.json        ← Tích nạp cuối tuần
│   │   │   ├── dengjijijin.json           ← Quỹ thăng cấp
│   │   │   ├── baoxiangjijin.json         ← Quỹ hộp bảo
│   │   │   ├── baoxiangzhuangyuan.json    ← Trạng nguyên hộp bảo
│   │   │   ├── baozilaile.json            ← Bao tử đến rồi
│   │   │   ├── chaozhixianli.json         ← Lễ siêu giá trị
│   │   │   ├── dongxueduobao.json         ← Đa bảo đông huyết
│   │   │   ├── fazhenshengdian.json       ← Thánh điển pháp trận
│   │   │   ├── gumochengjiu.json          ← Thành tựu cổ ma
│   │   │   ├── jifenzhuanpan.json         ← Vòng quay điểm
│   │   │   ├── jinrifenxiang.json         ← Chia sẻ hôm nay
│   │   │   ├── knight_card.json           ← Thẻ hiệp sĩ
│   │   │   ├── lingdilibao.json           ← Lĩnh địa lì xì
│   │   │   ├── mingwenchengjiu.json       ← Thành tựu minh văn
│   │   │   ├── pingfenjijin.json          ← Quỹ đánh giá
│   │   │   ├── randactivityopencfg.json   ← Cấu hình mở hoạt động
│   │   │   ├── randactivity_cfg.json      ← Cấu hình hoạt động ngẫu nhiên
│   │   │   ├── shangpinhanghui.json       ← Hội thương phẩm
│   │   │   ├── shenqiduobao.json          ← Đa bảo thần khí
│   │   │   ├── tianxuanzhili.json         ← Trí lực thiên tuyển
│   │   │   ├── wuxianzhanling.json        ← Chiến lĩnh vô hạn
│   │   │   ├── xinfubipin.json            ← Bí phẩm tân phúc
│   │   │   ├── xingtushengdian.json       ← Thánh điển hình đồ
│   │   │   ├── xingyunliyu.json           ← Lý ngư hạnh vận
│   │   │   ├── zhuanshulibao.json         ← Lì xì chuyên thuộc
│   │   │   └── zhoumolianchong.json       ← Nạp liên tục cuối tuần
│   │   └── ...
│   ├── monster_group.json         ← Nhóm quái vật
│   ├── audio.json                 ← Cấu hình âm thanh
│   ├── dropmanager.xml            ← Quản lý drop (718 dòng, list path 2000-4422)
│   ├── itemmanager.xml            ← Quản lý item (large)
│   └── battlemonstermanager.xml   ← Quản lý quái chiến đấu (14504 dòng)
│
└── serverconfig/                  ← Cấu hình server infrastructure
    ├── commonconfig.json          ← Cấu hình DB, network ports, timeout
    ├── commonconfig_table.json    ← Bảng cấu hình chung
    ├── commonconfig_comment.txt   ← Giải thích cấu hình
    ├── role_name.json             ← Tên nhân vật server
    └── string.xml                 ← Chuỗi localization
```

---

## 🎯 Flow Hoạt Động

```
[Game Designers] ──cập nhật config files──► [Config Service :8888]
                                                    │
[drop-service / shop-service / gameworld-service] ──► GET /api/config/file?path=xxx
                                                    │
                                            ◄── JSON/XML config data (ETag cached)
```

---

## 🔌 API Endpoints

```
GET  /api/config/file                    - Lấy config file đơn (query: path)
GET  /api/config/batch                   - Lấy nhiều config files (query: path[])
POST /internal/invalidate                - Xóa cache (param: path)
POST /internal/reload                    - Reload tất cả config từ file
GET  /api/c2s/fetch_privacy_notice       - Fetch privacy notice (param: spid)
GET  /api/c2s/user_info                  - User info reporting
```

---

## 📦 API Examples

### Lấy Skill Config
```bash
curl "http://localhost:8888/api/config/file?path=gameworld/skill/single_skill.json"
curl "http://localhost:8888/api/config/file?path=gameworld/skill/passive_skill.json"
```

### Lấy Drop Config
```bash
curl "http://localhost:8888/api/config/file?path=gameworld/drop/2000.xml"
```

### Lấy Server Config
```bash
curl "http://localhost:8888/api/config/file?path=serverconfig/commonconfig.json"
```

### Batch Fetch
```bash
curl "http://localhost:8888/api/config/batch?path=gameworld/logicconfig/roleexp.json&path=gameworld/logicconfig/shop_cfg.json"
```

### Invalidate Cache
```bash
curl -X POST "http://localhost:8888/internal/invalidate?path=gameworld/logicconfig/shop_cfg.json" \
  -H "Authorization: Bearer {admin-token}"
```

### Reload Config
```bash
curl -X POST http://localhost:8888/internal/reload \
  -H "Authorization: Bearer {admin-token}"
```

---

## ⚙️ Configuration

```yaml
server:
  port: 8888

config-service:
  mode: classpath          # classpath hoặc filesystem
  base-path: config        # Thư mục gốc chứa config files
  cache-ttl: 60            # Giây
  admin-token: ${CONFIG_ADMIN_TOKEN}
```

---

## 📊 Chi Tiết Cấu Hình Game

### 🗡️ Skill Config (`gameworld/skill/`)

#### `single_skill.json` — Active Skill
```json
{
  "skill_eff": [
    { "seq": "1", "effect_type": "1", "param1": "100", ... }
  ],
  "skill_cfg": [
    {
      "skill_id": "1", "skill_level": "1",
      "target_side_type": "1", "target_num": "1",
      "is_shanbi": "1",   // Có thể né tránh
      "is_baoji": "1",    // Có thể bạo kích
      "is_lianji": "1",   // Có thể liên kích
      "is_xixue": "1",    // Có thể hút máu
      "is_jiyun": "1",    // Có kỹ năng vận
      "is_fanji": "1",    // Có thể phản kích
      "effect_num": "1",  "effect_1": "1", ...
    }
  ]
}
```

#### `passive_skill.json` — Passive Skill (11943 dòng)
```json
{
  "passive_cfg": [
    {
      "skill_id": "1", "skill_level": "1",
      "skill_desc": "提高宠物6000生命",  // Tăng HP thú cưng
      "att_type": "1",
      "skill_att_type": "1",  // 1=HP, 2=ATK, 3=DEF, 4=SPD, 6=吸血
      "att_num2": "6000", ...
    }
  ]
}
```
> Skill att_type mapping: 1=HP, 2=ATK, 3=DEF, 4=SPD, 6=吸血(hút máu), 8=连击(liên kích), 9=闪避(né tránh), 10=暴击(bạo kích), 11=击晕(choáng), 12=忽视吸血, 13=忽视反击, 14=忽视连击, 15=忽视闪避, 16=忽视暴击, 17=忽视击晕

---

### 👾 Monster Config (`gameworld/`)

#### `battlemonstermanager.xml` — Index quái vật (14504 dòng)
- Liệt kê path `monster/1.xml` → `monster/N.xml`
- Mỗi nhóm comment ID cha (ví dụ `<!-- 111001 -->`)

#### `monster/monster.json` — Thông số quái vật

#### `monster_group.json` — Nhóm quái (dùng trong guild boss, escort...)

---

### 💀 Drop Config (`gameworld/drop/`)
- **253 file XML** (ID 2000 → 4422, bỏ qua 2197-2199 và 2215-2299)
- Được index bởi `dropmanager.xml`

**Cấu trúc mỗi file drop:**
```xml
<drop>
  <drop_id>2000</drop_id>
  <drop_item_prob_list>
    <drop_item_prob>
      <item_id>26659</item_id>
      <is_bind>0</is_bind>
      <prob>250</prob>   <!-- tỷ lệ /10000 -->
      <num>1</num>
      <broadcast>0</broadcast>
    </drop_item_prob>
  </drop_item_prob_list>
</drop>
```

---

### 🛡️ Item Config (`gameworld/item/`)
| File | Nội dung |
|------|----------|
| `equipment.json` | Trang bị nhân vật (ATK/DEF/HP) |
| `equipment_shilian.json` | Trang bị thí luyện |
| `gemstone.json` | Đá quý + thuộc tính |
| `pet_item.json` | Item thú cưng |
| `scroll_item.json` | Cuộn sách |
| `title_item.json` | Danh hiệu |
| `inscription_item.json` | Khắc văn |
| `harness_item.json` | Dây cương ngựa |
| `gift.json` | Gói quà tặng |

---

### 🎮 Logic Config (`gameworld/logicconfig/`)
| File | Mô tả |
|------|-------|
| `roleexp.json` | Bảng EXP cấp 1-200+ (`level` + `exp`) |
| `shop_cfg.json` | Cửa hàng: item_id, giá, quota_type, show_level (1228 entries) |
| `task_cfg.json` | Nhiệm vụ: task_id, next_task_id, condition, reward (11090 dòng) |
| `arena.json` | Đấu trường: score, rank, challenge mechanics, mùa giải |
| `guild.json` | Bang hội: guild_boss, member rules (10123 dòng) |
| `escort.json` | Áp tiêu: ship tiers, intercept rules, rewards (1071 dòng) |
| `mount.json` | Tọa kỵ: up_att (ATK/DEF/HP per level), up_item (14739 dòng) |
| `angel.json` | Thiên thần: upgrade per level (26304 dòng) |
| `starmap.json` | Bản đồ sao: 165 star nodes (4 routes: ATK/DEF/HP/SPD), new_superstar skills |
| `territory.json` | Lãnh địa: refresh rules, item tiers (1403 dòng) |
| `lingzhu.json` | Linh châu mechanics |
| `knights.json` | Hiệp sĩ/thuộc hạ |
| `inscription.json` | Khắc văn system |
| `pet.json` | Thú cưng nâng cấp |
| `unpack.json` | Mở gói trang bị: xác suất level, màu, thuộc tính, phần thưởng cố định, thời trang (7989 dòng) |

---

### 📦 Unpack (Mở Gói) Config — `gameworld/logicconfig/unpack.json`

> Dùng bởi **box-service** (`UnpackConfigCache.java`) — load qua `app.config.unpackPath`

File 7989 dòng với **9 sections** chính:

| Section | Entries | Mô tả |
|---------|---------|-------|
| `random_level` | 1195 | Xác suất level ngẫu nhiên khi mở gói: `level` × `random_level` → `rate` (/10000) |
| `random_color` | 30 | Tỷ lệ màu (độ hiếm) trang bị theo `box_level`: tỷ lệ màu, giá mua thêm lần, thời gian tăng tốc, phần thưởng |
| `other` | 1 (object) | Config tổng thể: số thuộc tính thêm, box tối đa, item tiền tệ/tăng tốc/mở gói, tỷ lệ nhận thời trang |
| `color_att` | 100 | Nhóm thuộc tính theo màu: `att_group`, `att_type`, `att_num_min`, `att_num_max`, `rate` |
| `att_describe` | 25 | Mô tả thuộc tính (hiện để trống `[]`) |
| `auto_unpack` | 21 | Cấu hình tự động mở (hiện để trống `[]`) |
| `shizhuang_rate` | 48 | Tỷ lệ drop thời trang: `seq`, `item_id`, `rate` |
| `fixed_reward` | 32 | Phần thưởng cố định theo thứ tự mở: `box_oder`, `item_id` |
| `getway` | 4 | Cách nhận gói (hiện để trống `[]`) |

**Cấu trúc mẫu:**
```json
{
  "random_level": [
    { "level": "1", "random_level": "1", "rate": "9500" },
    { "level": "1", "random_level": "2", "rate": "400"  },
    { "level": "1", "random_level": "3", "rate": "100"  }
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
  "color_att": [
    { "att_group": "1", "att_type": "6", "att_num_min": "40", "att_num_max": "50", "rate": "1666" }
  ],
  "shizhuang_rate": [
    { "seq": "1", "item_id": "40400", "rate": "400" }
  ],
  "fixed_reward": [
    { "box_oder": "1", "item_id": "1000" },
    { "box_oder": "2", "item_id": "8500" }
  ],
  "att_describe": [],
  "auto_unpack":  [],
  "getway":       []
}
```

> **Lưu ý**: `box-service` còn dùng `kaixiangdaji.json` qua `LuckUnpackConfigCache` (key `app.config.kaixiangPath`).

---

### 📅 Rand Activity Config (`gameworld/logicconfig/randactivity/`)
**31 file hoạt động** chia theo loại:
- **Nạp tiền**: `shouchong`, `leichong`, `lianchongzengli`, `zhoumoleichong`, `zhoumolianchong`
- **Thẻ**: `month_card`, `knight_card`
- **Quỹ tích lũy**: `dengjijijin`, `baoxiangjijin`, `pingfenjijin`
- **Sự kiện daily**: `richanglibao`, `jinrifenxiang`, `zhoumohaoli`
- **Sự kiện đặc biệt**: `xingyunliyu`, `shenqiduobao`, `dongxueduobao`, `fazhenshengdian`
- **IAP ưu đãi**: `xinfubipin`, `xinfutehui`, `chaozhixianli`

---

### 🌐 Server Config (`serverconfig/`)
#### `commonconfig.json` — Database & Network
```json
{
  "IPConfig": {
    "DB_Server_Addr": {
      "DBAccounter": { "Mysql_Addr_IP": "127.0.0.1", "Mysql_Addr_Port": "3306", ... },
      "DBGlobal": { ... }, "DBName": { ... }, "DBRole": { ... }, "DBCross": { ... }
    },
    "Battle_Server_Addr":  { "ListenPort": "55202/55203/55204" },
    "Scene_Server_Addr":   { "ListenPort": "55205" },
    "GameWorld_Server_Addr": { "ListenPort": "55206" (game) + "55207" (cross) },
    "GatewayModule":       { "ListenPort": "55208" }
  },
  "OtherConfig": {
    "SessionKey": "123453443567",
    "TimeOut": { "Gateway_Heartbeat_DeadTime_MS": "300000" }
  }
}
```

#### `config/cross.json` — Cross-server
```json
{
  "CrossServerAddr":       { "Port": "10777" },  // Tiểu cross
  "MiddleCrossServerAddr": { "Port": "10778" },  // Trung cross  
  "BigCrossServerAddr":    { "Port": "10779" }   // Đại cross
}
```

#### `config/openserver.xml` — Thời gian khai server
```xml
<server_real_start_time>2021-12-28 00:26:00</server_real_start_time>
<server_real_combine_time>2013-05-04 10:00:00</server_real_combine_time>
```

---

## 🔧 Business Logic

### Caching Strategy
- Mỗi config key được cache với ETag (MD5 hash của nội dung)
- Client gửi `If-None-Match: {etag}` → 304 Not Modified nếu không thay đổi
- TTL mặc định: **60 giây**
- Manual invalidate bằng API khi cần hot-update

### Config Path Convention
```
gameworld/skill/single_skill.json           ← active skill
gameworld/skill/passive_skill.json          ← passive skill
gameworld/drop/{drop_id}.xml                ← drop table (2000-4422)
gameworld/logicconfig/roleexp.json          ← level exp table
gameworld/logicconfig/shop_cfg.json         ← shop items
gameworld/logicconfig/task_cfg.json         ← tasks
serverconfig/commonconfig.json              ← DB/network
config/cross.json                           ← cross-server addr
config/local.json                           ← local env config
config/openserver.xml                       ← server open time
```

---

## 🚀 Running

```bash
cd GameServer/config-service
mvn clean install
mvn spring-boot:run
```

> ⚠️ Khởi động **SAU eureka-server**, **TRƯỚC tất cả business services**

---

## 🔗 Integration Points

### Services phụ thuộc vào Config Service
| Service | Files sử dụng |
|---------|---------------|
| **drop-service** | `gameworld/drop/*.xml`, `gameworld/dropmanager.xml` |
| **shop-service** | `gameworld/logicconfig/shop_cfg.json`, `shop_shenmi.json`, `cloth_shop.json` |
| **gameworld-service** | `gameworld/battlemonstermanager.xml`, `monster/monster.json`, `monster_group.json` |
| **arena-service** | `gameworld/logicconfig/arena.json`, `df_arena.json` |
| **guild-service** | `gameworld/logicconfig/guild.json` |
| **escort-service** | `gameworld/logicconfig/escort.json` |
| **mount-service** | `gameworld/logicconfig/mount.json` |
| **angel-service** | `gameworld/logicconfig/angel.json` |
| **task-service** | `gameworld/logicconfig/task_cfg.json` |
| **starmap-service** | `gameworld/logicconfig/starmap.json` |
| **territory-service** | `gameworld/logicconfig/territory.json` |
| **lingzhu-service** | `gameworld/logicconfig/lingzhu.json` |
| **knights-service** | `gameworld/logicconfig/knights.json` |
| **activity-service** | `gameworld/logicconfig/randactivity/*.json` |
| **role-service** | `gameworld/logicconfig/roleexp.json`, `serverconfig/commonconfig.json` |
| **gem-service** | `gameworld/item/gemstone.json`, `logicconfig/gem_cfg.json` |
| **pet-service** | `gameworld/item/pet_item.json`, `logicconfig/pet.json` |
| **scroll-service** | `gameworld/item/scroll_item.json`, `logicconfig/scroll.json` |
| **gateway-service** | `config/cross.json`, `serverconfig/commonconfig.json` |
| **box-service** | `gameworld/logicconfig/unpack.json`, `gameworld/logicconfig/kaixiangdaji.json` |

---

## ⚠️ Config Hotfix Guidelines (theo Skill Memory)

> Theo `gameserver-skill-memory` — luôn đọc file trước khi sửa, chỉ sửa tối thiểu.

1. **Thay đổi gameplay values** (exp, damage, drop rate):
   - Edit file JSON/XML trực tiếp
   - Gọi `POST /internal/invalidate?path={file_path}` để clear cache
   - Không cần restart service

2. **Thêm config key mới** (drop_id mới, skill mới):
   - Thêm file mới vào thư mục tương ứng
   - Nếu là drop XML: thêm `<path>` vào `dropmanager.xml`
   - Gọi `POST /internal/reload` để reload toàn bộ

3. **Cross-service impact check**:
   - `single_skill.json` → battleserver-service, gameworld-service
   - `shop_cfg.json` → shop-service, wallet-service (giá)
   - `roleexp.json` → role-service, activity-service (điều kiện level)
   - `commonconfig.json` → tất cả services có DB connection

---

## 📊 Statistics

```
Config Directories:     3 (config/, gameworld/, serverconfig/)
JSON Files (gameworld): 70+ files
XML Files (drop):       253 files (ID 2000-4422)
XML Files (monster):    ~ file
XML Manager Files:      3 (dropmanager, itemmanager, battlemonstermanager)
Active Skill entries:   1 (single_skill.json - template)
Passive Skill entries:  ~2000+ (passive_skill.json - 11943 lines)
Task entries:           ~500+ (task_cfg.json - 11090 lines)
Activity configs:       31 files
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL Config Files:     ~380+ files
```

---

## 🔑 Config Key Quick Reference

```
Skill:        gameworld/skill/single_skill.json
              gameworld/skill/passive_skill.json
Drop:         gameworld/drop/{2000-4422}.xml
Monster:      gameworld/monster/monster.json
              gameworld/battlemonstermanager.xml
Item:         gameworld/item/{equipment|gemstone|pet_item|...}.json
Role/Level:   gameworld/logicconfig/roleexp.json
Shop:         gameworld/logicconfig/shop_cfg.json
Task:         gameworld/logicconfig/task_cfg.json
Arena:        gameworld/logicconfig/arena.json
Guild:        gameworld/logicconfig/guild.json
Mount:        gameworld/logicconfig/mount.json
Starmap:      gameworld/logicconfig/starmap.json
Territory:    gameworld/logicconfig/territory.json
Angel:        gameworld/logicconfig/angel.json
Activity:     gameworld/logicconfig/randactivity/activity_main.json
Unpack:       gameworld/logicconfig/unpack.json
              gameworld/logicconfig/kaixiangdaji.json
Server DB:    serverconfig/commonconfig.json
Cross:        config/cross.json
OpenTime:     config/openserver.xml
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22  
**Analyzed by**: gameserver-skill-memory agent

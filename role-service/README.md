# Role Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8410 · **gRPC**: 9410  
**Database**: `game_role`

---

## 📋 Overview

Role Service quản lý **nhân vật (character/role)** của người chơi — đây là entity trung tâm của toàn bộ game. Lưu trữ thông tin nhân vật, cấp độ, exp, combat power, cài đặt hệ thống, và cung cấp gRPC server cho các services khác query thông tin role. Load config từ **config-service** với ETag cache.

### Core Features
- ✅ CRUD nhân vật (tạo, đọc, đổi tên)
- ✅ Tăng EXP và level up
- ✅ Tính combat power tổng hợp (từ equip, skills, pet, mount, angel, v.v.)
- ✅ Cấu hình nhân vật (RoleConfigCache: `roleexp.json`, `role_name.json`)
- ✅ Config kỹ năng (SkillConfigCache: `single_skill.json`, `passive_skill.json`)
- ✅ Tự động load config từ **config-service** qua Feign + ETag cache (TTL 60s)
- ✅ Retry khi khởi động nếu config-service chưa sẵn sàng
- ✅ Quảng cáo & reward (AdvertisementController)
- ✅ Mail in-game cơ bản (tích hợp mail-service)
- ✅ Cài đặt hệ thống người chơi (SettingsController)
- ✅ Hệ thống kỹ năng chủ động (SkillController) — MsgId 1470/1471
- ✅ Hệ thống thiên phú kỹ năng (SkillController) — MsgId 1480/1481
- ✅ Kafka producer (BagEventProducer khi level up)
- ✅ gRPC server cho inter-service queries

---

## 🎯 Flow Hoạt Động

```
[User đăng nhập lần đầu]
        │
        ▼
POST /api/role  ──► Tạo Role entity  ──► db_role
                           │
              ◄── roleId (dùng cho toàn bộ game)

[Tăng EXP từ combat/quest]
battle-service / task-service
        │
        ▼ Feign
POST /api/role/exp/add  { roleId, exp }
        │
        ▼
Role.exp += amount → Check level up → Update db_role
        │
        ▼ Kafka
BagEventProducer → bag-service (nếu level up reward)
```

---

## 🗄️ Database Schema

### role
```sql
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    level INT DEFAULT 1,
    exp BIGINT DEFAULT 0,
    vip_level INT DEFAULT 0,
    combat_power BIGINT DEFAULT 0,
    gender INT DEFAULT 1,           -- 1=Male, 2=Female
    avatar_id INT DEFAULT 1,
    server_id INT,
    created_at DATETIME NOT NULL,
    last_login_at DATETIME,
    total_recharge INT DEFAULT 0
);
```

### role_system_setting
```sql
CREATE TABLE role_system_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL UNIQUE,
    settings JSON,                  -- Cài đặt hệ thống người chơi
    updated_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
GET   /api/role/{roleId}            - Lấy thông tin nhân vật
GET   /api/role/by-user/{userId}    - Lấy role theo userId
POST  /api/role                     - Tạo nhân vật mới
POST  /api/role/exp/add             - Thêm EXP (body: roleId, exp)
POST  /api/role/{roleId}/rename     - Đổi tên nhân vật
POST  /api/role/{roleId}/wxinfo     - Cập nhật thông tin WeChat (tên + avatar)
GET   /api/role/{roleId}/combat-power - Lấy tổng combat power
GET   /api/role/{roleId}/basic-info - Lấy thông tin cơ bản
GET   /api/other-role/{uid}         - Xem thông tin nhân vật khác
POST  /api/role/settings            - Cập nhật cài đặt hệ thống
POST  /api/ads/claim                - Nhận phần thưởng quảng cáo
POST  /api/mail/list                - Danh sách mail (body: userId)
GET   /api/mail/{userId}/{mailId}   - Chi tiết mail và đánh dấu đã đọc
POST  /api/mail/{userId}/{mailId}/delete - Xóa mail
POST  /api/mail/{userId}/{mailId}/fetch  - Nhận phần thưởng mail

# ─── Kỹ năng (Skill) — MsgId CS:1470 SC:1471 ────────────────────────────
GET   /api/skill/{roleId}                    - Lấy toàn bộ kỹ năng của nhân vật
POST  /api/skill/{roleId}/learn              - Học / nâng cấp một kỹ năng
                                               body: { "skillId": <int>, "reqType": 1 }
POST  /api/skill/{roleId}/one-key-level-up   - Nâng cấp tất cả kỹ năng 1 cấp

# ─── Thiên phú (Talent) — MsgId CS:1480 SC:1481 ─────────────────────────
GET   /api/talent/{roleId}                   - Lấy toàn bộ thiên phú của nhân vật
POST  /api/talent/{roleId}/learn             - Học / nâng cấp một thiên phú
                                               body: { "skillId": <int>, "reqType": 1 }
```

---

## 📦 API Examples

### Tạo Nhân Vật
```bash
curl -X POST http://localhost:8410/api/role \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 12345,
    "name": "HeroWarrior",
    "gender": 1,
    "avatarId": 3,
    "serverId": 1
  }'
```

### Thêm EXP
```bash
curl -X POST http://localhost:8410/api/role/67890/exp/add \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000}'
# Response: { "roleId": 67890, "level": 15, "exp": 12500, "levelUp": true }
```

### Lấy Combat Power
```bash
curl http://localhost:8410/api/role/67890/combat-power
```

---

## 🔧 Business Logic

### Level Up
- Tham chiếu `RoleConfigCache` (level_table) để biết exp cần cho mỗi level
- Khi level up: unlock nội dung mới, gửi Kafka event để trao thưởng
- Level cap: tùy cấu hình server

### Load Config Từ Config Service
- `RoleConfigCache` gọi `config-service` qua Feign endpoint: `GET /api/config/file?path=...`
- Dùng `If-None-Match` / `ETag` để chỉ tải lại khi file thay đổi (304 nếu không đổi)
- Khi startup: retry theo `role.config.startup-retry-*`; nếu chưa kết nối được thì dùng default cache an toàn và scheduler sẽ refresh lại

### Skill / Talent Config (SkillConfigCache)
- `SkillConfigCache` load `single_skill.json` (active skill) + `passive_skill.json` (talent) từ `config-service`
- Cung cấp: `isValidSkillId`, `isValidTalentId`, `getSkillMaxLevel`, `getTalentMaxLevel`
- **Fail-open**: nếu config chưa tải (config-service chưa ready), mọi skillId đều pass validation
- Max level mặc định: `skill.config.default-max-level` (default: **20**)
- Quy tắc one-key level up: tăng **tất cả** kỹ năng có `level < maxLevel` lên 1 cấp

### Error Codes (SkillErrorCodes)
| Code | Tên | Mô tả |
|------|-----|-------|
| `0` | `OK` | Thao tác thành công |
| `1` | `ROLE_NOT_FOUND` | Nhân vật không tồn tại |
| `2` | `INVALID_SKILL_ID` | skillId không hợp lệ hoặc không có trong config |
| `3` | `ALREADY_MAX_LEVEL` | Kỹ năng / thiên phú đã đạt cấp tối đa |
| `4` | `ROLE_LEVEL_TOO_LOW` | Nhân vật chưa đủ cấp (reserved) |
| `5` | `INSUFFICIENT_RESOURCE` | Không đủ tài nguyên (reserved) |
| `6` | `SKILL_NOT_UNLOCKED` | Kỹ năng chưa được mở khóa (reserved) |

### Combat Power
- Tổng hợp từ nhiều sources: equip, skills, pet, mount, angel, artifact, rune, v.v.
- Được cache và cập nhật khi có thay đổi từ các sub-systems

### Flyway Migrations
- V1: init (tạo bảng ban đầu)
- V2: roleId bigint (migration schema)

---

## 🚀 Running

```bash
cd GameServer/role-service
mvn clean install
mvn spring-boot:run
```

> Khuyến nghị startup order (local/dev):
> 1) `eureka-server`
> 2) `config-service`
> 3) `role-service`

### Config Keys Từ config-service

| Config | Path | TTL | Mô tả |
|--------|------|-----|-------|
| `roleexp.json` | `gameworld/logicconfig/roleexp.json` | 60s | Level exp table: `level` → `exp` tích lũy |
| `role_name.json` | `gameworld/logicconfig/role_name.json` | 60s | Role name suggestions |
| `single_skill.json` | `gameworld/skill/single_skill.json` | 60s | Active skill config (skill_id → max level) |
| `passive_skill.json` | `gameworld/skill/passive_skill.json` | 60s | Passive talent config (skill_id → max level) |

**application.yml:**
```yaml
role:
  config:
    roleexp-path: ${ROLE_ROLEEXP_PATH:gameworld/logicconfig/roleexp.json}
    rolename-path: ${ROLE_ROLENAME_PATH:gameworld/logicconfig/role_name.json}
    refresh-interval-ms: 60000
    startup-retry-count: 6
    startup-retry-delay-ms: 2000

skill:
  config:
    single-skill-path: ${SKILL_SINGLE_SKILL_PATH:gameworld/skill/single_skill.json}
    passive-skill-path: ${SKILL_PASSIVE_SKILL_PATH:gameworld/skill/passive_skill.json}
    default-max-level: ${SKILL_DEFAULT_MAX_LEVEL:20}
    refresh-interval-ms: ${SKILL_CONFIG_REFRESH_MS:60000}
```


---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **config-service** | `GET /api/config/file?path=gameworld/logicconfig/roleexp.json` | Load level exp table (ETag cached, TTL 60s) |
| **config-service** | `GET /api/config/file?path=gameworld/logicconfig/role_name.json` | Load role name suggestions |
| **config-service** | `GET /api/config/file?path=gameworld/skill/single_skill.json` | Load active skill config (SkillConfigCache) |
| **config-service** | `GET /api/config/file?path=gameworld/skill/passive_skill.json` | Load passive talent config (SkillConfigCache) |

### Được gọi bởi (gRPC port 9410)
| Caller | Mục đích |
|--------|---------|
| **webSocket-server** | Lấy role info, tính combat power |
| **box-service** | Lấy player level để roll equip |
| **task-service** | Check role level, grant EXP |
| **arena-service** | Lấy role info, tính rank |
| **guild-service** | Lấy role info cho guild management |
| **leaderboard-service** | Lấy role info cho ranking |
| **mail-service** | Lấy role info khi gửi mail |

### Kafka Producer
- **BagEventProducer**: Gửi sự kiện khi level up hoặc có reward

### Được gọi bởi (Feign/REST)
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **box-service** | `GET /api/role/{roleId}` | Lấy player level khi roll equip |
| **equip-service** | `GET /api/role/{roleId}` | (fallback) Lấy role info |
| **task-service**, **arena-service**, **shop-service** | REST endpoints | Grant EXP, check level, v.v. |

---

## 📊 Statistics

```
Entities:        6 classes (Role, Mail, RoleSystemSetting, AdRewardClaim, RoleSkill, RoleTalent)
Repositories:    6 interfaces (+ RoleSkillRepository, RoleTalentRepository)
Controllers:     6 classes (RoleController, MailController, AdController, SettingsController, SkillController, v.v.)
Services:        6 classes (RoleService, MailService, AdService, SkillService, RoleConfigCache, SkillConfigCache)
Config caches:   2 (RoleConfigCache, SkillConfigCache — ETag cached, 60s TTL)
gRPC Server:     RoleServiceGrpcImpl
Kafka Producer:  BagEventProducer
DB tables:       5 (role, role_system_setting, mail, role_skill, role_talent)
Feign clients:   1 (ConfigServiceFeign)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~1,700 lines
```

---

**Status**: ✅ Production Ready  
**Last Updated**: 2026-03-22

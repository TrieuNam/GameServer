# Angel Service

**Version**: 1.0.0
**Phase**: P2 (Combat, World & Social)
**Port**: 8090 · **gRPC**: 9089
**Database**: `game_angel`

---

## 📋 Tổng quan

Angel Service quản lý hệ thống Angel/Wing companion để tăng cường sức mạnh và ngoại hình nhân vật. Người chơi có thể mở khóa, nâng cấp level, nâng cấp grade, và trang bị angels để nhận bonus chỉ số và tùy chỉnh ngoại hình. Service xử lý tiến độ angel qua nhiều hệ thống: levels, grades, stars, evolution, skills, blessing, và nâng cấp appearance.

### Core Features
- ✅ Quản lý Angel: Mở khóa, nâng level, nâng grade angels
- ✅ Trang bị: Trang bị/hiển thị angels, chuyển đổi angel đang dùng
- ✅ Skills: 4 skill slots với hệ thống nâng cấp
- ✅ Nâng cấp Star: Tầng sức mạnh bổ sung (tối đa 12 stars)
- ✅ Evolution: Các giai đoạn đột phá (tối đa 5 stages)
- ✅ Hệ thống Blessing: Tích lũy điểm blessing để nhận bonus
- ✅ Appearance: Skins và appearances tùy chỉnh với các level nâng cấp
- ✅ Đổi tên: Tên angel tùy chỉnh (tốn gold)
- ✅ Transform: Áp dụng appearance skins

---

## 🎯 Flow Hoạt Động

```
[Player unlocks new angel]
POST /api/angel/{roleId}/activate/{angelId}
        │
        ▼
AngelService.unlockAngel()
├── Create new Angel entity
├── Set initial stats (level 0, grade 0)
└── Save to angel table

[Player levels up angel]
POST /api/angel/{roleId}/levelup
        │
        ▼
AngelService.levelUpAngel()
├── Check current level < max (100)
├── Consume exp materials from bag
├── Increase level and exp
└── Update angel stats

[Player equips angel]
POST /api/angel/{roleId}/switch/{angelId}
        │
        ▼
AngelService.equipAngel()
├── Unequip current active angel
├── Set new angel as active
└── Return success
```

---

## 🗄️ Database Schema

### angel
```sql
CREATE TABLE angel (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    angel_index     INT NOT NULL,
    angel_id        INT NOT NULL,
    level           INT NOT NULL DEFAULT 0,
    grade           INT NOT NULL DEFAULT 0,
    exp             BIGINT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    is_equipped     BOOLEAN NOT NULL DEFAULT FALSE,
    star_level      INT NOT NULL DEFAULT 0,
    skill1_id       INT,
    skill1_level    INT NOT NULL DEFAULT 0,
    skill2_id       INT,
    skill2_level    INT NOT NULL DEFAULT 0,
    skill3_id       INT,
    skill3_level    INT NOT NULL DEFAULT 0,
    skill4_id       INT,
    skill4_level    INT NOT NULL DEFAULT 0,
    appearance_id   INT,
    appearance_level INT NOT NULL DEFAULT 0,
    evolution_stage INT NOT NULL DEFAULT 0,
    blessing_points BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(32),
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL,
    UNIQUE KEY uk_user_angel (user_id, angel_index),
    INDEX idx_user_id (user_id)
);
```

---

## 🔌 API Endpoints

```
GET   /api/angel/{roleId}                  - Get all angel data
POST  /api/angel/{roleId}/levelup          - Level up angel
POST  /api/angel/{roleId}/gradeup/{angelId} - Grade up angel
POST  /api/angel/{roleId}/activate/{angelId} - Activate/unlock new angel
POST  /api/angel/{roleId}/switch/{angelId} - Switch active angel
POST  /api/angel/{roleId}/skill/upgrade    - Upgrade skill
POST  /api/angel/{roleId}/appearance-upgrade - Upgrade appearance level
POST  /api/angel/{roleId}/blessing/{angelId} - Add blessing points
POST  /api/angel/{roleId}/transform         - Transform angel appearance (skin)
POST  /api/angel/{roleId}/rename            - Rename angel
```

---

## 📦 API Examples

### Lấy Dữ Liệu Angel
```bash
curl http://localhost:8090/api/angel/player123
# Response: {"success": true, "angels": [...]}
```

### Mở Khóa Angel Mới
```bash
curl -X POST http://localhost:8090/api/angel/player123/activate/101
# Response: {"success": true, "angelData": {...}}
```

### Nâng Level Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/levelup \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101}'
# Response: {"success": true, "newLevel": 5, "newExp": 1200}
```

### Nâng Grade Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/gradeup/101
# Response: {"success": true, "newGrade": 2, "angelId": 101}
```

### Chuyển Đổi Angel Đang Dùng
```bash
curl -X POST http://localhost:8090/api/angel/player123/switch/102
# Response: {"success": true, "activeAngelId": 102}
```

### Nâng Cấp Skill
```bash
curl -X POST http://localhost:8090/api/angel/player123/skill/upgrade \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "skillId": 1001}'
# Response: {"success": true, "angelId": 101}
```

### Đổi Tên Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/rename \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "newName": "Phoenix"}'
# Response: {"success": true, "newName": "Phoenix", "cost": 10000}
```

### Transform Appearance
```bash
curl -X POST http://localhost:8090/api/angel/player123/transform \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "transformId": 5001}'
# Response: {"success": true, "transformId": 5001}
```

### Nâng Cấp Appearance Level
```bash
curl -X POST http://localhost:8090/api/angel/player123/appearance-upgrade \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "targetLevel": 3}'
# Response: {"success": true, "angelId": 101, "newLevel": 3}
```

---

## 🔧 Business Logic

### Hệ Thống Level
- **Max Level**: 100
- Tăng chỉ số cơ bản (HP, ATK, DEF)
- Cần exp items để nâng level
- Level cao hơn mở khóa nhiều tính năng hơn

### Hệ Thống Grade
- **Max Grade**: 10
- Tầng chất lượng với hệ số nhân chỉ số
- Cần vật liệu đặc biệt để nâng grade
- Tăng đáng kể tất cả chỉ số

### Hệ Thống Star
- **Max Stars**: 12
- Tầng nâng cấp bổ sung trên level/grade
- Mỗi star tăng sức mạnh đáng kể
- Cần vật liệu hiếm

### Hệ Thống Evolution
- **Max Stages**: 5
- Các giai đoạn đột phá cho boost sức mạnh lớn
- Mở khóa appearances và skills mới
- Cần evolution stones

### Hệ Thống Skill
- **Slots**: 4 skills (3 active + 1 passive)
- **Max Skill Level**: 10
- Mỗi skill cung cấp khả năng độc đáo và bonus chỉ số
- Chi phí nâng cấp tăng theo level

### Hệ Thống Blessing
- Tích lũy điểm blessing theo thời gian
- Cung cấp bonus thụ động liên tục
- Có thể tăng qua sự kiện
- Điểm không bao giờ giảm

### Hệ Thống Appearance
- Skins tùy chỉnh (transform)
- Appearance level cung cấp chỉ số bổ sung
- Nhiều tùy chọn appearance mỗi angel
- Tùy chỉnh ngoại hình độc lập với sức mạnh

### Hệ Thống Đổi Tên
- Tên tùy chỉnh tốn 10,000 gold
- Tối đa 32 ký tự
- Có thể đổi tên nhiều lần

---

## 🚀 Running

```bash
cd GameServer/angel-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **config-service** | (potential) | Load angel.json configuration |
| **role-service** | (potential) | Update character attributes |
| **bag-service** | (potential) | Consume materials for upgrades |
| **wallet-service** | (potential) | Handle gold costs (rename, upgrades) |

### Được gọi bởi
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **webSocket-server** | REST API / gRPC | Angel operations, upgrades |
| **role-service** | (potential) | Fetch angel stats for power calculation |

### Protocol Messages
- **MsgID 2130**: Angel request (CSAngelReq)
- **MsgID 2131**: Angel info (SCAngelInfo)
- **MsgID 2132**: Angel operation result (SCAngelOpRet)

### Kafka Integration
- Produces angel state change events
- Bootstrap servers: localhost:29092

---

## 📊 Statistics

```
Entities:        1 class (Angel)
Repositories:    1 interface
Services:        1 class (AngelService)
Controllers:     1 class (AngelController)
DB tables:       1 (angel)
gRPC:            Port 9089
Kafka:           Producer (angel events)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready
**Last Updated**: 2026-03-30

# Angel Service

**Version**: 1.0.0
**Phase**: P2 (Combat, World & Social)
**Port**: 8090 · **gRPC**: 9089
**Database**: `game_angel`

---

## 📋 Overview

Angel Service manages the Angel/Wing companion system for character enhancement and appearance. Players can unlock, level up, grade up, and equip angels that provide stat bonuses and visual customization. The service handles angel progression through multiple systems: levels, grades, stars, evolution, skills, blessing, and appearance upgrades.

### Core Features
- ✅ Angel Management: Unlock, level up, grade up angels
- ✅ Equipment: Equip/display angels, switch active angel
- ✅ Skills: 4 skill slots with upgrade system
- ✅ Star Upgrade: Additional power tier (max 12 stars)
- ✅ Evolution: Breakthrough stages (max 5 stages)
- ✅ Blessing System: Accumulate blessing points for bonuses
- ✅ Appearance: Customizable skins and appearances with upgrade levels
- ✅ Rename: Custom angel names (costs gold)
- ✅ Transform: Apply appearance skins

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

### Get Angel Data
```bash
curl http://localhost:8090/api/angel/player123
# Response: {"success": true, "angels": [...]}
```

### Unlock New Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/activate/101
# Response: {"success": true, "angelData": {...}}
```

### Level Up Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/levelup \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101}'
# Response: {"success": true, "newLevel": 5, "newExp": 1200}
```

### Grade Up Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/gradeup/101
# Response: {"success": true, "newGrade": 2, "angelId": 101}
```

### Switch Active Angel
```bash
curl -X POST http://localhost:8090/api/angel/player123/switch/102
# Response: {"success": true, "activeAngelId": 102}
```

### Upgrade Skill
```bash
curl -X POST http://localhost:8090/api/angel/player123/skill/upgrade \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "skillId": 1001}'
# Response: {"success": true, "angelId": 101}
```

### Rename Angel
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

### Upgrade Appearance Level
```bash
curl -X POST http://localhost:8090/api/angel/player123/appearance-upgrade \
  -H "Content-Type: application/json" \
  -d '{"angelId": 101, "targetLevel": 3}'
# Response: {"success": true, "angelId": 101, "newLevel": 3}
```

---

## 🔧 Business Logic

### Level System
- **Max Level**: 100
- Increases base stats (HP, ATK, DEF)
- Requires exp items to level up
- Higher levels unlock more features

### Grade System
- **Max Grade**: 10
- Quality tier with stat multiplier
- Requires special materials for grade up
- Significantly boosts all stats

### Star System
- **Max Stars**: 12
- Additional upgrade tier on top of level/grade
- Each star increases power substantially
- Requires rare materials

### Evolution System
- **Max Stages**: 5
- Breakthrough stages for major power boost
- Unlocks new appearances and skills
- Requires evolution stones

### Skill System
- **Slots**: 4 skills (3 active + 1 passive)
- **Max Skill Level**: 10
- Each skill provides unique abilities and stat bonuses
- Upgrade costs increase with level

### Blessing System
- Accumulate blessing points over time
- Provides continuous passive bonuses
- Can be boosted through events
- Points never decrease

### Appearance System
- Customizable skins (transform)
- Appearance level provides additional stats
- Multiple appearance options per angel
- Visual customization independent of power

### Naming System
- Custom names cost 10,000 gold
- Max 32 characters
- Can rename multiple times

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

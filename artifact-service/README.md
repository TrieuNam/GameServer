# Artifact Service

**Version**: 1.0.0
**Phase**: P2 (Combat, World & Social)
**Port**: 8091 · **gRPC**: 9087
**Database**: `game_artifact`

---

## 📋 Overview

Artifact Service manages the Divine Artifact/Weapon (ShenQi 神器) system for legendary equipment. Players can unlock, upgrade, and customize powerful divine weapons with multiple progression systems including level, grade, refinement, awakening, blessing tiers, and random attributes. Includes a gacha draw system for acquiring new artifacts.

### Core Features
- ✅ Artifact Management: Unlock, level up, grade up divine weapons
- ✅ Equipment System: Equip/unequip artifacts
- ✅ Refinement: Advanced upgrade system (精炼, max level 15)
- ✅ Awakening: Breakthrough stages (觉醒, max stage 7)
- ✅ Soul Power: Accumulate soul power for bonuses (魂力)
- ✅ Divine Essence: Special currency for upgrades (神性精华)
- ✅ Blessing System: Tier-based blessing upgrades (祝福, max tier 10)
- ✅ Random Attributes: 4 attribute slots with refresh/lock system
- ✅ Gacha Draw: Single and 10-pull draws with pity system
- ✅ Skill System: 3 skill slots with upgrades
- ✅ Combat Power Calculation

---

## 🎯 Flow Hoạt Động

```
[Player unlocks artifact via gacha]
POST /api/artifact/{roleId}/draw
        │
        ▼
ArtifactService.draw()
├── Deduct cost (gold/diamond/ticket)
├── Roll random artifact with quality
├── Save draw record
└── Create new Artifact entity

[Player levels up artifact]
POST /api/artifact/{roleId}/levelup
        │
        ▼
ArtifactService.levelUpArtifact()
├── Check materials available
├── Consume exp items
├── Increase level (max 100)
└── Update artifact stats

[Player refines artifact]
POST /api/artifact/{roleId}/refine
        │
        ▼
ArtifactService.refineArtifact()
├── Check refinement materials
├── Consume materials
├── Increase refinement level (max 15)
└── Apply stat multiplier
```

---

## 🗄️ Database Schema

### artifact
```sql
CREATE TABLE artifact (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL,
    artifact_index   INT NOT NULL,
    artifact_id      INT NOT NULL,
    level            INT NOT NULL DEFAULT 0,
    grade            INT NOT NULL DEFAULT 0,
    exp              BIGINT NOT NULL DEFAULT 0,
    is_active        BOOLEAN NOT NULL DEFAULT FALSE,
    is_equipped      BOOLEAN NOT NULL DEFAULT FALSE,
    refinement_level INT NOT NULL DEFAULT 0,
    awakening_stage  INT NOT NULL DEFAULT 0,
    soul_power       BIGINT NOT NULL DEFAULT 0,
    divine_essence   BIGINT NOT NULL DEFAULT 0,
    attr1_type       INT,
    attr1_value      BIGINT,
    attr2_type       INT,
    attr2_value      BIGINT,
    attr3_type       INT,
    attr3_value      BIGINT,
    attr4_type       INT,
    attr4_value      BIGINT,
    blessing_tier    INT NOT NULL DEFAULT 0,
    skill1_level     INT NOT NULL DEFAULT 0,
    skill2_level     INT NOT NULL DEFAULT 0,
    skill3_level     INT NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL,
    UNIQUE KEY uk_user_artifact (user_id, artifact_index),
    INDEX idx_user_id (user_id)
);
```

### artifact_draw_record
```sql
CREATE TABLE artifact_draw_record (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    draw_type      INT NOT NULL,        -- 1=single, 10=ten-pull
    artifact_id    INT NOT NULL,
    quality        INT NOT NULL,        -- 1=common, 2=rare, 3=epic, 4=legendary, 5=mythic
    is_guaranteed  BOOLEAN NOT NULL DEFAULT FALSE,
    draw_timestamp DATETIME NOT NULL,
    cost_type      INT NOT NULL,        -- 1=gold, 2=diamond, 3=ticket
    cost_amount    BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_timestamp (user_id, draw_timestamp)
);
```

---

## 🔌 API Endpoints

```
# Query Endpoints
GET    /api/artifact/{roleId}                           - Get all artifacts
GET    /api/artifact/{roleId}/{artifactIndex}           - Get specific artifact
GET    /api/artifact/{roleId}/draw-records              - Get gacha draw history
GET    /api/artifact/{roleId}/{artifactIndex}/power     - Get combat power
GET    /api/artifact/{roleId}/{artifactIndex}/canlevelup - Check can level up
GET    /api/artifact/{roleId}/{artifactIndex}/cangradeup - Check can grade up
GET    /api/artifact/{roleId}/{artifactIndex}/canrefine  - Check can refine
GET    /api/artifact/{roleId}/{artifactIndex}/canawaken  - Check can awaken

# Mutation Endpoints
POST   /api/artifact/{roleId}/unlock         - Unlock new artifact
POST   /api/artifact/{roleId}/levelup        - Level up artifact
POST   /api/artifact/{roleId}/gradeup        - Grade up artifact
POST   /api/artifact/{roleId}/equip          - Equip artifact
DELETE /api/artifact/{roleId}/equip          - Unequip artifact
POST   /api/artifact/{roleId}/refine         - Refine artifact (精炼)
POST   /api/artifact/{roleId}/awaken         - Awaken artifact (觉醒)
POST   /api/artifact/{roleId}/soulpower      - Add soul power
POST   /api/artifact/{roleId}/essence        - Add divine essence
POST   /api/artifact/{roleId}/blessing       - Upgrade blessing tier
POST   /api/artifact/{roleId}/refresh        - Refresh random attributes
POST   /api/artifact/{roleId}/draw           - Gacha draw (single/10-pull)
POST   /api/artifact/{roleId}/upgrade-skill  - Upgrade skill
```

---

## 📦 API Examples

### Get All Artifacts
```bash
curl http://localhost:8091/api/artifact/player123
```

### Unlock Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/unlock?artifactId=3001"
```

### Level Up Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/levelup?artifactIndex=1"
```

### Grade Up Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/gradeup?artifactIndex=1"
```

### Equip Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/equip?artifactIndex=1"
```

### Refine Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/refine?artifactIndex=1"
```

### Awaken Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/awaken?artifactIndex=1"
```

### Gacha Draw
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/draw?drawType=1"
# drawType: 1=single, 10=ten-pull
```

### Get Draw Records
```bash
curl http://localhost:8091/api/artifact/player123/draw-records
```

### Get Combat Power
```bash
curl "http://localhost:8091/api/artifact/player123/1/power"
```

---

## 🔧 Business Logic

### Level System
- **Max Level**: 100
- Increases base stats progressively
- Requires exp materials to upgrade
- Higher levels unlock more features

### Grade System
- **Max Grade**: 10
- Quality tiers with significant stat multipliers
- Requires rare materials for grade up
- Each grade substantially boosts power

### Refinement System (精炼)
- **Max Refinement Level**: 15
- Advanced upgrade on top of level/grade
- Each level provides additional stat bonus
- Requires refinement stones

### Awakening System (觉醒)
- **Max Awakening Stage**: 7
- Breakthrough stages for major power boost
- Unlocks new abilities and appearances
- Requires awakening materials

### Blessing System (祝福)
- **Max Blessing Tier**: 10
- Progressive tier-based upgrades
- Each tier provides cumulative bonuses
- Requires blessing materials or divine essence

### Random Attribute System
- **4 Attribute Slots**: Each with type and value
- Refresh system to reroll attributes
- Lock system to preserve desired attributes
- Attribute pool based on artifact quality

### Soul Power (魂力)
- Accumulated resource from various activities
- Provides passive bonuses
- Can be used for special upgrades
- Never decreases

### Divine Essence (神性精华)
- Premium/special currency for upgrades
- Used for high-tier operations
- Can be obtained from events or decomposition

### Gacha System
- **Single Draw**: Draw one artifact
- **Ten Pull**: Draw 10 artifacts with guaranteed rare+
- Pity system for guaranteed legendary
- Quality tiers: Common, Rare, Epic, Legendary, Mythic
- Draw records tracked for history

### Skill System
- **3 Skill Slots**: skill1, skill2, skill3
- Each skill upgradable independently
- Skills provide combat abilities and bonuses

---

## 🚀 Running

```bash
cd GameServer/artifact-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **config-service** | (potential) | Load shenqi.json configuration |
| **role-service** | (potential) | Update character attributes |
| **bag-service** | (potential) | Consume materials for upgrades |
| **wallet-service** | (potential) | Handle currency costs (gold, diamond) |

### Được gọi bởi
| Caller | Endpoint | Mục đích |
|--------|----------|---------|
| **webSocket-server** | REST API / gRPC | Artifact operations, upgrades |
| **role-service** | (potential) | Fetch artifact stats for power calculation |

### Protocol Messages
- **MsgIDs 1675-1680**: ShenQi (Artifact) operations

### Kafka Integration
- Produces artifact state change events
- Bootstrap servers: localhost:29092

---

## 📊 Statistics

```
Entities:        2 classes (Artifact, ArtifactDrawRecord)
Repositories:    2 interfaces
Services:        1 class (ArtifactService)
Controllers:     1 class (ArtifactController)
DB tables:       2 (artifact, artifact_draw_record)
gRPC:            Port 9087
Kafka:           Producer (artifact events)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~800 lines
```

---

**Status**: ✅ Production Ready
**Last Updated**: 2026-03-30

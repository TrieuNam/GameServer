# Artifact Service

**Version**: 1.0.0
**Phase**: P2 (Combat, World & Social)
**Port**: 8091 · **gRPC**: 9087
**Database**: `game_artifact`

---

## 📋 Tổng quan

Artifact Service quản lý hệ thống Divine Artifact/Weapon (ShenQi 神器) cho trang bị huyền thoại. Người chơi có thể mở khóa, nâng cấp, và tùy chỉnh vũ khí thần thánh mạnh mẽ với nhiều hệ thống tiến hóa bao gồm level, grade, refinement, awakening, blessing tiers, và random attributes. Bao gồm hệ thống gacha draw để thu thập artifacts mới.

### Core Features
- ✅ Quản lý Artifact: Mở khóa, nâng level, nâng grade vũ khí thần
- ✅ Hệ thống Trang bị: Trang bị/tháo artifacts
- ✅ Refinement: Hệ thống nâng cấp cao cấp (精炼, tối đa level 15)
- ✅ Awakening: Các giai đoạn đột phá (觉醒, tối đa stage 7)
- ✅ Soul Power: Tích lũy soul power để nhận bonus (魂力)
- ✅ Divine Essence: Tiền tệ đặc biệt cho nâng cấp (神性精华)
- ✅ Hệ thống Blessing: Nâng cấp blessing theo tier (祝福, tối đa tier 10)
- ✅ Random Attributes: 4 attribute slots với hệ thống refresh/lock
- ✅ Gacha Draw: Single và 10-pull draws với pity system
- ✅ Hệ thống Skill: 3 skill slots với nâng cấp
- ✅ Tính toán Combat Power

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

### Lấy Tất Cả Artifacts
```bash
curl http://localhost:8091/api/artifact/player123
```

### Mở Khóa Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/unlock?artifactId=3001"
```

### Nâng Level Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/levelup?artifactIndex=1"
```

### Nâng Grade Artifact
```bash
curl -X POST "http://localhost:8091/api/artifact/player123/gradeup?artifactIndex=1"
```

### Trang Bị Artifact
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

### Lấy Lịch Sử Draw
```bash
curl http://localhost:8091/api/artifact/player123/draw-records
```

### Lấy Combat Power
```bash
curl "http://localhost:8091/api/artifact/player123/1/power"
```

---

## 🔧 Business Logic

### Hệ Thống Level
- **Max Level**: 100
- Tăng chỉ số cơ bản dần dần
- Cần exp materials để nâng cấp
- Level cao hơn mở khóa nhiều tính năng hơn

### Hệ Thống Grade
- **Max Grade**: 10
- Tầng chất lượng với hệ số nhân chỉ số đáng kể
- Cần vật liệu hiếm để nâng grade
- Mỗi grade tăng mạnh sức mạnh

### Hệ Thống Refinement (精炼)
- **Max Refinement Level**: 15
- Nâng cấp cao cấp trên level/grade
- Mỗi level cung cấp bonus chỉ số bổ sung
- Cần refinement stones

### Hệ Thống Awakening (觉醒)
- **Max Awakening Stage**: 7
- Các giai đoạn đột phá cho boost sức mạnh lớn
- Mở khóa abilities và appearances mới
- Cần awakening materials

### Hệ Thống Blessing (祝福)
- **Max Blessing Tier**: 10
- Nâng cấp tiến bộ theo tier
- Mỗi tier cung cấp bonus tích lũy
- Cần blessing materials hoặc divine essence

### Hệ Thống Random Attribute
- **4 Attribute Slots**: Mỗi slot có type và value
- Hệ thống Refresh để reroll attributes
- Hệ thống Lock để giữ attributes mong muốn
- Attribute pool dựa trên chất lượng artifact

### Soul Power (魂力)
- Tài nguyên tích lũy từ nhiều hoạt động
- Cung cấp bonus thụ động
- Có thể dùng cho nâng cấp đặc biệt
- Không bao giờ giảm

### Divine Essence (神性精华)
- Tiền tệ cao cấp/đặc biệt cho nâng cấp
- Dùng cho các thao tác cấp cao
- Có thể nhận từ sự kiện hoặc phân rã

### Hệ Thống Gacha
- **Single Draw**: Draw một artifact
- **Ten Pull**: Draw 10 artifacts với đảm bảo rare+
- Pity system đảm bảo legendary
- Tầng chất lượng: Common, Rare, Epic, Legendary, Mythic
- Lịch sử draw được ghi lại

### Hệ Thống Skill
- **3 Skill Slots**: skill1, skill2, skill3
- Mỗi skill nâng cấp độc lập
- Skills cung cấp abilities và bonuses trong combat

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

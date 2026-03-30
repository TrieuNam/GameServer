# Artifact Service

Divine Artifact/Weapon (ShenQi 神器) system microservice for legendary equipment.

## Features

- **Artifact Management**: Unlock, level up, grade up divine weapons
- **Refinement**: Advanced upgrade system (精炼)
- **Awakening**: Breakthrough stages (觉醒)
- **Soul Power**: Accumulate soul power for bonuses (魂力)
- **Divine Essence**: Special currency for upgrades (神性精华)
- **Blessing System**: Tier-based blessing upgrades (祝福)
- **Random Attributes**: 4 attribute slots with refresh/lock system

## API Endpoints

### Artifact Operations
- `GET /api/artifact/{roleId}` - Get all artifacts
- `GET /api/artifact/{roleId}/{artifactIndex}` - Get specific artifact
- `GET /api/artifact/{roleId}/draw-records` - Get gacha draw records
- `POST /api/artifact/{roleId}/unlock` - Unlock new artifact
- `POST /api/artifact/{roleId}/levelup` - Level up artifact
- `POST /api/artifact/{roleId}/gradeup` - Grade up artifact
- `POST /api/artifact/{roleId}/equip` - Equip artifact
- `DELETE /api/artifact/{roleId}/equip` - Unequip artifact
- `POST /api/artifact/{roleId}/refine` - Refine artifact
- `POST /api/artifact/{roleId}/awaken` - Awaken artifact
- `POST /api/artifact/{roleId}/soulpower` - Add soul power
- `POST /api/artifact/{roleId}/essence` - Add divine essence
- `POST /api/artifact/{roleId}/blessing` - Upgrade blessing
- `POST /api/artifact/{roleId}/refresh` - Refresh attributes
- `POST /api/artifact/{roleId}/draw` - Gacha draw
- `POST /api/artifact/{roleId}/upgrade-skill` - Upgrade skill
- `GET /api/artifact/{roleId}/{artifactIndex}/power` - Get combat power
- `GET /api/artifact/{roleId}/{artifactIndex}/canlevelup` - Check can level up
- `GET /api/artifact/{roleId}/{artifactIndex}/cangradeup` - Check can grade up
- `GET /api/artifact/{roleId}/{artifactIndex}/canrefine` - Check can refine
- `GET /api/artifact/{roleId}/{artifactIndex}/canawaken` - Check can awaken

## Database Schema

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

## Configuration

Port: 8091
Database: game_artifact

## Protocol Messages

MsgIDs: 1675-1680 (ShenQi)

## Game Mechanics

- **Level System**: Max level 100
- **Grade System**: Max grade 10 (quality tiers)
- **Refinement**: Max level 15 (精炼等级)
- **Awakening**: Max stage 7 (觉醒阶段)
- **Blessing**: Max tier 10 (祝福层级)
- **Attributes**: 4 random attributes with lock/refresh system
- **Soul Power**: Accumulated resource for bonuses
- **Divine Essence**: Premium currency for special upgrades

## TODO

- [ ] Feign clients (config-service, role-service, bag-service, wallet-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (shenqi.json)
- [ ] Complete business logic formulas
- [ ] Attribute pool configuration
- [ ] Unit and integration tests


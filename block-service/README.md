# Block Service

**Version**: 1.0.0
**Phase**: P2 (Gameplay Systems)
**Port**: 8335
**Database**: `game_block`

---

## 📋 Overview

Block Service manages the building block system for legacy Msg 2180/2181 parity. Players can place, remove, compose, activate, and wear building blocks on different maps. This service tracks block placement positions, colors, and activation states for each player.

### Core Features
- ✅ Block placement (inlay) on maps
- ✅ Block removal
- ✅ Block composition from multiple blocks
- ✅ Block activation sequence management
- ✅ Block map wearing system
- ✅ Per-role block state tracking
- ✅ Position-based block placement (X/Y coordinates)

---

## 🎯 Flow Hoạt Động

```
[Player places block on map]
POST /api/block/inlay
        │
        ▼
BlockStateService.inlay()
├── Validate roleId, mapId, blockIndex
├── Check position (posX, posY)
├── Create BlockNode with color
└── Save to block_node table

[Player composes blocks]
POST /api/block/compose
        │
        ▼
BlockStateService.compose()
├── Validate multiple blockIndexList
├── Process block composition logic
└── Update block_node records

[Player activates block sequence]
POST /api/block/activate
        │
        ▼
BlockStateService.activate()
├── Update activate_seq in role_block_state
└── Return activation result
```

---

## 🗄️ Database Schema

### role_block_state
```sql
CREATE TABLE role_block_state (
    role_id          BIGINT       NOT NULL PRIMARY KEY,
    activate_seq     INT          NOT NULL DEFAULT 0,
    wearing_map_id   INT          NOT NULL DEFAULT 0,
    next_block_index INT          NOT NULL DEFAULT 1,
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
```

### block_node
```sql
CREATE TABLE block_node (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id      BIGINT       NOT NULL,
    block_id     INT          NOT NULL,
    map_id       INT          NOT NULL,
    block_index  INT          NOT NULL,
    pos_x        INT          NOT NULL DEFAULT 0,
    pos_y        INT          NOT NULL DEFAULT 0,
    color        INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_block_role_map_index (role_id, map_id, block_index)
);
```

---

## 🔌 API Endpoints

```
GET   /api/block/info           - Get block info for a role
POST  /api/block/inlay          - Place a block on map at position
POST  /api/block/remove         - Remove a block from map
POST  /api/block/compose        - Compose blocks from multiple block indices
POST  /api/block/activate       - Activate block sequence
POST  /api/block/wear           - Wear a block map
```

---

## 📦 API Examples

### Get Block Info
```bash
curl "http://localhost:8335/api/block/info?roleId=player123"
```

### Place Block (Inlay)
```bash
curl -X POST http://localhost:8335/api/block/inlay \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "mapId": 1,
    "blockIndex": 5,
    "posX": 10,
    "posY": 15
  }'
```

### Remove Block
```bash
curl -X POST http://localhost:8335/api/block/remove \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "mapId": 1,
    "blockIndex": 5
  }'
```

### Compose Blocks
```bash
curl -X POST http://localhost:8335/api/block/compose \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "mapId": 1,
    "blockIndexList": [1, 2, 3, 4]
  }'
```

### Activate Block Sequence
```bash
curl -X POST http://localhost:8335/api/block/activate \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "seq": 10
  }'
```

### Wear Block Map
```bash
curl -X POST http://localhost:8335/api/block/wear \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player123",
    "mapId": 2
  }'
```

---

## 🔧 Business Logic

### Block Placement
- Each block has unique position (posX, posY) on a map
- Blocks are identified by blockIndex within a map
- Color attribute stored per block for customization
- Unique constraint: `role_id + map_id + block_index`

### Block State Management
- Each role has one `role_block_state` record
- Tracks current wearing map ID
- Maintains activation sequence number
- Auto-increments next block index

### Block Operations
- **Inlay**: Place block at specific coordinates
- **Remove**: Delete block from map by index
- **Compose**: Combine multiple blocks into composite block
- **Activate**: Progress activation sequence
- **Wear**: Switch to different block map

---

## 🚀 Running

```bash
cd GameServer/block-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **eureka-server** | Service discovery | Register and discover service |

### Được gọi bởi
- **webSocket-server**: MSGID_2180_BLOCK_REQ, MSGID_2181_BLOCK_REQ
- Legacy protocol compatibility for block operations

---

## 📊 Statistics

```
Entities:        2 classes (RoleBlockState, BlockNode)
Repositories:    2 interfaces
Services:        1 class (BlockStateService)
Controllers:     1 class (BlockController)
DB tables:       2 (role_block_state, block_node)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready
**Last Updated**: 2026-03-30

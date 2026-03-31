# GameWorld Service

**Version**: 1.0.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8105 · **gRPC**: 9105  
**Database**: `game_world`

---

## 📋 Overview

GameWorld Service quản lý **thế giới game** — maps, scenes, spawning monsters, theo dõi vị trí player, và điều phối các sự kiện trong thế giới (pickup items, interact với NPC, v.v.). Đây là service backend cho open-world exploration.

### Core Features
- ✅ Scene/map management
- ✅ Monster spawning và despawning
- ✅ Player position tracking
- ✅ Item pickup system
- ✅ NPC interaction
- ✅ gRPC server (port 9105)

---

## 🎯 Flow World Events

```
[Player di chuyển]
Client ──WebSocket──► POST /api/world-move { x, y, sceneId }
                              │
                    gameworld-service
                    ├── Cập nhật player position
                    ├── Check nearby monsters
                    ├── Trigger monster aggro nếu trong range
                    └── Broadcast position đến players khác trong scene

[Player pickup item]
POST /api/world/pickup { itemId, sceneId }
        │
        ▼
gameworld-service
├── Verify item tồn tại trong scene
├── Remove item từ scene
└──► bag-service: add item cho player

[Monster die → drop items]
gameworld-service ──► drop-service: roll drop table
                ──► Spawn items vào scene (tồn tại 5 phút)
```

---

## 🗄️ Database Schema

### scene_state
```sql
CREATE TABLE scene_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_id VARCHAR(50) NOT NULL UNIQUE,
    map_id INT NOT NULL,
    player_count INT DEFAULT 0,
    monster_count INT DEFAULT 0,
    Status**: ✅ Production Ready (Updated 2026-03-22)
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
# Qua HTTP REST
POST  /api/world/enter            - Player vào scene
POST  /api/world/leave            - Player rời scene
POST  /api/world/move             - Cập nhật vị trí
POST  /api/world/pickup           - Nhặt item
POST  /api/world/interact         - Tương tác NPC
GET   /api/world/scene/{sceneId}  - Thông tin scene

# gRPC (từ webSocket-server)
rpc EnterScene(EnterRequest) → SceneState
rpc GetSceneMonsters(SceneId) → MonsterList
rpc SpawnMonster(SpawnRequest) → MonsterState
```

---

## 📦 API Examples

### Vào Scene
```bash
curl -X POST http://localhost:8105/api/world/enter \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "sceneId": "map_001_scene_01"}'
```

### Di Chuyển
```bash
curl -X POST http://localhost:8105/api/world/move \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "sceneId": "map_001_scene_01", "x": 150, "y": 200}'
```

---

## 🔧 Business Logic

### Monster Spawn
- Mỗi scene có spawn config từ config-service
- Monsters respawn sau 30-120 giây (configurable)
- Respawn scheduler chạy background mỗi 5 giây

### Scene Lifecycle
- Scene được tạo khi player đầu tiên vào
- Scene được destroy khi không còn player
- Boss scenes: persistent, không destroy

---

## 🚀 Running

```bash
cd GameServer/gameworld-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### gRPC Server (port 9105)
- webSocket-server: Scene và monster queries

### Phụ thuộc
- **config-service**: Map/scene configs, monster spawn configs
- **drop-service**: Roll monster drops
- **bag-service**: Grant dropped items

Gameworld-service chuẩn hoá gọi config qua `GET /api/config/file?path=...`.

---

## 📊 Statistics

```
Entities:        2 classes (SceneState, MonsterState)
Repositories:    2 interfaces
Controllers:     1 class (GameWorldController)
Services:        2 (SceneService, MonsterSpawnService)
gRPC:            GameWorldGrpcImpl
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~800 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


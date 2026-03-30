# World Service Memory

Service-specific operational memory cho `world-service` trong GameServer.

## Identity
- Service name: `world-service`
- Path: `GameServer/world-service`
- Main port: 9040
- Database: world_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Game world map management (zones, regions, dungeons)
- NPC management
- Monster spawn management
- World events (invasions, seasonal events)
- Location-based mechanics

## Key Files & Anchors
- Service: `world-service/src/main/java/com/SouthMillion/world_service/service/WorldService.java`
- Entity: `world-service/src/main/java/com/SouthMillion/world_service/entity/Zone.java`

## Important APIs
```
GET    /api/world/zones             - List all zones
GET    /api/world/zone/{zoneId}     - Get zone details with NPCs/mobs
GET    /api/world/zone/{zoneId}/players - Get players in zone
POST   /api/world/event/start       - Start world event (admin)
GET    /api/world/events            - List active events
```

## Database Schema
```sql
CREATE TABLE zones (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  level_range INT,
  max_players INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE npcs (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  zone_id VARCHAR(36) NOT NULL,
  x INT, y INT, z INT,
  type VARCHAR(50),
  FOREIGN KEY (zone_id) REFERENCES zones(id)
);

CREATE TABLE monsters (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  zone_id VARCHAR(36) NOT NULL,
  level INT,
  spawn_rate INT,
  FOREIGN KEY (zone_id) REFERENCES zones(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: Monster spawn doesn't respect spawn rate
  - Fix: Use timer to spawn at correct intervals
- **Bug 2**: Player count not synchronized across instances
  - Fix: Use Redis counter for real-time count
- **Bug 3**: Event timeout not enforced
  - Fix: Add scheduled task to end expired events

## Cross-Service Dependencies
- **gameworld-service**: Real-time player positions
- **event-bus**: Broadcast world events

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/world_service_db
    username: root
    password: root

server:
  port: 9040

world:
  spawn-interval: 5000  # 5 seconds
  max-event-duration: 3600000  # 1 hour
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\world-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Spawn intervals respected?
- [ ] Max player limit enforced?
- [ ] Events auto-end on timeout?
- [ ] Zone transitions validated?
- [ ] NPC respawn logic correct?

## Update Log
- 2026-03-21 | Scope: world-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/WORLD_SERVICE_MEMORY.md`


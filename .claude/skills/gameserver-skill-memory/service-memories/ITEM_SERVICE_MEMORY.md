# Item Service Memory

Service-specific operational memory cho `item-service` trong GameServer.

## Identity
- Service name: `item-service`
- Path: `GameServer/item-service`
- Main port: 9035
- Database: item_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Item management (weapons, armor, consumables)
- Item inventory system
- Item rarity levels (common, rare, epic, legendary)
- Item trading/selling
- Item durability/upgrades

## Key Files & Anchors
- Controller: `item-service/src/main/java/com/SouthMillion/item_service/controller/ItemController.java`
- Service: `item-service/src/main/java/com/SouthMillion/item_service/service/ItemService.java`
- Entity: `item-service/src/main/java/com/SouthMillion/item_service/entity/Item.java`

## Important APIs
```
POST   /api/item/create             - Create new item (admin)
GET    /api/item/{id}               - Get item details
GET    /api/item/user/{userId}      - Get user inventory
POST   /api/item/{id}/use           - Use consumable item
POST   /api/item/{id}/sell          - Sell item
POST   /api/item/transfer/{toUserId} - Transfer item to another user
PUT    /api/item/{id}/upgrade       - Upgrade item level
```

## Database Schema
```sql
CREATE TABLE items (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(50) NOT NULL,
  rarity VARCHAR(20) DEFAULT 'COMMON',
  level INT DEFAULT 1,
  price BIGINT,
  durability INT,
  max_durability INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  item_id VARCHAR(36) NOT NULL,
  quantity INT DEFAULT 1,
  equipped BOOLEAN DEFAULT FALSE,
  acquired_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_user_item (user_id, item_id),
  FOREIGN KEY (item_id) REFERENCES items(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: Item duplication on concurrent buy/sell
  - Fix: Use pessimistic locking on inventory rows
- **Bug 2**: Durability doesn't decrease when used
  - Fix: Decrement durability on use, check before use
- **Bug 3**: User can trade item they don't have
  - Fix: Check inventory before transfer

## Cross-Service Dependencies
- **user-service**: Get user funds for purchase
- **wallet-service**: Update user balance on sell

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/item_service_db
    username: root
    password: root

server:
  port: 9035
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\item-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Item quantity cannot go negative?
- [ ] Durability checked before use?
- [ ] Transfer validates ownership?
- [ ] Price calculation correct?
- [ ] Concurrent inventory operations safe?

## Update Log
- 2026-03-21 | Scope: item-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/ITEM_SERVICE_MEMORY.md`


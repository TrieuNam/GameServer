# Guild Service Memory

Service-specific operational memory cho `guild-service` trong GameServer.

## Identity
- Service name: `guild-service`
- Path: `GameServer/guild-service`
- Main port: 9017
- Database: guild_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Guild creation, management, dissolution
- Member management (join, leave, kick, promote)
- Guild treasury/funds management
- Guild events (wars, tournaments)

## Key Files & Anchors
- Controller: `guild-service/src/main/java/com/SouthMillion/guild_service/controller/GuildController.java`
- Service: `guild-service/src/main/java/com/SouthMillion/guild_service/service/GuildDomainService.java`
- Entity: `guild-service/src/main/java/com/SouthMillion/guild_service/entity/Guild.java`
- DTO: `guild-service/src/main/java/com/SouthMillion/guild_service/dto/GuildDTO.java`

## Important APIs
```
POST   /api/guild/create            - Create new guild
GET    /api/guild/{id}              - Get guild details
PUT    /api/guild/{id}              - Update guild settings
GET    /api/guild/{id}/members      - List guild members
POST   /api/guild/{id}/join         - Join guild
POST   /api/guild/{id}/leave        - Leave guild
POST   /api/guild/{id}/kick/{userId} - Kick member
POST   /api/guild/{id}/war/declare  - Declare guild war
```

## Database Schema
```sql
CREATE TABLE guilds (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  leader_id VARCHAR(36) NOT NULL,
  level INT DEFAULT 1,
  treasury BIGINT DEFAULT 0,
  max_members INT DEFAULT 50,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guild_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guild_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  role VARCHAR(20) DEFAULT 'MEMBER',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_guild_member (guild_id, user_id),
  FOREIGN KEY (guild_id) REFERENCES guilds(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: Member count không update khi join/leave
  - Fix: Update guild cached member count after each join/leave
- **Bug 2**: Leader có thể self-kick
  - Fix: Check leader role, prevent self-kick
- **Bug 3**: Treasury transaction race condition
  - Fix: Use pessimistic locking, increment atomic

## Cross-Service Dependencies
- **user-service**: Validate user exists before adding member
- **event-bus**: Publish guild events

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/guild_service_db
    username: root
    password: root

server:
  port: 9017
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\guild-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Leader transfer validation OK?
- [ ] Member count stays consistent?
- [ ] Treasury transactions are atomic?
- [ ] Max member limit enforced?
- [ ] Guild name duplication check?

## Update Log
- 2026-03-21 | Scope: guild-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/GUILD_SERVICE_MEMORY.md`


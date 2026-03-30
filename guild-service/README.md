# Guild Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8440  
**Database**: `game_guild`

---

## ðŸ“‹ Overview

Guild Service is a complete microservice for managing game guilds (clans). It provides comprehensive guild management functionality including:

- **Guild creation** and management
- **Member management** (max 50 members per guild, 3 ranks)
- **Application system** (join requests)
- **Technology upgrades** (5 branches: ATK/DEF/HP/CRT/SPD)
- **Guild warehouse** (100 slots)
- **Donation system** (daily limits)
- **Leadership transfer**
- **Guild ranking**

---

## ðŸŽ¯ Features

### Core Features
- âœ… Guild CRUD operations
- âœ… Member management (add/remove/promote/demote)
- âœ… Application system (apply/approve/reject)
- âœ… Technology upgrades (5 branches, 10 levels each)
- âœ… Donation system (3 donations per day)
- âœ… Guild warehouse (100 slots)
- âœ… Leadership transfer
- âœ… Guild ranking
- âœ… Guild search

### Guild Properties
- **Max Level**: 10
- **Max Members**: 50 (maximum number of members allowed in one guild)
- **Creation Cost**: 10,000 gold
- **Technology Branches**: ATK, DEF, HP, CRT, SPD (max level 10 each)
- **Member Ranks**: Leader (3), Officer (2), Member (1)
- **Warehouse Slots**: 100
- **Daily Donation Limit**: 3 per member

---

## ðŸ—ï¸ Architecture

### Technology Stack
- **Spring Boot** 2.7.x
- **Spring Data JPA** (Hibernate)
- **MySQL** 8.0
- **Redis** (caching)
- **Spring Cloud** (Eureka, Feign, Config)
- **Lombok**

### Database Schema

#### guild
```sql
CREATE TABLE guild (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) UNIQUE NOT NULL,
    leader_id VARCHAR(50) NOT NULL,
    level INT DEFAULT 1,
    exp BIGINT DEFAULT 0,
    member_count INT DEFAULT 1,
    max_members INT DEFAULT 50,
    notice VARCHAR(500),
    tech_attack INT DEFAULT 1,
    tech_defense INT DEFAULT 1,
    tech_hp INT DEFAULT 1,
    tech_crit INT DEFAULT 1,
    tech_speed INT DEFAULT 1,
    funds BIGINT DEFAULT 0,
    donation_reset_time DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    disbanded_at DATETIME,
    active BOOLEAN DEFAULT TRUE
);
```

#### guild_member
```sql
CREATE TABLE guild_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_level INT NOT NULL,
    power BIGINT DEFAULT 0,
    rank INT DEFAULT 1, -- 1=Member, 2=Officer, 3=Leader
    contribution BIGINT DEFAULT 0,
    daily_donation_count INT DEFAULT 0,
    last_donation_time DATETIME,
    last_online_time DATETIME,
    online BOOLEAN DEFAULT FALSE,
    joined_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_guild_role (guild_id, role_id)
);
```

#### guild_application
```sql
CREATE TABLE guild_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_level INT NOT NULL,
    power BIGINT DEFAULT 0,
    message VARCHAR(200),
    status INT DEFAULT 0, -- 0=Pending, 1=Approved, 2=Rejected
    processor_id VARCHAR(50),
    processed_at DATETIME,
    applied_at DATETIME NOT NULL,
    UNIQUE KEY uk_app_guild_role (guild_id, role_id)
);
```

#### guild_warehouse
```sql
CREATE TABLE guild_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    item_id INT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    quantity INT DEFAULT 1,
    quality INT DEFAULT 1,
    depositor_id VARCHAR(50) NOT NULL,
    depositor_name VARCHAR(50) NOT NULL,
    deposited_at DATETIME NOT NULL
);
```

---

## ðŸ”Œ API Endpoints

### Guild Management

#### Create Guild
```http
POST /api/guild/create
Content-Type: application/json

{
  "name": "DragonSlayers",
  "leaderId": "player123",
  "notice": "Welcome to our guild!"
}

Response:
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "DragonSlayers",
    "leaderId": "player123",
    "level": 1,
    "exp": 0,
    "memberCount": 1,
    "maxMembers": 50,
    ...
  }
}
```

#### Get Guild Info
```http
GET /api/guild/{guildId}

Response:
{
  "code": 0,
  "message": "Success",
  "data": { ... }
}
```

#### Search Guilds
```http
POST /api/guild/search
Content-Type: application/json

{
  "keyword": "Dragon",
  "page": 0,
  "size": 20
}

Response:
{
  "code": 0,
  "message": "Success",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

#### Disband Guild
```http
DELETE /api/guild/{guildId}/disband          - Giải tán guild (param: leaderId)
```

### Member Management

#### Apply to Join
```http
POST /api/guild/apply
Content-Type: application/json

{
  "guildId": 1,
  "roleId": "player456",
  "roleName": "Warrior",
  "roleLevel": 50,
  "power": 100000,
  "message": "I want to join!"
}
```

#### Process Application
```http
POST /api/guild/application/process
Content-Type: application/json

{
  "applicationId": 1,
  "processorId": "player123",
  "approve": true
}
```

#### Leave Guild
```http
DELETE /api/guild/{guildId}/member/{roleId}
```

#### Kick Member
```http
DELETE /api/guild/{guildId}/kick             - Kick member (params: kickerId, targetId)
```

#### Promote Member
```http
PUT    /api/guild/{guildId}/promote          - Thăng cấp member (params: promoterId, targetId)
```

#### Demote Member
```http
PUT    /api/guild/{guildId}/demote           - Giáng cấp member (params: demoterId, targetId)
```

#### Transfer Leadership
```http
PUT /api/guild/transfer-leader
Content-Type: application/json

{
  "guildId": 1,
  "currentLeaderId": "player123",
  "newLeaderId": "player456"
}
```

### Guild Operations

#### Donate
```http
POST /api/guild/donate
Content-Type: application/json

{
  "guildId": 1,
  "roleId": "player123",
  "amount": 5000
}
```

#### Upgrade Technology
```http
POST /api/guild/tech/upgrade
Content-Type: application/json

{
  "guildId": 1,
  "roleId": "player123",
  "techType": "ATTACK"
}
```

#### Edit Notice
```http
PUT /api/guild/notice
Content-Type: application/json

{
  "guildId": 1,
  "roleId": "player123",
  "notice": "New guild notice!"
}
```

#### Get Members
```http
GET /api/guild/{guildId}/members
```

#### Get Applications
```http
GET /api/guild/{guildId}/applications
GET /api/guild/health
```

---

## ðŸš€ Running the Service

### Prerequisites
- Java 17+
- MySQL 8.0
- Redis
- Eureka Server running on port 8761

### Configuration
Edit `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/guild_db
    username: root
    password: root
  
  redis:
    host: localhost
    port: 6379

server:
  port: 8440
```

### Build & Run
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run JAR
java -jar target/guild-service-1.0.0.jar
```

### Docker
```bash
# Build image
docker build -t guild-service:1.0.0 .

# Run container
docker run -p 8440:8440 \
  -e MYSQL_HOST=mysql \
  -e REDIS_HOST=redis \
  -e EUREKA_HOST=eureka \
  guild-service:1.0.0
```

---

## ðŸ§ª Testing

### Health Check
```bash
curl http://localhost:8440/api/guild/health
```

### Create Test Guild
```bash
curl -X POST http://localhost:8440/api/guild/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "TestGuild",
    "leaderId": "test123",
    "notice": "Test guild"
  }'
```

---

## ðŸ“Š Integration

### WebSocket Handler Integration
Call flow: client -> webSocket-server (GuildHandler) -> guild-service (via Feign).

```java
// In GuildHandler.java
@Component
@RequiredArgsConstructor
public class GuildHandler implements MessageHandler {
    
  private final GuildFeignClient guildClient; // Feign client used to call guild-service
    
    private void handleCreateGuild(PlayerSession ps, ...) {
      // Call guild-service via Feign client
        GuildDTO.Response<GuildDTO.InfoResponse> response = 
            guildClient.createGuild(request);
        
        // Send response to client
        sendResponse(ps, response);
    }
}
```

### Feign Client
```java
@FeignClient(name = "guild-service")
public interface GuildFeignClient {
    
    @PostMapping("/api/guild/create")
    GuildDTO.Response<GuildDTO.InfoResponse> createGuild(
        @RequestBody GuildDTO.CreateRequest request);
    
    @GetMapping("/api/guild/{guildId}")
    GuildDTO.Response<GuildDTO.InfoResponse> getGuildInfo(
        @PathVariable Long guildId);
    
    // ... other methods
}
```

---

## ðŸ“ˆ Performance

### Caching Strategy
- Guild info: Redis cache (5 minutes TTL)
- Member list: Redis cache (2 minutes TTL)
- Rankings: Redis cache (10 minutes TTL)

### Database Indexes
- `idx_guild_name` on `name`
- `idx_guild_level` on `level`
- `idx_member_guild` on `guild_id`
- `idx_member_role` on `role_id`
- `uk_guild_role` unique on `(guild_id, role_id)`

---

## ðŸ”§ Maintenance

### Daily Tasks
- Reset donation counts (scheduled task at midnight)
- Clean up old processed applications (7+ days)

### Monitoring
- Guild creation rate
- Active guilds count
- Average members per guild
- Technology upgrade frequency
- Donation amount tracking

---

## ðŸ“ Error Codes

| Code | Message |
|------|---------|
| 0 | Success |
| -1 | Guild name already exists / Guild not found |
| -2 | Player already in guild / Guild disbanded |
| -3 | Guild is full |
| -4 | Player already in a guild |
| -5 | Application already pending |
| -6 | No permission |
| -7 | Insufficient funds |
| -8 | Invalid operation |

---

## ðŸŽ¯ Next Steps

### Phase 1: Core Features âœ…
- [x] Guild CRUD
- [x] Member management
- [x] Application system
- [x] Technology upgrades
- [x] Donation system

### Phase 2: Advanced Features
- [ ] Guild warehouse implementation
- [ ] Guild war system
- [ ] Guild skills
- [ ] Guild shop
- [ ] Guild boss

### Phase 3: Optimization
- [ ] Redis caching
- [ ] Performance optimization
- [ ] Scheduled tasks
- [ ] Event notifications (Kafka)

---

## ðŸ“š Related Services

- **role-service**: Get player info (name, level, power)
- **wallet-service**: Deduct gold for guild creation/donations
- **chat-service**: Guild chat channel
- **webSocket-server**: GuildHandler integration

---

## ðŸ“§ Contact

For questions or issues, contact the Game Server Team.

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22




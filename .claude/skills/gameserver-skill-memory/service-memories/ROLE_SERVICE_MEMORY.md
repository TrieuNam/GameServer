# Role Service Memory

Service-specific operational memory cho `role-service` trong GameServer.

## Identity
- Service name: `role-service`
- Path: `GameServer/role-service`
- Main port: 8410
- Database: role_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Character profile and progression (role, level, exp, rename, wxinfo)
- Role-related utility APIs (combat power, basic-info)
- Role settings, mail, ads claim, and other-role view
- Skill/talent data APIs used by `webSocket-server`

## Key Files & Anchors
- Controller: `role-service/src/main/java/com/SouthMillion/role_service/controller/RoleController.java`
- Service: `role-service/src/main/java/com/SouthMillion/role_service/service/RoleService.java`
- Entity: `role-service/src/main/java/com/SouthMillion/role_service/entity/Role.java`
- Skill controller: `role-service/src/main/java/com/SouthMillion/role_service/controller/SkillController.java`
- Skill service: `role-service/src/main/java/com/SouthMillion/role_service/service/SkillService.java`
- Flyway migration: `role-service/src/main/resources/db/migration/V1__init_role_service.sql`

## Important APIs
```
GET    /api/role/{roleId}                 - Get role by roleId
GET    /api/role/by-user/{userId}         - List roles by userId
POST   /api/role                          - Create role
POST   /api/role/exp/add                  - Add exp
POST   /api/role/{roleId}/rename          - Rename role
POST   /api/role/{roleId}/wxinfo          - Set wx name/avatar
GET    /api/role/{roleId}/combat-power    - Get combat power snapshot
GET    /api/role/{roleId}/basic-info      - Lightweight role info

POST   /api/role/settings                 - Apply role settings

POST   /api/mail/list                     - Mail list
GET    /api/mail/{userId}/{mailId}        - Mail detail + mark read
POST   /api/mail/{userId}/{mailId}/delete - Delete mail
POST   /api/mail/{userId}/{mailId}/fetch  - Fetch mail rewards

POST   /api/ads/claim                     - Claim ad rewards
GET    /api/other-role/{uid}?roleId=...   - Other role info

# Skill APIs (thêm 2026-03-21)
GET    /api/skill/{roleId}                   - Toàn bộ kỹ năng của nhân vật
POST   /api/skill/{roleId}/learn             - Học/nâng cấp kỹ năng { skillId }
POST   /api/skill/{roleId}/one-key-level-up  - Nâng cấp tất cả kỹ năng

# Talent APIs (thêm 2026-03-21)
GET    /api/talent/{roleId}                  - Toàn bộ thiên phú
POST   /api/talent/{roleId}/learn            - Học/nâng cấp thiên phú { skillId }
```

## Skill/Talent Contract Notes (Client Mapping)
- Client MsgId anchor: `document/client/LineR/assets/script/manager/MsgIdManger.ts`
- Client proto anchor: `document/client/LineR/assets/script/proto/proto.d.ts`
- Skill pair: `1470 (CS)` <-> `1471 (SC)`
- Talent pair: `1480 (CS)` <-> `1481 (SC)`
- Server ws handler: `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/skill/SkillHandler.java`
- DTO contract: `GameServer/common-lib/src/main/java/org/SouthMillion/dto/skill/SkillDTOs.java`

## Database Schema
```sql
CREATE TABLE role (
  role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  level INT NOT NULL DEFAULT 1,
  exp BIGINT NOT NULL DEFAULT 0,
  hp BIGINT NOT NULL DEFAULT 0,
  attack_value BIGINT NOT NULL DEFAULT 0,
  defense_value BIGINT NOT NULL DEFAULT 0,
  speed INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_role_user_name (user_id, name)
);

CREATE TABLE role_system_setting (
  user_id VARCHAR(64) PRIMARY KEY,
  data JSON NOT NULL,
  updated_at DATETIME(3) NOT NULL
);

CREATE TABLE mail (
  mail_id VARCHAR(26) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(128) NOT NULL,
  items JSON NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  is_fetched TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE ad_reward_claim (
  id VARCHAR(26) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  seq INT NOT NULL,
  claim_day DATE NOT NULL,
  UNIQUE KEY uk_ad_claim_user_seq_day (user_id, seq, claim_day)
);
```

## Common Bugs & Patterns
- **Bug 1**: UserId vs RoleId bị dùng lẫn nhau khi gọi API
  - Fix: bám theo contract endpoint (`/by-user/{userId}` vs `/{roleId}`)
- **Bug 2**: MsgId/proto mapping lệch giữa client và ws handler
  - Fix: đối chiếu 2 chiều `MsgIdManger.ts` + `MsgIds.java` + handler emit
- **Bug 3**: Skill/talent contract đổi nhưng quên update memory
  - Fix: update `ROLE_SERVICE_MEMORY.md` + `CLIENT_PROTO_MAPPING_REFERENCE.md`

## Cross-Service Dependencies
- **webSocket-server**: consumer chính qua Feign (`RoleFeign`, `SkillFeign`)
- **config-service**: role base stats/config load path
- **bag-service**: nhận/publish bag grant related flows (Kafka integration)
- **session-service**: login/auth flow phía gateway/ws sử dụng role data

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/role_service_db
    username: root
    password: root
  cache:
    type: redis
    redis:
      time-to-live: 3600000

server:
  port: 8410
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\role-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] UserId/RoleId dùng đúng theo endpoint contract?
- [ ] MsgId pair (1470/1471, 1480/1481) map đúng client-server?
- [ ] Proto field mapping không lệch (proto.d.ts vs server emit)?
- [ ] Skill/talent API thay đổi có backward compatibility note?
- [ ] Flyway schema đã cover table/entity mới chưa?

## Update Log
- 2026-03-21 | Scope: role-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/ROLE_SERVICE_MEMORY.md`
- 2026-03-21 | Scope: role-service memory | Change: sửa lại scope/API/schema theo code hiện tại + thêm mapping notes skill/talent | Why: tránh lệch khi support client-server mapping | Ref: `RoleController.java`, `SkillController.java`, `V1__init_role_service.sql`


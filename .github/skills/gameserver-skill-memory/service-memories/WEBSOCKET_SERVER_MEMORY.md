# WebSocket Server Memory

Service-specific operational memory cho `webSocket-server` trong GameServer.

## Identity
- Service name: `webSocket-server`
- Path: `GameServer/webSocket-server`
- Main port: 8094
- Database: none (stateful gateway, Redis/session integration)
- Build: Maven (`mvn clean install`)

## Core Scope
- WebSocket gateway for client realtime messages.
- MsgId-based dispatch to handlers (role, bag, guild, task, skill, v.v.).
- Orchestration layer: parse proto -> call downstream (Feign/gRPC) -> emit proto response.
- Login bootstrap flow pushes initial state from multiple handlers.

## Key Files & Anchors
- App config: `webSocket-server/src/main/resources/application.yml`
- MsgId constants: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java`
- Base handler contract: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MessageHandler.java`
- Emit utility: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/Emitters.java`
- Skill handler: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/skill/SkillHandler.java`
- Login bootstrap: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/login/LoginBootstrapHandler.java`
- Skill feign bridge: `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/service/client/SkillFeign.java`

## Important APIs / Contracts

### WS MsgIds for skill/talent
```
1470 CS_ROLE_SKILL_OPERA_REQ
1471 SC_ROLE_SKILL_ALL_INFO
1480 CS_ROLE_TALENT_OPERA_REQ
1481 SC_ROLE_TALENT_ALL_INFO
```

### Skill reqType contract
```
1470 reqType:
  0 = info
  1 = learn skill (param1 = skillId)
  2 = one-key level up

1480 reqType:
  0 = info
  1 = learn talent (param1 = skillId)
```

### Downstream role-service APIs used by SkillFeign
```
GET  /api/skill/{roleId}
POST /api/skill/{roleId}/learn
POST /api/skill/{roleId}/one-key-level-up
GET  /api/talent/{roleId}
POST /api/talent/{roleId}/learn
```

## Common Bugs & Patterns
- Bug 1: CS MsgId wired nhưng interests() không đăng ky.
  - Fix: doi chieu `MsgIds.java` va `interests()` cua handler.
- Bug 2: Parse payload OK nhưng emit sai SC MsgId.
  - Fix: assert dung pair CS/SC va emit proto dung schema.
- Bug 3: Service dich down lam login bootstrap cham.
  - Fix: fail-fast timeout + fallback theo handler, khong de block toan bo login.
- Bug 4: Handler moi co code main nhung khong co test.
  - Fix: them test cho parse/dispatch/emit va case invalid payload.

## Cross-Service Dependencies
- role-service (skill/talent role APIs)
- session-service (session/auth flow)
- bag/equip/shop/world/... via Feign/gRPC clients
- Redis for session-related runtime state

## Config & Environment
```yaml
server:
  port: 8094

spring:
  application:
    name: websocket-server
  main:
    web-application-type: reactive

eureka:
  client:
    fetchRegistry: true
    registerWithEureka: true
```

## Test Anchors
- Existing handler tests folder:
  `webSocket-server/src/test/java/com/southMillion/webSocket_server/handler/`
- Current status for skill flow:
  no dedicated `SkillHandlerTest` yet.

## Risk Checklist
- [ ] MsgId pair CS/SC da map dung trong handler va MsgIds?
- [ ] Proto field mapping dung voi client `proto.d.ts`?
- [ ] Downstream timeout/fallback khong block login bootstrap?
- [ ] Handler moi da co unit tests toi thieu?
- [ ] Emit response co du field bat buoc (`count`, `list`) chua?

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\webSocket-server
mvn clean install
mvn test

# Kiem tra nhanh co SkillHandler test hay chua
Get-ChildItem -Path src\test\java -Recurse -File |
  Where-Object { $_.Name -like "*Skill*" -or $_.FullName -like "*handler\\skill*" }
```

## Update Log
- 2026-03-21 | Scope: websocket-server memory | Change: thay template bang memory thuc te + bo sung skill/talent mapping va test gap | Why: tranh agent doc placeholder sai context | Ref: `SkillHandler.java`, `MsgIds.java`, `SkillFeign.java`


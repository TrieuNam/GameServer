# Client Proto Mapping Reference

Tai lieu nay la quick reference de doi chieu client-server khi lam mapping MsgId/proto.

## Scope
- Client source root: `D:/project/serverGame/document/client/LineR/assets`
- Server source root: `D:/project/serverGame/GameServer`
- Focus: message-based mapping (MsgId + proto + ws handler + service contract)

## Canonical Anchors
- Client MsgId map: `document/client/LineR/assets/script/manager/MsgIdManger.ts`
- Client proto schema/runtime:
  - `document/client/LineR/assets/script/proto/proto.d.ts`
  - `document/client/LineR/assets/script/proto/proto.js`
- Client skill config:
  - `document/client/LineR/assets/resources/config/single_skill_auto.json`
  - `document/client/LineR/assets/resources/config/passive_skill_auto.json`

- Server MsgId constants:
  - `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java`
- Server ws handlers:
  - `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler`
- Server proto source:
  - `GameServer/common-lib/src/main/proto/sc/msgskill.proto`

## Skill/Talent Mapping (Current Baseline)
- Skill request: `1470` (`PB_CSRoleSkillOperaReq`)
- Skill response: `1471` (`PB_SCRoleSkillAllInfo`)
- Talent request: `1480` (`PB_CSRoleTalentOperaReq`)
- Talent response: `1481` (`PB_SCRoleTalentAllInfo`)

Server flow anchors:
- WS handler: `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/skill/SkillHandler.java`
- Feign: `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/service/client/SkillFeign.java`
- Role APIs:
  - `/api/skill/{roleId}`
  - `/api/skill/{roleId}/learn`
  - `/api/skill/{roleId}/one-key-level-up`
  - `/api/talent/{roleId}`
  - `/api/talent/{roleId}/learn`

## Repeatable Mapping Checklist
- [ ] Tim MsgId o client (`MsgIdManger.ts`).
- [ ] Xac nhan ten request/response proto o client (`proto.d.ts`/`proto.js`).
- [ ] Doi chieu MsgId trong server `MsgIds.java`.
- [ ] Tim handler ws phu hop va xac nhan `interests()`.
- [ ] Doi chieu contract xuong service (Feign/gRPC + DTO).
- [ ] Xac nhan response emit dung MsgId + dung proto fields.
- [ ] Kiem tra backward compatibility (khong doi field/semantics ngoai scope).

## Common Drift Risks
- Client proto field camelCase vs server snake_case mapping sai.
- MsgId dung o client nhung chua dang ky o `MsgIds.java`.
- Handler co parse request nhung emit sai SC MsgId.
- Config data khac nhau giua `*_auto.json` (client) va config-service (server).

## Quick Verify Commands (PowerShell)
```powershell
Set-Location "D:/project/serverGame"
Select-String -Path "document/client/LineR/assets/script/manager/MsgIdManger.ts" -Pattern "1470|1471|1480|1481"

Set-Location "D:/project/serverGame/GameServer"
Select-String -Path "webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java" -Pattern "1470|1471|1480|1481"
Select-String -Path "webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/skill/SkillHandler.java" -Pattern "Emitters.emit|CS_ROLE_SKILL|CS_ROLE_TALENT"
```


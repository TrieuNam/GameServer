---
name: gameserver-skill-memory
description: Operational skill for working in GameServer microservices. Use for bug fix, feature edit, review, refactor, and context recovery tasks. Triggers on skill memory, gameserver context, review checklist, risk checklist, workflow playbook.
---

# GameServer Skill Memory

Skill nay la runbook thuc chien de agent lam viec on dinh trong `GameServer`.

## Identity
- Workspace: `D:\project\serverGame`
- Project root: `D:\project\serverGame\GameServer`
- Type: Java Maven multi-service backend
- Main build file: `GameServer/pom.xml`
- Runtime logs: `D:\project\serverGame\build_logs`

## Activation Rules
Kich hoat skill nay khi co 1 trong cac truong hop:
- User can sua code theo service path cu the.
- User can review code va can finding theo muc do.
- User can tranh quen context project/traces/docs.
- Task co rui ro cross-service (API, DTO, DB, event).

## Non-Negotiable Rules
- Luon doc file lien quan truoc khi sua.
- Khong revert thay doi cua user neu user khong yeu cau.
- Sua toi thieu, dung scope.
- Neu co the verify, phai verify bang test/check phu hop.
- Neu chua verify runtime/build, phai noi ro "chua verify".
- Neu thay doi bat ngo khong do minh tao ra trong luc dang sua, tam dung va hoi user.

## Startup Protocol (2-minute)
- [ ] Doc lai request va output user muon.
- [ ] Xac dinh service/module theo path user dua.
- [ ] Tim 3 nhom file: implementation, test, config/docs.
- [ ] Kiem tra anh huong cheo service.
- [ ] Chot pham vi sua toi thieu truoc khi edit.

## Repository Anchor Map
- `GameServer/README.md`
- `GameServer/SERVICES_SUMMARY.md`
- `GameServer/docs/TESTING_GUIDE_STEP_BY_STEP.md`
- `GameServer/docs/SERVICE-PORT-DB-MAPPING.md`
- `GameServer/docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/SKILL.md`
- `GameServer/task-service/src/test/java/com/SouthMillion/task_service/service/TaskDomainServiceTest.java`

## Client Source Anchor Map (for mapping)
- Client root: `D:/project/serverGame/document/client/LineR/assets`
- MsgId map: `D:/project/serverGame/document/client/LineR/assets/script/manager/MsgIdManger.ts`
- Client proto runtime: `D:/project/serverGame/document/client/LineR/assets/script/proto/proto.js`
- Client proto typing: `D:/project/serverGame/document/client/LineR/assets/script/proto/proto.d.ts`
- Client skill config: `D:/project/serverGame/document/client/LineR/assets/resources/config/single_skill_auto.json`
- Client passive skill config: `D:/project/serverGame/document/client/LineR/assets/resources/config/passive_skill_auto.json`
- Server proto source: `GameServer/common-lib/src/main/proto/sc/msgskill.proto`
- Server ws MsgId constants: `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java`

## Client-Server Mapping Workflow
- [ ] Xac nhan MsgId tu client (`MsgIdManger.ts`) va ten proto request/response.
- [ ] Xac nhan struct field trong `proto.d.ts`/`proto.js` de tranh lech field name.
- [ ] Kiem tra server `MsgIds.java` da map dung cap CS/SC chua.
- [ ] Kiem tra ws handler theo flow: `handle -> Feign/grpc -> Emitters.emit`.
- [ ] Kiem tra DTO va REST/grpc contract o service dich.
- [ ] Kiem tra config parity neu co (vd skill/passive_skill auto vs server config).
- [ ] Neu thay doi contract, cap nhat memory + ghi ro backward compatibility impact.

## Task Playbooks

### Playbook A - Bug Fix
- [ ] Tai hien loi neu co buoc.
- [ ] Tim root cause dung layer (controller/service/repository/integration).
- [ ] Sua toi thieu, khong doi behavior ngoai scope.
- [ ] Bo sung/chinh test de chan regression.
- [ ] Verify + note risk con lai.

### Playbook B - Small Feature
- [ ] Chot contract vao/ra (request/response/event).
- [ ] Sua theo luong API -> domain -> persistence.
- [ ] Cap nhat test don vi/tich hop toi thieu.
- [ ] Check backward compatibility.

### Playbook C - Refactor
- [ ] Xac nhan baseline test truoc khi doi cau truc.
- [ ] Khong doi nghiep vu neu user khong yeu cau.
- [ ] Refactor tung buoc nho de de review.
- [ ] Chay lai test sau refactor.

### Playbook D - Code Review
- [ ] Tim bug/runtime risk truoc style.
- [ ] Liet ke finding theo muc do: Critical -> High -> Medium -> Low.
- [ ] Moi finding co du: file, line, impact, de xuat fix.
- [ ] Neu khong co finding, ghi ro va neu test gap con thieu.

## Risk Gate (Before Done)
- [ ] API contract co doi khong?
- [ ] DTO/schema/query co thay doi nhay cam khong?
- [ ] Null/empty/invalid inputs da duoc cover khong?
- [ ] Retry/timeout/transaction co bi anh huong khong?
- [ ] Da co test bao ve regression chua?
- [ ] MsgId/proto mapping client-server da doi chieu 2 chieu chua?

## Command Snippets (PowerShell)
Lenh mau de verify co ban, can chay o dung thu muc.

```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn -T 1C clean install -DskipTests

Set-Location "D:\project\serverGame\GameServer\task-service"
mvn test
mvn -Dtest=TaskDomainServiceTest test

Set-Location "D:\project\serverGame\GameServer"
```

## Response Templates

### Template 1 - Task Receipt
"Da nhan task. Ke hoach: (1) doc file lien quan, (2) sua toi thieu theo scope, (3) verify bang test/check, (4) bao cao thay doi + cach kiem tra."

### Template 2 - Change Report
"Da sua trong `<path>`. Thay doi chinh: `<item>`. Ly do: `<reason>`. Da verify: `<test/check>`. Rui ro con lai: `<neu co>`."

### Template 3 - Review Report
"Da review. Findings theo muc do: ... Neu khong co finding nghiem trong, neu ro test gap/risk con lai."

## Service Memory Index
Khi 1 service lap lai nhieu task, tao memory rieng theo mau:
- `ACTIVITY_SERVICE_MEMORY.md`
- `TASK_SERVICE_MEMORY.md`
- `USER_SERVICE_MEMORY.md`
- `GATEWAY_SERVICE_MEMORY.md`

### Canonical Memory Paths
- Canonical service memories live in:
	`GameServer/.github/skills/gameserver-skill-memory/service-memories/*.md`
- Legacy nested folder exists for historical reasons:
	`GameServer/.github/skills/gameserver-skill-memory/service-memories/service-memories/*.md`
- Rule: prefer canonical top-level files; do not use nested legacy copies unless explicitly requested.

Mau section cho moi service memory:
- Scope nghiep vu
- API + docs anchor
- DTO/entity map
- Test anchors
- Known bugs + fix patterns

## Weekly Maintenance
- [ ] Them bug patterns moi.
- [ ] Bo quy tac da loi thoi.
- [ ] Re-check cac path tham chieu.
- [ ] Cap nhat service uu tien cao.

## Update Log Template

```md
- YYYY-MM-DD | Scope: <service/module> | Change: <tom tat ngan> | Why: <ly do> | Ref: <path hoac ticket>
```

## Update Log
- 2026-03-21 | Scope: role-service + client skill transport | Change: thêm Flyway migration cho `role_skill`/`role_talent`, chặn one-key level-up vượt max, bổ sung client MsgId + `SkillCtrl`/`SkillData` + `CfgSingleSkill` | Why: memory trước đó ghi đã hoàn tất nhưng thực tế còn thiếu persistence và client transport/state tối thiểu | Ref: `V2__add_role_skill_and_role_talent.sql`, `SkillService.java`, `MsgIdManger.ts`, `SkillCtrl.ts`
- 2026-03-21 | Scope: skill_agent | Change: move vào ai-agents-skills và giữ structure SKILL-style | Why: để tham khảo và tái sử dụng dễ hơn | Ref: `GameServer/docs/skill_agent/ai-agents-skills/skills/gameserver-skill-memory/SKILL.md`
- 2026-03-21 | Scope: role-service + webSocket-server | Change: triển khai hệ thống kỹ năng (skill/talent) đầy đủ | Why: proto msgskill 1470-1481 chưa có handler | Ref: `SkillService.java`, `SkillController.java`, `SkillHandler.java`, `SkillFeign.java`
- 2026-03-21 | Scope: skill_agent memory | Change: bo sung client anchor map (`document/client/LineR/assets`) va workflow doi chieu msg/proto | Why: giam sai lech khi map client-server | Ref: `SKILL.md`, `references/CLIENT_PROTO_MAPPING_REFERENCE.md`
- 2026-03-21 | Scope: skill_agent memory hygiene | Change: define canonical memory path and deprecate nested legacy copies | Why: avoid stale memory selection during agent context load | Ref: `service-memories/ROLE_SERVICE_MEMORY.md`, `service-memories/WEBSOCKET_SERVER_MEMORY.md`


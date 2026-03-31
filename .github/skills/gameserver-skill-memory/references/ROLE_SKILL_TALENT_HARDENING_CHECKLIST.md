# Role Skill/Talent Hardening + Test Coverage Checklist

Checklist implement cho server side (role-service + webSocket-server), uu tien theo rui ro production.

## Scope
- Service chinh: `GameServer/role-service`
- Transport layer: `GameServer/webSocket-server`
- Shared DTO/proto: `GameServer/common-lib`

## Current Gaps (verified)
- Wallet/economy integration chua co trong `SkillService`.
- Max level/cost logic dang hard-coded trong service.
- Chua co test cho `SkillHandler` (webSocket-server).
- Test `SkillService` moi cover mot phan happy path + input invalid.
- Build local role-service dang gap compile issue lien quan `SkillDTOs` resolution.

## Phase 0 - Build Baseline (Blocking)
Goal: dam bao local va CI co the chay test truoc khi hardening.

1. Confirm dependency resolution cho `org.SouthMillion:common-lib:1.0.0`.
- Check parent reactor build from `GameServer/pom.xml`.
- Check artifact availability in active Maven local repository path.
- Estimate: 0.5 day
- Done when:
  - `mvn -pl common-lib,role-service -am test -DskipTests` compile pass.

2. Stabilize role-service test command in docs/memory.
- Add a reproducible command sequence in memory/runbook.
- Estimate: 0.25 day
- Done when:
  - Team can run `role-service` test command without manual workaround.

## Phase 1 - Economy Hardening (Role Service)
Goal: hoc/nang cap skill,talent phai check/tru tai nguyen atomically.

1. Define upgrade cost contract.
- Decide source of truth:
  - Option A: config-service driven cost table.
  - Option B: static config loaded in role-service startup.
- Include level-based cost for skill and talent.
- Estimate: 0.5 day
- Done when:
  - Cost contract reviewed and documented.

2. Integrate wallet deduction flow in `SkillService`.
- Add wallet client (Feign or gRPC) for check+deduct.
- Enforce: no level mutation if deduct fails.
- Map business errors (insufficient currency, wallet timeout, invalid role).
- Estimate: 1 day
- Done when:
  - `learnSkill`, `learnTalent`, `oneKeyLevelUp` all enforce cost rules.
  - Failure path does not mutate DB state.

3. Transaction boundary and idempotency guard.
- Keep lock + DB transaction semantics consistent.
- Avoid double-deduct in retry scenarios.
- Add operation id or deterministic retry-safe handling if needed.
- Estimate: 0.75 day
- Done when:
  - Concurrency test demonstrates no duplicate deduction.

## Phase 2 - Config Parity Hardening
Goal: server behavior khop voi config/proto expectation.

1. Remove hard-coded max level (20) from `SkillService`.
- Read max from authoritative config.
- Validate fallback behavior if config missing.
- Estimate: 0.5 day
- Done when:
  - Max level change can be applied via config only.

2. Validate skillId/talentId existence against config.
- Reject unknown ids before DB mutate.
- Estimate: 0.5 day
- Done when:
  - Invalid ids return safe response and no DB write.

## Phase 3 - Test Coverage Expansion
Goal: chan regression o domain va transport.

1. Expand `SkillServiceTest`.
- Add tests:
  - wallet fail => no level up
  - wallet success => level up + response update
  - max level boundary for skill and talent
  - one-key mixed list (some max, some not)
  - concurrent learn/upgrade on same role (lock semantics)
- Estimate: 1 day
- Done when:
  - >= 12 focused tests pass for skill/talent business paths.

2. Add `SkillHandlerTest` in webSocket-server.
- Add tests:
  - parse 1470/1480 payload and dispatch by reqType
  - emit 1471/1481 with proper proto fields
  - invalid payload handled without crashing session
  - downstream feign error path logs + graceful return
- Estimate: 1 day
- Done when:
  - Handler test class exists and passes in module test run.

3. Add repository/integration coverage for one-key update.
- Verify `incrementAllLevels` updates only levels `< max`.
- Estimate: 0.5 day
- Done when:
  - Integration test asserts row-level update count and final state.

## Phase 4 - Client/Server Contract Safety
Goal: tranh drift MsgId/proto/field mapping.

1. Contract verification checklist in CI (lightweight).
- Validate presence of MsgId pairs and proto messages for 1470/1471/1480/1481.
- Estimate: 0.5 day
- Done when:
  - CI step fails if required symbol mapping missing.

2. Backward compatibility notes.
- Document request/response changes and migration notes.
- Estimate: 0.25 day
- Done when:
  - Release notes include compatibility section.

## Suggested Delivery Plan
- Sprint 1 (2.5-3 days): Phase 0 + Phase 1
- Sprint 2 (2-2.5 days): Phase 2 + Phase 3(1)
- Sprint 3 (1.5-2 days): Phase 3(2,3) + Phase 4

Total estimate: 6-8 engineering days

## Fast-Track Minimum (if need quick production safety)
- Must-do:
  - Phase 0
  - Phase 1 (all)
  - Phase 3 item 1 (wallet + boundary tests)
- Fast-track estimate: 2.5-3.5 days

## Verification Commands
```powershell
Set-Location "D:\project\serverGame\GameServer"
mvn -pl common-lib,role-service,webSocket-server -am test

Set-Location "D:\project\serverGame\GameServer\role-service"
mvn -Dtest=SkillServiceTest test

Set-Location "D:\project\serverGame\GameServer\webSocket-server"
mvn test
```

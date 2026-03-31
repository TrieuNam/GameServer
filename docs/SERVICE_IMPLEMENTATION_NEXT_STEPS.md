# Service Implementation Notes (Handover)

> Updated: 2026-03-21
> Purpose: Backlog implement service sau khi xong front-end.

## Scope
Tài liệu này gom các service còn cần implement/hoàn thiện để tiếp tục backend theo thứ tự ưu tiên, có checklist rõ ràng cho từng service.

---

## 1) Current Snapshot (da lam)

### webSocket-server
- [x] `TrialHandler` da migrate sang gRPC (`TrialGrpcClient`), bo phu thuoc REST cho flow trial chinh.
- [x] Them `BattleHandler` de route battle qua `BattleServerGrpcClient`.
- [x] Them `MsgIds` battle:
  - `CS_BATTLE_REQ = 9650`
  - `SC_BATTLE_RESP = 9651`
- [x] `WorldHandler` da toi uu broadcast movement:
  - dung `getNearbyPlayers(...)` thay vi query full zone.

### battleserver-service
- [x] Fix publish event logic trong `CombatServiceGrpcImpl`:
  - compare `winnerId` dung type `Long`.
  - fallback `combatType` null-safe.
- [x] `endCombat(...)` da publish combat event (session-mode combat cung vao analytics).

### Test/Verify (da chay)
- [x] `webSocket-server` compile + targeted tests.
- [x] `battleserver-service` compile + targeted tests (`CombatServiceGrpcImplTest`).

---

## 2) Priority Backlog (nen lam tiep)

## P0 - Core Flow Bat Buoc

### A. world-service + gameworld-service (hoan thien gRPC flow)
**Muc tieu:** bo REST fallback cho world movement/interact/pickup, dong bo mot luong world qua gRPC.

- [ ] Rasoat `WorldHandler`: endpoint nao con goi `WorldFeign` thi migrate sang gRPC client tuong ung.
- [ ] Bo sung/hoan thien gRPC methods cho pickup/interact neu service chua co.
- [ ] Dinh nghia ro behavior timeout/fallback (khong block event loop, co ACK loi ro rang).
- [ ] Add integration test: move/pickup/interact trong dieu kien service cham/down.

**Acceptance Criteria**
- [ ] Khong con call REST cho world critical path (move/pickup/interact).
- [ ] ACK tra ve on dinh khi downstream loi.
- [ ] Co log metrics timeout theo msgId.

---

### B. battleserver-service (event model day du)
**Muc tieu:** event combat dung cho analytics/leaderboard/anti-cheat.

- [ ] Publish event cho ca attacker va defender (2 stream perspective neu can).
- [ ] Chuan hoa event field: `combatType`, `duration`, `damage`, `winner`, `sessionId`.
- [ ] Dam bao event key partition hop ly (theo roleId hoac sessionId).
- [ ] Add test verify event payload cho:
  - [ ] `calculateCombat`
  - [ ] `endCombat`

**Acceptance Criteria**
- [ ] Event schema on dinh, khong null field critical.
- [ ] Consumer analytics doc duoc va khong can map workaround.

---

### C. webSocket-server battle protocol stabilization
**Muc tieu:** protocol battle on dinh giua client va server.

- [ ] Chot payload contract `CS_BATTLE_REQ`:
  - [ ] JSON format chinh thuc (field names/type)
  - [ ] Binary fallback co can giu hay khong
- [ ] Chot op codes:
  - [ ] `1 = calculate`
  - [ ] `2 = start session`
  - [ ] `3 = execute action`
  - [ ] `4 = end session`
- [ ] Bo sung validation payload + error code map.
- [ ] Add document cho FE: request/response examples.

**Acceptance Criteria**
- [ ] FE co 1 contract file de implement khong doan.
- [ ] BattleHandler khong throw khi payload thieu field, tra loi JSON loi co cau truc.

---

## P1 - High Value

### D. arena-service performance path
- [ ] Verify Arena handler da dung gRPC 100% (khong fallback REST).
- [ ] Benchmark latency arena operations peak-time.
- [ ] Neu can, them cache rank snapshot theo interval.

### E. trial-service completion
- [ ] Ra soat trial reward flow qua gRPC (claim/reset/stage).
- [ ] Add regression tests cho trial operation matrix.

### F. observability
- [ ] Them metrics theo handler/msgId: success/error/timeout/p95.
- [ ] Dashboard nhanh cho:
  - [ ] battle
  - [ ] world move
  - [ ] trial

---

## P2 - Medium / Nice-to-have

### G. Kafka consumers follow-up
- [ ] analytics-service consume combat event chuan schema moi.
- [ ] leaderboard-service consume battle event de update rank async.
- [ ] notification-service consume event ket qua tran dau (neu gameplay can).

### H. service memory/docs synchronization
- [ ] Cap nhat docs service memory cho cac module vua migrate.
- [ ] Dong bo `SERVICE_MEMORY_INDEX` voi status thuc te.

---

## 3) Service-by-Service Task List

## webSocket-server
- [ ] Finalize battle protocol docs.
- [ ] Remove unnecessary REST fallback trong world path.
- [ ] Add integration test suite cho handlers: battle/world/trial.

## battleserver-service
- [ ] Hoan thien event payload + 2-perspective publish.
- [ ] Add test coverage cho event publishing all paths.
- [ ] Verify kafka topic naming consistency (`combat.result`).

## world-service / gameworld-service
- [ ] Ensure gRPC API cover full world actions (move/pickup/interact/zone updates).
- [ ] Validate near-player query correctness (radius, maxPlayers, perf).

## arena-service
- [ ] Verify gRPC path + stress test.
- [ ] Clean stale TODO/skeleton if any.

## trial-service
- [ ] Re-check all operation branches with gRPC response mapping.
- [ ] Add invalid payload and timeout behavior tests.

---

## 4) Suggested Execution Order (sau khi xong FE)

- [ ] Step 1: Chot battle contract (`CS_BATTLE_REQ/SC_BATTLE_RESP`) + FE examples.
- [ ] Step 2: Hoan tat world gRPC-only path (pickup/interact).
- [ ] Step 3: Chuan hoa combat event schema + consumers analytics/leaderboard.
- [ ] Step 4: Bo sung observability + load/regression tests.
- [ ] Step 5: Dong bo docs/service memory.

---

## 5) Quick Verify Commands

```powershell
Set-Location "D:\project\serverGame\GameServer\webSocket-server"
mvn -q -DskipTests compile
mvn -q "-Dtest=WorldHandlerTest,BattleHandlerTest" test
```

```powershell
Set-Location "D:\project\serverGame\GameServer\battleserver-service"
mvn -q -DskipTests compile
mvn -q "-Dtest=CombatServiceGrpcImplTest" test
```

---

## 6) Notes for Resume Context

Khi quay lai implement, uu tien mo cac file sau truoc:
- `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/battle/BattleHandler.java`
- `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/world/WorldHandler.java`
- `battleserver-service/src/main/java/com/SouthMillion/battleserver_service/grpc/CombatServiceGrpcImpl.java`
- `webSocket-server/src/main/java/com/SouthMillion/webSocket_server/net/MsgIds.java`

Neu can boi canh nhanh cho FE, dung section "Priority Backlog" + "Service-by-Service Task List" la du de chia task.


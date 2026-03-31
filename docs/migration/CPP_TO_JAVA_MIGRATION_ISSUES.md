# C++ → Java Migration Guide — Những Chỗ Sai & Cần Implement Lại

> **Mục đích:** So sánh chi tiết giữa C++ original (`开箱h5/`) và Java microservices (`GameServer/`)  
> **Cập nhật:** 2026-03-02  
> **Trạng thái:** 🟡 Critical fixes DONE — High priority đang làm

---

## 📋 Mục Lục

1. [Tổng Quan Sai Biệt](#1-tổng-quan-sai-biệt)
2. [Protocol — MsgId Mapping](#2-protocol--msgid-mapping)
3. [Message Packet Format](#3-message-packet-format)
4. [Gateway — Luồng Kết Nối Khác](#4-gateway--luồng-kết-nối-khác)
5. [Login Flow & Guard](#5-login-flow--guard)
6. [Handler Status — Từng Module](#6-handler-status--từng-module)
7. [Service Thiếu — Không Có Trong Java](#7-service-thiếu--không-có-trong-java)
8. [gRPC Migration — P2 Services](#8-grpc-migration--p2-services)
9. [Database Schema Sai Biệt](#9-database-schema-sai-biệt)
10. [Checklist & Thứ Tự Implement](#10-checklist--thứ-tự-implement)

---

## 1. Tổng Quan Sai Biệt

### C++ Original Architecture

```
Client (H5) → Python Gateway (Tornado ws) → C++ GameWorld (TCP) → MySQL
```

### Java Current Architecture

```
Client (H5) → Spring WebSocket-Server (port 8094, path /ws/game)
            → Microservices (REST Feign / gRPC) → MySQL per service
```

### Status Tổng Hợp

| # | Vấn đề | Mức độ | Status |
|---|--------|--------|--------|
| 1 | **Arena MsgId** — `CS_ARENA_REQ` was 2300 → phải 9610 | 🔴 Critical | ✅ **FIXED** |
| 2 | **Cross Arena thiếu** — CS 9613, SC 9614/9615/9616 chưa có | 🔴 Critical | ✅ **FIXED** |
| 3 | **Guild MsgId** — `CS_GUILD_REQ` was 2000 → phải 9640 | 🔴 Critical | ✅ **FIXED** |
| 4 | **Login guard** — WsGatewayHandler không chặn unauthenticated msgs | 🔴 Critical | ✅ **FIXED** |
| 5 | **WebSocket path** — Java `/ws/game`, C++ root `/` — client cần đổi URL | 🟠 High | Client-side fix |
| 6 | **P2 services dùng REST thay gRPC** — Arena/trial/escort latency cao | 🟠 High | ⏳ Đang làm |
| 7 | **ShenQi ops 8-9** — Handler OK, cần verify artifact-service backend | 🟡 Medium | ⏳ Cần verify |
| 8 | **WaBao SC completeness** — Cần verify đủ 10 SC messages 1642–1651 | 🟡 Medium | ⏳ Cần verify |
| 9 | **CrossServer design** — Java Redis HMAC vs C++ TCP inter-server | 🟡 Medium | Different design |
| 10 | **Battle mock** — Arena battle là random win/lose, chưa real damage | 🟡 Medium | ⏳ Sau |

---

## 2. Protocol — MsgId Mapping

### Vấn đề root cause

`MsgIds.java` có 2 nhóm MsgId sai so với client gốc `MsgIdManger.ts`:

### ✅ FIXED — `MsgIds.java` sau khi sửa

```java
// ===== Guild (FIXED: was 2000/2001)
int CS_GUILD_REQ         = 9640; // PB_CSGuildReq
int SC_GUILD_SEARCH_LIST = 9641; // PB_SCGuildSearchList  (NEW)
int SC_GUILD_INFO        = 9642; // PB_SCGuildInfo
int SC_GUILD_REPORT_LIST = 9643; // PB_SCGuildReportList  (NEW)
int SC_GUILD_MEMBER_LIST = 9644; // PB_SCGuildMemberList  (NEW)
int SC_GUILD_APP_LIST    = 9645; // PB_SCGuildAppList     (NEW)
int SC_GUILD_ROLE_INFO   = 9646; // PB_SCGuildRoleInfo    (NEW)

// ===== Arena (FIXED: was 2300/2301/2302)
int CS_ARENA_REQ              = 9610; // PB_CSArenaReq
int SC_ARENA_INFO             = 9611; // PB_SCArenaInfo
int SC_ARENA_REPORT_LIST      = 9612; // PB_SCArenaReportList
int CS_CROSS_ARENA_REQ        = 9613; // PB_CSCrossArenaReq   (NEW)
int SC_CROSS_ARENA_INFO       = 9614; // PB_SCCrossArenaInfo   (NEW)
int SC_CROSS_ARENA_REPORT_LIST= 9615; // PB_SCCrossArenaReportList (NEW)
int SC_CROSS_ARENA_FIGHT_RET  = 9616; // PB_SCCrossArenaFightRet   (NEW)
```

### Bảng đầy đủ các MsgId đã verify

| Hệ thống | CS MsgId | SC MsgId | Handler | Status |
|----------|----------|----------|---------|--------|
| Login | 7056 | 7000 | LoginBootstrapHandler | ✅ Đúng |
| Heartbeat | 1053 | 1003 | HeartbeatTimeHandler | ✅ Đúng |
| Role | — | 1400–1403 | RoleServiceHandler | ✅ Đúng |
| Bag | 1500, 1501 | 1505–1508 | BagHandler | ✅ Đúng |
| Equip | 1600 | 1601–1609 | EquipHandler | ✅ Đúng |
| Box | 1610 | 1642 | BoxHandler | ✅ Đúng |
| ShenQi | 1675 | 1676–1680 | ShenQiHandler | ✅ Đúng |
| WaBao | 1640–1641 | 1642–1651 | WaBaoHandler | ✅ Đúng |
| Pet | 2100 | 2101–2110 | PetHandler | ✅ Đúng |
| Mount | 2140 | 2141–2149 | MountHandler | ✅ Đúng |
| Angel | 2130 | 2131–2139 | AngelHandler | ✅ Đúng |
| StarMap | 2150 | 2151–2159 | StarMapHandler | ✅ Đúng |
| Rune | 1670 | 1671–1680 | RuneHandler | ✅ Đúng |
| Activity | 3000 | 3001+ | ActivityHandler | ✅ Đúng |
| Mail | 9551 | 9501/9504–9506 | MailHandler | ✅ Đúng |
| **Arena** | **9610** | **9611, 9612** | ArenaHandler | ✅ **FIXED** |
| **Cross Arena** | **9613** | **9614, 9615, 9616** | ArenaHandler | ✅ **FIXED** |
| **Guild** | **9640** | **9641–9646** | GuildHandler | ✅ **FIXED** |
| Escort | 9620 | 9621–9626 | EscortHandler | ✅ Đúng (hardcode) |
| Rank | 9602 | 9601 | RankHandler | ✅ Đúng |

---

## 3. Message Packet Format

### Kết luận: ✅ Format đúng, không cần fix

**C++ Python Gateway** (`ws_handler.py`):
```
Frame: [4B total_len] [4B msgId] [N bytes protobuf]
total_len = 4 + N
```

**Java `PacketCodec.java`**:
```java
// decode: đọc bodyLen(4B) + msgId(4B) + payload(bodyLen-4 bytes)
// encode: write bodyLen(4B) + msgId(4B) + payload bytes
// → Format giống hệt, semantics tương đương
```

⚠️ **Note:** Java dùng `BIG_ENDIAN` — client TypeScript phải gửi Big Endian integers.

---

## 4. Gateway — Luồng Kết Nối Khác

| Aspect | C++ Python Gateway | Java Spring WebSocket |
|--------|-------------------|-----------------------|
| Port | config XML `listen_port_for_user` | **8094** |
| **Path** | `/` (root) | **`/ws/game`** |
| NetID | Pool 0–65534 | Spring Session UUID |
| Queue | 2 queues × 100,000, consume 1ms | Spring Virtual Threads direct |
| Internal protocol | WGProtocol TCP (MT_HAS_CHECK etc.) | Direct Java method call |
| CORS | `check_origin() → True` | Spring Security config |

### 🟠 Client cần đổi URL (chưa fix — phía client)

```typescript
// ❌ C++ Python Gateway (root path)
ws://host:port/

// ✅ Java Spring WebSocket
ws://host:8094/ws/game
```

---

## 5. Login Flow & Guard

### C++ flow có MT_HAS_CHECK guard

```
1. Client → CS:7056 PB_CSLoginToAccount
2. Gateway → GameWorld TCP
3. GameWorld → Gateway: WGProtocol MT_HAS_CHECK
4. Gateway: ws_handler.has_login = True  ← chỉ sau bước này mới nhận broadcasts
5. GameWorld → SC:7000 → Client
```

### Java flow — ✅ FIXED: Login guard đã được thêm

```java
// WsGatewayHandler.dispatch() — ADDED:
if (!ps.isLoggedIn() && msgId != 7056 && msgId != 1053) {
    log.warn("[gateway] Reject msgId={} — unauthenticated", msgId);
    return Mono.empty();  // ← Guard như C++ has_login check
}

// LoginBootstrapHandler — đã có:
ps.setLoggedIn(true);  // set khi introspect token thành công
```

---

## 6. Handler Status — Từng Module

### 6.1 Arena Handler ✅ FIXED

**Thay đổi:**
- `interests()` → `{9610, 9613}` (was `{CS_ARENA_REQ = 2300}`)
- `handle()` route msgId 9613 → `handleCrossArenaMsg()`
- Thêm 4 methods: `handleCrossArenaGetInfo`, `handleCrossArenaChallenge`, `handleCrossArenaRefresh`, `handleCrossArenaRevenge`

```java
// ArenaHandler.java — sau fix
@Override
public int[] interests() {
    return new int[]{MessageIds.CS_ARENA_REQ, MessageIds.CS_CROSS_ARENA_REQ}; // 9610, 9613
}
```

**Cross Arena ops (CS 9613):**
- type 0 → GET_INFO → SC:9614 PB_SCCrossArenaInfo
- type 1 → CHALLENGE(p1=targetIndex) → SC:9616 PB_SCCrossArenaFightRet + refresh 9614
- type 2 → REFRESH → refresh matchmaking + SC:9614
- type 3 → REVENGE(p1=targetIndex) → same as CHALLENGE

### 6.2 Guild Handler ✅ FIXED

**Thay đổi:**
- `interests()` → `{CS_GUILD_REQ = 9640}` (was 2000)
- `sendGuildInfoResponse()` → `session.send(SC_GUILD_INFO=9642, ...)`
- `sendGuildMembersResponse()` → `session.send(SC_GUILD_MEMBER_LIST=9644, ...)`
- `sendGuildSearchResponse()` → `session.send(SC_GUILD_SEARCH_LIST=9641, ...)`

### 6.3 Escort Handler ✅ Đúng (hardcode 9620)

```java
@Override
public int[] interests() { return new int[]{9620}; }
```

**Cần verify** SC messages đủ 9621–9626.

### 6.4 ShenQi Handler — Ops 8-9 ĐÃ IMPLEMENT (khác status report cũ)

- op=8 `handleDraw()` → `artifactFeign.drawArtifacts()` → SC:1679
- op=9 `handleGetRecords()` → `artifactFeign.getDrawRecords()` → SC:1680

**Cần verify:** `artifact-service` có endpoint `POST /api/artifact/{id}/draw` và `GET /api/artifact/{id}/records` chưa?

### 6.5 WaBao Handler — Cần verify 10 SC messages

```
SC 1642 PB_SCWaBaoInfo         ← base info
SC 1643 PB_SCWaBaoMapInfo      ← map
SC 1644 PB_SCWaBaoItemInfo     ← item found
SC 1645 PB_SCWaBaoIntegrityInfo
SC 1646 PB_SCWaBaoCollectionListInfo
SC 1647 PB_SCWaBaoToolInfo
SC 1648 PB_SCWaBaoTaskInfo
SC 1649 PB_SCWaBaoSetingInfo   ← auto-sweep settings
SC 1650 PB_SCWaBaoCollectionBookInfo
SC 1651 PB_SCWaBaoBookListInfo
```

### 6.6 LingZhu — Tên proto dễ nhầm

```protobuf
// SC:2009 PB_SCLingZhuInfo chứa PB_CSLingZhuData
// "CS" trong tên là DATA type, không phải CS message
message PB_SCLingZhuInfo {
    repeated PB_CSLingZhuData lingzhu_list = 1;  // ← data struct
}
```

---

## 7. Service Thiếu — Không Có Trong Java

### 7.1 Scene/World System — ✅ Scope out có chủ ý

H5 version bỏ 3D world. AOI/movement/NPC không cần implement.

### 7.2 BattleServer — Mock combat

Arena battle hiện tại là random win/lose. Cần implement:

```java
// battleserver-service/BattleService.java
// 1. Load 2 roles' attrs (HP, ATK, DEF, SPD)
// 2. finalDmg = ATK * factor - DEF * factor2
// 3. Turn order by SPD
// 4. Return PB_SCBattleReport với battle_rounds, winner, damage log
```

### 7.3 WaBao Auto-Sweep Stop Conditions

```protobuf
// PB_WaBaoSet { eqality, eqality_mark, new_record, new_book }
// Java WaBaoHandler CS:1641 PB_CSWaBaoSetReq — verify có xử lý stop conditions không
```

---

## 8. gRPC Migration — P2 Services

### Tại sao cần migrate

| Service | Tần suất | Latency REST hiện tại | Target gRPC |
|---------|----------|-----------------------|-------------|
| arena-service | 10-20 req/min/player | ~15ms | ~5ms |
| trial-service | 5-10 req/min/player | ~12ms | ~4ms |
| escort-service | 2-5 req/min/player | ~10ms | ~3ms |
| territory-service | 1-2 req/min/player | ~8ms | ~3ms |

### Arena gRPC Migration Steps

**Bước 1:** `common-lib/src/main/proto/arena_service.proto`
```protobuf
syntax = "proto3";
service ArenaService {
  rpc GetArenaInfo(ArenaInfoRequest) returns (ArenaInfoResponse);
  rpc Challenge(ChallengeRequest) returns (ChallengeResponse);
  rpc GetOpponents(ArenaOpponentsRequest) returns (ArenaOpponentsResponse);
  rpc ClaimRewards(ArenaRewardRequest) returns (ArenaRewardResponse);
  rpc GetHistory(ArenaHistoryRequest) returns (ArenaHistoryResponse);
  rpc BuyChallenge(BuyArenaRequest) returns (BuyArenaResponse);
}
```

**Bước 2:** `arena-service/ArenaServiceGrpcImpl.java` (annotate `@GrpcService`)

**Bước 3:** `webSocket-server/ArenaGrpcClient.java` — implement TODO stubs

**Bước 4:** `ArenaHandler.java` — switch từ `ArenaFeign` → `ArenaGrpcClient`

---

## 9. Database Schema Sai Biệt

### 9.1 Arena — Kiểm tra cross_arena tables

```sql
-- Cần có trong arena_db:
CREATE TABLE IF NOT EXISTS cross_arena_player (
    role_id              BIGINT PRIMARY KEY,
    cross_score          INT DEFAULT 0,
    today_refresh_times  INT DEFAULT 0,
    last_refresh_time    INT DEFAULT 0,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cross_arena_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    attacker_id  BIGINT NOT NULL,
    defender_id  BIGINT NOT NULL,
    is_win       TINYINT NOT NULL,
    score_change INT NOT NULL,
    fight_time   BIGINT NOT NULL,
    INDEX idx_attacker (attacker_id),
    INDEX idx_time (fight_time)
);
```

### 9.2 Escort — ship_key column

```sql
-- escort_db.escort_ship cần có:
ALTER TABLE escort_ship ADD COLUMN IF NOT EXISTS ship_key INT DEFAULT 0;
```

### 9.3 ShenQi — draw_record table

```sql
-- artifact_db cần có:
CREATE TABLE IF NOT EXISTS shenqi_draw_record (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id   BIGINT NOT NULL,
    draw_time INT NOT NULL,
    cell_list VARCHAR(100) NOT NULL,  -- JSON array, max 6 cells
    INDEX idx_role_time (role_id, draw_time DESC)
);
```

### 9.4 Mount — harness 8 attr slots

```sql
-- mount_db.mount_harness_bag phải có:
-- attr_type1..8 INT, attr_value1..8 INT, lock_flag TINYINT
```

---

## 10. Checklist & Thứ Tự Implement

```
CRITICAL — ✅ ĐÃ FIX (2026-03-02):
[x] 1. MsgIds.java: CS_ARENA_REQ 2300 → 9610, SC 2301→9611, 2302→9612
[x] 2. MsgIds.java: Thêm CS_CROSS_ARENA_REQ=9613, SC 9614/9615/9616
[x] 3. MsgIds.java: CS_GUILD_REQ 2000 → 9640, SC 2001→9642, thêm 9641/9643-9646
[x] 4. ArenaHandler.interests(): {9610, 9613} + Cross Arena methods thêm vào
[x] 5. WsGatewayHandler: login guard chặn unauthenticated messages
[x] 6. GuildHandler: send với MessageIds.SC_GUILD_INFO/MEMBER_LIST/SEARCH_LIST
[ ] 7. Client WebSock.ts: đổi URL → ws://host:8094/ws/game  (client-side, skip for now)

HIGH — ✅ ĐÃ FIX (2026-03-02):
[x] 8.  Arena gRPC: @GrpcService trên ArenaServiceGrpcImpl + grpc dep vào arena-service pom
        + grpc.server.port=9084 + grpc client config trong webSocket-server application.yml
[x] 9.  Escort gRPC:
        - escort_service.proto tạo mới tại common-lib/src/main/proto/
        - EscortServiceGrpcImpl.java tạo mới trong escort-service
        - grpc-spring-boot-starter dep thêm vào escort-service pom
        - grpc.server.port=9085 trong escort-service application.yml
        - EscortGrpcClient: stub placeholder (cần `mvn install` common-lib để active)
[x] 10. Artifact-service: verify endpoints POST /draw + GET /draw-records — ĐÃ CÓ ✅
[x] 11. Flyway V1__init_artifact_tables.sql — tạo artifact + artifact_draw_record tables
[x] 12. WaBaoHandler: thêm đủ SC 1643/1645/1646/1647/1648/1650/1651 (total 10 types)
        pushAll() gửi tất cả 9 SC types (1644 item_info skip vì chỉ push khi open)
[x] 13. arena-service V2__add_cross_arena.sql: cross_arena_player + cross_arena_history

MEDIUM — ✅ ĐÃ FIX (2026-03-02):
[x] 14. EscortServiceGrpc hand-written stubs (EscortServiceGrpc.java, EscortProtos.java,
         EscortResponses.java) in common-lib — cho phép compile ngay không cần protoc
         EscortGrpcClient.java rewrite với fully qualified type references
[x] 15. Arena battle formula: simulateBattle() nâng cấp — ELO + streak bonus + clamp 10-90%
         calculateRatingChange() 3-tier bracket (stomp/even/upset) với linear interpolation
[x] 16. Mount harness: thêm entry5_type..entry8_value (8 slots) vào MountHarness entity
         + V1__init_mount_tables.sql tạo mount + mount_harness tables với 8 attr slots
[x] 17. Trial gRPC: @Service → @GrpcService in TrialServiceGrpcImpl
         + grpc-spring-boot-starter dep thêm vào trial-service pom.xml
         Territory gRPC: TerritoryServiceGrpcImpl.java tạo mới
         + TerritoryServiceGrpc.java hand-written stub trong common-lib
         + grpc-spring-boot-starter dep + grpc.server.port=9086 trong territory-service
         + territory-service + trial-service grpc client addresses trong webSocket-server application.yml
[x] 18. WaBaoHandler: handleSetReq() lưu auto-sweep stop conditions qua POST /setting
         + BoxFeign.saveSetting() added + field name fix (wabao_set thay vì box_set)
         + BoxDTOs.SaveSettingReq added (documentation purposes)
[x] 19. EscortStats: thêm escort_count, intercept_count, help_count, current_ship_level, rewards_claimed
         EscortMission: thêm ship_key column
         V2__add_escort_proto_columns.sql migration
```

---

## Tham khảo

| Tài liệu | Vị trí |
|----------|--------|
| Proto gốc C++ | `document/开箱h5/client/LineR/proto/` |
| MsgId client gốc | `document/开箱h5/client/LineR/assets/script/manager/MsgIdManger.ts` |
| Python gateway | `document/开箱h5/server/server/src/gateway/` |
| Java MsgIds | `GameServer/webSocket-server/src/main/java/.../net/MsgIds.java` |
| Handler audit | `GameServer/docs/webSocket-server/HANDLER_COMPLETENESS_AUDIT.md` |
| P2 status | `GameServer/docs/phases/P2_IMPLEMENTATION_STATUS_REPORT.md` |
| Service comm | `GameServer/docs/architecture/SERVICE_COMMUNICATION_STRATEGY.md` |


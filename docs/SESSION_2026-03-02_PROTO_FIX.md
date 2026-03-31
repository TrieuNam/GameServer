# Session 2026-03-02 — Proto Stub & Handler Fix

## Tóm tắt

Tìm và fix các compile errors trong `webSocket-server` liên quan đến proto stubs và gRPC message types.

---

## Root Cause

Proto build (`mvn compile -pl common-lib`) đã generate classes vào:
- `common-lib/target/generated-sources/protobuf/java/` — proto message classes
- `common-lib/target/generated-sources/protobuf/grpc-java/` — gRPC service stubs

Nhưng có các **hand-written stub files** trong `src/main/java` tạo ra **duplicate class errors**.

---

## Files Fixed

### 1. Hand-written stubs → Empty (không xóa để tránh git issues)

| File (src/main/java) | Action |
|---|---|
| `org/SouthMillion/proto/Msgwabao/Msgwabao.java` | Replaced with empty package declaration |
| `org/SouthMillion/proto/Msgescort/Msgescort.java` | Replaced with empty package declaration |
| `org/SouthMillion/proto/Msgterritory/Msgterritory.java` | Replaced with empty package declaration |
| `org/SouthMillion/grpc/territory/TerritoryServiceGrpc.java` | Replaced with empty package declaration |

**Generated versions used instead:**
- `Msgwabao` → `target/generated-sources/protobuf/java/org/SouthMillion/proto/Msgwabao/Msgwabao.java`
- `Msgescort` → `target/generated-sources/protobuf/java/org/SouthMillion/proto/Msgescort/Msgescort.java`
- `Msgterritory` → `target/generated-sources/protobuf/java/org/SouthMillion/proto/Msgterritory/Msgterritory.java`
- `TerritoryServiceGrpc` → `target/generated-sources/protobuf/grpc-java/org/SouthMillion/grpc/territory/TerritoryServiceGrpc.java`
- `TerritoryInfoResponse` etc. → `target/generated-sources/protobuf/java/org/SouthMillion/grpc/territory/*.java`

### 2. TerritoryGrpcClient.java

- Import `TerritoryServiceGrpc` chuyển sang `org.SouthMillion.grpc.territory.*`
- Tất cả `TerritoryServiceGrpc.TerritoryXxxResponse` → `TerritoryXxxResponse` (direct type)
- `TerritoryServiceGrpc.TerritoryRequest.newBuilder()` → `TerritoryRequest.newBuilder()`

### 3. TerritoryHandler.java

- Import `TerritoryServiceGrpc` → `org.SouthMillion.grpc.territory.*`
- `resp.getLevel()` → `resp.getTerritoryLevel()` (generated field name)
- `resp.isSuccess()` → `resp.getSuccess()` (proto bool getter pattern)
- `TerritoryServiceGrpc.TerritoryXxxResponse` → `TerritoryXxxResponse`

---

## Generated Proto API Mapping

### TerritoryInfoResponse (generated)
| Field | Getter |
|---|---|
| `territory_level` | `getTerritoryLevel()` |
| `bot_num` | `getBotNum()` |
| `bot_run_num` | `getBotRunNum()` |
| `bot_buy_count` | `getBotBuyCount()` |
| `reward_count` | `getRewardCount()` |
| `reason` | `getReason()` |

### TerritoryActionResponse (generated)
| Field | Getter |
|---|---|
| `success` | `getSuccess()` *(not `isSuccess()`!)* |
| `result_type` | `getResultType()` |

### PB_CSEscortReq (generated)
| Field | Getter |
|---|---|
| `type` | `getType()` / `hasType()` |
| `p1` | `getP1()` / `hasP1()` |

### PB_SCEscortRet (generated)
| Field | Setter (Builder) |
|---|---|
| `type` | `setType(int)` |
| `p1` | `setP1(int)` |
| `p2` | `setP2(int)` |

---

## Status After Fix

| Handler | Errors | Warnings |
|---|---|---|
| EscortHandler | ✅ 0 | 2 (JavaDoc blank lines) |
| TerritoryHandler | ✅ 0 | 3 (JavaDoc blank lines) |
| WaBaoHandler | ✅ 0 | 3 (JavaDoc + Math.max) |
| TerritoryGrpcClient | ✅ 0 | warnings only |

---

## Important Note

> Khi `mvn clean compile -pl common-lib` được chạy, các file trong `target/` sẽ bị xóa và re-generate.
> Các empty stub files trong `src/main/java` phải được giữ empty (hoặc xóa hẳn sau khi commit) để tránh duplicate.
>
> **TODO**: Xóa các empty stub files hoặc add chúng vào `.gitignore` khi project ổn định.


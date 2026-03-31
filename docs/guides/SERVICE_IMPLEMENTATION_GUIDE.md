# Service Implementation Quick Reference

**Quick guide for implementing remaining services following proven pattern**

---

## Pattern Overview

Each system requires **4 components**:

1. **Backend Service** (Spring Boot) - Already exists ✅
2. **WebSocket Handler** (Java) - Fix/expand if needed
3. **TypeScript Client** (Service class)
4. **React Hook** (State management)

**Time per system**: ~2 hours

---

## Completed Systems ✅

| System | Phase | Backend | Handler | Client | Status |
|--------|-------|---------|---------|--------|--------|
| Pet | P0 | ✅ 8110 | ✅ 5 ops | ✅ | COMPLETE |
| Arena | P1 | ✅ | ✅ 7 ops | ✅ | COMPLETE |
| Guild | P1 | ✅ | ✅ 10 ops | ✅ | COMPLETE |
| Bag | P2 | ✅ | ✅ 4 ops | ✅ | COMPLETE |

---

## Pending Systems (Priority Order)

### Phase 2 - HIGH PRIORITY

| System | Proto | MsgId | Backend | Handler Status | Est. Time |
|--------|-------|-------|---------|----------------|-----------|
| Equip | msgequip.proto | 1300 | ✅ equip-service | CHECK | 2h |
| Shop | msgshop.proto | 1600 | ✅ shop-service | CHECK | 2h |

### Phase 3 - MEDIUM PRIORITY

| System | Proto | MsgId | Backend | Handler Status | Est. Time |
|--------|-------|-------|---------|----------------|-----------|
| Task | msgtask.proto | 1700 | ✅ task-service | CHECK | 2h |
| Rank | msgrank.proto | 1800 | ✅ rank-service | CHECK | 2h |
| World | msgworld.proto | 9620 | ✅ world-service | CHECK | 2h |
| Escort | msgescort.proto | 9650 | ✅ escort-service | CHECK | 2h |

### Phase 4 - LOW PRIORITY

| System | Proto | MsgId | Backend | Handler Status | Est. Time |
|--------|-------|-------|---------|----------------|-----------|
| Territory | msgterritory.proto | 9660 | ✅ | CHECK | 2h |
| StarMap | msgstarmap.proto | 9670 | ✅ | CHECK | 2h |
| Rune | msgrune.proto | 9680 | ✅ | CHECK | 2h |
| Artifact | msgartifact.proto | 9690 | ✅ | CHECK | 2h |
| Mount | msgmount.proto | 2200 | ✅ | BROKEN | 3h |
| Angel | msgangel.proto | 2300 | ✅ | BROKEN | 3h |

---

## Implementation Checklist (Per System)

### Step 1: Analyze Proto (5 min)

```bash
# Check proto file
cat GameServer/common-lib/src/main/proto/cs/msg*.proto

# Look for:
# - Message ID (MsgId:XXXX comment)
# - Request fields (req_type, type, op, param)
# - Response structure
```

**Example**:
```protobuf
message PB_CSSystemReq {  //!< MsgId:1234
    optional int32 req_type = 1;
    repeated int32 param = 2;
}
```

### Step 2: Check Handler (10 min)

```bash
# Find handler
find . -name "*Handler.java" | grep -i system

# Read handler
# Check:
# - Does it exist?
# - Does it compile?
# - How many operations?
# - Uses Feign or gRPC?
```

**If broken**:
- Check proto field names
- Fix imports
- Update operation routing

**If incomplete**:
- Expand operations following backend endpoints
- Add switch-case routing

### Step 3: Check Backend Endpoints (5 min)

```bash
# Find Feign or gRPC client
grep -r "SystemFeign" webSocket-server/
grep -r "SystemGrpcClient" webSocket-server/

# Verify:
# - Endpoint paths match backend
# - Parameter types match proto
# - Return types consistent
```

**REST (Feign)**:
```java
@FeignClient(name = "system-service")
public interface SystemFeign {
    @GetMapping("/api/system/{roleId}")
    Map<String, Object> getInfo(@PathVariable("roleId") String roleId);
}
```

**gRPC**:
```java
@Component
public class SystemGrpcClient {
    public List<ItemView> getItems(String roleId) { ... }
}
```

### Step 4: Create TypeScript Client (30 min)

**Template**:
```typescript
// src/services/SystemService.ts
import { WebSocketService } from './WebSocketService';

export class SystemService {
    private wsService: WebSocketService;

    constructor(wsService: WebSocketService) {
        this.wsService = wsService;
    }

    async getInfo(): Promise<SystemResponse> {
        const request = {
            req_type: 1,  // or type, op depending on proto
            param: []     // or p1, p2
        };
        return this.wsService.sendMessage(MSG_ID, request);
    }

    async operation2(paramId: number): Promise<any> {
        const request = {
            req_type: 2,
            param: [paramId]
        };
        return this.wsService.sendMessage(MSG_ID, request);
    }
}

export interface SystemResponse {
    // Match proto response fields
}
```

**Proto Field Mapping**:
- `req_type` or `type` or `op` → Operation code
- `param[]` or `p1`, `p2` → Parameters
- `str_param` → String parameter (encode with TextEncoder)

### Step 5: Create React Hook (45 min)

**Template**:
```typescript
// src/hooks/useSystemService.ts
import { useState, useCallback, useEffect } from 'react';
import { SystemService, SystemResponse } from '../services/SystemService';
import { useWebSocket } from './useWebSocket';

export const useSystemService = () => {
    const wsService = useWebSocket();
    const [systemService] = useState(() => new SystemService(wsService));
    const [data, setData] = useState<SystemResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const loadInfo = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await systemService.getInfo();
            setData(response);
        } catch (err: any) {
            setError(err.message || 'Failed to load');
        } finally {
            setLoading(false);
        }
    }, [systemService]);

    const operation = useCallback(async (param: number): Promise<boolean> => {
        setLoading(true);
        try {
            await systemService.operation2(param);
            await loadInfo(); // Auto-refresh
            return true;
        } catch (err: any) {
            setError(err.message);
            return false;
        } finally {
            setLoading(false);
        }
    }, [systemService, loadInfo]);

    // Auto-load on mount
    useEffect(() => {
        loadInfo();
    }, [loadInfo]);

    return {
        data,
        loading,
        error,
        loadInfo,
        operation
    };
};
```

### Step 6: Compile & Test (20 min)

```bash
# Compile handler
cd GameServer/webSocket-server
mvn compile -DskipTests

# Check for errors
# If errors, fix proto fields or imports

# TypeScript (no compile needed, runtime checked)
```

---

## Common Patterns

### Proto Request Types

**Pattern 1: req_type + param[]** (Most common)
```protobuf
message PB_CSSystemReq {
    optional int32 req_type = 1;
    repeated int32 param = 2;
}
```

**Pattern 2: type + p1/p2**
```protobuf
message PB_CSSystemReq {
    optional int32 type = 1;
    optional int32 p1 = 2;
    optional int32 p2 = 3;
}
```

**Pattern 3: op + param_1/param_list**
```protobuf
message PB_CSSystemReq {
    optional int32 req_type = 1;
    optional int32 param_1 = 2;
    repeated int32 param_list = 3;
}
```

**Pattern 4: With string**
```protobuf
message PB_CSSystemReq {
    optional int32 req_type = 1;
    repeated int32 param = 2;
    optional bytes str_param = 3;
}
```

### Handler Routing

```java
@Override
public void handle(PlayerSession session, int msgId, byte[] payload) {
    try {
        ProtoMsg.Request req = ProtoMsg.Request.parseFrom(payload);
        int operation = req.getReqType(); // or getType(), getOp()
        
        switch (operation) {
            case 1: handleOp1(session); break;
            case 2: handleOp2(session, req.getParam(0)); break;
            case 3: handleOp3(session, req); break;
            default: log.warn("Unknown op: {}", operation);
        }
    } catch (Exception e) {
        log.error("Handler error", e);
    }
}
```

### TypeScript String Parameters

```typescript
// Encode string
const encoder = new TextEncoder();
const request = {
    req_type: 2,
    str_param: encoder.encode(stringValue)
};

// Decode string
const decoder = new TextDecoder();
const text = decoder.decode(response.str_param);
```

---

## MessageIds Constants

**Location**: `webSocket-server/src/main/java/com/southMillion/webSocket_server/constant/MessageIds.java`

**Add if missing**:
```java
// System messages
public static final int CS_SYSTEM_REQ = 1234;
public static final int SC_SYSTEM_INFO = 1235;
```

**Update handler**:
```java
@Override
public int[] interests() {
    return new int[]{MessageIds.CS_SYSTEM_REQ};
}
```

---

## MessageDispatcher Routing

**Location**: `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/MessageDispatcher.java`

**If handler disabled**:
```java
// 1. Uncomment import
import com.southMillion.webSocket_server.handler.system.SystemHandler;

// 2. Uncomment field
private final SystemHandler systemHandler;

// 3. Uncomment routing
case "SYSTEM":
    systemHandler.handle(session, msgId, payload);
    break;
```

---

## Troubleshooting

### Compilation Errors

**Error**: "cannot find symbol: method setRetCode()"
- **Fix**: Proto doesn't have this field, remove from handler

**Error**: "incompatible types: String cannot be converted to Long"
- **Fix**: Convert roleId: `Long.parseLong(session.getRoleId())`

**Error**: "package org.SouthMillion.proto.Msgsystem does not exist"
- **Fix**: Run `mvn compile` in common-lib first

### Handler Issues

**Handler not receiving messages**:
1. Check msgId in interests()
2. Check MessageDispatcher routing
3. Check handler is in Spring @Component

**Proto parse fails**:
1. Verify proto fields match request
2. Check optional vs required fields
3. Verify proto was compiled (mvn compile in common-lib)

---

## Time Estimates (Proven)

| Task | First Time | With Pattern | After 4 Systems |
|------|------------|--------------|-----------------|
| Analyze Proto | 10 min | 5 min | 5 min |
| Check Handler | 20 min | 10 min | 5 min |
| Fix/Expand Handler | 60 min | 30 min | 20 min |
| Create Client | 45 min | 30 min | 20 min |
| Create Hook | 60 min | 45 min | 30 min |
| Compile & Test | 30 min | 20 min | 10 min |
| **TOTAL** | **225 min (3.75h)** | **140 min (2.3h)** | **90 min (1.5h)** |

**Current efficiency**: ~2 hours per system (proven with Pet/Arena/Guild/Bag)

---

## Success Criteria

- [ ] Handler compiles (BUILD SUCCESS)
- [ ] Handler has all operations from backend
- [ ] Client has async methods matching operations
- [ ] Hook manages state properly
- [ ] Hook auto-refreshes after mutations
- [ ] Inline documentation (JSDoc + comments)

---

## Next Target: Equip System

**Quick Start**:
```bash
# 1. Check proto
cat GameServer/common-lib/src/main/proto/cs/msgequip.proto

# 2. Find handler
cat GameServer/webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/equip/EquipHandler.java

# 3. Check Feign
cat GameServer/webSocket-server/src/main/java/com/southMillion/webSocket_server/service/client/EquipFeign.java

# 4. Create client
# client/LineR/src/services/EquipService.ts

# 5. Create hook
# client/LineR/src/hooks/useEquipService.ts
```

---

*Last Updated: 2026-01-31*  
*Pattern Proven: Pet, Arena, Guild, Bag*  
*Efficiency: 2h per system*

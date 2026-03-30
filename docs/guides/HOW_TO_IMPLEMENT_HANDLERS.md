# 📘 How to Implement Handlers from Documents

## Overview

The `D:\project\serverGame\document\handler\` directory contains **431 detailed message handler documentation files** with complete implementation examples. This guide shows how to use them to implement handlers in `webSocket-server`.

---

## Document Structure

Each `MSGID_*.md` file contains:

### 1. Message Information Table
```markdown
| Property | Value |
|----------|-------|
| **Message ID** | 3301 |
| **Direction** | Client → Server (CS) |
| **Category** | Arena System |
| **Handler** | ArenaHandler.java |
```

### 2. Proto Definition
```protobuf
message CS_ARENA_INFO_REQ {
    int32 req_type = 1;    // Operation type
    int32 param1 = 2;      // Parameter 1
    // ...
}
```

### 3. Business Logic Description
- Feature description
- Operation types (e.g., 1=getInfo, 2=challenge, 3=getRanking)
- Required parameters
- Validation rules

### 4. Handler Code Example (Java)
Complete working implementation with:
- Service injection
- Request parsing
- Business logic
- Response building
- Error handling

### 5. Client Code Example (TypeScript)
Client-side implementation showing how messages are sent

### 6. Database Schema
Required tables and fields

### 7. Test Cases
Example requests and responses

---

## Implementation Process

### Step 1: Find Your Handler's Documents

**Pattern:** `MSGID_[number]_[NAME].md`

**Examples:**
- Arena: `MSGID_3301_CS_ARENA_INFO_REQ.md` to `MSGID_3316_SC_ARENA_BUY_CHALLENGE_RES.md`
- Pet: `MSGID_4101_CS_PET_LIST_REQ.md` to `MSGID_4112_SC_PET_FEED_RES.md`
- Guild: `MSGID_3001_CS_CREATE_GUILD_REQ.md` to `MSGID_3053_SC_GUILD_WAR_RESULT_ACK.md`

**Quick Find:**
```bash
# Search by system name
ls D:\project\serverGame\document\handler\MSGID_*ARENA*.md
ls D:\project\serverGame\document\handler\MSGID_*PET*.md
ls D:\project\serverGame\document\handler\MSGID_*GUILD*.md
```

---

### Step 2: Read the Main Request Document

**Focus on:**
1. **Message ID** - Handler routing number
2. **req_type/operation** - Operation switch cases
3. **Parameters** - What data is sent
4. **Handler code example** - Copy the pattern

**Example from MSGID_3301_CS_ARENA_INFO_REQ.md:**
```java
@MessageHandler(msgId = 3301)
public class ArenaInfoReqHandler implements IMessageHandler {
    
    @Autowired
    private ArenaService arenaService;
    
    @Override
    public void handle(Session session, Message message) {
        long userId = session.getUserId();
        
        // Get arena data
        ArenaData data = arenaService.getArenaData(userId);
        
        // Build response
        SC_ARENA_INFO_RES.Builder response = SC_ARENA_INFO_RES.newBuilder();
        response.setRanking(data.getRanking());
        response.setRating(data.getRating());
        // ... more fields
        
        // Send
        ResponseUtil.sendResponse(session, 3302, response.build());
    }
}
```

---

### Step 3: Map to Existing Handler Structure

**Our webSocket-server structure:**
```java
@Component
@RequiredArgsConstructor
public class ArenaHandler implements MessageHandler {
    
    private final ArenaFeign arenaFeign;  // Service client
    
    @Override
    public int[] interests() {
        return new int[]{1800};  // MsgID to listen
    }
    
    @Override
    public void handle(PlayerSession session, int msgId, byte[] payload) {
        // Parse request
        PB_CSArenaReq req = PB_CSArenaReq.parseFrom(payload);
        int operation = req.getReqType();  // or getType() depending on proto
        
        // Route to operations
        switch (operation) {
            case 1: handleGetInfo(session); break;
            case 2: handleChallenge(session, req); break;
            // ...
        }
    }
}
```

---

### Step 4: Implement Operations

For each operation documented:

**Document shows:**
```java
// Operation 1: Get Info
ArenaData data = arenaService.getArenaData(userId);
response.setRanking(data.getRanking());
```

**Convert to our pattern:**
```java
private void handleGetInfo(PlayerSession session) {
    try {
        // Call Feign client
        Map<String, Object> data = arenaFeign.getArenaInfo(session.getRoleId());
        
        // Build proto response
        PB_SCArenaInfo.Builder response = PB_SCArenaInfo.newBuilder();
        if (data != null) {
            response.setNowRank(((Number) data.get("rank")).intValue());
            response.setNowScore(((Number) data.get("score")).intValue());
            // Map more fields...
        }
        
        sendResponse(session, response.build());
        
    } catch (Exception e) {
        log.error("[Arena] Error getting info", e);
        sendErrorResponse(session);
    }
}
```

---

### Step 5: Handle Proto Field Mappings

**Document field names vs Our proto names:**

| Document | Our Proto | Type |
|----------|-----------|------|
| ranking | now_rank | int32 |
| rating | now_score | int32 |
| remainingChallenges | fight_times | int32 |
| highestRanking | history_top_rank | int32 |

**Mapping helper:**
```java
// Document: data.getRanking()
// Our code: response.setNowRank(...)

// Document: data.getTotalWins()
// Our code: response.setWinNum(...)
```

**Check proto file for correct method names:**
```bash
# Find proto definition
grep -r "PB_SCArenaInfo" D:\project\serverGame\GameServer\common-lib\src\main\proto\
```

---

### Step 6: Add Service Client Calls

**Document shows direct service calls:**
```java
ArenaData data = arenaService.getArenaData(userId);
```

**Convert to Feign/gRPC:**
```java
// Option 1: Feign (REST)
Map<String, Object> data = arenaFeign.getArenaInfo(session.getRoleId());

// Option 2: gRPC
ArenaInfoResponse response = arenaGrpcClient.getArenaInfo(
    ArenaInfoRequest.newBuilder()
        .setUserId(session.getRoleId())
        .build()
);
```

**If service doesn't exist yet:**
```java
// Return stub data for now
Map<String, Object> data = new HashMap<>();
data.put("rank", 0);
data.put("score", 1000);
// TODO: Implement arena-service
```

---

## Common Patterns

### Pattern 1: Simple Query Handler

**Document Example:**
```java
List<Pet> pets = petService.getOwnedPets(userId);
for (Pet pet : pets) {
    response.addPets(buildPetData(pet));
}
```

**Implementation:**
```java
private void handleGetPetList(PlayerSession session) {
    List<Map<String, Object>> pets = petFeign.getRolePets(session.getRoleId());
    
    PB_SCRolePetData.Builder response = PB_SCRolePetData.newBuilder();
    if (pets != null) {
        for (Map<String, Object> pet : pets) {
            PB_PetInfo.Builder petInfo = PB_PetInfo.newBuilder();
            petInfo.setPetId(((Number) pet.get("petId")).longValue());
            petInfo.setLevel(((Number) pet.get("level")).intValue());
            response.addPets(petInfo);
        }
    }
    
    sendResponse(session, response.build());
}
```

---

### Pattern 2: Operation with Parameters

**Document Example:**
```java
int targetRank = request.getTargetRank();
BattleResult result = arenaService.challenge(userId, targetRank);
```

**Implementation:**
```java
private void handleChallenge(PlayerSession session, PB_CSArenaReq req) {
    int targetRank = req.getP1();  // Parameter from proto
    
    Map<String, Object> result = arenaFeign.challenge(
        session.getRoleId(), 
        targetRank
    );
    
    PB_SCArenaInfo.Builder response = PB_SCArenaInfo.newBuilder();
    if (result != null) {
        response.setNowRank(((Number) result.get("newRank")).intValue());
        // ... more fields
    }
    
    sendResponse(session, response.build());
}
```

---

### Pattern 3: Resource Consumption

**Document Example:**
```java
// Check item availability
if (!bagService.hasItem(userId, materialId, count)) {
    throw new InsufficientMaterialException();
}

// Consume items
bagService.consumeItem(userId, materialId, count);

// Upgrade
pet.setLevel(pet.getLevel() + 1);
```

**Implementation:**
```java
private void handleUpgrade(PlayerSession session, PB_CSPetReq req) {
    int petId = req.getP1();
    int materialId = req.getP2();
    int count = req.getP3();
    
    // Call service (service handles validation and consumption)
    Map<String, Object> result = petFeign.upgradePet(
        session.getRoleId(),
        petId,
        materialId,
        count
    );
    
    if (result != null && (Boolean) result.get("success")) {
        // Send success response
        PB_SCPetUpgradeRet.Builder response = PB_SCPetUpgradeRet.newBuilder();
        response.setRetCode(0);
        response.setNewLevel(((Number) result.get("newLevel")).intValue());
        sendResponse(session, response.build());
    } else {
        // Send error
        sendErrorResponse(session, (String) result.get("error"));
    }
}
```

---

## Quick Reference: Handler Template

```java
package com.southMillion.webSocket_server.handler.[system];

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.PacketCodec;
import com.southMillion.webSocket_server.service.client.[System]Feign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msg[system].Msg[system];

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class [System]Handler implements MessageHandler {

    private final [System]Feign [system]Feign;

    @Override
    public int[] interests() {
        return new int[]{[MSGID]}; // From document
    }

    @Override
    public void handle(PlayerSession session, int msgId, byte[] payload) {
        try {
            // Parse request
            Msg[system].PB_CS[System]Req req = Msg[system].PB_CS[System]Req.parseFrom(payload);
            int operation = req.getReqType(); // or getType()
            
            log.debug("[[System]] op={}, roleId={}", operation, session.getRoleId());
            
            // Route operations
            switch (operation) {
                case 1: handleOperation1(session, req); break;
                case 2: handleOperation2(session, req); break;
                // ... add more from document
                default:
                    log.warn("[[System]] Unknown op: {}", operation);
            }
            
        } catch (Exception e) {
            log.error("[[System]] Error for roleId={}", session.getRoleId(), e);
            sendErrorResponse(session);
        }
    }
    
    private void handleOperation1(PlayerSession session, Msg[system].PB_CS[System]Req req) {
        try {
            // 1. Call service
            Map<String, Object> result = [system]Feign.operation1(session.getRoleId());
            
            // 2. Build response
            Msg[system].PB_SC[System]Res.Builder response = 
                Msg[system].PB_SC[System]Res.newBuilder();
            
            if (result != null) {
                // Map fields from document
                response.setField1(((Number) result.get("field1")).intValue());
                response.setField2((String) result.get("field2"));
                // ... more fields
            }
            
            // 3. Send
            sendResponse(session, response.build());
            log.info("[[System]] op1 success for roleId={}", session.getRoleId());
            
        } catch (Exception e) {
            log.error("[[System]] op1 error", e);
            sendErrorResponse(session);
        }
    }
    
    private void sendResponse(PlayerSession session, Msg[system].PB_SC[System]Res response) {
        try {
            byte[] responseBytes = response.toByteArray();
            byte[] packet = PacketCodec.encode([RESPONSE_MSGID], responseBytes);
            session.sendBinary(packet);
        } catch (Exception e) {
            log.error("[[System]] Send response failed", e);
        }
    }
    
    private void sendErrorResponse(PlayerSession session) {
        Msg[system].PB_SC[System]Res.Builder response = 
            Msg[system].PB_SC[System]Res.newBuilder();
        response.setRetCode(-1);
        sendResponse(session, response.build());
    }
}
```

---

## Document Index

**Full list:** `D:\project\serverGame\document\handler\INDEX.md`

**Quick lookup:**
```bash
# By system
grep "Arena" D:\project\serverGame\document\handler\INDEX.md
grep "Pet" D:\project\serverGame\document\handler\INDEX.md
grep "Guild" D:\project\serverGame\document\handler\INDEX.md

# By MsgID
ls D:\project\serverGame\document\handler\MSGID_3301*.md
```

---

## Next Steps

1. **Choose a system** from [HANDLER_IMPLEMENTATION_PLAN.md](HANDLER_IMPLEMENTATION_PLAN.md)
2. **Read its documents** from `document/handler/`
3. **Copy handler template** above
4. **Map operations** from document examples
5. **Implement step-by-step** with logging
6. **Test compilation** (`mvn compile`)
7. **Create service if needed** (Feign interface)

**Estimated time per handler:**
- Simple query handler: 30 minutes
- Complex handler (8+ operations): 2-3 hours
- With new service creation: +1-2 hours

---

## Tips

### Document Reading Tips
- Focus on "Handler Processing" section for code
- Check "Data Fields" table for parameter meanings
- Look at "Client Code" for usage patterns
- Database schema shows what data should exist

### Proto Mapping Tips
- Document field names may differ from proto
- Use `grep` to find proto definitions
- Check common-lib generated classes for method names
- Proto field types matter (int32 vs int64, string vs bytes)

### Service Call Tips
- Start with stub responses if service doesn't exist
- Add TODO comments for future service implementation
- Log service calls for debugging
- Handle null/empty responses gracefully

### Error Handling Tips
- Always wrap in try-catch
- Log errors with context (userId, operation, parameters)
- Send error responses (don't leave client hanging)
- Return sensible defaults when possible

---

## Support

**Documentation:** 431 files with 94% coverage  
**Implementation Plan:** [HANDLER_IMPLEMENTATION_PLAN.md](HANDLER_IMPLEMENTATION_PLAN.md)  
**Mapping Status:** [HANDLER_SERVICE_MAPPING.md](HANDLER_SERVICE_MAPPING.md)  
**Integration Status:** [WEBSOCKET_SERVICE_INTEGRATION_STATUS.md](WEBSOCKET_SERVICE_INTEGRATION_STATUS.md)

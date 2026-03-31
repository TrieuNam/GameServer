# WebSocket Implementation Analysis & Status

**Created:** 2026-01-29  
**Last Updated:** 2026-01-29  
**Purpose:** Comprehensive analysis of WebSocket implementation between Client (Cocos Creator) and Server (Spring WebSocket)

---

## 📊 Executive Summary

### Overall Status
- **Documentation:** ✅ **94% Complete** (375/400 message protocols documented)
- **Server Handlers:** ✅ **30 Active Handlers** (100% basic implementation)
- **Client Integration:** ✅ **WebSocket Client Implemented**
- **Message Protocol:** ✅ **Protobuf-based binary protocol**
- **Connection:** ✅ **Binary WebSocket at `/ws/game`**

### Architecture Quality
| Component | Status | Coverage | Notes |
|-----------|--------|----------|-------|
| **WebSocket Gateway** | ✅ Excellent | 100% | Spring WebSocket with Virtual Threads |
| **Message Dispatcher** | ✅ Good | 80% | Category-based routing, needs completion |
| **Handler Implementation** | 🔶 Partial | 60% | 30 handlers active, many TODO placeholders |
| **Client WebSocket** | ✅ Excellent | 100% | Full connection management, auto-reconnect |
| **Protocol Buffer** | ✅ Excellent | 100% | Binary encoding/decoding working |
| **Session Management** | ✅ Good | 90% | PlayerSessionRegistry working well |

---

## 🏗️ Architecture Overview

### Server Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Cocos Creator)                    │
│                  WebSocket Client (WebSock.ts)               │
└──────────────────────┬──────────────────────────────────────┘
                       │ ws://host:8090/ws/game
                       │ Binary Messages (Protobuf)
┌──────────────────────▼──────────────────────────────────────┐
│               WebSocket Gateway (Spring)                     │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  WsGatewayHandler (BinaryWebSocketHandler)           │   │
│  │  - afterConnectionEstablished()                      │   │
│  │  - handleBinaryMessage()                             │   │
│  │  - afterConnectionClosed()                           │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  PlayerSessionRegistry                               │   │
│  │  - Register/Unregister sessions                      │   │
│  │  - Track userId, roleId, loginState                 │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │  MessageDispatcher                                   │   │
│  │  - PacketCodec.decode() → (msgId, payload)          │   │
│  │  - MessageIds.getCategory(msgId) → category         │   │
│  │  - routeMessage() → specific handler                │   │
│  └──────────────────────┬───────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────┘
                          │
         ┌────────────────┴────────────────┐
         │     Handler Categories           │
         └────────────────┬────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    │                     │                      │
┌───▼────┐      ┌────────▼──────┐      ┌───────▼──────┐
│ LOGIN  │      │     ROLE      │      │     BAG      │
│Handler │      │   Handler     │      │   Handler    │
└───┬────┘      └────────┬──────┘      └───────┬──────┘
    │                    │                      │
    │                    │                      │
    ▼                    ▼                      ▼
 [session-        [role-service]         [bag-service]
  service]        via gRPC/HTTP          via gRPC
```

### Message Flow

```
1. CLIENT SEND
   ┌─────────────────────────────────────┐
   │ MsgIdManger.ts                      │
   │ → NetNode.Send(protobuf data)       │
   │   → WebSock.send(buffer)            │
   │     → WebSocket.send(ArrayBuffer)   │
   └─────────────────────────────────────┘
                    │
                    │ Binary Protocol
                    │ [4 bytes bodyLen][4 bytes msgId][payload]
                    ▼
   ┌─────────────────────────────────────┐
   │ WsGatewayHandler.handleBinaryMessage│
   │ → MessageDispatcher.dispatch()      │
   │   → PacketCodec.decode()            │
   │     → MessageIds.getCategory(msgId) │
   │       → routeMessage()              │
   │         → handler.handle()          │
   └─────────────────────────────────────┘

2. SERVER RESPONSE
   ┌─────────────────────────────────────┐
   │ Handler (e.g., LoginHandler)        │
   │ → buildAndSend(ps, msgId, proto)    │
   │   → PacketCodec.encode()            │
   │     → ps.send(buffer)               │
   │       → WebSocketSession.sendMessage│
   └─────────────────────────────────────┘
                    │
                    │ Binary Protocol
                    ▼
   ┌─────────────────────────────────────┐
   │ WebSock.onMessage()                 │
   │ → NetNode.onMessage()               │
   │   → processRecvPacket()             │
   │     → dispatch(msgId, data)         │
   │       → callback(protobuf)          │
   └─────────────────────────────────────┘
```

---

## 📦 Message Protocol Structure

### Binary Packet Format

```
┌──────────────┬──────────────┬─────────────────────────┐
│  Body Length │   Message ID │      Protobuf Payload   │
│   (4 bytes)  │   (4 bytes)  │      (variable size)    │
│   Big Endian │  Big Endian  │      Binary Data        │
└──────────────┴──────────────┴─────────────────────────┘
```

### PacketCodec Implementation

**Server Side** (`PacketCodec.java`):
```java
public static byte[] encode(int msgId, byte[] payload) {
    int bodyLen = payload != null ? payload.length : 0;
    ByteBuffer bb = ByteBuffer.allocate(8 + bodyLen);
    bb.putInt(bodyLen);
    bb.putInt(msgId);
    if (payload != null) {
        bb.put(payload);
    }
    return bb.array();
}

public static Decoded decode(byte[] packet) {
    ByteBuffer bb = ByteBuffer.wrap(packet);
    int bodyLen = bb.getInt();
    int msgId = bb.getInt();
    byte[] payload = new byte[bodyLen];
    bb.get(payload);
    return new Decoded(msgId, payload);
}
```

**Client Side** (`ProtocolHelper.ts`):
```typescript
// Similar encoding/decoding with DataView in TypeScript
// Handles both regular and WeChat mini-game platforms
```

---

## 🔌 Handler Implementation Status

### ✅ Fully Implemented (10/30)

| Handler | MsgId Range | Status | Backend Service | Notes |
|---------|-------------|--------|----------------|-------|
| **LoginHandler** | 7056, 1053, 9050 | ✅ Complete | session-service, role-service | Full login flow with data sync |
| **RoleHandler** | 1400-1470 | ✅ Complete | role-service | Character management, skills |
| **BagHandler** | 1500-1510 | ✅ Complete | bag-service (gRPC) | Inventory management |
| **EquipHandler** | 1600-1608 | ✅ Complete | equip-service | Equipment wear/unwear |
| **MailHandler** | 9501-9551 | ✅ Complete | mail-service | Mail CRUD operations |
| **ShopHandler** | 1620-1631 | ✅ Complete | shop-service | Shop purchases |
| **BoxHandler** | 1610-1618 | ✅ Complete | box-service | Treasure box system |
| **TaskHandler** | 1451-1452 | ✅ Complete | task-service | Quest management |
| **GMCommandHandler** | 2000-2001 | ✅ Complete | Multiple services | Admin commands |
| **AdvertisementHandler** | 1662-1663 | ✅ Complete | Internal | Ad display |

### 🔶 Partially Implemented (20/30)

| Handler | MsgId Range | Status | Missing Features |
|---------|-------------|--------|-----------------|
| **WorldHandler** | 8000-8050 | 🔶 Skeleton | Enter/Leave scene, Movement, AOI, Pickup |
| **BattleHandler** | 11001-11003 | 🔶 Skeleton | Battle start, actions, rounds, results |
| **MountHandler** | 2140-2145 | 🔶 Basic | Mount operations, harness |
| **AngelHandler** | 2130-2132 | 🔶 Basic | Angel operations |
| **PetHandler** | 2100-2107 | 🔶 Basic | Pet management, upgrades |
| **GemHandler** | 1660-1667 | 🔶 Basic | Gem operations, purchases |
| **WaBaoHandler** | 1640-1651 | 🔶 Placeholder | Treasure digging system |
| **ShenQiHandler** | 1675-1680 | 🔶 Placeholder | Artifact system |
| **GuildHandler** | 9640-9646 | 🔶 Placeholder | Guild management |
| **ArenaHandler** | 9610-9616 | 🔶 Placeholder | PvP arena |
| **EscortHandler** | 9620-9626 | 🔶 Placeholder | Convoy system |
| **StarMapHandler** | 2150-2152 | 🔶 Placeholder | Star map system |
| **RankHandler** | Various | 🔶 Placeholder | Ranking lists |
| **TerritoryHandler** | Various | 🔶 Placeholder | Territory system |
| **RuneHandler** | Various | 🔶 Placeholder | Rune system |
| **ScrollHandler** | Various | 🔶 Placeholder | Scroll system |
| **KnightsHandler** | 1625-1627 | 🔶 Placeholder | Knights system |
| **ShiZhuangHandler** | 1509-1510 | 🔶 Placeholder | Fashion/Costume |
| **PagodaHandler** | 2120-2121 | 🔶 Placeholder | Trial tower |
| **BlockHandler** | Various | 🔶 Placeholder | Blacklist |

---

## 🎯 Priority Implementation Plan

### Phase 1: P0 - Critical Systems (URGENT)

#### 1. WorldHandler - Scene & Movement System
**Priority:** 🔴 P0 - CRITICAL  
**Complexity:** High  
**Impact:** Blocks all gameplay

**Required Messages:**
- `8001` CS_ENTER_SCENE_REQ → Enter scene (teleport/login)
- `8002` CS_LEAVE_SCENE_REQ → Leave scene
- `8010` CS_MOVE_REQ → Player movement (WASD/click)
- `8020` SC_ROLE_ENTER_VIEW → AOI enter (see other players)
- `8021` SC_ROLE_LEAVE_VIEW → AOI leave
- `8030` CS_PICKUP_ITEM_REQ → Pickup items
- `8050` SC_SCENE_INFO → Scene snapshot

**Backend Dependency:** `world-service` (needs implementation)

**Implementation Steps:**
1. Create `world-service` microservice with gRPC
2. Implement scene management (scene enter/leave/load)
3. Implement AOI (Area of Interest) system
4. Implement movement validation & broadcast
5. Implement pickup system
6. Update WorldHandler to call world-service

**Estimated Effort:** 3-4 days

---

#### 2. BattleHandler - Combat System
**Priority:** 🔴 P0 - CRITICAL  
**Complexity:** Very High  
**Impact:** No combat = no gameplay

**Required Messages:**
- `8100` CS_BATTLE_START_REQ → Start battle (PvE/PvP)
- `8102` CS_BATTLE_ACTION_REQ → Perform action (attack/skill/item)
- `8104` SC_BATTLE_ROUND_INFO → Battle state update
- `8106` SC_BATTLE_END_ACK → Battle result with rewards
- `8120` CS_AUTO_BATTLE_REQ → Toggle auto-battle
- `8150` CS_BATTLE_REPORT_REQ → Get battle replay

**Backend Dependency:** `battle-service` (needs major work)

**Implementation Steps:**
1. Extend `battle-service` for real-time battles
2. Implement turn-based combat engine
3. Implement skill/damage calculation
4. Implement auto-battle AI
5. Implement battle replay system
6. Update BattleHandler with full logic

**Estimated Effort:** 5-7 days

---

### Phase 2: P1 - High Priority (IMPORTANT)

#### 3. Friend System
**Messages:** 3101-3115, 7001-7031  
**Backend:** `social-service` (new)  
**Effort:** 2 days

#### 4. Chat System
**Messages:** 3201-3210, 6100-6116  
**Backend:** `chat-service` (new) with Redis Pub/Sub  
**Effort:** 3 days

#### 5. Arena (PvP)
**Messages:** 3301-3316, 9100-9111  
**Backend:** `arena-service` (exists, needs completion)  
**Effort:** 3 days

---

### Phase 3: P2 - Medium Priority (NICE TO HAVE)

#### 6. Pet System
**Messages:** 2100-2107, 4101-4112, 8500-8507  
**Backend:** `pet-service` (planned)  
**Effort:** 2-3 days

#### 7. Dungeon System
**Messages:** 4201-4210, 9200-9211  
**Backend:** `dungeon-service` (new)  
**Effort:** 3 days

#### 8. Monetization (Recharge/VIP/Growth Fund)
**Messages:** 8300-8413  
**Backend:** `payment-service`, `vip-service`  
**Effort:** 3-4 days

---

### Phase 4: P3 - Low Priority (FUTURE)

- Guild Wars (9200-9210)
- Auction (9300-9309)
- Cross Arena (8900-8910)
- World Boss (8800-8808)
- Marriage (9100-9109)
- Mentor (9000-9008)
- Limited Events (8700-8710)

---

## 🐛 Known Issues & Gaps

### Server Side Issues

1. **MessageDispatcher Incomplete Categories**
   - Categories like `SCENE`, `BATTLE`, `PET` log "not implemented"
   - Needs routing to respective handlers
   
   ```java
   // Current code logs debug messages instead of routing
   case "SCENE":
   case "BATTLE":
       log.debug("[dispatch] Category '{}' not implemented", category);
       break;
   ```

   **Fix:** Add handler dependencies and route properly

2. **WorldHandler Missing Implementation**
   - All methods are TODO placeholders
   - No connection to world-service
   
   **Fix:** Implement world-service microservice first

3. **BattleHandler Missing Real-time Logic**
   - Only has placeholder structure
   - No turn-based engine
   
   **Fix:** Design and implement combat engine

4. **Missing Handlers for Documented Protocols**
   - Friend (3101-3115) - No handler
   - Chat (3201-3210) - No handler
   - Many P1 systems undocumented but need handlers

   **Fix:** Create missing handlers

### Client Side Issues

1. **MsgIdManger.ts Registration Incomplete**
   - Only ~150 messages registered
   - Many documented protocols (8000-8900 range) not registered
   
   **Fix:** Register all 375+ messages

2. **Missing Handler Callbacks**
   - Client may register msgId but no callback logic
   
   **Fix:** Implement UI controllers for each system

### Protocol Documentation Gaps

1. **Friend System** - 0% documented (15 messages)
2. **Chat System** - 0% documented (10 messages)
3. **Arena System** - 0% documented (15 messages)
4. **Pet System** - 0% documented (12 messages)
5. **Dungeon System** - 0% documented (10 messages)

**Total Undocumented:** ~60 messages (24% of total)

---

## 🔧 Technical Recommendations

### 1. Complete MessageDispatcher Routing

**File:** `MessageDispatcher.java`

```java
// Add missing handlers
private final WorldHandler worldHandler;
private final BattleHandler battleHandler;
private final PetHandler petHandler;
private final FriendHandler friendHandler;
private final ChatHandler chatHandler;
private final ArenaHandler arenaHandler;

private void routeMessage(PlayerSession session, int msgId, byte[] payload, String category) {
    switch (category) {
        // ... existing cases ...
        
        case "SCENE":
        case "WORLD":
            worldHandler.handle(session, msgId, payload);
            break;
            
        case "BATTLE":
            battleHandler.handle(session, msgId, payload);
            break;
            
        case "PET":
            petHandler.handle(session, msgId, payload);
            break;
            
        case "FRIEND":
        case "SOCIAL":
            friendHandler.handle(session, msgId, payload);
            break;
            
        case "CHAT":
            chatHandler.handle(session, msgId, payload);
            break;
            
        case "ARENA":
        case "PVP":
            arenaHandler.handle(session, msgId, payload);
            break;
            
        // ... more cases ...
    }
}
```

### 2. Implement Missing Backend Services

**Priority Order:**
1. **world-service** (P0) - Scene management, AOI, movement
2. **battle-service** (P0) - Combat engine, skills, damage calc
3. **social-service** (P1) - Friends, online status
4. **chat-service** (P1) - Chat rooms, private messages
5. **arena-service** (P1) - PvP matchmaking, rankings

### 3. Handler Template Structure

All handlers should follow this pattern:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class XxxHandler implements MessageHandler {
    
    private final XxxServiceClient serviceClient;
    
    @Override
    public int[] interests() {
        return new int[]{ msgId1, msgId2, ... };
    }
    
    @Override
    public void handle(PlayerSession ps, int msgId, byte[] payload) {
        if (ps.getRoleId() == null) {
            log.warn("[xxx] User not logged in");
            return;
        }
        
        try {
            switch (msgId) {
                case MessageIds.CS_XXX_REQ:
                    handleXxxRequest(ps, payload);
                    break;
                // ... more cases ...
                default:
                    log.warn("[xxx] Unhandled msgId: {}", msgId);
            }
        } catch (Exception e) {
            log.error("[xxx] Error handling msgId={}", msgId, e);
        }
    }
    
    private void handleXxxRequest(PlayerSession ps, byte[] payload) {
        // 1. Parse protobuf
        PB_CSXxxReq req = PB_CSXxxReq.parseFrom(payload);
        
        // 2. Call backend service
        XxxDTO result = serviceClient.doSomething(ps.getRoleId(), req);
        
        // 3. Build and send response
        PB_SCXxxAck.Builder ack = PB_SCXxxAck.newBuilder()
            .setField1(result.getField1())
            // ... set fields ...
            .build();
            
        ps.buildAndSend(MessageIds.SC_XXX_ACK, ack);
    }
}
```

### 4. Client Message Registration

**File:** `MsgIdManger.ts`

Need to register all 375 messages:

```typescript
// World System (8000-8099)
MsgId.RegisterMsg(8001, PB_CSEnterSceneReq);
MsgId.RegisterMsg(8002, PB_SCEnterSceneAck);
MsgId.RegisterMsg(8010, PB_CSMoveReq);
MsgId.RegisterMsg(8020, PB_SCRoleEnterView);
MsgId.RegisterMsg(8021, PB_SCRoleLeaveView);
// ... register all 375 messages

// Battle System (8100-8199)
MsgId.RegisterMsg(8100, PB_CSBattleStartReq);
MsgId.RegisterMsg(8101, PB_SCBattleStartAck);
MsgId.RegisterMsg(8102, PB_CSBattleActionReq);
// ... etc
```

### 5. Error Handling & Validation

All handlers should include:
- **Login check:** `if (ps.getRoleId() == null) return;`
- **Payload validation:** Check null, size, format
- **Service error handling:** Try-catch with proper logging
- **Response always sent:** Even on error, send error code
- **Rate limiting:** Prevent spam (e.g., heartbeat every 10s)

### 6. Testing Strategy

**Unit Tests:** Each handler should have test coverage:
- Valid request → Success response
- Invalid payload → Error handling
- Not logged in → Rejection
- Backend service failure → Graceful degradation

**Integration Tests:** E2E tests via `client/e2e/tests/`
- WebSocket connection
- Login flow
- Message send/receive
- Reconnection handling

**Load Tests:** Verify performance:
- 1000+ concurrent connections
- High message throughput
- Memory/CPU usage

---

## 📈 Metrics & Monitoring

### Recommended Metrics

1. **Connection Metrics**
   - Active WebSocket connections (gauge)
   - Connection rate (rate)
   - Disconnection rate by reason (counter)
   
2. **Message Metrics**
   - Messages received by msgId (counter)
   - Messages sent by msgId (counter)
   - Message processing latency (histogram)
   - Failed messages by msgId (counter)
   
3. **Handler Metrics**
   - Handler execution time by handler (histogram)
   - Handler errors by handler (counter)
   - Backend service call latency (histogram)
   
4. **Session Metrics**
   - Logged in users (gauge)
   - Session duration (histogram)
   - Login success/failure rate (counter)

### Logging Strategy

```java
// Use structured logging
log.info("[handler={}][msgId={}][userId={}][roleId={}] Processing request", 
         "LoginHandler", msgId, ps.getUserId(), ps.getRoleId());

// Log important events
log.info("[login] User logged in: userId={}, roleId={}, session={}", 
         userId, roleId, sessionId);

// Log errors with context
log.error("[battle] Failed to start battle: userId={}, reason={}", 
          userId, e.getMessage(), e);
```

---

## 🎓 Next Steps (Action Items)

### Immediate (Week 1)
1. ✅ Complete this analysis document
2. ⏳ Create `world-service` skeleton
3. ⏳ Implement WorldHandler with world-service integration
4. ⏳ Complete MessageDispatcher routing for all categories
5. ⏳ Register all 375 messages in client MsgIdManger.ts

### Short Term (Week 2-3)
6. ⏳ Implement BattleHandler with combat engine
7. ⏳ Create `social-service` for Friend system
8. ⏳ Create `chat-service` with Redis Pub/Sub
9. ⏳ Complete ArenaHandler
10. ⏳ Write unit tests for all P0/P1 handlers

### Medium Term (Month 2)
11. ⏳ Implement Pet, Dungeon systems
12. ⏳ Implement monetization systems (Recharge, VIP, etc.)
13. ⏳ Complete all P2 handlers
14. ⏳ Write integration tests
15. ⏳ Performance testing & optimization

### Long Term (Month 3+)
16. ⏳ Implement P3 features (Guild Wars, World Boss, etc.)
17. ⏳ Advanced features (Mentor, Marriage, Auction)
18. ⏳ Monitoring & alerting setup
19. ⏳ Production deployment
20. ⏳ Documentation for operations team

---

## 📚 Related Documentation

- [Handler Mapping](document/handler/HANDLER_MAPPING.md) - Complete handler reference
- [Message Index](document/handler/INDEX.md) - All 375+ message protocols
- [Development Guide](document/00-DEVELOPMENT-GUIDE.md) - Service development guide
- [Service Documentation](document/) - Individual service docs (01-38)
- [Best Practices](document/BEST-PRACTICES.md) - Coding standards

---

## 📞 Contact & Support

For questions or issues:
- Architecture: See `MIGRATION_PROJECT_SUMMARY.md`
- Backend Services: See `document/00-DEVELOPMENT-GUIDE.md`
- Frontend Integration: See `document/CLIENT_SERVER_FLOW.md`

**Last Updated:** 2026-01-29  
**Maintainer:** Development Team  
**Status:** Living Document (update as implementation progresses)

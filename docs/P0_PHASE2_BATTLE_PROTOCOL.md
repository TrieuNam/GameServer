# P0 Phase 2: Battle Protocol & Events

**Date:** 2026-04-09
**Status:** 📋 **PLANNED**
**Phase:** P0 - Priority 0 (Critical) - Phase 2

---

## 📊 OVERVIEW

Phase 2 focuses on **Battle Protocol & Events** - ensuring the combat system has a well-defined protocol for frontend integration and publishes standardized events for analytics and leaderboard systems.

---

## 🎯 OBJECTIVES

### Primary Goals:
1. ✅ Document BattleHandler protocol for frontend team
2. ✅ Standardize combat event schema
3. ✅ Implement dual-perspective event publishing
4. ✅ Add validation and error handling

### Success Criteria:
- Frontend team has complete protocol documentation
- Combat events follow standardized schema
- Events published for both attacker and defender perspectives
- Analytics service can consume events without transformation

---

## 📋 TASKS

### Task 1: Battle Protocol Documentation ❌

**Objective:** Create comprehensive documentation for frontend developers

**Deliverables:**
- `/docs/BATTLE_PROTOCOL_SPEC.md` file
- Request/response format examples
- Error code reference
- Integration examples

**Implementation:**

#### Request Format (CS_BATTLE_REQ = 9650)

```json
{
  "op": 1,              // Operation code (1-4)
  "targetRoleId": 123,  // Target player/monster ID
  "combatType": 1,      // 1=PVE, 2=PVP, 3=ARENA, 4=DUNGEON
  "context": {          // Optional context data
    "stageId": 100,
    "difficulty": 2,
    "teamMembers": [456, 789]
  }
}
```

#### Operation Codes:
- **1: CALCULATE_COMBAT** - Calculate instant combat result
- **2: START_SESSION** - Start turn-based combat session
- **3: EXECUTE_ACTION** - Execute action in active session
- **4: END_SESSION** - End combat session

#### Response Format (SC_BATTLE_RESP = 9651)

**Success Response:**
```json
{
  "success": true,
  "combatId": "uuid-string",
  "winnerId": 456,
  "combatLog": [
    {
      "round": 1,
      "attackerId": 123,
      "defenderId": 456,
      "damage": 150,
      "remainingHp": 850
    }
  ],
  "rewards": {
    "exp": 100,
    "gold": 50,
    "items": [{"itemId": 1001, "count": 1}]
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "errorCode": 1001,
  "error": "INSUFFICIENT_STAMINA",
  "message": "Not enough stamina to start combat"
}
```

#### Error Codes:
- `1000` - INVALID_TARGET
- `1001` - INSUFFICIENT_STAMINA
- `1002` - PLAYER_IN_COMBAT
- `1003` - INVALID_COMBAT_TYPE
- `1004` - SESSION_NOT_FOUND
- `1005` - INVALID_ACTION

**Files to Create:**
- `/docs/BATTLE_PROTOCOL_SPEC.md`
- `/docs/battle/REQUEST_EXAMPLES.md`
- `/docs/battle/ERROR_CODES.md`

---

### Task 2: Combat Event Schema Standardization ❌

**Objective:** Define and implement standardized event schema

**Current Issues:**
- Event schema not fully standardized
- Missing defender perspective
- Partition key strategy unclear

**Target Event Schema:**

```json
{
  "eventType": "COMBAT_RESULT",
  "eventVersion": "1.0",
  "timestamp": 1712621234567,
  "combatId": "uuid-string",
  "sessionId": "session-uuid",
  "combatType": "PVP",
  "duration": 1500,

  "attacker": {
    "roleId": 123,
    "name": "Player1",
    "level": 50,
    "power": 5000,
    "damage": 1500,
    "survived": true
  },

  "defender": {
    "roleId": 456,
    "name": "Player2",
    "level": 48,
    "power": 4800,
    "damage": 1200,
    "survived": false
  },

  "result": {
    "winnerId": 123,
    "winnerSide": "ATTACKER",
    "totalRounds": 5,
    "xpGained": 100,
    "goldGained": 50
  },

  "metadata": {
    "stageId": 100,
    "difficulty": 2,
    "isPvp": true,
    "server": "server-01"
  }
}
```

**Implementation Steps:**

1. Create `CombatEvent` DTO:
```java
// battleserver-service/dto/CombatEvent.java
public class CombatEvent {
    private String eventType;
    private String eventVersion;
    private long timestamp;
    private String combatId;
    private String sessionId;
    private String combatType;
    private int duration;

    private Combatant attacker;
    private Combatant defender;
    private CombatResult result;
    private Map<String, Object> metadata;
}
```

2. Update `CombatServiceGrpcImpl.java`:
```java
private void publishCombatEvent(CombatResult result, CombatRequest request) {
    CombatEvent event = CombatEvent.builder()
        .eventType("COMBAT_RESULT")
        .eventVersion("1.0")
        .timestamp(System.currentTimeMillis())
        .combatId(UUID.randomUUID().toString())
        .combatType(request.getCombatType())
        // ... populate all fields
        .build();

    // Publish for attacker perspective
    eventPublisher.publish("combat.result.attacker", event);

    // Publish for defender perspective
    eventPublisher.publish("combat.result.defender", event);
}
```

3. Define partition strategy:
   - Use `roleId` as partition key for player-specific events
   - Ensures all events for a player go to same partition
   - Enables ordered processing per player

**Files to Modify:**
- `battleserver-service/src/main/java/com/SouthMillion/battleserver_service/dto/CombatEvent.java` (CREATE)
- `battleserver-service/src/main/java/com/SouthMillion/battleserver_service/grpc/CombatServiceGrpcImpl.java`
- `battleserver-service/src/main/java/com/SouthMillion/battleserver_service/publisher/CombatEventPublisher.java`

---

### Task 3: Dual-Perspective Event Publishing ❌

**Objective:** Publish events from both attacker and defender viewpoints

**Why Needed:**
- Analytics needs to track statistics for both players
- Leaderboard updates for both winner and loser
- Achievement tracking for both participants

**Implementation:**

```java
private void publishDualPerspectiveEvents(CombatResult result, CombatRequest request) {
    String combatId = UUID.randomUUID().toString();

    // Attacker perspective
    CombatEvent attackerEvent = buildCombatEvent(result, request, combatId, "ATTACKER");
    eventPublisher.publishWithKey(
        "combat.result",
        String.valueOf(request.getAttackerRoleId()),
        attackerEvent
    );

    // Defender perspective
    CombatEvent defenderEvent = buildCombatEvent(result, request, combatId, "DEFENDER");
    eventPublisher.publishWithKey(
        "combat.result",
        String.valueOf(request.getDefenderRoleId()),
        defenderEvent
    );

    log.info("[Combat] Published dual-perspective events for combatId={}", combatId);
}

private CombatEvent buildCombatEvent(CombatResult result, CombatRequest request,
                                     String combatId, String perspective) {
    return CombatEvent.builder()
        .combatId(combatId)
        .perspective(perspective)
        .isWinner(result.getWinnerId().equals(
            perspective.equals("ATTACKER") ?
            request.getAttackerRoleId() :
            request.getDefenderRoleId()
        ))
        // ... other fields
        .build();
}
```

**Kafka Topic Strategy:**
- Topic: `combat.result`
- Partition Key: `roleId`
- Consumer Groups:
  - `analytics-group` - For analytics service
  - `leaderboard-group` - For leaderboard updates
  - `achievement-group` - For achievement tracking

---

### Task 4: Event Validation & Testing ❌

**Objective:** Ensure events are valid and consumable

**Test Cases:**

1. **Schema Validation Test:**
```java
@Test
void testCombatEventSchemaValid() {
    CombatEvent event = createTestEvent();

    assertNotNull(event.getEventType());
    assertNotNull(event.getCombatId());
    assertNotNull(event.getAttacker());
    assertNotNull(event.getDefender());
    assertNotNull(event.getResult());

    assertTrue(event.getTimestamp() > 0);
    assertTrue(event.getDuration() >= 0);
}
```

2. **Dual Publishing Test:**
```java
@Test
void testDualPerspectivePublishing() {
    CombatResult result = calculateTestCombat();

    publishDualPerspectiveEvents(result, testRequest);

    // Verify 2 events published
    verify(eventPublisher, times(2)).publishWithKey(any(), any(), any());

    // Verify correct partition keys
    verify(eventPublisher).publishWithKey(
        eq("combat.result"),
        eq("123"), // attacker ID
        any()
    );
    verify(eventPublisher).publishWithKey(
        eq("combat.result"),
        eq("456"), // defender ID
        any()
    );
}
```

3. **Consumer Integration Test:**
```java
@Test
void testAnalyticsServiceCanConsume() {
    CombatEvent event = createTestEvent();
    String json = objectMapper.writeValueAsString(event);

    // Verify deserializable
    CombatEvent deserialized = objectMapper.readValue(json, CombatEvent.class);
    assertEquals(event, deserialized);
}
```

---

## 🔧 TECHNICAL DETAILS

### BattleHandler Location:
`webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/battle/BattleHandler.java`

**Current Implementation:**
- ✅ 4 operation codes defined
- ✅ gRPC client integration
- ❌ Missing protocol documentation
- ❌ Error handling incomplete

### CombatServiceGrpcImpl Location:
`battleserver-service/src/main/java/com/SouthMillion/battleserver_service/grpc/CombatServiceGrpcImpl.java`

**Current Implementation:**
- ✅ Basic event publishing
- ❌ Schema not standardized
- ❌ Single perspective only
- ❌ Missing validation

---

## 📊 INTEGRATION POINTS

### BattleHandler → BattleServer
- **Method:** gRPC `calculateCombat()`
- **Request:** `CombatRequest`
- **Response:** `CombatResponse`

### BattleServer → Kafka
- **Topic:** `combat.result`
- **Schema:** `CombatEvent` (to be standardized)
- **Consumers:** analytics-service, leaderboard-service

### Frontend → WebSocket
- **Message:** `CS_BATTLE_REQ` (9650)
- **Response:** `SC_BATTLE_RESP` (9651)

---

## 🚀 DEPLOYMENT PLAN

### Step 1: Create Documentation (Day 1)
- Write BATTLE_PROTOCOL_SPEC.md
- Add request/response examples
- Document error codes
- Share with frontend team

### Step 2: Implement Schema (Day 2)
- Create CombatEvent DTO
- Update event publisher
- Add validation

### Step 3: Dual Publishing (Day 3)
- Implement dual-perspective logic
- Update partition strategy
- Test with Kafka locally

### Step 4: Testing (Day 4)
- Unit tests for event generation
- Integration tests with Kafka
- Consumer compatibility tests

### Step 5: Deploy & Monitor (Day 5)
- Deploy to staging
- Monitor event publishing
- Verify consumer processing
- Production deployment

---

## 📋 TESTING CHECKLIST

- [ ] Protocol documentation complete
- [ ] Frontend team reviewed and approved
- [ ] CombatEvent schema defined
- [ ] Schema validation tests pass
- [ ] Dual-perspective publishing implemented
- [ ] Partition key strategy tested
- [ ] Analytics service can consume events
- [ ] Leaderboard service can consume events
- [ ] Performance: <50ms to publish event
- [ ] No event loss (acknowledged by Kafka)

---

## 🔍 MONITORING & METRICS

### Key Metrics:
```java
// Event publishing success rate
Counter eventPublishSuccess = Counter.build()
    .name("combat_events_published_total")
    .help("Total combat events published")
    .labelNames("perspective", "combat_type")
    .register();

// Event publishing latency
Histogram eventPublishLatency = Histogram.build()
    .name("combat_event_publish_duration_seconds")
    .help("Time to publish combat event")
    .register();

// Event validation failures
Counter eventValidationErrors = Counter.build()
    .name("combat_event_validation_errors_total")
    .help("Invalid combat events")
    .register();
```

### Alerts:
- Event publishing failure rate > 1%
- Event publishing latency > 100ms p95
- Consumer lag > 1000 messages

---

## 📚 DOCUMENTATION DELIVERABLES

1. **BATTLE_PROTOCOL_SPEC.md**
   - Complete protocol definition
   - Request/response formats
   - Error codes
   - Examples

2. **COMBAT_EVENT_SCHEMA.md**
   - Event structure
   - Field descriptions
   - Version history
   - Migration guide

3. **INTEGRATION_GUIDE.md**
   - Frontend integration steps
   - Testing procedures
   - Troubleshooting

---

## 🔜 NEXT STEPS AFTER COMPLETION

Once Phase 2 is complete:
- ✅ Frontend can integrate battle system
- ✅ Analytics has standardized events
- ✅ Leaderboard updates automatically
- → Move to **P0 Phase 3: World Movement Integration**

---

**Estimated Effort:** 4-5 days
**Priority:** HIGH
**Dependencies:** P0 Phase 1 (Complete)
**Blocks:** Analytics implementation, Leaderboard updates

**Last Updated:** 2026-04-09

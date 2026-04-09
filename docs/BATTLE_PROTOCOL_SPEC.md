# Battle Protocol Specification

**Version:** 1.0
**Date:** 2026-04-09
**Status:** ✅ **ACTIVE**

---

## 📋 OVERVIEW

This document defines the **Battle Protocol** for client-server communication in the GameServer project. It provides complete specifications for frontend developers to integrate the combat system.

**WebSocket Messages:**
- **Request:** `CS_BATTLE_REQ` (9650)
- **Response:** `SC_BATTLE_RESP` (9651)

---

## 🎯 OPERATION CODES

The battle system supports 4 operation modes:

| Code | Operation | Description |
|------|-----------|-------------|
| `1` | `CALCULATE_COMBAT` | Calculate instant combat result (auto-battle) |
| `2` | `START_SESSION` | Start turn-based combat session |
| `3` | `EXECUTE_ACTION` | Execute action in active session |
| `4` | `END_SESSION` | End combat session |

---

## 📤 REQUEST FORMAT

### Message Structure

```json
{
  "op": 1,
  "targetRoleId": 123,
  "combatType": 1,
  "context": {
    "stageId": 100,
    "monsterId": 5001,
    "isBoss": false
  }
}
```

### Field Definitions

#### Core Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `op` | Integer | Yes | Operation code (1-4) |
| `targetRoleId` | Long | Yes* | Target player/monster ID (*required for op 1, 2) |
| `combatType` | Integer | No | Combat type code (default: 1) |
| `combatTypeName` | String | No | Combat type name (overrides combatType) |

#### Combat Type Codes

| Code | Name | Description |
|------|------|-------------|
| `1` | `PVP` | Player vs Player (default) |
| `2` | `ARENA` | Arena combat |
| `3` | `TRIAL` | Trial mode |
| `4` | `DUNGEON` | Dungeon combat |
| `5` | `BOSS` | Boss battle |

#### Context Object (Optional)

```json
{
  "stageId": 100,
  "monsterId": 5001,
  "isBoss": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `stageId` | Integer | Stage/level ID for PVE combat |
| `monsterId` | Integer | Monster template ID |
| `isBoss` | Boolean | Is this a boss fight? |

---

## 📥 RESPONSE FORMAT

### Success Response

```json
{
  "success": true,
  "data": {
    "attackerWins": true,
    "rounds": 5,
    "durationMs": 1250,
    "combatType": "PVE"
  }
}
```

### Error Response

```json
{
  "success": false,
  "error": "targetRoleId is required"
}
```

---

## 🔧 OPERATION DETAILS

### Operation 1: CALCULATE_COMBAT

**Purpose:** Calculate instant combat result (auto-battle mode)

**Request:**
```json
{
  "op": 1,
  "targetRoleId": 5001,
  "combatType": 1,
  "stageId": 100,
  "monsterId": 5001,
  "isBoss": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "attackerWins": true,
    "rounds": 5,
    "durationMs": 1250,
    "combatType": "PVE"
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `attackerWins` | Boolean | Did the attacker win? |
| `rounds` | Integer | Number of combat rounds |
| `durationMs` | Long | Combat duration in milliseconds |
| `combatType` | String | Resolved combat type name |

---

### Operation 2: START_SESSION

**Purpose:** Start turn-based combat session for manual control

**Request:**
```json
{
  "op": 2,
  "targetRoleId": 456,
  "combatType": 2,
  "attackerRoleIds": [123],
  "defenderRoleIds": [456]
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `attackerRoleIds` | Array[String] | No | Attacker team (defaults to session roleId) |
| `defenderRoleIds` | Array[String] | No | Defender team (or use targetRoleId) |
| `targetRoleId` | Long | No | Single target (alternative to defenderRoleIds) |

**Response:**
```json
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "startTime": 1712621234567,
    "combatType": "ARENA"
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | String | Unique session identifier (UUID) |
| `startTime` | Long | Session start timestamp (ms) |
| `combatType` | String | Resolved combat type |

---

### Operation 3: EXECUTE_ACTION

**Purpose:** Execute a combat action in an active session

**Request:**
```json
{
  "op": 3,
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "actorRoleId": "123",
  "actionType": 1,
  "skillId": 2001,
  "targetRoleId": "456"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | String | Yes | Combat session ID from START_SESSION |
| `actorRoleId` | String | No | Acting player (defaults to session roleId) |
| `actionType` | Integer | No | Action type (default: 1) |
| `skillId` | Integer | No | Skill ID to use (0 = basic attack) |
| `targetRoleId` | String | No | Target player ID |

**Action Types:**

| Code | Name | Description |
|------|------|-------------|
| `1` | `ATTACK` | Basic attack |
| `2` | `SKILL` | Use skill (requires skillId) |
| `3` | `DEFEND` | Defensive stance |
| `4` | `ITEM` | Use item |

**Response:**
```json
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "combatEnded": false,
    "winnerSide": "",
    "statusCode": 200,
    "statusMessage": "Action executed"
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | String | Combat session ID |
| `combatEnded` | Boolean | Did combat end with this action? |
| `winnerSide` | String | "ATTACKER" or "DEFENDER" if ended |
| `statusCode` | Integer | Status code (200 = success) |
| `statusMessage` | String | Status message |

---

### Operation 4: END_SESSION

**Purpose:** Manually end a combat session

**Request:**
```json
{
  "op": 4,
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "endReason": "NORMAL_END"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionId` | String | Yes | Combat session ID |
| `endReason` | String | No | Reason for ending (default: "NORMAL_END") |

**End Reasons:**

| Value | Description |
|-------|-------------|
| `NORMAL_END` | Normal completion |
| `PLAYER_QUIT` | Player quit/disconnected |
| `TIMEOUT` | Session timeout |
| `ERROR` | Error occurred |

**Response:**
```json
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "attackerWins": true,
    "totalRounds": 8,
    "durationMs": 15234,
    "statusCode": 200,
    "statusMessage": "Combat ended"
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `sessionId` | String | Combat session ID |
| `attackerWins` | Boolean | Did attacker win? |
| `totalRounds` | Integer | Total rounds executed |
| `durationMs` | Long | Total combat duration (ms) |
| `statusCode` | Integer | Status code |
| `statusMessage` | String | Status message |

---

## ⚠️ ERROR CODES

### Standard Error Response

```json
{
  "success": false,
  "errorCode": 1001,
  "error": "INSUFFICIENT_STAMINA",
  "message": "Not enough stamina to start combat"
}
```

### Error Code Reference

| Code | Error | Description | Resolution |
|------|-------|-------------|------------|
| `1000` | `INVALID_TARGET` | Target roleId is invalid or not found | Check targetRoleId exists |
| `1001` | `INSUFFICIENT_STAMINA` | Player doesn't have enough stamina | Wait or use stamina item |
| `1002` | `PLAYER_IN_COMBAT` | Player is already in another combat | End current combat first |
| `1003` | `INVALID_COMBAT_TYPE` | Combat type code is invalid | Use valid combatType (1-5) |
| `1004` | `SESSION_NOT_FOUND` | Combat session doesn't exist | Check sessionId or start new session |
| `1005` | `INVALID_ACTION` | Action type is invalid | Use valid actionType (1-4) |
| `1006` | `INVALID_SKILL` | Skill ID is invalid or not owned | Check skillId |
| `1007` | `TARGET_REQUIRED` | Target is required for this action | Provide targetRoleId |
| `1008` | `SESSION_ENDED` | Combat session has already ended | Start new session |
| `1009` | `NOT_YOUR_TURN` | It's not this player's turn | Wait for turn |
| `1010` | `COOLDOWN_ACTIVE` | Skill is on cooldown | Wait for cooldown |

---

## 📊 PAYLOAD FORMATS

### Binary Format (Legacy)

For binary payloads, the format is:

```
Byte 0-3:   op (int32)
Byte 4-11:  targetRoleId (int64)
Byte 12-15: combatType (int32)
Byte 16-19: stageId (int32)
Byte 20-23: monsterId (int32)
Byte 24-27: isBoss (int32, 0 or 1)
Byte 28+:   sessionId (UTF-8 string, optional)
```

### JSON Format (Recommended)

JSON payloads must start with `{` character:

```json
{
  "op": 1,
  "targetRoleId": 123,
  "combatType": 1,
  "stageId": 100
}
```

**Note:** JSON format is recommended for all new integrations. Binary format is supported for backward compatibility only.

---

## 🔗 INTEGRATION EXAMPLES

### Example 1: Quick PVE Combat

```javascript
// Start auto-battle against monster
const request = {
  op: 1, // CALCULATE_COMBAT
  targetRoleId: 5001,
  combatType: 1, // PVE
  context: {
    stageId: 100,
    monsterId: 5001,
    isBoss: false
  }
};

// Send via WebSocket
ws.send(msgId: 9650, payload: JSON.stringify(request));

// Response
{
  "success": true,
  "data": {
    "attackerWins": true,
    "rounds": 5,
    "durationMs": 1250
  }
}
```

### Example 2: Turn-Based Arena Combat

```javascript
// Step 1: Start combat session
const startRequest = {
  op: 2, // START_SESSION
  targetRoleId: 456,
  combatType: 2 // ARENA
};
ws.send(msgId: 9650, payload: JSON.stringify(startRequest));

// Response: { success: true, data: { sessionId: "uuid..." } }

// Step 2: Execute actions
const actionRequest = {
  op: 3, // EXECUTE_ACTION
  sessionId: "uuid...",
  actionType: 2, // SKILL
  skillId: 2001,
  targetRoleId: "456"
};
ws.send(msgId: 9650, payload: JSON.stringify(actionRequest));

// Step 3: End combat
const endRequest = {
  op: 4, // END_SESSION
  sessionId: "uuid...",
  endReason: "NORMAL_END"
};
ws.send(msgId: 9650, payload: JSON.stringify(endRequest));
```

### Example 3: Boss Battle

```javascript
const request = {
  op: 1,
  targetRoleId: 9001,
  combatType: 5, // BOSS
  context: {
    stageId: 500,
    monsterId: 9001,
    isBoss: true
  }
};

ws.send(msgId: 9650, payload: JSON.stringify(request));
```

---

## 🧪 TESTING

### Test Scenarios

1. **Auto-battle PVE:**
   - Send op=1 with valid monster
   - Verify attackerWins boolean
   - Check rounds and duration

2. **Session Management:**
   - Start session (op=2)
   - Execute multiple actions (op=3)
   - End session (op=4)
   - Verify session cleanup

3. **Error Handling:**
   - Send invalid targetRoleId → expect error
   - Send op=3 without sessionId → expect error
   - Send op=4 for non-existent session → expect error

4. **Combat Types:**
   - Test each combatType (1-5)
   - Verify correct monster/player stats
   - Check boss flag behavior

---

## 🔐 SECURITY NOTES

1. **Authentication:**
   - All requests require valid player session
   - roleId is extracted from session, not request
   - Cannot attack on behalf of other players

2. **Validation:**
   - Server validates all player stats
   - Cannot manipulate combat calculations
   - Invalid stats are rejected

3. **Rate Limiting:**
   - Max 10 combat requests per second per player
   - Max 5 concurrent combat sessions per player
   - Session timeout: 5 minutes

---

## 📈 PERFORMANCE

### Expected Performance

| Metric | Target | Notes |
|--------|--------|-------|
| Response time | <100ms | p95 for calculateCombat |
| Session start | <50ms | p95 for startSession |
| Action execute | <30ms | p95 for executeAction |
| Throughput | >1000/s | Concurrent combat calculations |

---

## 📝 CHANGELOG

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-04-09 | Initial protocol specification |

---

## 🔗 RELATED DOCUMENTS

- [Combat Event Schema](./COMBAT_EVENT_SCHEMA.md)
- [P0 Phase 2 Implementation Plan](./P0_PHASE2_BATTLE_PROTOCOL.md)
- [WebSocket Message IDs](./WEBSOCKET_MESSAGE_IDS.md)

---

**Contact:** Backend Team
**Last Updated:** 2026-04-09
**Status:** ✅ Active

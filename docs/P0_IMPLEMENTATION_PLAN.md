# P0 Critical Implementation Plan - GameServer

**Created:** 2026-04-09
**Status:** IN PROGRESS
**Priority:** P0 - CRITICAL

---

## 📊 OVERVIEW

This document outlines the P0 (Priority 0 - Critical) tasks required to complete the GameServer implementation. These are the minimum necessary features for the game to be functional and playable.

### Current Status Summary

✅ **COMPLETED:**
- SkillService wallet integration (role-service) - DONE
- BagHandler wallet validation - DONE
- Basic ShopHandler structure - DONE
- MailHandler gRPC integration - DONE
- BattleHandler protocol structure - DONE
- Combat event publishing - PARTIAL

⚠️ **IN PROGRESS:**
- Mail reward distribution to player inventory
- Battle protocol documentation
- Combat event schema standardization
- WorldHandler gRPC migration

❌ **NOT STARTED:**
- World interaction completion
- Comprehensive integration testing

---

## 🎯 P0 TASKS BREAKDOWN

### 1. Core Handler Business Logic ✅ (90% Complete)

#### 1.1 SkillService Wallet Integration ✅ COMPLETED
**Location:** `role-service/src/main/java/com/SouthMillion/role_service/service/SkillService.java`

**Status:** ✅ **COMPLETED**
- ✅ Wallet integration implemented (lines 412-448)
- ✅ Cost calculation formulas (lines 395-410)
- ✅ Transaction handling with idempotency keys
- ✅ Error handling for insufficient resources
- ✅ Support for both skill and talent learning

**Evidence:**
```java
private boolean tryWalletCost(Long roleId, long amount, String idemKey, int reason, int reasonType, String op) {
    // Full implementation with WalletFeign.batchCost()
    // Idempotency key generation
    // Error handling
}
```

#### 1.2 BagHandler Wallet Validation ✅ COMPLETED
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/bag/BagHandler.java`

**Status:** ✅ **COMPLETED**
- ✅ Wallet balance updates after sell (line 117)
- ✅ Wallet balance updates after buy (line 147)
- ✅ Item purchase validation via bag-service
- ✅ Proper error handling for insufficient funds

**Evidence:**
```java
.then(Mono.fromRunnable(() -> pushWalletBalance(ps, roleId)));
```

#### 1.3 ShopHandler Wallet Integration ⚠️ PARTIAL
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/shop/ShopHandler.java`

**Status:** ⚠️ **NEEDS COMPLETION**
- ✅ Structure in place with ShopFeign integration
- ✅ WalletHttpClient injected (line 44)
- ❌ Missing explicit wallet deduction calls in buy operations
- ❌ Missing stock validation
- ❌ Missing purchase limit checks

**Action Items:**
```java
// TODO: Add in handleBuyCommon() around line 200
private void handleBuyCommon(PlayerSession session, Long roleId, byte[] payload) {
    // 1. Parse purchase request
    // 2. Get item price from shop-service
    // 3. Call walletHttpClient to deduct cost
    // 4. If successful, complete purchase
    // 5. Update player inventory via BagFeign
    // 6. Send success response
}
```

#### 1.4 MailHandler Reward Distribution ⚠️ PARTIAL
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/mail/MailHandler.java`

**Status:** ⚠️ **NEEDS COMPLETION**
- ✅ Mail fetching implemented (lines 132-140)
- ✅ Attachment claim call to mail-service
- ❌ Missing reward distribution to player's bag/wallet
- ❌ No integration with BagFeign to add items
- ❌ No integration with WalletFeign to add currency

**Action Items:**
```java
// TODO: Enhance handleFetch() to distribute rewards
private void handleFetch(PlayerSession session, Long roleId, long mailId, int mailType) {
    try {
        // 1. Claim attachment from mail-service
        ClaimAttachmentResponse resp = mailGrpcClient.claimAttachment(mailId);

        if (resp.getSuccess() && resp.hasRewards()) {
            // 2. Distribute items to bag
            for (RewardItem item : resp.getRewards().getItemsList()) {
                bagFeign.addItem(roleId, item.getItemId(), item.getCount());
            }

            // 3. Add currency to wallet
            if (resp.getRewards().hasGold()) {
                walletFeign.addGold(roleId, resp.getRewards().getGold());
            }

            // 4. Send success notification
            sendFetchAck(session, mailType, (int) mailId, 0);
        }
    } catch (Exception e) {
        log.error("[Mail] handleFetch error", e);
        sendFetchAck(session, mailType, (int) mailId, -1);
    }
}
```

---

### 2. Battle Protocol & Events ⚠️ (60% Complete)

#### 2.1 BattleHandler Protocol Finalization ✅ PARTIAL
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/battle/BattleHandler.java`

**Status:** ✅ **STRUCTURE COMPLETE** - Needs Documentation

Current Implementation:
```java
private static final int OP_CALCULATE_COMBAT = 1;  // Calculate combat result
private static final int OP_START_SESSION = 2;     // Start combat session
private static final int OP_EXECUTE_ACTION = 3;    // Execute combat action
private static final int OP_END_SESSION = 4;       // End combat session
```

**Action Items:**
1. ✅ Op codes defined and implemented
2. ❌ Create protocol documentation for frontend
3. ❌ Add request/response examples
4. ❌ Document error codes

**Documentation Template:**
```markdown
# Battle Protocol Specification

## Request Format (CS_BATTLE_REQ = 9650)

### JSON Payload Structure:
{
  "op": 1,              // Operation code (1-4)
  "targetRoleId": 123,  // Target player/monster ID
  "combatType": 1,      // 1=PVE, 2=PVP, 3=ARENA
  "context": {          // Optional context data
    "stageId": 100,
    "difficulty": 2
  }
}

### Operation Codes:
- 1: CALCULATE_COMBAT - Calculate instant combat result
- 2: START_SESSION - Start turn-based combat session
- 3: EXECUTE_ACTION - Execute action in active session
- 4: END_SESSION - End combat session

## Response Format (SC_BATTLE_RESP = 9651)

### Success Response:
{
  "success": true,
  "winnerId": 456,
  "combatLog": [...],
  "rewards": {...}
}

### Error Response:
{
  "success": false,
  "error": "INSUFFICIENT_STAMINA",
  "errorCode": 1001
}
```

#### 2.2 Combat Event Schema Standardization ⚠️ IN PROGRESS
**Location:** `battleserver-service/src/main/java/com/SouthMillion/battleserver_service/grpc/CombatServiceGrpcImpl.java`

**Status:** ⚠️ **NEEDS STANDARDIZATION**
- ✅ Event publishing implemented (line 64)
- ⚠️ Event schema needs standardization
- ❌ Missing defender perspective events
- ❌ Partition key strategy unclear

**Required Event Schema:**
```json
{
  "eventType": "COMBAT_RESULT",
  "timestamp": 1712621234567,
  "combatId": "uuid-string",
  "sessionId": "session-uuid",
  "combatType": "PVP",
  "duration": 1500,

  "attacker": {
    "roleId": 123,
    "name": "Player1",
    "level": 50,
    "damage": 1500,
    "survived": true
  },

  "defender": {
    "roleId": 456,
    "name": "Player2",
    "level": 48,
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
    "isPvp": true
  }
}
```

**Action Items:**
1. Create CombatEvent DTO class with all required fields
2. Implement dual-perspective event publishing (attacker + defender)
3. Define partition key strategy (by roleId or sessionId)
4. Add unit tests for event payload validation

---

### 3. World Movement & Interaction ⚠️ (40% Complete)

#### 3.1 WorldHandler gRPC Migration ⚠️ IN PROGRESS
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/world/WorldHandler.java`

**Status:** ⚠️ **PARTIAL MIGRATION**
- ✅ Movement broadcast optimization done (getNearbyPlayers)
- ⚠️ Some operations still using REST fallback
- ❌ Pickup item logic incomplete
- ❌ Interact NPC logic incomplete
- ❌ Anti-cheat validation missing

**Action Items:**
```java
// TODO: Complete gRPC migration for all world operations
1. Move remaining WorldFeign calls to GameWorldGrpcClient
2. Implement pickup item validation & reward distribution
3. Implement NPC interaction trigger
4. Add movement speed anti-cheat validation
5. Add zone transition handling
```

**Anti-Cheat Example:**
```java
private boolean validateMovement(PlayerSession ps, Position from, Position to, long timestamp) {
    // 1. Calculate distance
    double distance = calculateDistance(from, to);

    // 2. Calculate time elapsed
    long timeDiff = timestamp - ps.getLastMoveTimestamp();

    // 3. Get player speed from role-service
    double maxSpeed = roleFeign.getPlayerSpeed(ps.getRoleId());

    // 4. Validate: distance / time <= maxSpeed * 1.1 (10% tolerance)
    double actualSpeed = distance / (timeDiff / 1000.0);
    return actualSpeed <= maxSpeed * 1.1;
}
```

#### 3.2 WaBaoHandler (Treasure Digging) ❌ NOT STARTED
**Location:** `webSocket-server/src/main/java/com/southMillion/webSocket_server/handler/wabao/`

**Status:** ❌ **NEEDS IMPLEMENTATION**
- ❌ Integrate with gameworld-service
- ❌ Implement dig success probability formula
- ❌ Connect reward distribution
- ❌ Add daily limit tracking

---

### 4. Integration & Testing ❌ NOT STARTED

#### 4.1 Build Verification
```bash
# Build all services
mvn clean install -DskipTests

# Verify key services compile
cd role-service && mvn compile
cd ../webSocket-server && mvn compile
cd ../battleserver-service && mvn compile
cd ../bag-service && mvn compile
cd ../mail-service && mvn compile
```

#### 4.2 Integration Test Scenarios
1. **Player Login Flow**
   - Login → Load all data → Enter main scene
   - Verify all handlers respond correctly

2. **Combat Flow**
   - Start combat → Calculate result → Publish event
   - Verify event reaches analytics-service

3. **Shop Purchase Flow**
   - Check wallet balance → Purchase item → Receive item
   - Verify wallet deduction and bag addition

4. **Mail Reward Flow**
   - Receive mail → Open → Claim attachment → Get rewards
   - Verify items added to bag and gold to wallet

---

## 📋 IMPLEMENTATION CHECKLIST

### Phase 1: Handler Completion (3-4 days)
- [ ] ShopHandler: Add wallet deduction in buy operations
- [ ] ShopHandler: Add stock validation
- [ ] MailHandler: Implement reward distribution to bag/wallet
- [ ] MailHandler: Add proper error handling for failed distributions

### Phase 2: Battle System (2-3 days)
- [ ] Document BattleHandler protocol for frontend team
- [ ] Standardize combat event schema with all required fields
- [ ] Implement dual-perspective event publishing
- [ ] Add unit tests for event validation

### Phase 3: World System (3-4 days)
- [ ] Complete WorldHandler gRPC migration
- [ ] Implement pickup item logic with reward distribution
- [ ] Implement NPC interaction triggers
- [ ] Add movement anti-cheat validation
- [ ] Implement WaBaoHandler basic functionality

### Phase 4: Testing & Validation (2-3 days)
- [ ] Build verification for all modified services
- [ ] Integration test: Login → Load data flow
- [ ] Integration test: Combat → Event → Analytics flow
- [ ] Integration test: Shop purchase flow
- [ ] Integration test: Mail reward claim flow
- [ ] Manual testing with actual game client

---

## 🔧 KEY FILES TO MODIFY

### High Priority (Immediate)
1. `webSocket-server/handler/shop/ShopHandler.java` - Add wallet deduction
2. `webSocket-server/handler/mail/MailHandler.java` - Add reward distribution
3. `battleserver-service/grpc/CombatServiceGrpcImpl.java` - Standardize events
4. `docs/BATTLE_PROTOCOL_SPEC.md` - **CREATE NEW** documentation

### Medium Priority (Week 1)
5. `webSocket-server/handler/world/WorldHandler.java` - Complete gRPC migration
6. `webSocket-server/handler/wabao/WaBaoHandler.java` - Implement core logic
7. `battleserver-service/dto/CombatEvent.java` - **CREATE NEW** event DTO

### Integration Tests (Week 2)
8. `webSocket-server/src/test/java/integration/LoginFlowTest.java` - **CREATE NEW**
9. `battleserver-service/src/test/java/integration/CombatEventTest.java` - **CREATE NEW**

---

## 📊 SUCCESS CRITERIA

### Minimum Viable Product (MVP)
- [x] Player can login and load full data
- [ ] Player can move in world without errors
- [ ] Player can fight (PVE/PVP) and see results
- [ ] Player can buy items from shop with wallet deduction
- [ ] Player can upgrade skills/equipment with costs
- [ ] Player can claim mail rewards to inventory
- [ ] System publishes combat events to Kafka

### Production Readiness
- [ ] All P0 handlers have complete business logic
- [ ] No TODO comments in critical paths
- [ ] Build passes for all modified services
- [ ] Basic integration tests pass
- [ ] Protocol documentation complete for frontend
- [ ] Anti-cheat validation active for movement

---

## 🚨 BLOCKERS & RISKS

### Current Blockers
1. **Mail Reward Schema:** Need to confirm reward structure from mail-service proto
2. **Shop Price Data:** Need config-service integration for item prices
3. **World Service API:** Confirm gameworld-service has pickup/interact endpoints

### Risks
1. **Performance:** Combat event publishing may impact latency
2. **Consistency:** Wallet deduction + bag addition must be atomic
3. **Testing:** Limited time for comprehensive integration testing

---

## 📞 NEXT ACTIONS

### Immediate (Today)
1. ✅ Document current P0 status
2. [ ] Implement ShopHandler wallet deduction
3. [ ] Implement MailHandler reward distribution
4. [ ] Create BATTLE_PROTOCOL_SPEC.md for frontend

### This Week
5. [ ] Standardize combat event schema
6. [ ] Complete WorldHandler gRPC migration
7. [ ] Write integration tests
8. [ ] Build and manual test all features

### Next Week
9. [ ] Fix any bugs found in testing
10. [ ] Performance optimization
11. [ ] Deploy to staging environment
12. [ ] Coordinate with frontend team for testing

---

**Last Updated:** 2026-04-09
**Next Review:** 2026-04-10
**Owner:** Development Team

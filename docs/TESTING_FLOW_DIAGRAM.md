# 📊 Testing Flow Diagram

```
╔════════════════════════════════════════════════════════════════╗
║                    TESTING WORKFLOW                            ║
║                   Client + P0 + P1                             ║
╚════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────┐
│ PHASE 0: BUILD (5-10 min)                                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Build common-lib                                      │
│  ┌────────────────────┐                                        │
│  │ mvn clean install  │ ──► common-lib-1.0.0.jar               │
│  └────────────────────┘                                        │
│                                                                 │
│  Step 2: Build P0 Services                                     │
│  ┌────────────────────┐                                        │
│  │ eureka-server      │ ──► eureka-server-1.0.0.jar            │
│  │ config-service     │ ──► config-service-1.0.0.jar           │
│  │ gateway-service    │ ──► gateway-service-1.0.0.jar          │
│  │ webSocket-server   │ ──► webSocket-server-1.0.0.jar         │
│  │ session-service    │ ──► session-service-1.0.0.jar          │
│  └────────────────────┘                                        │
│                                                                 │
│  Step 3: Build P1 Services (Optional)                          │
│  ┌────────────────────┐                                        │
│  │ item-service       │ ──► item-service-1.0.0.jar             │
│  │ wallet-service     │ ──► wallet-service-1.0.0.jar           │
│  │ bag-service        │ ──► bag-service-1.0.0.jar              │
│  └────────────────────┘                                        │
│                                                                 │
│  ✅ All JARs ready                                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1: START SERVICES (5-10 min)                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Terminal 1:  Eureka Server (8761)                             │
│  ┌──────────────────────────────────────┐                      │
│  │ java -jar eureka-server.jar          │ ──► 🟢 UP           │
│  │ Wait 30 sec                          │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
│  Terminal 2:  Config Service (8091)                            │
│  ┌──────────────────────────────────────┐                      │
│  │ java -jar config-service.jar         │ ──► 🟢 UP           │
│  │ Wait 20 sec                          │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
│  Terminal 3:  Gateway Service (8080) ◄── CLIENT CONNECTS HERE  │
│  ┌──────────────────────────────────────┐                      │
│  │ java -jar gateway-service.jar        │ ──► 🟢 UP           │
│  │ Wait 15 sec                          │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
│  Terminal 4:  WebSocket Server (8090)                          │
│  ┌──────────────────────────────────────┐                      │
│  │ java -jar webSocket-server.jar       │ ──► 🟢 UP           │
│  │ Wait 10 sec                          │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
│  Terminal 5:  Session Service (8081)                           │
│  ┌──────────────────────────────────────┐                      │
│  │ java -jar session-service.jar        │ ──► 🟢 UP           │
│  │ Wait 10 sec                          │                      │
│  └──────────────────────────────────────┘                      │
│                                                                 │
│  Check: http://localhost:8761 (Eureka Dashboard)               │
│  ✅ All services registered                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2: TEST INFRASTRUCTURE (10-15 min)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Test 1: Gateway Health                                        │
│  ┌────────────────────────────────────────────┐                │
│  │ curl http://localhost:8080/actuator/health │                │
│  └────────────────────────────────────────────┘                │
│         │                                                       │
│         ├─► ✅ {"status":"UP"}                                 │
│         └─► ❌ Connection refused → Check Gateway              │
│                                                                 │
│  Test 2: Login Endpoint                                        │
│  ┌──────────────────────────────────────────────┐              │
│  │ POST /session-service/api/session/login      │              │
│  │ Body: {"username":"test","password":"test"}  │              │
│  └──────────────────────────────────────────────┘              │
│         │                                                       │
│         ├─► ✅ {"token":"eyJ...","userId":"123"}               │
│         └─► ❌ 404 → Check Session service                     │
│                                                                 │
│  Test 3: WebSocket Connection                                  │
│  ┌───────────────────────────────────────────────────┐         │
│  │ ws://localhost:8080/websocket-server/ws/game      │         │
│  │    ?token=YOUR_JWT_TOKEN                          │         │
│  └───────────────────────────────────────────────────┘         │
│         │                                                       │
│         ├─► ✅ Connection established                          │
│         ├─► ✅ Heartbeat working (MsgID 1003)                  │
│         └─► ❌ 401 → Check token                               │
│                                                                 │
│  Test 4: Eureka Dashboard                                      │
│  ┌────────────────────────────┐                                │
│  │ http://localhost:8761      │                                │
│  └────────────────────────────┘                                │
│         │                                                       │
│         ├─► ✅ 4-5 services listed                             │
│         └─► ❌ Empty → Wait 30 sec                             │
│                                                                 │
│  ✅ Phase P0 Infrastructure OK                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 3: TEST ECONOMY SERVICES (5-10 min) - OPTIONAL            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Test 5: Item Service                                          │
│  ┌─────────────────────────────────────────┐                   │
│  │ GET /item-service/api/items/1           │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         ├─► ✅ Item data returned                              │
│         └─► ❌ 404 → Item not found (OK)                       │
│                                                                 │
│  Test 6: Wallet Service                                        │
│  ┌─────────────────────────────────────────────┐               │
│  │ GET /wallet-service/api/wallet/balance/123  │               │
│  └─────────────────────────────────────────────┘               │
│         │                                                       │
│         ├─► ✅ Balance data                                    │
│         └─► ❌ Empty balances (OK)                             │
│                                                                 │
│  Test 7: Bag Service                                           │
│  ┌──────────────────────────────────┐                          │
│  │ GET /bag-service/api/bag/123     │                          │
│  └──────────────────────────────────┘                          │
│         │                                                       │
│         ├─► ✅ Bag contents                                    │
│         └─► ❌ Empty bag (OK)                                  │
│                                                                 │
│  ✅ Phase P1 Economy Services OK                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 4: CLIENT INTEGRATION (10-15 min)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Update Client Configuration                           │
│  ┌─────────────────────────────────────────┐                   │
│  │ File: client/src/config/ServerConfig.ts │                   │
│  │                                          │                   │
│  │ OLD: localhost:7456  (Cocos)             │                   │
│  │ NEW: localhost:8080  (GameServer)        │                   │
│  │                                          │                   │
│  │ SERVER_URL = "http://localhost:8080"     │                   │
│  │ WS_URL = "ws://localhost:8080/           │                   │
│  │           websocket-server/ws/game"      │                   │
│  └─────────────────────────────────────────┘                   │
│                                                                 │
│  Step 2: Update Connection Code                                │
│  ┌─────────────────────────────────────────┐                   │
│  │ 1. Add REST login flow                  │                   │
│  │ 2. Get JWT token                        │                   │
│  │ 3. Connect WebSocket with token         │                   │
│  │ 4. Use Big Endian encoding              │                   │
│  │                                          │                   │
│  │ Copy from:                               │                   │
│  │ docs/CLIENT_CONNECTION_TEMPLATE.ts       │                   │
│  └─────────────────────────────────────────┘                   │
│                                                                 │
│  Step 3: Start Client                                          │
│  ┌─────────────────────────────────────────┐                   │
│  │ cd client/LineR                          │                   │
│  │ Use Laya IDE or npm run dev             │                   │
│  └─────────────────────────────────────────┘                   │
│         │                                                       │
│         └─► Browser opens: http://localhost:7456               │
│                                                                 │
│  Step 4: Test Login Flow                                       │
│  ┌─────────────────────────────────────────┐                   │
│  │ 1. Enter username: testuser              │                   │
│  │ 2. Enter password: testpass              │                   │
│  │ 3. Click Login                           │                   │
│  │                                          │                   │
│  │ Console Output:                          │                   │
│  │ ✅ "Login successful!"                    │                   │
│  │ ✅ "Connected to GameServer"              │                   │
│  │ ✅ "Sent login (MsgID 7056)"              │                   │
│  │ ✅ "Received ACK (MsgID 7000)"            │                   │
│  │ ✅ "Heartbeat working"                    │                   │
│  └─────────────────────────────────────────┘                   │
│                                                                 │
│  ✅ Client Integration Complete                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 5: END-TO-END VALIDATION (5-10 min)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Complete User Flow:                                           │
│                                                                 │
│  ┌──────┐                                                      │
│  │Client│                                                      │
│  └──┬───┘                                                      │
│     │                                                           │
│     │ 1. Login Request (REST)                                  │
│     ├─────────────────────────► Gateway (8080)                │
│     │                                   │                      │
│     │                                   ├──► Session (8081)    │
│     │                                   │                      │
│     │ 2. JWT Token Response            │                      │
│     │◄─────────────────────────────────┤                      │
│     │                                                           │
│     │ 3. WebSocket Connect (token)                             │
│     ├─────────────────────────► Gateway (8080)                │
│     │                                   │                      │
│     │                                   ├──► WebSocket (8090)  │
│     │                                   │                      │
│     │ 4. Connection Established         │                      │
│     │◄─────────────────────────────────┤                      │
│     │                                                           │
│     │ 5. Send Login (MsgID 7056)                               │
│     ├──────────────────────────────────►│                      │
│     │                                                           │
│     │ 6. Login ACK (MsgID 7000)                                │
│     │◄──────────────────────────────────┤                      │
│     │                                                           │
│     │ 7. Request Bag (MsgID 1500)                              │
│     ├──────────────────────────────────►│                      │
│     │                                   │                      │
│     │                                   ├──► Bag Service       │
│     │                                   │                      │
│     │ 8. Bag Info (MsgID 1505)          │                      │
│     │◄──────────────────────────────────┤                      │
│     │                                                           │
│     │ 9. Heartbeat (every 30s)                                 │
│     ├──────────────────────────────────►│                      │
│     │◄──────────────────────────────────┤                      │
│     │                                                           │
│  ✅ All Communication Working                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ SUCCESS CRITERIA ✅                                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ All services running                                        │
│  ✅ All services in Eureka                                      │
│  ✅ Gateway accessible                                          │
│  ✅ REST APIs working                                           │
│  ✅ WebSocket connected                                         │
│  ✅ Messages send/receive                                       │
│  ✅ Client integrated                                           │
│  ✅ No errors in logs                                           │
│  ✅ E2E flow complete                                           │
│                                                                 │
│  🎉 SYSTEM FULLY OPERATIONAL                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘


╔════════════════════════════════════════════════════════════════╗
║                   QUICK START COMMANDS                         ║
╚════════════════════════════════════════════════════════════════╝

1. Build:
   cd D:\project\serverGame\GameServer\common-lib
   mvn clean install -DskipTests

2. Test:
   cd D:\project\serverGame\GameServer
   quick-test.cmd

3. Verify:
   http://localhost:8761  (Eureka Dashboard)
   http://localhost:8080/actuator/health  (Gateway)

4. Client:
   Update URLs → localhost:8080
   Start client
   Test login

╔════════════════════════════════════════════════════════════════╗
║                      TIME BREAKDOWN                            ║
╚════════════════════════════════════════════════════════════════╝

Phase 0: Build           →  5-10 min
Phase 1: Start Services  →  5-10 min
Phase 2: Test P0         → 10-15 min
Phase 3: Test P1         →  5-10 min (optional)
Phase 4: Client          → 10-15 min
Phase 5: E2E             →  5-10 min

TOTAL: 30-45 minutes (quick) or 45-60 minutes (detailed)

╔════════════════════════════════════════════════════════════════╗
║                       RESOURCES                                ║
╚════════════════════════════════════════════════════════════════╝

📘 Detailed Guide: TESTING_GUIDE_STEP_BY_STEP.md
🚀 Quick Test:    quick-test.cmd
📝 Client Guide:  docs/CLIENT_DIRECT_CONNECTION_GUIDE.md
💻 Code Template: docs/CLIENT_CONNECTION_TEMPLATE.ts
📄 Quick Ref:     docs/CLIENT_CONNECTION_QUICKREF.md
```


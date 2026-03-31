# 🧪 HƯỚNG DẪN TEST CLIENT + PHASE P0 + P1

**Mục đích**: Kiểm tra toàn bộ hệ thống hoạt động từ Client → Phase P0 (Infrastructure) → Phase P1 (Economy Services)

**Thời gian**: ~30-45 phút

**Date**: 2025-11-09

---

## 📋 CHUẨN BỊ

### Yêu Cầu
- ✅ Java 21 installed
- ✅ Maven installed
- ✅ Git installed
- ✅ Laya Engine hoặc web browser
- ✅ Port available: 8080, 8081, 8090, 8091, 8761
- ✅ MySQL running (cho Phase P1 services)

### Tools Cần Thiết
- ✅ Browser DevTools (F12)
- ✅ Postman hoặc curl
- ✅ Text editor
- ✅ Terminal/Command Prompt

---

## 🎯 PHASE 0: BUILD ALL SERVICES

### Bước 0.1: Build Common-Lib (CRITICAL)

**Vị trí**: `D:\project\serverGame\GameServer\common-lib`

```bash
cd D:\project\serverGame\GameServer\common-lib
mvn clean install -DskipTests
```

**Kiểm tra thành công**:
```bash
# Phải thấy:
[INFO] BUILD SUCCESS
[INFO] Installing D:\project\serverGame\GameServer\common-lib\target\common-lib-1.0.0.jar

# Check file JAR tồn tại:
dir target\common-lib-1.0.0.jar
```

**❌ Nếu BUILD FAILURE**:
- Check errors trong console
- Fix Lombok issues trong DTOs
- See: `docs/migration/phase-p1_BUILD_ERRORS.md`
- Liên hệ để được hỗ trợ fix

**⏱️ Thời gian**: 15-30 giây

---

### Bước 0.2: Build Phase P0 Services (Infrastructure)

**Build lần lượt từng service**:

#### 1. Eureka Server
```bash
cd D:\project\serverGame\GameServer\eureka-server
mvn clean install -DskipTests
```
**Check**: `target\eureka-server-1.0.0.jar` tồn tại

#### 2. Config Service
```bash
cd D:\project\serverGame\GameServer\config-service
mvn clean install -DskipTests
```
**Check**: `target\config-service-1.0.0.jar` tồn tại

#### 3. Gateway Service
```bash
cd D:\project\serverGame\GameServer\gateway-service
mvn clean install -DskipTests
```
**Check**: `target\gateway-service-1.0.0.jar` tồn tại

#### 4. WebSocket Server
```bash
cd D:\project\serverGame\GameServer\webSocket-server
mvn clean install -DskipTests
```
**Check**: `target\webSocket-server-1.0.0.jar` tồn tại

#### 5. Session Service
```bash
cd D:\project\serverGame\GameServer\session-service
mvn clean install -DskipTests
```
**Check**: `target\session-service-1.0.0.jar` tồn tại

**✅ Checklist Phase P0 Build**:
- [ ] common-lib-1.0.0.jar
- [ ] eureka-server-1.0.0.jar
- [ ] config-service-1.0.0.jar
- [ ] gateway-service-1.0.0.jar
- [ ] webSocket-server-1.0.0.jar
- [ ] session-service-1.0.0.jar

**⏱️ Thời gian**: 3-5 phút (tất cả services)

---

### Bước 0.3: Build Phase P1 Services (Economy) - OPTIONAL

**Chỉ build nếu muốn test economy features**:

#### Core Services (Built Successfully)
```bash
# Item Service
cd D:\project\serverGame\GameServer\item-service
mvn clean install -DskipTests

# Wallet Service
cd D:\project\serverGame\GameServer\wallet-service
mvn clean install -DskipTests

# Bag Service
cd D:\project\serverGame\GameServer\bag-service
mvn clean install -DskipTests
```

#### Extended Services (May Have Errors)
```bash
# Shop Service
cd D:\project\serverGame\GameServer\shop-service
mvn clean install -DskipTests

# Equip Service
cd D:\project\serverGame\GameServer\equip-service
mvn clean install -DskipTests

# Drop Service
cd D:\project\serverGame\GameServer\drop-service
mvn clean install -DskipTests

# Gift Service
cd D:\project\serverGame\GameServer\gift-service
mvn clean install -DskipTests

# Box Service
cd D:\project\serverGame\GameServer\box-service
mvn clean install -DskipTests
```

**⚠️ Lưu ý**: Nếu extended services fail, không sao - vẫn test được với core services

**⏱️ Thời gian**: 5-8 phút (nếu build P1)

---

## 🚀 PHASE 1: START SERVICES

### Bước 1.1: Start Eureka Server (Service Discovery)

**Terminal 1**:
```bash
cd D:\project\serverGame\GameServer\eureka-server
java -jar target\eureka-server-1.0.0.jar
```

**Đợi thấy**:
```
Started EurekaServerApplication in X seconds
```

**Kiểm tra**:
- Mở browser: http://localhost:8761
- Phải thấy Eureka Dashboard
- Application: Chưa có service nào (sẽ có sau khi start các service khác)

**⏱️ Đợi**: 20-30 giây để Eureka khởi động hoàn toàn

---

### Bước 1.2: Start Config Service (Configuration)

**Terminal 2**:
```bash
cd D:\project\serverGame\GameServer\config-service
java -jar target\config-service-1.0.0.jar
```

**Đợi thấy**:
```
Started ConfigServiceApplication in X seconds
```

**Kiểm tra**:
```bash
curl http://localhost:8091/actuator/health
```
**Expected**: `{"status":"UP"}`

**⏱️ Đợi**: 15-20 giây

---

### Bước 1.3: Start Gateway Service (API Gateway - CLIENT CONNECTION POINT)

**Terminal 3**:
```bash
cd D:\project\serverGame\GameServer\gateway-service
java -jar target\gateway-service-1.0.0.jar
```

**Đợi thấy**:
```
Started GatewayServiceApplication in X seconds
```

**Kiểm tra**:
```bash
curl http://localhost:8080/actuator/health
```
**Expected**: `{"status":"UP"}`

**⚠️ QUAN TRỌNG**: Gateway là điểm client kết nối!

**⏱️ Đợi**: 15-20 giây

---

### Bước 1.4: Start WebSocket Server (Real-time Communication)

**Terminal 4**:
```bash
cd D:\project\serverGame\GameServer\webSocket-server
java -jar target\webSocket-server-1.0.0.jar
```

**Đợi thấy**:
```
Started WebSocketServerApplication in X seconds
```

**Kiểm tra**: Check logs không có errors

**⏱️ Đợi**: 10-15 giây

---

### Bước 1.5: Start Session Service (Authentication)

**Terminal 5**:
```bash
cd D:\project\serverGame\GameServer\session-service
java -jar target\session-service-1.0.0.jar
```

**Đợi thấy**:
```
Started SessionServiceApplication in X seconds
```

**Kiểm tra**:
```bash
curl http://localhost:8081/actuator/health
```
**Expected**: `{"status":"UP"}`

**⏱️ Đợi**: 10-15 giây

---

### Bước 1.6: Verify All Services Registered

**Mở Eureka Dashboard**: http://localhost:8761

**Phải thấy ít nhất 4 services**:
- ✅ CONFIG-SERVICE
- ✅ GATEWAY-SERVICE
- ✅ WEBSOCKET-SERVER
- ✅ SESSION-SERVICE

**❌ Nếu không thấy service nào**:
- Đợi thêm 30 giây
- Check logs của service đó
- Check port đã bị chiếm chưa: `netstat -ano | findstr :8080`

---

### Bước 1.7: Start Phase P1 Services (OPTIONAL)

**Nếu muốn test economy features**:

**Terminal 6**:
```bash
cd D:\project\serverGame\GameServer\item-service
java -jar target\item-service-1.0.0.jar
```

**Terminal 7**:
```bash
cd D:\project\serverGame\GameServer\wallet-service
java -jar target\wallet-service-1.0.0.jar
```

**Terminal 8**:
```bash
cd D:\project\serverGame\GameServer\bag-service
java -jar target\bag-service-1.0.0.jar
```

**Kiểm tra Eureka**: Should see ITEM-SERVICE, WALLET-SERVICE, BAG-SERVICE

**⏱️ Tổng thời gian start all services**: 5-10 phút

---

## 🧪 PHASE 2: TEST INFRASTRUCTURE (P0)

### Test 2.1: Gateway Health Check

**Command**:
```bash
curl http://localhost:8080/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP"
}
```

**✅ Pass**: Gateway đang hoạt động

---

### Test 2.2: Session Service - Login Endpoint

**Test login endpoint qua Gateway**:

```bash
curl -X POST http://localhost:8080/session-service/api/session/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testuser\",\"password\":\"testpass\"}"
```

**Expected Response** (Success):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "123",
  "roleId": "456"
}
```

**hoặc** (User not found - still OK, endpoint works):
```json
{
  "error": "User not found"
}
```

**❌ Error Response**:
- 404 Not Found → Session service chưa registered hoặc chưa start
- Connection refused → Gateway hoặc Session service chưa chạy
- 500 Server Error → Check logs

**✅ Pass**: Nhận được JSON response (dù là error)

**💾 Lưu token**: Nếu login thành công, copy JWT token để dùng cho test WebSocket

---

### Test 2.3: WebSocket Connection (Manual Test)

**Mở Browser DevTools** (F12) → Console tab

**Paste code sau** (replace YOUR_TOKEN nếu có):

```javascript
// Test 1: Connect WITHOUT token (should fail or need auth)
const ws1 = new WebSocket('ws://localhost:8080/websocket-server/ws/game');
ws1.onopen = () => console.log('✅ Connected without token');
ws1.onerror = (err) => console.log('❌ Error (expected):', err);
ws1.onclose = () => console.log('Connection closed');

// Test 2: Connect WITH token (if you have one)
// Replace YOUR_JWT_TOKEN with actual token from login
const token = "YOUR_JWT_TOKEN";
const ws2 = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws2.binaryType = 'arraybuffer';

ws2.onopen = () => {
    console.log('✅ WebSocket connected with token!');
    
    // Send heartbeat
    const buffer = new ArrayBuffer(8);
    const view = new DataView(buffer);
    view.setInt32(0, 0, false);    // bodyLen = 0
    view.setInt32(4, 1053, false); // CS_HEARTBEAT_REQ
    ws2.send(buffer);
    console.log('Sent heartbeat (MsgID 1053)');
};

ws2.onmessage = (event) => {
    const view = new DataView(event.data);
    const bodyLen = view.getInt32(0, false);
    const msgId = view.getInt32(4, false);
    console.log('📨 Received:', { msgId, bodyLen });
};

ws2.onerror = (err) => console.error('❌ WebSocket error:', err);
ws2.onclose = () => console.log('WebSocket closed');
```

**Expected Results**:
- ✅ Connection established
- ✅ Sent heartbeat message
- ✅ Received response (MsgID 1003 - SC_HEARTBEAT_RESP)

**✅ Pass**: WebSocket connects và có thể send/receive messages

---

### Test 2.4: Time Sync Endpoint

```bash
curl http://localhost:8080/session-service/api/session/timesync
```

**Expected Response**:
```json
{
  "serverTime": 1699545600000,
  "timestamp": "2025-11-09T14:30:00Z"
}
```

**✅ Pass**: Nhận được server time

---

### Test 2.5: Eureka Service List

**Mở**: http://localhost:8761

**Verify**:
- ✅ Instances currently registered with Eureka: ít nhất 4-5 services
- ✅ Status: UP (màu đỏ)
- ✅ All services có heartbeat (green checkmark)

**✅ Pass**: All services healthy trong Eureka

---

## 🧪 PHASE 3: TEST ECONOMY SERVICES (P1) - OPTIONAL

### Test 3.1: Item Service

**Get item by ID**:
```bash
curl http://localhost:8080/item-service/api/items/1
```

**Expected Response**:
```json
{
  "itemId": 1,
  "name": "Gold Coin",
  "type": "CURRENCY",
  "description": "..."
}
```

**hoặc 404** (item not found - still OK, endpoint works)

**✅ Pass**: Endpoint responds

---

### Test 3.2: Wallet Service

**Check balance**:
```bash
curl http://localhost:8080/wallet-service/api/wallet/balance/123
```

**Expected Response**:
```json
{
  "userId": "123",
  "currencies": [
    {"currencyId": 1, "amount": 1000}
  ]
}
```

**hoặc empty balances** (still OK)

**✅ Pass**: Endpoint responds

---

### Test 3.3: Bag Service

**Get bag contents**:
```bash
curl http://localhost:8080/bag-service/api/bag/123
```

**Expected Response**:
```json
{
  "userId": "123",
  "items": [],
  "capacity": 100
}
```

**✅ Pass**: Endpoint responds

---

## 🎮 PHASE 4: TEST CLIENT CONNECTION

### Bước 4.1: Update Client Configuration

**File**: `D:\project\serverGame\client\LineR\src\config\ServerConfig.ts` (hoặc tương tự)

**Update to**:
```typescript
export class ServerConfig {
    static SERVER_URL = "http://localhost:8080";
    static WS_URL = "ws://localhost:8080/websocket-server/ws/game";
}
```

**Tìm và thay đổi TẤT CẢ references tới**:
- `localhost:7456` → `localhost:8080`
- `ws://localhost:7456/ws` → `ws://localhost:8080/websocket-server/ws/game`

---

### Bước 4.2: Update Client Connection Code

**Copy code từ**: `docs/CLIENT_CONNECTION_TEMPLATE.ts`

**Vào client project**: `D:\project\serverGame\client\LineR\assets\script\network\`

**Hoặc update existing code**:

1. **Add JWT Authentication**:
```typescript
// Login via REST first
const response = await fetch('http://localhost:8080/session-service/api/session/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
});
const { token } = await response.json();
localStorage.setItem('jwt_token', token);
```

2. **Connect WebSocket with Token**:
```typescript
const token = localStorage.getItem('jwt_token');
const ws = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws.binaryType = 'arraybuffer';
```

3. **Use Big Endian Encoding**:
```typescript
function encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
    const buffer = new ArrayBuffer(8 + payload.length);
    const view = new DataView(buffer);
    view.setInt32(0, payload.length, false); // Big Endian!
    view.setInt32(4, msgId, false);          // Big Endian!
    new Uint8Array(buffer).set(payload, 8);
    return buffer;
}
```

---

### Bước 4.3: Start Client

**Using Laya IDE**:
1. Open `D:\project\serverGame\client\LineR` in Laya IDE
2. Click "Run" hoặc F5
3. Browser sẽ mở tự động

**Using Command Line**:
```bash
cd D:\project\serverGame\client\LineR
npm run dev
# hoặc
laya compile
```

**Access**: http://localhost:7456 (hoặc port mà Laya IDE config)

---

### Bước 4.4: Test Client-Server Communication

**Trong client UI**:

1. **Enter credentials**:
   - Username: `testuser`
   - Password: `testpass`

2. **Click Login**

3. **Check Browser Console** (F12):
```
✅ "Logging in as testuser..."
✅ "Login successful! Token: eyJ..."
✅ "Connected to GameServer"
✅ "Sent login message (MsgID 7056)"
✅ "Received: {msgId: 7000, bodyLen: ...}" // Login ACK
✅ "Heartbeat response (MsgID 1003)"
```

**❌ Nếu lỗi**:
- Check server logs
- Check browser console for errors
- Verify URLs updated correctly
- Check JWT token exists

**✅ Pass**: Client connects, login works, messages exchanged

---

## 📊 PHASE 5: END-TO-END TEST

### Test 5.1: Complete User Flow

**Scenario**: User login → Get bag → Get wallet

#### Step 1: Login via Client
- Open client
- Enter username/password
- Click login
- **Verify**: "Login successful" message

#### Step 2: WebSocket Connected
- **Verify**: Console shows "Connected to GameServer"
- **Verify**: Heartbeat messages every 30 seconds

#### Step 3: Request Bag Info
**In browser console**:
```javascript
// Send bag request
const ws = window.ws; // Assume WebSocket stored globally
const msgId = 1500; // CS_KNAPSACK_REQ
const buffer = new ArrayBuffer(8);
const view = new DataView(buffer);
view.setInt32(0, 0, false);
view.setInt32(4, msgId, false);
ws.send(buffer);
```

**Expected**: Receive MsgID 1505 (SC_KNAPSACK_ALL_INFO)

#### Step 4: Request Wallet Info
**Via REST** (in browser console):
```javascript
fetch('http://localhost:8080/wallet-service/api/wallet/balance/123')
  .then(r => r.json())
  .then(data => console.log('Wallet:', data));
```

**Expected**: Wallet data returned

**✅ Pass**: Complete flow works từ client → gateway → services

---

## 📋 FINAL CHECKLIST

### Infrastructure (Phase P0)

- [ ] **Eureka Server**: Running on 8761, dashboard accessible
- [ ] **Config Service**: Running on 8091, health check OK
- [ ] **Gateway Service**: Running on 8080, health check OK
- [ ] **WebSocket Server**: Running on 8090, accessible via gateway
- [ ] **Session Service**: Running on 8081, login endpoint works
- [ ] **All Services**: Registered in Eureka dashboard
- [ ] **Health Checks**: All return `{"status":"UP"}`

### Economy Services (Phase P1) - Optional

- [ ] **Item Service**: Running on 8220, item endpoint works
- [ ] **Wallet Service**: Running on 8210, balance endpoint works
- [ ] **Bag Service**: Running on 8230, bag endpoint works
- [ ] **Shop Service**: Running on 8260 (if built)
- [ ] **Services**: Registered in Eureka

### Client Integration

- [ ] **Client URLs**: Updated to localhost:8080
- [ ] **WebSocket URL**: Updated to ws://localhost:8080/websocket-server/ws/game
- [ ] **JWT Auth**: Implemented in client
- [ ] **Big Endian**: Packet encoding correct
- [ ] **Message IDs**: Updated to GameServer IDs
- [ ] **Login Flow**: Works end-to-end
- [ ] **WebSocket**: Connects successfully
- [ ] **Messages**: Send and receive working
- [ ] **Heartbeat**: Working every 30 seconds

### Testing

- [ ] **REST API**: Login endpoint responds
- [ ] **WebSocket**: Connection established
- [ ] **Binary Protocol**: Messages encoded/decoded correctly
- [ ] **Service Discovery**: All services in Eureka
- [ ] **Gateway Routing**: Routes to correct services
- [ ] **No Errors**: No errors in server logs
- [ ] **No Errors**: No errors in browser console

---

## 🎯 SUCCESS CRITERIA

### ✅ MINIMUM (Must Have)

**Phase P0 Working**:
- All 5 infrastructure services running
- Gateway accessible on 8080
- Session login endpoint works
- WebSocket connection establishes
- Heartbeat messages working

**Client Connected**:
- Client can login via REST
- Client receives JWT token
- Client connects WebSocket
- Client sends/receives messages

### ✅ OPTIMAL (Nice to Have)

**Phase P1 Working**:
- Item, Wallet, Bag services running
- Endpoints responding correctly
- Data operations working

**Full Integration**:
- Client → Gateway → All Services
- No errors in any logs
- All features functional

---

## 🚨 COMMON ISSUES & SOLUTIONS

### Issue 1: Build Failures

**Symptom**: `mvn install` fails

**Solutions**:
```bash
# 1. Clean Maven cache
mvn clean

# 2. Update dependencies
mvn dependency:resolve

# 3. Skip tests
mvn install -DskipTests

# 4. Check Java version
java -version  # Should be 21

# 5. If common-lib fails, fix DTOs first
cd common-lib
# Fix Lombok issues
mvn clean install -DskipTests
```

---

### Issue 2: Port Already in Use

**Symptom**: `Port 8080 already in use`

**Solutions**:
```bash
# 1. Find process using port
netstat -ano | findstr :8080

# 2. Kill process
taskkill /PID <PID> /F

# 3. Or change port in application.yml
# Edit: src/main/resources/application.yml
# server:
#   port: 8081
```

---

### Issue 3: Service Not Registering in Eureka

**Symptom**: Service running but không xuất hiện trong Eureka

**Solutions**:
```bash
# 1. Wait 30 seconds (registration delay)

# 2. Check eureka.client config in application.yml
# eureka:
#   client:
#     serviceUrl:
#       defaultZone: http://localhost:8761/eureka/

# 3. Check service logs for "Registered with Eureka"

# 4. Restart service
```

---

### Issue 4: WebSocket Connection Fails

**Symptom**: WebSocket won't connect

**Solutions**:
```javascript
// 1. Check URL correct
ws://localhost:8080/websocket-server/ws/game

// 2. Check token included
?token=YOUR_JWT_TOKEN

// 3. Check binaryType set
ws.binaryType = 'arraybuffer';

// 4. Check Gateway is routing WebSocket
// In gateway logs: "WebSocket connection established"

// 5. Check WebSocket Server logs
```

---

### Issue 5: 401 Unauthorized

**Symptom**: WebSocket connection rejected with 401

**Solutions**:
```bash
# 1. Login first via REST
curl -X POST http://localhost:8080/session-service/api/session/login ...

# 2. Use the JWT token
ws://...?token=ACTUAL_TOKEN_HERE

# 3. Check token not expired
# JWT tokens have expiry time

# 4. Check token format
# Should start with: eyJ...
```

---

### Issue 6: Messages Not Decoded

**Symptom**: Messages received but gibberish

**Solutions**:
```javascript
// MUST use Big Endian (false parameter)
const view = new DataView(buffer);
view.getInt32(0, false);  // ← false = Big Endian
view.getInt32(4, false);  // ← false = Big Endian

// NOT Little Endian (true)
// view.getInt32(0, true);  // ❌ WRONG
```

---

## 📞 SUPPORT

### Nếu Test Fail

**Check These Documents**:
1. `docs/CLIENT_DIRECT_CONNECTION_GUIDE.md` - Complete guide
2. `docs/CLIENT_CONNECTION_TEMPLATE.ts` - Working code
3. `docs/CLIENT_SERVER_CONNECTION.md` - Protocol specs
4. `docs/migration/phase-p1_BUILD_ERRORS.md` - Known build issues

### Debug Checklist

- [ ] Check all services running: `netstat -ano | findstr :8080`
- [ ] Check Eureka dashboard: http://localhost:8761
- [ ] Check service logs for errors
- [ ] Check browser console for errors
- [ ] Verify URLs updated in client
- [ ] Verify JWT token present
- [ ] Verify Big Endian encoding
- [ ] Test each endpoint individually

---

## 🎉 SUCCESS!

**Nếu tất cả tests pass**:

✅ **Phase P0**: Infrastructure hoạt động hoàn hảo  
✅ **Phase P1**: Economy services hoạt động (nếu built)  
✅ **Client**: Connect trực tiếp tới GameServer  
✅ **No Cocos**: Không cần Cocos server nữa!  

**Hệ thống ready for**:
- Development
- Integration testing
- Performance testing
- Production deployment

---

## ⏱️ TIME ESTIMATE

| Phase | Task | Time |
|-------|------|------|
| 0 | Build all services | 5-10 min |
| 1 | Start all services | 5-10 min |
| 2 | Test P0 infrastructure | 10-15 min |
| 3 | Test P1 economy | 5-10 min |
| 4 | Test client connection | 10-15 min |
| 5 | End-to-end test | 5-10 min |
| **TOTAL** | **Full test cycle** | **30-45 min** |

---

**🎯 FOLLOW THESE STEPS IN ORDER - Good luck!**

*Testing Guide v1.0*  
*Date: 2025-11-09*  
*For: GameServer Phase P0 + P1 + Client Integration*


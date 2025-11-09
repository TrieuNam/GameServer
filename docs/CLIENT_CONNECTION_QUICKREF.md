# 🚀 Quick Reference - Client Direct Connection

## ONE-PAGE CHEAT SHEET

### 📍 OLD (Cocos) vs NEW (Direct)

| Aspect | OLD (Cocos Server) | NEW (GameServer Direct) |
|--------|-------------------|------------------------|
| **REST API** | `http://localhost:7456` | `http://localhost:8080` |
| **WebSocket** | `ws://localhost:7456/ws` | `ws://localhost:8080/websocket-server/ws/game` |
| **Authentication** | Cocos handshake | JWT token via REST API |
| **Protocol** | Cocos custom | Big Endian binary |
| **Packet Format** | Cocos format | `[BodyLen(4)][MsgID(4)][Payload]` |

---

## 🔧 3-STEP SETUP

### STEP 1: Start GameServer
```bash
# Run this script
D:\project\serverGame\GameServer\start-gameserver-for-client.cmd

# Or manually:
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar config-service/target/config-service-1.0.0.jar
java -jar gateway-service/target/gateway-service-1.0.0.jar
java -jar webSocket-server/target/webSocket-server-1.0.0.jar
java -jar session-service/target/session-service-1.0.0.jar
```

### STEP 2: Update Client Config

**File**: `client/LineR/src/config/ServerConfig.ts`
```typescript
// CHANGE THIS:
static SERVER_URL = "http://localhost:7456";  // OLD
static WS_URL = "ws://localhost:7456/ws";     // OLD

// TO THIS:
static SERVER_URL = "http://localhost:8080";
static WS_URL = "ws://localhost:8080/websocket-server/ws/game";
```

### STEP 3: Update Connection Flow

**OLD Flow**:
```typescript
// Connect to Cocos
ws = new WebSocket("ws://localhost:7456/ws");
ws.onopen = () => sendCocosHandshake();
```

**NEW Flow**:
```typescript
// 1. Login via REST
const response = await fetch('http://localhost:8080/session-service/api/session/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
});
const { token } = await response.json();

// 2. Connect WebSocket with token
ws = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws.binaryType = 'arraybuffer';
ws.onopen = () => sendLogin();  // GameServer protocol
```

---

## 📊 PACKET FORMAT

### Encoding (Send to Server)
```typescript
function encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
    const bodyLen = payload.length;
    const buffer = new ArrayBuffer(8 + bodyLen);
    const view = new DataView(buffer);
    
    view.setInt32(0, bodyLen, false);  // Big Endian!
    view.setInt32(4, msgId, false);    // Big Endian!
    
    new Uint8Array(buffer).set(payload, 8);
    return buffer;
}
```

### Decoding (Receive from Server)
```typescript
ws.onmessage = (event) => {
    const view = new DataView(event.data);
    const bodyLen = view.getInt32(0, false);  // Big Endian!
    const msgId = view.getInt32(4, false);    // Big Endian!
    const payload = new Uint8Array(event.data, 8, bodyLen);
    
    handleMessage(msgId, payload);
};
```

---

## 🔑 COMMON MESSAGE IDs

```typescript
// Login
CS_LOGIN_REQ = 7056
SC_LOGIN_ACK = 7000

// Heartbeat
CS_HEARTBEAT_REQ = 1053
SC_HEARTBEAT_RESP = 1003

// Bag/Inventory
CS_KNAPSACK_REQ = 1500
SC_KNAPSACK_ALL_INFO = 1505

// Mail
CS_MAIL_LIST_REQ = 9551
SC_MAIL_INFO = 9504
```

---

## 🧪 TESTING

### Test REST API
```bash
curl -X POST http://localhost:8080/session-service/api/session/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

### Test WebSocket (Browser Console)
```javascript
const token = "YOUR_JWT_TOKEN";
const ws = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws.binaryType = 'arraybuffer';

ws.onopen = () => {
    console.log('Connected!');
    // Send heartbeat
    const buffer = new ArrayBuffer(8);
    const view = new DataView(buffer);
    view.setInt32(0, 0, false);    // bodyLen
    view.setInt32(4, 1053, false); // CS_HEARTBEAT_REQ
    ws.send(buffer);
};

ws.onmessage = (e) => {
    const view = new DataView(e.data);
    console.log('MsgID:', view.getInt32(4, false));
};
```

---

## 🚨 TROUBLESHOOTING

### Connection Refused
```
✅ Check Gateway running: http://localhost:8080/actuator/health
✅ Check all 5 services started
✅ Check firewall
```

### 401 Unauthorized
```
✅ Login first via REST API
✅ Get valid JWT token
✅ Pass token in WebSocket URL: ?token=XXX
```

### Messages Not Working
```
✅ Use Big Endian (false in DataView)
✅ Check Message ID correct
✅ Verify packet format: [BodyLen(4)][MsgID(4)][Payload]
```

### CORS Error
```
✅ Add client origin to Gateway CORS config
✅ Check: gateway-service/src/main/resources/application.yml
```

---

## 📝 CHECKLIST

**Server Setup**:
- [ ] Run start-gameserver-for-client.cmd
- [ ] All 5 services running
- [ ] Eureka dashboard shows all services
- [ ] Gateway health check OK

**Client Update**:
- [ ] SERVER_URL → localhost:8080
- [ ] WS_URL → ws://localhost:8080/websocket-server/ws/game
- [ ] Removed Cocos server code
- [ ] Added JWT authentication
- [ ] Big Endian encoding
- [ ] Updated Message IDs

**Testing**:
- [ ] REST login works
- [ ] JWT token received
- [ ] WebSocket connects
- [ ] Messages send/receive
- [ ] Heartbeat working

---

## 📚 FULL DOCUMENTATION

- **Complete Guide**: `docs/CLIENT_DIRECT_CONNECTION_GUIDE.md`
- **Code Template**: `docs/CLIENT_CONNECTION_TEMPLATE.ts`
- **Message IDs**: `docs/CLIENT_SERVER_CONNECTION.md`
- **Integration**: `docs/CLIENT_INTEGRATION_GUIDE.md`

---

## ⚡ QUICK START COMMANDS

```bash
# 1. Build (if needed)
cd D:\project\serverGame\GameServer
mvn clean install -DskipTests

# 2. Start server
start-gameserver-for-client.cmd

# 3. Verify
curl http://localhost:8080/actuator/health
curl http://localhost:8761  # Eureka dashboard

# 4. Test login
curl -X POST http://localhost:8080/session-service/api/session/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"user\",\"password\":\"pass\"}"

# 5. Start client
cd D:\project\serverGame\client\LineR
# Use Laya IDE or npm run dev
```

---

**🎉 SUCCESS!**

Your client now connects directly to GameServer - No Cocos server needed!

**Ports Summary**:
- Gateway: 8080 (Client connects here)
- WebSocket: 8090 (Via Gateway proxy)
- Session: 8081 (Via Gateway)
- Config: 8091
- Eureka: 8761

**Connection**: `Client → Gateway (8080) → WebSocket (8090)`

---

*Quick Reference v1.0*  
*Date: 2025-11-09*  
*For: Laya Engine → Spring Boot GameServer*


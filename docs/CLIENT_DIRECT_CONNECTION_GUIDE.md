# 🔌 Client-Server Direct Connection Guide

**Mục đích**: Connect trực tiếp Laya Engine client → Spring Boot GameServer (không qua Cocos)

**Ngày**: 2025-11-09

---

## 🎯 OVERVIEW

### Kiến Trúc Cũ (Với Cocos)
```
Laya Client → Cocos Server (proxy) → GameServer (Spring Boot)
              (Port 7456)              (Port 8080/8090)
```

### Kiến Trúc Mới (Trực Tiếp)
```
Laya Client → Gateway (Spring Boot) → Services
              (Port 8080)              (WebSocket 8090)
```

---

## 📋 PREREQUISITES

### Server Side (GameServer)

**Đã có sẵn**:
- ✅ Gateway Service (Port 8080) - HTTP/REST + WebSocket proxy
- ✅ WebSocket Server (Port 8090) - Binary protocol
- ✅ Session Service (Port 8081) - Authentication
- ✅ Eureka Server (Port 8761) - Service discovery
- ✅ Config Service (Port 8091) - Configuration

**Cần chạy**:
```bash
# Start theo thứ tự:
1. Eureka Server (8761)
2. Config Service (8091)
3. Gateway (8080)
4. WebSocket Server (8090)
5. Session Service (8081)
```

### Client Side

**Client path**: `D:\project\serverGame\client\LineR\`

**Cần update**:
1. Server URL configuration
2. WebSocket connection endpoint
3. Remove Cocos server dependency

---

## 🔧 STEP-BY-STEP SETUP

### STEP 1: Start GameServer Services

#### 1.1. Start Eureka (Service Discovery)
```bash
cd D:\project\serverGame\GameServer\eureka-server
java -jar target\eureka-server-1.0.0.jar
```
**Verify**: http://localhost:8761

#### 1.2. Start Config Service
```bash
cd D:\project\serverGame\GameServer\config-service
java -jar target\config-service-1.0.0.jar
```
**Verify**: http://localhost:8091/actuator/health

#### 1.3. Start Gateway
```bash
cd D:\project\serverGame\GameServer\gateway-service
java -jar target\gateway-service-1.0.0.jar
```
**Verify**: http://localhost:8080/actuator/health

#### 1.4. Start WebSocket Server
```bash
cd D:\project\serverGame\GameServer\webSocket-server
java -jar target\webSocket-server-1.0.0.jar
```
**Verify**: Check logs for "Started WebSocketServerApplication"

#### 1.5. Start Session Service
```bash
cd D:\project\serverGame\GameServer\session-service
java -jar target\session-service-1.0.0.jar
```
**Verify**: http://localhost:8081/actuator/health

---

### STEP 2: Update Client Configuration

#### 2.1. Locate Client Config File

**File paths to check**:
```
D:\project\serverGame\client\LineR\assets\config\server.json
D:\project\serverGame\client\LineR\assets\config\config.json
D:\project\serverGame\client\LineR\src\config\ServerConfig.ts
D:\project\serverGame\client\LineR\bin\index.html
```

#### 2.2. Update Server URL

**OLD Configuration** (Cocos):
```json
{
  "serverUrl": "http://localhost:7456",
  "wsUrl": "ws://localhost:7456/ws"
}
```

**NEW Configuration** (Direct to GameServer):
```json
{
  "serverUrl": "http://localhost:8080",
  "wsUrl": "ws://localhost:8080/websocket-server/ws/game",
  "apiEndpoint": "http://localhost:8080"
}
```

#### 2.3. Update TypeScript/JavaScript Config

**File**: `src/config/ServerConfig.ts` hoặc tương tự

**OLD**:
```typescript
export class ServerConfig {
    static SERVER_URL = "http://localhost:7456";
    static WS_URL = "ws://localhost:7456/ws";
}
```

**NEW**:
```typescript
export class ServerConfig {
    // Gateway URL (REST API)
    static SERVER_URL = "http://localhost:8080";
    
    // WebSocket URL (Binary protocol)
    static WS_URL = "ws://localhost:8080/websocket-server/ws/game";
    
    // Session service (Authentication)
    static SESSION_URL = "http://localhost:8080/session-service";
    
    // API endpoints
    static API = {
        login: `${ServerConfig.SESSION_URL}/api/session/login`,
        logout: `${ServerConfig.SESSION_URL}/api/session/logout`,
        timesync: `${ServerConfig.SESSION_URL}/api/session/timesync`,
        
        // Economy services
        bag: "http://localhost:8080/bag-service/api/bag",
        wallet: "http://localhost:8080/wallet-service/api/wallet",
        item: "http://localhost:8080/item-service/api/items",
        shop: "http://localhost:8080/shop-service/api/shop"
    };
}
```

---

### STEP 3: Update WebSocket Connection Code

#### 3.1. Locate WebSocket Connection File

**Possible locations**:
```
D:\project\serverGame\client\LineR\assets\script\network\WebSocketManager.ts
D:\project\serverGame\client\LineR\assets\script\net\NetManager.ts
D:\project\serverGame\client\LineR\src\net\SocketManager.ts
```

#### 3.2. Update Connection Logic

**OLD Code** (Cocos server):
```typescript
class WebSocketManager {
    connect() {
        // Kết nối tới Cocos server
        this.ws = new WebSocket("ws://localhost:7456/ws");
        this.ws.binaryType = 'arraybuffer';
        
        this.ws.onopen = () => {
            console.log("Connected to Cocos server");
            // Cocos specific handshake
            this.sendCocosHandshake();
        };
    }
    
    sendCocosHandshake() {
        // Cocos specific protocol
        // ...
    }
}
```

**NEW Code** (Direct GameServer):
```typescript
class WebSocketManager {
    private token: string = "";
    
    /**
     * Connect to GameServer WebSocket
     * Must call after login to get token
     */
    connect(token: string) {
        this.token = token;
        
        // Connect với token authentication
        const wsUrl = `ws://localhost:8080/websocket-server/ws/game?token=${token}`;
        this.ws = new WebSocket(wsUrl);
        this.ws.binaryType = 'arraybuffer';
        
        this.ws.onopen = () => {
            console.log("Connected to GameServer");
            // Send login message (Big Endian protocol)
            this.sendLogin();
        };
        
        this.ws.onmessage = (event) => {
            this.handleMessage(event.data);
        };
        
        this.ws.onerror = (error) => {
            console.error("WebSocket error:", error);
        };
        
        this.ws.onclose = () => {
            console.log("Disconnected from GameServer");
            this.reconnect();
        };
    }
    
    /**
     * Send login message via WebSocket
     * Message ID: CS_LOGIN_REQ = 7056
     */
    sendLogin() {
        const msgId = 7056; // CS_LOGIN_REQ
        const payload = this.createLoginPayload();
        
        // Encode: [BodyLen(4)][MsgID(4)][Payload]
        const packet = this.encodePacket(msgId, payload);
        this.ws.send(packet);
    }
    
    /**
     * Encode packet: Big Endian format
     */
    encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
        const bodyLen = payload.length;
        const buffer = new ArrayBuffer(8 + bodyLen);
        const view = new DataView(buffer);
        
        // Big Endian
        view.setInt32(0, bodyLen, false);  // BodyLen
        view.setInt32(4, msgId, false);    // MsgID
        
        // Copy payload
        const uint8View = new Uint8Array(buffer);
        uint8View.set(payload, 8);
        
        return buffer;
    }
    
    /**
     * Decode incoming packet
     */
    handleMessage(data: ArrayBuffer) {
        const view = new DataView(data);
        
        // Read Big Endian
        const bodyLen = view.getInt32(0, false);
        const msgId = view.getInt32(4, false);
        
        // Extract payload
        const payload = new Uint8Array(data, 8, bodyLen);
        
        // Route to handler
        this.routeMessage(msgId, payload);
    }
    
    /**
     * Route message by ID
     */
    routeMessage(msgId: number, payload: Uint8Array) {
        switch(msgId) {
            case 7000: // SC_LOGIN_ACK
                this.handleLoginAck(payload);
                break;
            case 1003: // SC_HEARTBEAT_RESP
                this.handleHeartbeat(payload);
                break;
            case 1505: // SC_KNAPSACK_ALL_INFO
                this.handleBagInfo(payload);
                break;
            // ... other message handlers
            default:
                console.log(`Unknown message ID: ${msgId}`);
        }
    }
}
```

---

### STEP 4: Update Authentication Flow

#### 4.1. Login via REST API First

**NEW Flow**:
```typescript
class LoginManager {
    async login(username: string, password: string): Promise<string> {
        try {
            // 1. Call REST API để login
            const response = await fetch('http://localhost:8080/session-service/api/session/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            });
            
            if (!response.ok) {
                throw new Error('Login failed');
            }
            
            const data = await response.json();
            
            // 2. Lưu token
            const token = data.token;
            const userId = data.userId;
            const roleId = data.roleId;
            
            localStorage.setItem('jwt_token', token);
            localStorage.setItem('user_id', userId);
            localStorage.setItem('role_id', roleId);
            
            console.log('Login successful!', { userId, roleId });
            
            // 3. Connect WebSocket với token
            WebSocketManager.getInstance().connect(token);
            
            return token;
            
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    }
    
    logout() {
        // Call logout API
        fetch('http://localhost:8080/session-service/api/session/logout', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
            }
        });
        
        // Clear local storage
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('role_id');
        
        // Disconnect WebSocket
        WebSocketManager.getInstance().disconnect();
    }
}
```

---

### STEP 5: Update Message IDs

#### 5.1. Message ID Constants

**File**: `src/net/MessageIds.ts` (create if not exists)

```typescript
/**
 * Message IDs for GameServer protocol
 * Reference: docs/CLIENT_SERVER_CONNECTION.md
 */
export class MsgIds {
    // Login & Session
    static readonly CS_LOGIN_REQ = 7056;
    static readonly SC_LOGIN_ACK = 7000;
    static readonly SC_ACCOUNT_KEY_ERR = 7004;
    static readonly CS_LOGOUT_REQ = 7057;
    static readonly SC_LOGOUT_ACK = 7001;
    
    // Heartbeat
    static readonly CS_HEARTBEAT_REQ = 1053;
    static readonly SC_HEARTBEAT_RESP = 1003;
    
    // Time Sync
    static readonly CS_TIME_SYNC_REQ = 9050;
    static readonly SC_TIME_SYNC_RESP = 9000;
    
    // Role Info
    static readonly CS_ROLE_INFO_REQ = 1400;
    static readonly SC_ROLE_INFO_FULL = 1401;
    static readonly SC_ROLE_INFO_BRIEF = 1402;
    static readonly SC_ROLE_INFO_UPDATE = 1403;
    
    // Inventory/Bag
    static readonly CS_KNAPSACK_REQ = 1500;
    static readonly SC_KNAPSACK_ALL_INFO = 1505;
    static readonly SC_KNAPSACK_ADD = 1501;
    static readonly SC_KNAPSACK_UPDATE = 1502;
    static readonly SC_KNAPSACK_DEL = 1503;
    
    // Mail
    static readonly CS_MAIL_LIST_REQ = 9551;
    static readonly SC_MAIL_INFO = 9504;
    static readonly SC_MAIL_UPDATE = 9505;
    static readonly SC_MAIL_DEL = 9506;
    
    // Shop
    static readonly CS_SHOP_BUY_REQ = 2100;
    static readonly SC_SHOP_BUY_ACK = 2101;
    
    // ... Add more as needed
}
```

---

### STEP 6: Remove Cocos Dependencies

#### 6.1. Files to Check and Remove

**Remove or comment out**:
```
- Cocos server connection code
- Cocos specific protocol handlers
- Cocos proxy configurations
- Old server URLs (7456)
```

#### 6.2. Search and Replace

**In all client files**:
- Find: `localhost:7456` → Replace: `localhost:8080`
- Find: `ws://localhost:7456/ws` → Replace: `ws://localhost:8080/websocket-server/ws/game`
- Find: Cocos handshake code → Remove
- Find: Cocos protocol → Replace with GameServer protocol

---

### STEP 7: Testing Connection

#### 7.1. Test REST API

**Test login**:
```bash
curl -X POST http://localhost:8080/session-service/api/session/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'
```

**Expected response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "123",
  "roleId": "456"
}
```

#### 7.2. Test WebSocket Connection

**In browser console** (after login):
```javascript
// Get token from login
const token = localStorage.getItem('jwt_token');

// Connect
const ws = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws.binaryType = 'arraybuffer';

ws.onopen = () => {
    console.log('Connected!');
    
    // Send heartbeat
    const msgId = 1053; // CS_HEARTBEAT_REQ
    const buffer = new ArrayBuffer(8);
    const view = new DataView(buffer);
    view.setInt32(0, 0, false); // bodyLen = 0
    view.setInt32(4, msgId, false); // msgId
    ws.send(buffer);
};

ws.onmessage = (event) => {
    const view = new DataView(event.data);
    const bodyLen = view.getInt32(0, false);
    const msgId = view.getInt32(4, false);
    console.log('Received:', { msgId, bodyLen });
};
```

#### 7.3. Test in Laya Engine

**Start client**:
```bash
cd D:\project\serverGame\client\LineR
# Dùng Laya IDE hoặc
npm run dev
# hoặc
laya compile
```

**Check logs**:
- ✅ "Connected to GameServer"
- ✅ "Login successful"
- ✅ Receiving messages

---

## 🔍 TROUBLESHOOTING

### Issue 1: Connection Refused

**Problem**: `WebSocket connection failed`

**Solutions**:
1. Check Gateway is running on 8080
2. Check WebSocket Server is running on 8090
3. Check firewall settings
4. Verify URL: `ws://localhost:8080/websocket-server/ws/game`

### Issue 2: 401 Unauthorized

**Problem**: WebSocket rejected, 401 error

**Solutions**:
1. Login first via REST API
2. Get valid JWT token
3. Pass token in WebSocket URL: `?token=${token}`
4. Check token not expired

### Issue 3: Message Not Understood

**Problem**: Server doesn't respond or sends error

**Solutions**:
1. Verify Big Endian byte order
2. Check Message ID is correct (e.g., 7056 for login)
3. Verify packet format: [BodyLen(4)][MsgID(4)][Payload]
4. Check Protobuf encoding if using protobuf payload

### Issue 4: CORS Error

**Problem**: Browser blocks REST API calls

**Solutions**:
1. Gateway should have CORS enabled
2. Check gateway-service application.yml:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:7456"  # Add client origin
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders:
              - "*"
```

---

## 📊 COMPARISON: OLD vs NEW

### Cocos Server (OLD)

**Pros**:
- Simple proxy
- Easy to setup
- Development convenience

**Cons**:
- Extra hop (latency)
- Another service to maintain
- Protocol translation needed
- Limited control

### Direct Connection (NEW)

**Pros**:
- ✅ Lower latency (no proxy)
- ✅ Full control
- ✅ Native protocol
- ✅ Better security (JWT)
- ✅ Scalable architecture

**Cons**:
- Need to update client code
- More complex setup initially

---

## 🚀 PRODUCTION SETUP

### For Production Deployment

**Update URLs to production**:
```typescript
export class ServerConfig {
    private static ENV = process.env.NODE_ENV || 'development';
    
    private static configs = {
        development: {
            SERVER_URL: "http://localhost:8080",
            WS_URL: "ws://localhost:8080/websocket-server/ws/game"
        },
        production: {
            SERVER_URL: "https://api.yourgame.com",
            WS_URL: "wss://api.yourgame.com/websocket-server/ws/game"
        }
    };
    
    static get SERVER_URL() {
        return this.configs[this.ENV].SERVER_URL;
    }
    
    static get WS_URL() {
        return this.configs[this.ENV].WS_URL;
    }
}
```

**SSL/TLS**:
- HTTP → HTTPS
- WS → WSS
- Configure SSL certificates on Gateway

---

## 📝 CHECKLIST

### Server Setup ✅

- [ ] Eureka Server running (8761)
- [ ] Config Service running (8091)
- [ ] Gateway running (8080)
- [ ] WebSocket Server running (8090)
- [ ] Session Service running (8081)
- [ ] All services registered in Eureka

### Client Update ✅

- [ ] Updated server URL to localhost:8080
- [ ] Updated WebSocket URL to ws://localhost:8080/websocket-server/ws/game
- [ ] Removed Cocos server code
- [ ] Implemented JWT authentication
- [ ] Updated to Big Endian protocol
- [ ] Added Message ID constants
- [ ] Tested login flow
- [ ] Tested WebSocket connection
- [ ] Tested message sending/receiving

### Testing ✅

- [ ] REST API login works
- [ ] JWT token received
- [ ] WebSocket connects successfully
- [ ] Messages sent/received correctly
- [ ] Heartbeat working
- [ ] No errors in console

---

## 🎯 QUICK START SCRIPT

**Create file**: `start-gameserver.cmd`

```batch
@echo off
echo Starting GameServer services...
echo.

echo [1/5] Starting Eureka Server (8761)...
start "Eureka" cmd /k "cd D:\project\serverGame\GameServer\eureka-server && java -jar target\eureka-server-1.0.0.jar"
timeout /t 30

echo [2/5] Starting Config Service (8091)...
start "Config" cmd /k "cd D:\project\serverGame\GameServer\config-service && java -jar target\config-service-1.0.0.jar"
timeout /t 20

echo [3/5] Starting Gateway (8080)...
start "Gateway" cmd /k "cd D:\project\serverGame\GameServer\gateway-service && java -jar target\gateway-service-1.0.0.jar"
timeout /t 15

echo [4/5] Starting WebSocket Server (8090)...
start "WebSocket" cmd /k "cd D:\project\serverGame\GameServer\webSocket-server && java -jar target\webSocket-server-1.0.0.jar"
timeout /t 10

echo [5/5] Starting Session Service (8081)...
start "Session" cmd /k "cd D:\project\serverGame\GameServer\session-service && java -jar target\session-service-1.0.0.jar"

echo.
echo All services started!
echo Check http://localhost:8761 for Eureka dashboard
echo.
pause
```

---

## 📚 REFERENCES

### Documentation

- **Architecture**: `docs/CLIENT_SERVER_CONNECTION.md`
- **Integration Guide**: `docs/CLIENT_INTEGRATION_GUIDE.md`
- **Message IDs**: `webSocket-server/src/main/java/*/MsgIds.java`
- **Protocol Spec**: Section 3 in CLIENT_SERVER_CONNECTION.md

### Example Code

See `docs/CLIENT_INTEGRATION_GUIDE.md` for:
- Complete GameClient TypeScript class
- Packet encoding/decoding examples
- Message handling patterns

---

## 💡 TIPS

1. **Use Browser DevTools**: Network tab để debug WebSocket
2. **Enable Logging**: Thêm console.log trong message handlers
3. **Test Incrementally**: Test từng bước (login → connect → send → receive)
4. **Keep Token Fresh**: JWT có expiry, implement refresh logic
5. **Handle Reconnection**: Auto-reconnect khi connection lost

---

**🎉 SUCCESS!**

Sau khi setup xong, bạn sẽ có:
- ✅ Direct connection: Client → GameServer
- ✅ No Cocos server needed
- ✅ Native protocol
- ✅ Better performance
- ✅ Production-ready architecture

---

*Guide created*: 2025-11-09  
*For*: Laya Engine → Spring Boot GameServer  
*Status*: Production ready


# Client-Server Connection Architecture

**Last Updated**: 2025-11-09  
**Status**: Production Ready

## Overview

Game client kết nối với Spring backend thông qua 2 channels chính:
1. **RESTful API** - Cho các operations không real-time
2. **WebSocket** - Cho real-time game communication với binary protocol (Protobuf)

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Game Client                               │
│                    (LineR - Laya Engine)                        │
│                                                                  │
│  ┌──────────────────┐              ┌──────────────────┐        │
│  │   HTTP Client    │              │  WebSocket Client│        │
│  │  (RESTful API)   │              │  (Binary/Proto)  │        │
│  └────────┬─────────┘              └────────┬─────────┘        │
│           │                                  │                  │
└───────────┼──────────────────────────────────┼──────────────────┘
            │                                  │
            │ HTTP/HTTPS                       │ WS/WSS
            │ Port: 8080                       │ Port: 8080
            │                                  │
┌───────────▼──────────────────────────────────▼──────────────────┐
│                      API Gateway Service                         │
│                      (Spring Cloud Gateway)                      │
│                         Port: 8080                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Features:                                              │    │
│  │  • CORS Configuration                                   │    │
│  │  • Auth Filter (JWT validation)                        │    │
│  │  • Route Management                                     │    │
│  │  • WebSocket Proxy                                      │    │
│  │  • Service Discovery Integration                        │    │
│  └────────────────────────────────────────────────────────┘    │
└───────────┬──────────────────────────────────┬──────────────────┘
            │                                  │
            │ Load Balanced                    │ WS Proxy
            │ (Eureka Discovery)               │ lb:ws://
            │                                  │
    ┌───────┴────────┐                ┌───────▼────────┐
    │                │                │                 │
┌───▼────────┐  ┌───▼────────┐  ┌───▼──────────────┐ │
│ Session    │  │ User       │  │ WebSocket Server │ │
│ Service    │  │ Service    │  │   (Reactive)     │ │
│ Port: 8081 │  │ Port: 8082 │  │   Port: 8090     │ │
└────────────┘  └────────────┘  └───┬──────────────┘ │
                                    │                 │
┌───▼────────┐  ┌───▼────────┐     │  Feign Calls   │
│ Item       │  │ Wallet     │◄────┘                 │
│ Service    │  │ Service    │                        │
│ Port: 8220 │  │ Port: 8210 │                        │
└────────────┘  └────────────┘                        │
                                                       │
┌───▼────────┐  ┌───▼────────┐                       │
│ Bag        │  │ Shop       │                        │
│ Service    │  │ Service    │                        │
│ Port: 8230 │  │ Port: 8260 │                        │
└────────────┘  └────────────┘                        │
                                                       │
                    ┌──────────────────────────────────┘
                    │
            ┌───────▼────────┐
            │  Kafka Cluster │
            │  (Event Bus)   │
            └────────────────┘
```

---

## 1. RESTful API Connection

### 1.1 Client Configuration

**Base URL**: `http://localhost:8080` (Gateway)

**Endpoints Pattern**:
```
http://localhost:8080/{service-name}/{api-path}
```

**Example**:
```javascript
// Login endpoint
POST http://localhost:8080/session-service/api/session/login

// Get user info
GET http://localhost:8080/user-service/api/users/{userId}

// Item metadata
GET http://localhost:8080/item-service/api/items/{itemId}
```

### 1.2 Authentication Flow

```
┌──────────┐                ┌─────────┐                 ┌──────────┐
│  Client  │                │ Gateway │                 │ Session  │
│          │                │         │                 │ Service  │
└────┬─────┘                └────┬────┘                 └────┬─────┘
     │                           │                           │
     │  POST /session/login      │                           │
     │  {username, password}     │                           │
     ├──────────────────────────►│                           │
     │                           │  Forward request          │
     │                           ├──────────────────────────►│
     │                           │                           │
     │                           │  Validate credentials     │
     │                           │  Generate JWT token       │
     │                           │                           │
     │                           │◄──────────────────────────┤
     │                           │  {token, userId, roleId}  │
     │◄──────────────────────────┤                           │
     │  {token, userId, roleId}  │                           │
     │                           │                           │
     │  Store token in client    │                           │
     │                           │                           │
```

### 1.3 Authenticated Requests

**Headers Required**:
```http
Authorization: Bearer {jwt-token}
Content-Type: application/json
```

**Example Request**:
```javascript
// Client code (pseudo)
const token = localStorage.getItem('authToken');

fetch('http://localhost:8080/bag-service/api/bag/123', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
})
.then(response => response.json())
.then(data => console.log(data));
```

### 1.4 CORS Configuration

Gateway automatically handles CORS for allowed origins:

**Allowed Origins**:
- `http://localhost:7456`
- `http://127.0.0.1:7456`

**Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS  
**Allowed Headers**: All (*)  
**Credentials**: true

---

## 2. WebSocket Connection

### 2.1 Connection URL

```
ws://localhost:8080/websocket-server/ws/game
```

**With Authentication**:
```
ws://localhost:8080/websocket-server/ws/game?token={jwt-token}
```

**Or via Header**:
```javascript
// WebSocket with token in subprotocol
new WebSocket('ws://localhost:8080/websocket-server/ws/game', [token]);
```

### 2.2 Connection Flow

```
┌──────────┐          ┌─────────┐          ┌──────────┐          ┌──────────┐
│  Client  │          │ Gateway │          │WebSocket │          │ Session  │
│          │          │         │          │ Server   │          │ Service  │
└────┬─────┘          └────┬────┘          └────┬─────┘          └────┬─────┘
     │                     │                    │                     │
     │  WS Connect         │                    │                     │
     │  ?token=xxx         │                    │                     │
     ├────────────────────►│                    │                     │
     │                     │  Proxy WS          │                     │
     │                     │  lb:ws://          │                     │
     │                     ├───────────────────►│                     │
     │                     │                    │  Create             │
     │                     │                    │  PlayerSession      │
     │                     │                    │  Register in        │
     │                     │                    │  SessionRegistry    │
     │                     │                    │                     │
     │◄────────────────────┴────────────────────┤                     │
     │  WS Connection Established               │                     │
     │                                          │                     │
     │  CS_LOGIN_REQ (7056)                     │                     │
     │  Binary packet with credentials          │                     │
     ├─────────────────────────────────────────►│                     │
     │                                          │  Validate token     │
     │                                          ├────────────────────►│
     │                                          │                     │
     │                                          │◄────────────────────┤
     │                                          │  User info          │
     │◄─────────────────────────────────────────┤                     │
     │  SC_LOGIN_ACK (7000)                     │                     │
     │  Binary packet with user data            │                     │
     │                                          │                     │
```

### 2.3 Binary Protocol Format

**Packet Structure** (Big Endian):

```
┌─────────────┬─────────────┬──────────────────────┐
│  Body Len   │   Msg ID    │       Payload        │
│   4 bytes   │   4 bytes   │    Variable length   │
│   (int32)   │   (int32)   │    (Protobuf bytes)  │
└─────────────┴─────────────┴──────────────────────┘

Total: 8 + payload length bytes
```

**Example**:
```
Body Length: 100 bytes (includes MsgID + Payload)
Msg ID: 7056 (CS_LOGIN_REQ)
Payload: 96 bytes of Protobuf-encoded data
```

**Encoding** (Java - Server Side):
```java
public static byte[] encode(int msgId, byte[] payload) {
    int bodyLen = 4 + (payload == null ? 0 : payload.length);
    ByteBuffer buf = ByteBuffer.allocate(4 + bodyLen)
                               .order(ByteOrder.BIG_ENDIAN);
    buf.putInt(bodyLen);
    buf.putInt(msgId);
    if (payload != null) buf.put(payload);
    return buf.array();
}
```

**Decoding** (Java - Server Side):
```java
public static Decoded decode(byte[] frame) {
    if (frame == null || frame.length < 8) return null;
    var buf = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
    int bodyLen = buf.getInt();
    if (bodyLen != frame.length - 4) return null;
    int msgId = buf.getInt();
    byte[] payload = new byte[bodyLen - 4];
    buf.get(payload);
    return new Decoded(msgId, payload);
}
```

### 2.4 Message IDs Reference

**Login Messages**:
- `CS_LOGIN_REQ = 7056` - Client → Server login request
- `SC_LOGIN_ACK = 7000` - Server → Client login success
- `SC_ACCOUNT_KEY_ERR = 7004` - Server → Client login error

**Heartbeat Messages**:
- `CS_HEARTBEAT_REQ = 1053` - Client → Server heartbeat
- `SC_HEARTBEAT_RESP = 1003` - Server → Client heartbeat response

**Time Sync**:
- `CS_TIME_REQ = 9050` - Client requests server time
- `SC_TIME_ACK = 9000` - Server responds with time
- `SC_DISCONNECT_NOTICE = 9001` - Server notifies disconnect

**Role Information**:
- `SC_ROLE_INFO_ACK = 1400` - Role information
- `SC_ROLE_ATTR_LIST = 1401` - Role attributes
- `SC_ROLE_EXP_CHANGE = 1402` - Experience change
- `SC_ROLE_LEVEL_CHANGE = 1403` - Level change

**Inventory/Bag**:
- `CS_KNAPSACK_REQ = 1500` - Request bag info
- `SC_KNAPSACK_ALL_INFO = 1505` - All bag items
- `SC_KNAPSACK_SINGLE_INFO = 1506` - Single item info
- `SC_ITEM_NOT_ENOUGH_NOTICE = 1504` - Item insufficient

**Mail System**:
- `CS_MAIL_REQ = 9551` - Mail operation request
- `SC_MAIL_LIST_ACK = 9504` - Mail list response
- `SC_MAIL_DETAIL = 9505` - Mail detail
- `SC_FETCH_MAIL_ACK = 9506` - Fetch mail response

**Full list**: See `MsgIds.java` in webSocket-server

---

## 3. Client Implementation Guide

### 3.1 WebSocket Client (Laya Engine)

```typescript
// WebSocket connection manager
class WebSocketManager {
    private ws: WebSocket;
    private token: string;
    private messageHandlers: Map<number, Function>;

    constructor() {
        this.messageHandlers = new Map();
    }

    // Connect to server
    connect(token: string): Promise<void> {
        return new Promise((resolve, reject) => {
            this.token = token;
            const url = `ws://localhost:8080/websocket-server/ws/game?token=${token}`;
            
            this.ws = new WebSocket(url);
            this.ws.binaryType = 'arraybuffer';

            this.ws.onopen = () => {
                console.log('WebSocket connected');
                resolve();
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                reject(error);
            };

            this.ws.onmessage = (event) => {
                this.handleMessage(event.data);
            };

            this.ws.onclose = () => {
                console.log('WebSocket closed');
                this.reconnect();
            };
        });
    }

    // Send message to server
    sendMessage(msgId: number, payload: Uint8Array): void {
        const packet = this.encodePacket(msgId, payload);
        this.ws.send(packet);
    }

    // Encode packet (Big Endian)
    private encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
        const bodyLen = 4 + (payload ? payload.length : 0);
        const buffer = new ArrayBuffer(4 + bodyLen);
        const view = new DataView(buffer);
        
        view.setInt32(0, bodyLen, false); // Big Endian
        view.setInt32(4, msgId, false);   // Big Endian
        
        if (payload) {
            const uint8 = new Uint8Array(buffer, 8);
            uint8.set(payload);
        }
        
        return buffer;
    }

    // Decode packet
    private decodePacket(data: ArrayBuffer): {msgId: number, payload: Uint8Array} {
        const view = new DataView(data);
        const bodyLen = view.getInt32(0, false); // Big Endian
        const msgId = view.getInt32(4, false);   // Big Endian
        const payload = new Uint8Array(data, 8, bodyLen - 4);
        
        return { msgId, payload };
    }

    // Handle incoming message
    private handleMessage(data: ArrayBuffer): void {
        const { msgId, payload } = this.decodePacket(data);
        
        const handler = this.messageHandlers.get(msgId);
        if (handler) {
            handler(payload);
        } else {
            console.warn(`No handler for message ID: ${msgId}`);
        }
    }

    // Register message handler
    registerHandler(msgId: number, handler: Function): void {
        this.messageHandlers.set(msgId, handler);
    }

    // Reconnect logic
    private reconnect(): void {
        setTimeout(() => {
            console.log('Reconnecting...');
            this.connect(this.token);
        }, 3000);
    }
}
```

### 3.2 HTTP Client (RESTful API)

```typescript
// HTTP client for REST API
class HttpClient {
    private baseUrl: string = 'http://localhost:8080';
    private token: string;

    setToken(token: string): void {
        this.token = token;
    }

    // Generic GET request
    async get<T>(path: string): Promise<T> {
        const response = await fetch(`${this.baseUrl}${path}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    // Generic POST request
    async post<T>(path: string, body: any): Promise<T> {
        const response = await fetch(`${this.baseUrl}${path}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    // Login (no token required)
    async login(username: string, password: string): Promise<{token: string, userId: string}> {
        const response = await fetch(`${this.baseUrl}/session-service/api/session/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            throw new Error('Login failed');
        }

        const data = await response.json();
        this.token = data.token;
        return data;
    }

    // Get bag items
    async getBagItems(userId: string): Promise<any> {
        return this.get(`/bag-service/api/bag/${userId}`);
    }

    // Get user wallet
    async getWallet(userId: string): Promise<any> {
        return this.get(`/wallet-service/api/wallet/${userId}`);
    }
}
```

### 3.3 Complete Connection Example

```typescript
// Main game client initialization
class GameClient {
    private wsManager: WebSocketManager;
    private httpClient: HttpClient;

    constructor() {
        this.wsManager = new WebSocketManager();
        this.httpClient = new HttpClient();
        this.setupMessageHandlers();
    }

    // Initialize connection
    async initialize(username: string, password: string): Promise<void> {
        try {
            // 1. Login via HTTP to get token
            console.log('Logging in...');
            const loginData = await this.httpClient.login(username, password);
            console.log('Login successful, token:', loginData.token);

            // 2. Connect WebSocket with token
            console.log('Connecting WebSocket...');
            await this.wsManager.connect(loginData.token);

            // 3. Send login message via WebSocket
            console.log('Sending WS login...');
            const loginPayload = this.buildLoginPayload(loginData.userId);
            this.wsManager.sendMessage(7056, loginPayload); // CS_LOGIN_REQ

            // 4. Load initial data via HTTP
            console.log('Loading initial data...');
            const bagData = await this.httpClient.getBagItems(loginData.userId);
            const walletData = await this.httpClient.getWallet(loginData.userId);

            console.log('Client initialized successfully');
            console.log('Bag:', bagData);
            console.log('Wallet:', walletData);

        } catch (error) {
            console.error('Initialization failed:', error);
            throw error;
        }
    }

    // Setup message handlers
    private setupMessageHandlers(): void {
        // Login response
        this.wsManager.registerHandler(7000, (payload: Uint8Array) => {
            console.log('Login ACK received');
            // Decode protobuf payload
            // Update client state
        });

        // Heartbeat response
        this.wsManager.registerHandler(1003, (payload: Uint8Array) => {
            console.log('Heartbeat received');
        });

        // Role info
        this.wsManager.registerHandler(1400, (payload: Uint8Array) => {
            console.log('Role info received');
            // Update role information
        });

        // Bag update
        this.wsManager.registerHandler(1505, (payload: Uint8Array) => {
            console.log('Bag info received');
            // Update inventory UI
        });
    }

    // Build login payload (Protobuf)
    private buildLoginPayload(userId: string): Uint8Array {
        // Use protobuf.js to encode
        // const LoginReq = root.lookupType("PB_CSLoginToAccount");
        // const message = LoginReq.create({ userId: userId });
        // return LoginReq.encode(message).finish();
        
        // Placeholder
        return new Uint8Array(0);
    }

    // Send heartbeat
    startHeartbeat(): void {
        setInterval(() => {
            const payload = new Uint8Array(0);
            this.wsManager.sendMessage(1053, payload); // CS_HEARTBEAT_REQ
        }, 30000); // Every 30 seconds
    }
}

// Usage
const client = new GameClient();
client.initialize('player1', 'password123')
    .then(() => {
        client.startHeartbeat();
        console.log('Game client ready!');
    })
    .catch(error => {
        console.error('Failed to start client:', error);
    });
```

---

## 4. Server-Side Message Handling

### 4.1 WebSocket Message Flow

```
Client                WebSocketServer              MessageHandler           Business Service
  │                          │                           │                         │
  │  Binary Packet           │                           │                         │
  ├─────────────────────────►│                           │                         │
  │                          │  Decode Packet            │                         │
  │                          │  (PacketCodec)            │                         │
  │                          │                           │                         │
  │                          │  Find Handler             │                         │
  │                          │  (HandlerRegistry)        │                         │
  │                          ├──────────────────────────►│                         │
  │                          │                           │  Process Message        │
  │                          │                           │  Decode Protobuf        │
  │                          │                           │                         │
  │                          │                           │  Call Business Logic    │
  │                          │                           ├────────────────────────►│
  │                          │                           │                         │
  │                          │                           │  Execute & Return       │
  │                          │                           │◄────────────────────────┤
  │                          │                           │                         │
  │                          │  Encode Response          │                         │
  │                          │◄──────────────────────────┤                         │
  │                          │                           │                         │
  │  Binary Response         │                           │                         │
  │◄─────────────────────────┤                           │                         │
  │                          │                           │                         │
```

### 4.2 Handler Registration

Handlers are registered in `HandlerRegistry`:

```java
@Configuration
public class HandlerRegistry {
    private final Map<Integer, MessageHandler> handlers = new HashMap<>();

    // Register handler for specific message ID
    public void register(int msgId, MessageHandler handler) {
        handlers.put(msgId, handler);
    }

    // Find handler by message ID
    public Optional<MessageHandler> find(int msgId) {
        return Optional.ofNullable(handlers.get(msgId));
    }
}
```

**Example Handler**:
```java
@Component
public class LoginHandler implements MessageHandler {
    
    @PostConstruct
    public void init() {
        registry.register(MsgIds.CS_LOGIN_REQ, this);
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        // 1. Decode protobuf
        PB_CSLoginToAccount req = PB_CSLoginToAccount.parseFrom(payload);
        
        // 2. Validate token/credentials
        // 3. Call session service via Feign
        // 4. Update PlayerSession
        // 5. Encode response
        PB_SCLoginToAccount resp = buildLoginResponse(...);
        byte[] respPayload = resp.toByteArray();
        byte[] packet = PacketCodec.encode(MsgIds.SC_LOGIN_ACK, respPayload);
        
        // 6. Send back to client
        return Emitters.emit(ps, packet);
    }
}
```

---

## 5. Integration Points

### 5.1 Gateway ↔ WebSocket Server

**Gateway Configuration**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: game-ws
          uri: lb:ws://websocket-server  # Load-balanced WebSocket
          predicates:
            - Path=/websocket-server/**
          filters:
            - StripPrefix=1
```

**How it works**:
1. Client connects to `ws://localhost:8080/websocket-server/ws/game`
2. Gateway strips `/websocket-server` prefix
3. Proxies to `ws://websocket-server-instance/ws/game`
4. WebSocket Server handles connection

### 5.2 WebSocket Server ↔ Business Services

**Feign Clients** in WebSocket Server:

```java
@FeignClient(name = "wallet-service")
public interface WalletHttpClient {
    @PostMapping("/internal/wallet/transaction")
    TransactionResult transaction(@RequestBody TransactionRequest req);
}

@FeignClient(name = "bag-service")
public interface BagHttpClient {
    @PostMapping("/internal/bag/add")
    BagAddItemResp addItems(@RequestBody BagAddItemReq req);
}
```

**Usage in Handler**:
```java
public Mono<Void> handleBuyItem(PlayerSession ps, BuyItemRequest req) {
    // 1. Deduct currency via Wallet Service
    walletClient.transaction(new TransactionRequest(...));
    
    // 2. Add item via Bag Service
    bagClient.addItems(new BagAddItemReq(...));
    
    // 3. Send success response to client
    return Emitters.emit(ps, successPacket);
}
```

### 5.3 Service ↔ Service via Kafka

**Event Publishing** (e.g., Bag Service):
```java
// When bag changes, publish event
kafkaTemplate.send("gameh5.bag.changed", bagChangedEvent);
```

**Event Consuming** (e.g., WebSocket Server):
```java
@KafkaListener(topics = "gameh5.bag.changed")
public void onBagChanged(BagChangedEvent event) {
    // Find player session
    PlayerSession ps = sessionRegistry.findByUserId(event.getUserId());
    
    // Build notification packet
    byte[] packet = PacketCodec.encode(
        MsgIds.SC_KNAPSACK_SINGLE_INFO, 
        buildBagUpdatePayload(event)
    );
    
    // Push to client
    Emitters.emit(ps, packet);
}
```

---

## 6. Security & Authentication

### 6.1 JWT Token Validation

**Gateway Auth Filter**:
```java
// For HTTP requests
if (!isWhitelisted(path)) {
    String token = extractToken(request);
    validateTokenViaSessionService(token);
}

// For WebSocket
String token = extractTokenFromQuery(exchange) 
            || extractTokenFromHeader(exchange);
validateTokenViaSessionService(token);
```

**Whitelisted Paths** (No auth required):
- `/actuator/**`
- `/session-service/api/session/timesync`
- `/session-service/api/session/login`
- `/config-service/api/config/file/**`

### 6.2 Session Management

**PlayerSession** (Server-side):
```java
@Builder
public class PlayerSession {
    private WebSocketSession ws;        // WebSocket connection
    private String sessionId;           // Session ID from auth
    private Long userId;                // User ID
    private Long roleId;                // Role ID
    private Sinks.Many<byte[]> outbound; // Message queue
}
```

**Session Registry**:
```java
public interface PlayerSessionRegistry {
    void put(PlayerSession ps);
    void remove(PlayerSession ps);
    Optional<PlayerSession> findByUserId(Long userId);
    Optional<PlayerSession> findBySessionId(String sessionId);
}
```

---

## 7. Error Handling

### 7.1 Connection Errors

**Client-Side**:
```typescript
ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    // Show error message to user
    // Attempt reconnect
};

ws.onclose = (event) => {
    if (event.code !== 1000) { // Not normal closure
        console.error('Abnormal close:', event.code, event.reason);
        // Attempt reconnect with backoff
    }
};
```

**Server-Side**:
```java
Mono<Void> recv = session.receive()
    .onErrorResume(ex -> {
        log.warn("WS recv error: {}", ex.toString());
        // Clean up session
        sessionRegistry.remove(ps);
        return Mono.empty();
    });
```

### 7.2 Protocol Errors

**Invalid Packet**:
```java
if (decoded == null || decoded.msgId() <= 0) {
    log.warn("Invalid packet from session: {}", ps.getSessionId());
    // Send error response
    // Or close connection
}
```

**Unknown Message ID**:
```java
if (handler == null) {
    log.warn("Unknown MsgId {} from {}", msgId, ps.getUserId());
    // Ignore or send error
}
```

---

## 8. Performance Optimization

### 8.1 Connection Pooling

**HTTP Client** (Feign):
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  httpclient:
    enabled: true
    max-connections: 200
    max-connections-per-route: 50
```

### 8.2 WebSocket Frame Size

**Gateway**:
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        websocket:
          max-frame-payload-length: 1048576  # 1MB
```

### 8.3 Message Batching

**Client can batch small messages**:
```typescript
// Instead of sending many small packets
// Batch them and send as one
const batch = [msg1, msg2, msg3];
sendBatchPacket(batch);
```

---

## 9. Monitoring & Debugging

### 9.1 Gateway Metrics

```
http://localhost:8080/actuator/gateway/routes
http://localhost:8080/actuator/health
```

### 9.2 WebSocket Metrics

```java
@Component
public class WebSocketMetrics {
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    
    public void onConnect() {
        activeConnections.incrementAndGet();
    }
    
    public void onDisconnect() {
        activeConnections.decrementAndGet();
    }
    
    public void onMessage() {
        totalMessages.incrementAndGet();
    }
}
```

### 9.3 Logging

**Enable debug logging**:
```yaml
logging:
  level:
    com.southMillion.webSocket_server: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

---

## 10. Deployment Considerations

### 10.1 Production URLs

**Update client URLs**:
```typescript
// Development
const WS_URL = 'ws://localhost:8080/websocket-server/ws/game';
const API_URL = 'http://localhost:8080';

// Production
const WS_URL = 'wss://game-api.yourdomain.com/websocket-server/ws/game';
const API_URL = 'https://game-api.yourdomain.com';
```

### 10.2 SSL/TLS

**Use WSS for production**:
```
wss://game-api.yourdomain.com/websocket-server/ws/game
```

**Gateway with SSL**:
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

### 10.3 Load Balancing

**Multiple WebSocket Server Instances**:
- Use sticky sessions (session affinity)
- Or use Redis for shared session registry
- Kafka for broadcasting messages to all instances

---

## Summary

### Connection Flow Summary

1. **Client Login** via HTTP → Get JWT token
2. **WebSocket Connect** with token
3. **Binary Communication** with Protobuf
4. **Real-time Updates** via WebSocket push
5. **REST API** for non-real-time operations

### Key Components

- **Gateway**: Entry point, routing, auth
- **WebSocket Server**: Real-time communication hub
- **Business Services**: Domain logic (Bag, Wallet, Item, etc.)
- **Kafka**: Event bus for service communication
- **Eureka**: Service discovery

### Protocols

- **HTTP/REST**: JSON-based API calls
- **WebSocket**: Binary protocol with Protobuf messages
- **Message Format**: [BodyLen(4)][MsgID(4)][Payload(n)]

---

**End of Client-Server Connection Architecture**


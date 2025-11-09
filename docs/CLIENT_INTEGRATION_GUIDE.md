# Client Integration Quick Start

**For Game Client Developers**

## 1. Quick Connection Setup

### Prerequisites
- Game server running on `localhost:8080` (or your server URL)
- Valid user credentials

### Step 1: Login via HTTP

```typescript
// Login and get authentication token
const response = await fetch('http://localhost:8080/session-service/api/session/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        username: 'your_username',
        password: 'your_password'
    })
});

const { token, userId, roleId } = await response.json();
console.log('Login successful! Token:', token);
```

### Step 2: Connect WebSocket

```typescript
// Connect WebSocket with token
const ws = new WebSocket(`ws://localhost:8080/websocket-server/ws/game?token=${token}`);
ws.binaryType = 'arraybuffer';

ws.onopen = () => {
    console.log('WebSocket connected!');
    // Send login message (optional, depending on your flow)
    sendLoginMessage(ws, userId);
};

ws.onmessage = (event) => {
    const packet = decodePacket(event.data);
    handleMessage(packet.msgId, packet.payload);
};
```

### Step 3: Send/Receive Messages

```typescript
// Encode and send message
function sendMessage(ws, msgId, protobufPayload) {
    const packet = encodePacket(msgId, protobufPayload);
    ws.send(packet);
}

// Example: Send heartbeat
function sendHeartbeat(ws) {
    sendMessage(ws, 1053, new Uint8Array(0)); // CS_HEARTBEAT_REQ
}

// Handle incoming messages
function handleMessage(msgId, payload) {
    switch(msgId) {
        case 7000: // SC_LOGIN_ACK
            console.log('Login acknowledged');
            break;
        case 1003: // SC_HEARTBEAT_RESP
            console.log('Heartbeat received');
            break;
        case 1505: // SC_KNAPSACK_ALL_INFO
            updateInventory(payload);
            break;
        default:
            console.log('Unknown message:', msgId);
    }
}
```

---

## 2. Packet Format (Binary Protocol)

### Structure

```
┌──────────────┬──────────────┬────────────────┐
│  Body Length │   Message ID │    Payload     │
│   4 bytes    │   4 bytes    │   N bytes      │
│  (Big Endian)│ (Big Endian) │  (Protobuf)    │
└──────────────┴──────────────┴────────────────┘
```

### Encoding (TypeScript)

```typescript
function encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
    const bodyLen = 4 + (payload ? payload.length : 0);
    const buffer = new ArrayBuffer(4 + bodyLen);
    const view = new DataView(buffer);
    
    // Write body length (Big Endian)
    view.setInt32(0, bodyLen, false);
    
    // Write message ID (Big Endian)
    view.setInt32(4, msgId, false);
    
    // Write payload
    if (payload && payload.length > 0) {
        const uint8 = new Uint8Array(buffer, 8);
        uint8.set(payload);
    }
    
    return buffer;
}
```

### Decoding (TypeScript)

```typescript
function decodePacket(data: ArrayBuffer): {msgId: number, payload: Uint8Array} {
    const view = new DataView(data);
    
    // Read body length (Big Endian)
    const bodyLen = view.getInt32(0, false);
    
    // Read message ID (Big Endian)
    const msgId = view.getInt32(4, false);
    
    // Read payload
    const payload = new Uint8Array(data, 8, bodyLen - 4);
    
    return { msgId, payload };
}
```

---

## 3. Common Message IDs

### Login & Session
| Message | ID | Direction | Description |
|---------|-------|-----------|-------------|
| CS_LOGIN_REQ | 7056 | Client→Server | Login request |
| SC_LOGIN_ACK | 7000 | Server→Client | Login success |
| SC_ACCOUNT_KEY_ERR | 7004 | Server→Client | Login error |

### Heartbeat & Connection
| Message | ID | Direction | Description |
|---------|-------|-----------|-------------|
| CS_HEARTBEAT_REQ | 1053 | Client→Server | Heartbeat ping |
| SC_HEARTBEAT_RESP | 1003 | Server→Client | Heartbeat pong |
| CS_TIME_REQ | 9050 | Client→Server | Request server time |
| SC_TIME_ACK | 9000 | Server→Client | Server time response |
| SC_DISCONNECT_NOTICE | 9001 | Server→Client | Disconnect notification |

### Player Role
| Message | ID | Direction | Description |
|---------|-------|-----------|-------------|
| SC_ROLE_INFO_ACK | 1400 | Server→Client | Role information |
| SC_ROLE_ATTR_LIST | 1401 | Server→Client | Role attributes |
| SC_ROLE_EXP_CHANGE | 1402 | Server→Client | Experience changed |
| SC_ROLE_LEVEL_CHANGE | 1403 | Server→Client | Level changed |

### Inventory/Bag
| Message | ID | Direction | Description |
|---------|-------|-----------|-------------|
| CS_KNAPSACK_REQ | 1500 | Client→Server | Request bag info |
| SC_KNAPSACK_ALL_INFO | 1505 | Server→Client | All bag items |
| SC_KNAPSACK_SINGLE_INFO | 1506 | Server→Client | Single item update |
| SC_ITEM_NOT_ENOUGH_NOTICE | 1504 | Server→Client | Item insufficient |

### Mail
| Message | ID | Direction | Description |
|---------|-------|-----------|-------------|
| CS_MAIL_REQ | 9551 | Client→Server | Mail operation |
| SC_MAIL_LIST_ACK | 9504 | Server→Client | Mail list |
| SC_MAIL_DETAIL | 9505 | Server→Client | Mail detail |
| SC_FETCH_MAIL_ACK | 9506 | Server→Client | Fetch mail reward |

---

## 4. REST API Endpoints

### Base URL
```
http://localhost:8080
```

### Authentication
All API calls (except login) require JWT token in header:
```
Authorization: Bearer {your-jwt-token}
```

### Session Service
```
POST /session-service/api/session/login
    Body: { username, password }
    Response: { token, userId, roleId }

POST /session-service/api/session/logout
    Headers: Authorization: Bearer {token}
    
GET /session-service/api/session/timesync
    Response: { serverTime }
```

### User Service
```
GET /user-service/api/users/{userId}
    Response: User details

PUT /user-service/api/users/{userId}
    Body: User update data
```

### Bag Service
```
GET /bag-service/api/bag/{userId}
    Response: All items in bag

POST /bag-service/api/bag/add
    Body: { userId, roleId, items[], source }
    
POST /bag-service/api/bag/consume
    Body: { userId, roleId, itemId, amount }
```

### Wallet Service
```
GET /wallet-service/api/wallet/balance/{userId}
    Response: { userId, balances[] }

POST /wallet-service/internal/wallet/transaction
    Body: { roleId, changes[], idemKey, reason }
```

### Item Service
```
GET /item-service/api/items/{itemId}
    Response: Item metadata

GET /item-service/api/items/batch?ids={id1,id2,id3}
    Response: Array of item metadata
```

### Shop Service
```
GET /shop-service/api/shop/catalog
    Response: Available shop items

POST /shop-service/api/shop/purchase
    Body: { userId, itemId, quantity }
```

---

## 5. Complete Integration Example

```typescript
class GameClient {
    private ws: WebSocket;
    private token: string;
    private userId: string;
    private apiBaseUrl = 'http://localhost:8080';

    // Initialize client
    async initialize(username: string, password: string) {
        try {
            // 1. Login
            await this.login(username, password);
            
            // 2. Connect WebSocket
            await this.connectWebSocket();
            
            // 3. Load initial data
            await this.loadInitialData();
            
            // 4. Start heartbeat
            this.startHeartbeat();
            
            console.log('Client initialized successfully');
        } catch (error) {
            console.error('Initialization failed:', error);
            throw error;
        }
    }

    // Step 1: Login
    private async login(username: string, password: string) {
        const response = await fetch(`${this.apiBaseUrl}/session-service/api/session/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            throw new Error('Login failed');
        }

        const data = await response.json();
        this.token = data.token;
        this.userId = data.userId;
        
        console.log('Login successful');
    }

    // Step 2: Connect WebSocket
    private connectWebSocket(): Promise<void> {
        return new Promise((resolve, reject) => {
            const wsUrl = `ws://localhost:8080/websocket-server/ws/game?token=${this.token}`;
            this.ws = new WebSocket(wsUrl);
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
                this.handleWebSocketMessage(event.data);
            };

            this.ws.onclose = () => {
                console.log('WebSocket closed');
                this.reconnect();
            };
        });
    }

    // Step 3: Load initial data
    private async loadInitialData() {
        // Get bag items
        const bagResponse = await fetch(
            `${this.apiBaseUrl}/bag-service/api/bag/${this.userId}`,
            {
                headers: { 'Authorization': `Bearer ${this.token}` }
            }
        );
        const bagData = await bagResponse.json();
        console.log('Bag loaded:', bagData);

        // Get wallet
        const walletResponse = await fetch(
            `${this.apiBaseUrl}/wallet-service/api/wallet/balance/${this.userId}`,
            {
                headers: { 'Authorization': `Bearer ${this.token}` }
            }
        );
        const walletData = await walletResponse.json();
        console.log('Wallet loaded:', walletData);
    }

    // Step 4: Heartbeat
    private startHeartbeat() {
        setInterval(() => {
            this.sendMessage(1053, new Uint8Array(0)); // CS_HEARTBEAT_REQ
        }, 30000); // Every 30 seconds
    }

    // Send WebSocket message
    private sendMessage(msgId: number, payload: Uint8Array) {
        const packet = this.encodePacket(msgId, payload);
        this.ws.send(packet);
    }

    // Handle incoming WebSocket message
    private handleWebSocketMessage(data: ArrayBuffer) {
        const { msgId, payload } = this.decodePacket(data);
        
        switch(msgId) {
            case 7000: // SC_LOGIN_ACK
                console.log('Server login acknowledged');
                break;
            case 1003: // SC_HEARTBEAT_RESP
                console.log('Heartbeat OK');
                break;
            case 1400: // SC_ROLE_INFO_ACK
                this.handleRoleInfo(payload);
                break;
            case 1505: // SC_KNAPSACK_ALL_INFO
                this.handleBagUpdate(payload);
                break;
            default:
                console.log('Unhandled message:', msgId);
        }
    }

    // Encode packet (Big Endian)
    private encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
        const bodyLen = 4 + (payload ? payload.length : 0);
        const buffer = new ArrayBuffer(4 + bodyLen);
        const view = new DataView(buffer);
        
        view.setInt32(0, bodyLen, false); // Big Endian
        view.setInt32(4, msgId, false);   // Big Endian
        
        if (payload && payload.length > 0) {
            new Uint8Array(buffer, 8).set(payload);
        }
        
        return buffer;
    }

    // Decode packet
    private decodePacket(data: ArrayBuffer): {msgId: number, payload: Uint8Array} {
        const view = new DataView(data);
        const bodyLen = view.getInt32(0, false);
        const msgId = view.getInt32(4, false);
        const payload = new Uint8Array(data, 8, bodyLen - 4);
        return { msgId, payload };
    }

    // Reconnect on disconnect
    private reconnect() {
        setTimeout(() => {
            console.log('Reconnecting...');
            this.connectWebSocket();
        }, 3000);
    }

    // Handle role info
    private handleRoleInfo(payload: Uint8Array) {
        // Decode protobuf payload
        // Update UI with role information
        console.log('Role info received');
    }

    // Handle bag update
    private handleBagUpdate(payload: Uint8Array) {
        // Decode protobuf payload
        // Update inventory UI
        console.log('Bag updated');
    }

    // API: Purchase item
    async purchaseItem(itemId: number, quantity: number) {
        const response = await fetch(
            `${this.apiBaseUrl}/shop-service/api/shop/purchase`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: this.userId,
                    itemId,
                    quantity
                })
            }
        );

        if (!response.ok) {
            throw new Error('Purchase failed');
        }

        return await response.json();
    }
}

// Usage
const client = new GameClient();
client.initialize('player1', 'password123')
    .then(() => {
        console.log('Game client ready!');
        
        // Example: Purchase item
        client.purchaseItem(1001, 5)
            .then(() => console.log('Item purchased'))
            .catch(err => console.error('Purchase error:', err));
    })
    .catch(error => {
        console.error('Failed to initialize:', error);
    });
```

---

## 6. Troubleshooting

### WebSocket Connection Fails

**Problem**: Cannot connect to WebSocket  
**Solution**:
1. Check if server is running: `http://localhost:8080/actuator/health`
2. Verify token is valid
3. Check CORS settings in gateway
4. Ensure WebSocket URL is correct

### Messages Not Received

**Problem**: Sent message but no response  
**Solution**:
1. Verify message ID is correct
2. Check payload is valid Protobuf
3. Check server logs for errors
4. Ensure handler is registered for message ID

### Authentication Errors

**Problem**: 401 Unauthorized  
**Solution**:
1. Login again to get fresh token
2. Ensure token is in `Authorization` header
3. Check token expiration
4. Verify endpoint is not whitelisted incorrectly

### Packet Encoding Issues

**Problem**: Server doesn't understand packets  
**Solution**:
1. Verify Big Endian byte order
2. Check body length calculation
3. Ensure payload is valid Protobuf bytes
4. Test with simple message (heartbeat) first

---

## 7. Best Practices

### Connection Management
- ✅ Always reconnect on disconnect
- ✅ Implement exponential backoff for reconnects
- ✅ Send heartbeat every 30 seconds
- ✅ Handle connection state properly

### Error Handling
- ✅ Wrap all API calls in try-catch
- ✅ Show user-friendly error messages
- ✅ Log errors for debugging
- ✅ Implement retry logic for transient failures

### Performance
- ✅ Reuse WebSocket connection
- ✅ Batch multiple API calls when possible
- ✅ Cache static data (item metadata)
- ✅ Use WebSocket for real-time data, HTTP for queries

### Security
- ✅ Store token securely
- ✅ Don't log sensitive data
- ✅ Use HTTPS/WSS in production
- ✅ Validate all server responses

---

## 8. Testing

### Test Login
```bash
curl -X POST http://localhost:8080/session-service/api/session/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'
```

### Test API with Token
```bash
curl http://localhost:8080/bag-service/api/bag/123 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Test WebSocket (wscat)
```bash
npm install -g wscat
wscat -c "ws://localhost:8080/websocket-server/ws/game?token=YOUR_TOKEN"
```

---

## 9. Production Deployment

### Update URLs
```typescript
// config.ts
export const config = {
    development: {
        apiUrl: 'http://localhost:8080',
        wsUrl: 'ws://localhost:8080/websocket-server/ws/game'
    },
    production: {
        apiUrl: 'https://api.yourgame.com',
        wsUrl: 'wss://api.yourgame.com/websocket-server/ws/game'
    }
};

const env = process.env.NODE_ENV || 'development';
export const API_URL = config[env].apiUrl;
export const WS_URL = config[env].wsUrl;
```

### Enable SSL
- Use WSS instead of WS
- Use HTTPS instead of HTTP
- Configure proper SSL certificates on server

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────┐
│                   Quick Reference                        │
├─────────────────────────────────────────────────────────┤
│ Gateway URL:     http://localhost:8080                  │
│ WebSocket:       ws://localhost:8080/websocket-server/  │
│                  ws/game?token={token}                   │
│                                                          │
│ Login:           POST /session-service/api/session/login│
│ Get Bag:         GET /bag-service/api/bag/{userId}      │
│ Get Wallet:      GET /wallet-service/api/wallet/        │
│                  balance/{userId}                        │
│                                                          │
│ Heartbeat:       MsgID 1053 → 1003                      │
│ Login WS:        MsgID 7056 → 7000                      │
│ Bag Update:      MsgID 1500 → 1505                      │
│                                                          │
│ Packet Format:   [BodyLen(4)][MsgID(4)][Payload(N)]     │
│ Byte Order:      Big Endian                             │
│ Payload:         Protobuf binary                        │
└─────────────────────────────────────────────────────────┘
```

---

**For more details, see**: [CLIENT_SERVER_CONNECTION.md](./CLIENT_SERVER_CONNECTION.md)


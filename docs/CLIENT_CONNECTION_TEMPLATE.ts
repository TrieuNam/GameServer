/**
 * Client Connection Example for GameServer
 * Thay thế Cocos server - Connect trực tiếp
 *
 * File này là TEMPLATE - Copy vào project client của bạn và customize
 * Location suggestion: D:\project\serverGame\client\LineR\assets\script\network\
 */

// ============================================
// 1. SERVER CONFIGURATION
// ============================================

class ServerConfig {
    // QUAN TRỌNG: Đổi từ Cocos server (7456) sang GameServer (8080)

    // OLD: static SERVER_URL = "http://localhost:7456";
    // NEW:
    static SERVER_URL = "http://localhost:8080";

    // OLD: static WS_URL = "ws://localhost:7456/ws";
    // NEW:
    static WS_URL = "ws://localhost:8080/websocket-server/ws/game";

    // API Endpoints
    static API = {
        // Authentication
        login: "http://localhost:8080/session-service/api/session/login",
        logout: "http://localhost:8080/session-service/api/session/logout",
        timesync: "http://localhost:8080/session-service/api/session/timesync",

        // Economy services
        bag: "http://localhost:8080/bag-service/api/bag",
        wallet: "http://localhost:8080/wallet-service/api/wallet/balance",
        item: "http://localhost:8080/item-service/api/items",
        shop: "http://localhost:8080/shop-service/api/shop"
    };
}

// ============================================
// 2. MESSAGE IDS (From GameServer)
// ============================================

class MsgIds {
    // Login & Session
    static CS_LOGIN_REQ = 7056;
    static SC_LOGIN_ACK = 7000;
    static SC_ACCOUNT_KEY_ERR = 7004;

    // Heartbeat
    static CS_HEARTBEAT_REQ = 1053;
    static SC_HEARTBEAT_RESP = 1003;

    // Time Sync
    static CS_TIME_SYNC_REQ = 9050;
    static SC_TIME_SYNC_RESP = 9000;

    // Role Info
    static CS_ROLE_INFO_REQ = 1400;
    static SC_ROLE_INFO_FULL = 1401;

    // Inventory
    static CS_KNAPSACK_REQ = 1500;
    static SC_KNAPSACK_ALL_INFO = 1505;
    static SC_KNAPSACK_ADD = 1501;

    // Mail
    static CS_MAIL_LIST_REQ = 9551;
    static SC_MAIL_INFO = 9504;
}

// ============================================
// 3. AUTHENTICATION MANAGER
// ============================================

class AuthManager {
    private static instance: AuthManager;
    private token: string = "";
    private userId: string = "";
    private roleId: string = "";

    static getInstance(): AuthManager {
        if (!this.instance) {
            this.instance = new AuthManager();
        }
        return this.instance;
    }

    /**
     * BƯỚC 1: Login qua REST API (thay vì Cocos handshake)
     */
    async login(username: string, password: string): Promise<boolean> {
        try {
            console.log(`[Auth] Logging in as ${username}...`);

            // Gọi Spring Boot Session Service
            const response = await fetch(ServerConfig.API.login, {
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
                const error = await response.text();
                console.error('[Auth] Login failed:', error);
                return false;
            }

            const data = await response.json();

            // Lưu credentials
            this.token = data.token;
            this.userId = data.userId;
            this.roleId = data.roleId;

            // Persist to localStorage
            localStorage.setItem('jwt_token', this.token);
            localStorage.setItem('user_id', this.userId);
            localStorage.setItem('role_id', this.roleId);

            console.log('[Auth] Login successful!', {
                userId: this.userId,
                roleId: this.roleId,
                tokenLength: this.token.length
            });

            return true;

        } catch (error) {
            console.error('[Auth] Login error:', error);
            return false;
        }
    }

    async logout(): Promise<void> {
        try {
            await fetch(ServerConfig.API.logout, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            });
        } catch (error) {
            console.error('[Auth] Logout error:', error);
        } finally {
            this.clearCredentials();
        }
    }

    clearCredentials(): void {
        this.token = "";
        this.userId = "";
        this.roleId = "";
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_id');
        localStorage.removeItem('role_id');
    }

    getToken(): string {
        return this.token || localStorage.getItem('jwt_token') || "";
    }

    getUserId(): string {
        return this.userId || localStorage.getItem('user_id') || "";
    }

    getRoleId(): string {
        return this.roleId || localStorage.getItem('role_id') || "";
    }

    isLoggedIn(): boolean {
        return this.getToken().length > 0;
    }
}

// ============================================
// 4. WEBSOCKET MANAGER
// ============================================

class WebSocketManager {
    private static instance: WebSocketManager;
    private ws: WebSocket | null = null;
    private isConnected: boolean = false;
    private heartbeatTimer: any = null;
    private reconnectTimer: any = null;
    private messageHandlers: Map<number, Function> = new Map();

    static getInstance(): WebSocketManager {
        if (!this.instance) {
            this.instance = new WebSocketManager();
        }
        return this.instance;
    }

    /**
     * BƯỚC 2: Connect WebSocket (sau khi login thành công)
     * QUAN TRỌNG: Phải có JWT token từ login
     */
    connect(): void {
        const token = AuthManager.getInstance().getToken();

        if (!token) {
            console.error('[WS] Cannot connect: No JWT token. Login first!');
            return;
        }

        // URL mới - kèm token authentication
        const wsUrl = `${ServerConfig.WS_URL}?token=${token}`;

        console.log('[WS] Connecting to GameServer...', wsUrl);

        this.ws = new WebSocket(wsUrl);
        this.ws.binaryType = 'arraybuffer'; // QUAN TRỌNG: Binary protocol

        this.ws.onopen = () => {
            this.onConnected();
        };

        this.ws.onmessage = (event) => {
            this.onMessage(event.data);
        };

        this.ws.onerror = (error) => {
            console.error('[WS] Error:', error);
        };

        this.ws.onclose = () => {
            this.onDisconnected();
        };
    }

    private onConnected(): void {
        console.log('[WS] Connected to GameServer!');
        this.isConnected = true;

        // KHÔNG CÒN COCOS HANDSHAKE!
        // Thay vào đó, send login message via WebSocket
        this.sendLogin();

        // Start heartbeat
        this.startHeartbeat();
    }

    private onDisconnected(): void {
        console.log('[WS] Disconnected from GameServer');
        this.isConnected = false;
        this.stopHeartbeat();

        // Auto reconnect after 3 seconds
        this.reconnectTimer = setTimeout(() => {
            console.log('[WS] Attempting to reconnect...');
            this.connect();
        }, 3000);
    }

    /**
     * Gửi WebSocket login message
     * Message ID: CS_LOGIN_REQ = 7056
     */
    private sendLogin(): void {
        console.log('[WS] Sending login message...');

        // TODO: Create protobuf LoginReq payload
        // For now, send empty payload
        const payload = new Uint8Array(0);

        this.sendMessage(MsgIds.CS_LOGIN_REQ, payload);
    }

    /**
     * ENCODE PACKET: Big Endian format
     * Format: [BodyLen(4 bytes)][MsgID(4 bytes)][Payload(N bytes)]
     */
    private encodePacket(msgId: number, payload: Uint8Array): ArrayBuffer {
        const bodyLen = payload.length;
        const totalLen = 8 + bodyLen;

        const buffer = new ArrayBuffer(totalLen);
        const view = new DataView(buffer);

        // QUAN TRỌNG: Big Endian (false parameter)
        view.setInt32(0, bodyLen, false);  // BodyLen
        view.setInt32(4, msgId, false);    // MsgID

        // Copy payload
        if (bodyLen > 0) {
            const uint8View = new Uint8Array(buffer);
            uint8View.set(payload, 8);
        }

        return buffer;
    }

    /**
     * DECODE PACKET: Big Endian format
     */
    private onMessage(data: ArrayBuffer): void {
        if (data.byteLength < 8) {
            console.error('[WS] Invalid packet: too short');
            return;
        }

        const view = new DataView(data);

        // Read Big Endian
        const bodyLen = view.getInt32(0, false);
        const msgId = view.getInt32(4, false);

        // Extract payload
        const payload = new Uint8Array(data, 8, bodyLen);

        console.log(`[WS] Received message: ID=${msgId}, Len=${bodyLen}`);

        // Route to handler
        this.routeMessage(msgId, payload);
    }

    /**
     * Route message to registered handler
     */
    private routeMessage(msgId: number, payload: Uint8Array): void {
        const handler = this.messageHandlers.get(msgId);

        if (handler) {
            try {
                handler(payload);
            } catch (error) {
                console.error(`[WS] Handler error for msgId ${msgId}:`, error);
            }
        } else {
            // Default handlers
            switch (msgId) {
                case MsgIds.SC_LOGIN_ACK:
                    this.handleLoginAck(payload);
                    break;
                case MsgIds.SC_HEARTBEAT_RESP:
                    this.handleHeartbeat(payload);
                    break;
                default:
                    console.warn(`[WS] No handler for message ${msgId}`);
            }
        }
    }

    /**
     * Register message handler
     */
    registerHandler(msgId: number, handler: Function): void {
        this.messageHandlers.set(msgId, handler);
    }

    /**
     * Send message
     */
    sendMessage(msgId: number, payload: Uint8Array): void {
        if (!this.isConnected || !this.ws) {
            console.error('[WS] Cannot send: not connected');
            return;
        }

        const packet = this.encodePacket(msgId, payload);
        this.ws.send(packet);

        console.log(`[WS] Sent message: ID=${msgId}, Len=${payload.length}`);
    }

    /**
     * Heartbeat mechanism
     */
    private startHeartbeat(): void {
        this.heartbeatTimer = setInterval(() => {
            const payload = new Uint8Array(0);
            this.sendMessage(MsgIds.CS_HEARTBEAT_REQ, payload);
        }, 30000); // Every 30 seconds
    }

    private stopHeartbeat(): void {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }

    private handleLoginAck(payload: Uint8Array): void {
        console.log('[WS] Login ACK received!');
        // TODO: Decode protobuf LoginAck
    }

    private handleHeartbeat(payload: Uint8Array): void {
        console.log('[WS] Heartbeat response');
    }

    disconnect(): void {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }
        this.stopHeartbeat();

        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        this.isConnected = false;
    }
}

// ============================================
// 5. GAME CLIENT - MAIN ENTRY POINT
// ============================================

class GameClient {
    private static instance: GameClient;

    static getInstance(): GameClient {
        if (!this.instance) {
            this.instance = new GameClient();
        }
        return this.instance;
    }

    /**
     * COMPLETE LOGIN FLOW
     * Thay thế Cocos connection flow
     */
    async login(username: string, password: string): Promise<boolean> {
        try {
            console.log('========================================');
            console.log('GameClient: Starting login flow');
            console.log('========================================');

            // Step 1: REST API Login
            console.log('Step 1: Authenticating via REST API...');
            const success = await AuthManager.getInstance().login(username, password);

            if (!success) {
                console.error('Login failed at authentication step');
                return false;
            }

            console.log('Step 1: ✅ Authentication successful');

            // Step 2: WebSocket Connection
            console.log('Step 2: Connecting WebSocket...');
            WebSocketManager.getInstance().connect();

            // Wait for connection
            await this.waitForConnection(5000);

            console.log('Step 2: ✅ WebSocket connected');

            console.log('========================================');
            console.log('Login complete! Ready to play');
            console.log('========================================');

            return true;

        } catch (error) {
            console.error('Login error:', error);
            return false;
        }
    }

    private waitForConnection(timeout: number): Promise<void> {
        return new Promise((resolve, reject) => {
            const startTime = Date.now();
            const checkInterval = setInterval(() => {
                if (WebSocketManager.getInstance()['isConnected']) {
                    clearInterval(checkInterval);
                    resolve();
                } else if (Date.now() - startTime > timeout) {
                    clearInterval(checkInterval);
                    reject(new Error('Connection timeout'));
                }
            }, 100);
        });
    }

    logout(): void {
        console.log('Logging out...');
        WebSocketManager.getInstance().disconnect();
        AuthManager.getInstance().logout();
    }
}

// ============================================
// 6. USAGE EXAMPLE
// ============================================

/*
// In your game initialization code:

// Initialize
const client = GameClient.getInstance();

// Login
async function startGame() {
    const username = "testuser";
    const password = "testpass";

    const success = await client.login(username, password);

    if (success) {
        console.log("Game started!");

        // Now you can send game messages
        const ws = WebSocketManager.getInstance();

        // Request bag info
        ws.sendMessage(MsgIds.CS_KNAPSACK_REQ, new Uint8Array(0));

        // Register handler for bag info
        ws.registerHandler(MsgIds.SC_KNAPSACK_ALL_INFO, (payload) => {
            console.log("Received bag info!");
            // TODO: Decode protobuf and update UI
        });

    } else {
        console.error("Failed to start game");
    }
}

// Logout
function quitGame() {
    client.logout();
}

// Call startGame when user clicks "Play" button
startGame();
*/

// ============================================
// 7. MIGRATION CHECKLIST
// ============================================

/*
✅ THINGS TO REMOVE/UPDATE IN YOUR CLIENT:

1. Remove Cocos server connection code
   - Old: const ws = new WebSocket("ws://localhost:7456/ws");
   - New: Use ServerConfig.WS_URL

2. Remove Cocos handshake
   - Old: sendCocosHandshake()
   - New: sendLogin() with GameServer protocol

3. Update all server URLs
   - Find: "localhost:7456"
   - Replace: "localhost:8080"

4. Update protocol
   - Old: Cocos protocol
   - New: Big Endian binary protocol

5. Add JWT authentication
   - Login via REST first
   - Pass token in WebSocket URL

6. Update Message IDs
   - Use MsgIds constants from GameServer
   - Reference: docs/CLIENT_SERVER_CONNECTION.md

7. Update binary encoding
   - Use Big Endian (DataView second param = false)
   - Format: [BodyLen(4)][MsgID(4)][Payload]

✅ TEST CHECKLIST:

- [ ] GameServer services running (use start-gameserver-for-client.cmd)
- [ ] REST API login working
- [ ] JWT token received
- [ ] WebSocket connection successful
- [ ] Login message sent
- [ ] Login ACK received
- [ ] Heartbeat working
- [ ] Game messages sending/receiving
- [ ] No Cocos server needed!

🎉 SUCCESS CRITERIA:

Your client should now connect directly to GameServer without any Cocos server!
*/

// ============================================
// END OF TEMPLATE
// ============================================


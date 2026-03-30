# 🌐 Frontend-Backend Communication Flow

> **Tài liệu**: Luồng hoạt động từ Frontend (Cocos Creator) đến Backend (Java Microservices)  
> **Ngày**: 2026-01-19  
> **Mục đích**: Hiểu rõ từng bước xử lý message từ client đến server và ngược lại

---

## 📋 MỤC LỤC

1. [Kiến Trúc Tổng Quan](#kiến-trúc-tổng-quan)
2. [Flow Chi Tiết - Ví Dụ Open Box](#flow-chi-tiết---ví-dụ-open-box)
3. [Các Component Chính](#các-component-chính)
4. [Protocol Buffers](#protocol-buffers)
5. [Message ID Mapping](#message-id-mapping)
6. [Error Handling & Retry](#error-handling--retry)
7. [Best Practices](#best-practices)

---

## 🏗️ KIẾN TRÚC TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND LAYER                              │
│                    (Cocos Creator + TypeScript)                     │
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐           │
│  │  BoxView.ts  │──▶│ BoxCtrl.ts   │──▶│  BoxData.ts  │           │
│  │   (UI/UX)    │   │ (Controller) │   │    (Model)   │           │
│  └──────────────┘   └──────┬───────┘   └──────────────┘           │
│                            │                                        │
│                            ▼                                        │
│                     ┌──────────────┐                                │
│                     │ BaseCtrl.ts  │                                │
│                     │ SendToServer │                                │
│                     └──────┬───────┘                                │
│                            │                                        │
│                            ▼                                        │
│  ┌────────────────────────────────────────────────────┐            │
│  │          NetworkMgr / NetNode.ts                    │            │
│  │  • Serialize Protocol Buffer (msgId + payload)     │            │
│  │  • Add 8-byte header (4B bodyLen + 4B msgId)       │            │
│  │  • Send via WebSocket                               │            │
│  └────────────────────┬───────────────────────────────┘            │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         │ WebSocket Binary Message
                         │ [4B Length][4B MsgId][Protobuf Payload]
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      GATEWAY LAYER (Port 8094)                      │
│                      WebSocket-Server (Spring)                      │
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         WsGatewayHandler.java                        │          │
│  │  • Receive WebSocket binary message                  │          │
│  │  • Extract PlayerSession from registry              │          │
│  │  • Forward to MessageDispatcher                     │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         PacketCodec.java                             │          │
│  │  • Decode 8-byte header (bodyLen + msgId)           │          │
│  │  • Extract protobuf payload                         │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         MessageDispatcher.java                       │          │
│  │  • Route by msgId to specific handler               │          │
│  │  • Example: msgId 1610 → BoxHandler                 │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxHandler.java                              │          │
│  │  • Deserialize protobuf (PB_CSBoxReq)               │          │
│  │  • Validate request                                 │          │
│  │  • Call Feign client (box-service)                  │          │
│  └─────────────────────┬────────────────────────────────┘          │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         │ HTTP REST Call (Feign)
                         │ POST /api/box/open
                         │ Content-Type: application/json
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    BUSINESS LAYER (Port 8290)                       │
│                       box-service (Spring Boot)                     │
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxController.java                           │          │
│  │  • @PostMapping("/api/box/open")                     │          │
│  │  • Validate request DTO                              │          │
│  │  • Call service layer                                │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxService.java                              │          │
│  │  • Business logic execution                          │          │
│  │  • Call other services via Feign:                   │          │
│  │    - drop-service (roll rewards)                     │          │
│  │    - bag-service (grant items)                       │          │
│  │    - wallet-service (deduct cost)                    │          │
│  │  • Transaction management                            │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxRepository.java (JPA)                     │          │
│  │  • Database operations (MySQL)                       │          │
│  │  • Save box opening records                          │          │
│  │  • Update user settings                              │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│                    MySQL Database                                  │
│                    (game_box schema)                               │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         │ Response DTO (JSON)
                         │ { result, rewards, ... }
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      GATEWAY LAYER (Return)                         │
│                      WebSocket-Server                               │
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxHandler.java                              │          │
│  │  • Receive REST response                             │          │
│  │  • Convert DTO → Protobuf (PB_SCBoxInfo)            │          │
│  │  • Serialize protobuf                                │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         PacketCodec.java                             │          │
│  │  • Encode: [4B Length][4B MsgId][Protobuf]          │          │
│  │  • Example: msgId 1616 (PB_SCBoxInfo)               │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         WsGatewayHandler.java                        │          │
│  │  • Send binary message via WebSocket                │          │
│  │  • session.sendMessage(binaryMessage)                │          │
│  └─────────────────────┬────────────────────────────────┘          │
└────────────────────────┼────────────────────────────────────────────┘
                         │
                         │ WebSocket Binary Message
                         │ [4B Length][4B MsgId 1616][PB_SCBoxInfo]
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND LAYER (Receive)                    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         NetNode.ts                                   │          │
│  │  • onMessage() receives binary data                  │          │
│  │  • Decode header (4B bodyLen + 4B msgId)            │          │
│  │  • Extract msgId = 1616                              │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         ProtocolHelper.ts                            │          │
│  │  • Deserialize protobuf payload                      │          │
│  │  • PB_SCBoxInfo.decode(payload)                      │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxCtrl.ts                                   │          │
│  │  • recvBoxInfo(data: PB_SCBoxInfo)                   │          │
│  │  • Update BoxData model                              │          │
│  │  • Trigger UI refresh                                │          │
│  └─────────────────────┬────────────────────────────────┘          │
│                        │                                            │
│                        ▼                                            │
│  ┌──────────────────────────────────────────────────────┐          │
│  │         BoxView.ts                                   │          │
│  │  • Listen to data change events                      │          │
│  │  • Update UI components                              │          │
│  │  • Show rewards animation                            │          │
│  └──────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 FLOW CHI TIẾT - VÍ DỤ OPEN BOX

### **Bước 1: User Click Button (Frontend)**

**File**: `BoxView.ts` (UI Component)
```typescript
// User clicks "Open Box" button
onOpenBoxClicked() {
    let mode = 0; // 0 = single, 1 = x5
    BoxCtrl.Inst().SendBoxReq(BoxReqType.OPEN_BOX, mode);
}
```

---

### **Bước 2: Controller Tạo Request (Frontend)**

**File**: `BoxCtrl.ts` (Controller)
```typescript
export class BoxCtrl extends BaseCtrl {
    // Defined in BoxCtrl
    public static MT_BOX_REQ_CS = 1610;     // Client → Server
    public static MT_BOX_INFO_SC = 1616;    // Server → Client
    
    // Send request to server
    public SendBoxReq(req_type: BoxReqType, param?: number) {
        // Create protobuf message
        let protocol = this.GetProtocol(PB_CSBoxReq);
        protocol.reqType = req_type;        // OPEN_BOX = 1
        protocol.param = param ?? 0;        // mode = 0 (single)
        
        // Send via WebSocket
        this.SendToServer(protocol);
    }
    
    // Register message handler
    MsgCfg(): regMsg[] {
        return [
            { msgType: PB_SCBoxInfo, func: this.recvBoxInfo },
            { msgType: PB_SCBoxEquipInfo, func: this.recvBoxEquipInfo },
            // ...
        ]
    }
}
```

---

### **Bước 3: Serialize & Send (Frontend)**

**File**: `BaseCtrl.ts` → `NetNode.ts`
```typescript
// BaseCtrl.ts
protected SendToServer(protocol: any) {
    let msgId = this.getMsgId(protocol); // 1610 (MT_BOX_REQ_CS)
    NetworkMgr.SendMsg(msgId, protocol);
}

// NetNode.ts
public Send(data: { msgId: number, proto: any }): boolean {
    // 1. Serialize protobuf to binary
    var buf = this._protocolHelper.handlePackageData(data);
    
    // 2. Add 8-byte header:
    //    [4 bytes bodyLen][4 bytes msgId][protobuf payload]
    //    Example: [00 00 00 0C][00 00 06 4A][proto bytes...]
    //             (12 bytes)  (msgId 1610)
    
    // 3. Send via WebSocket
    return this._socket.send(buf);
}
```

**Packet Structure**:
```
Offset | Size | Field       | Example Value
-------|------|-------------|---------------
0      | 4B   | bodyLen     | 0x0000000C (12 bytes)
4      | 4B   | msgId       | 0x0000064A (1610)
8      | N    | protobuf    | [serialized PB_CSBoxReq]
```

---

### **Bước 4: WebSocket Nhận Message (Backend)**

**File**: `WsGatewayHandler.java` (Port 8094)
```java
@Component
public class WsGatewayHandler extends BinaryWebSocketHandler {
    
    @Override
    protected void handleBinaryMessage(
        WebSocketSession session, 
        BinaryMessage message
    ) throws Exception {
        // 1. Get player session from registry
        PlayerSession ps = sessionRegistry.findBySessionId(session.getId());
        
        // 2. Extract binary payload
        byte[] payload = message.getPayload().array();
        // payload = [00 00 00 0C][00 00 06 4A][protobuf bytes...]
        
        // 3. Dispatch to MessageDispatcher
        messageDispatcher.dispatch(ps, payload);
    }
}
```

---

### **Bước 5: Decode Packet (Backend)**

**File**: `PacketCodec.java`
```java
public class PacketCodec {
    public static Decoded decode(byte[] fullPacket) {
        // 1. Read 4-byte body length
        int bodyLen = ByteBuffer.wrap(fullPacket, 0, 4)
                                .order(ByteOrder.BIG_ENDIAN)
                                .getInt();
        // bodyLen = 12
        
        // 2. Read 4-byte message ID
        int msgId = ByteBuffer.wrap(fullPacket, 4, 4)
                              .order(ByteOrder.BIG_ENDIAN)
                              .getInt();
        // msgId = 1610
        
        // 3. Extract protobuf payload (skip 8-byte header)
        byte[] payload = Arrays.copyOfRange(fullPacket, 8, fullPacket.length);
        // payload = [protobuf bytes of PB_CSBoxReq]
        
        return new Decoded(bodyLen, msgId, payload);
    }
}
```

---

### **Bước 6: Route Message (Backend)**

**File**: `MessageDispatcher.java`
```java
@Component
public class MessageDispatcher {
    
    public void dispatch(PlayerSession session, byte[] payload) {
        // 1. Decode packet
        PacketCodec.Decoded decoded = PacketCodec.decode(payload);
        int msgId = decoded.msgId();              // 1610
        byte[] actualPayload = decoded.payload(); // protobuf bytes
        
        // 2. Get category from msgId
        String category = MessageIds.getCategory(msgId);
        // msgId 1610 → category = "BOX"
        
        // 3. Route to handler
        routeMessage(session, msgId, actualPayload, category);
    }
    
    private void routeMessage(...) {
        switch (category) {
            case "BOX":
                boxHandler.handle(session, msgId, payload);
                break;
            case "BAG":
                bagHandler.handle(session, msgId, payload);
                break;
            // ...
        }
    }
}
```

---

### **Bước 7: Handler Xử Lý (Backend)**

**File**: `BoxHandler.java`
```java
@Component
public class BoxHandler {
    
    @Autowired
    private BoxServiceClient boxServiceClient; // Feign client
    
    public void handle(PlayerSession session, int msgId, byte[] payload) {
        switch (msgId) {
            case 1610: // MT_BOX_REQ_CS
                handleBoxReq(session, payload);
                break;
            case 1611:
                // ...
                break;
        }
    }
    
    private void handleBoxReq(PlayerSession session, byte[] payload) {
        // 1. Deserialize protobuf
        PB_CSBoxReq req = PB_CSBoxReq.parseFrom(payload);
        int reqType = req.getReqType(); // OPEN_BOX = 1
        int param = req.getParam();      // mode = 0
        
        // 2. Validate
        if (reqType == BoxReqType.OPEN_BOX) {
            // 3. Call box-service via Feign
            BoxOpenRequest request = BoxOpenRequest.builder()
                .userId(session.getUserId())
                .mode(param == 0 ? BoxMode.SINGLE : BoxMode.BATCH_5)
                .build();
            
            BoxOpenResponse response = boxServiceClient.openBox(request);
            
            // 4. Convert response to protobuf
            PB_SCBoxInfo.Builder builder = PB_SCBoxInfo.newBuilder()
                .setResult(response.getResult())
                .addAllRewards(convertRewards(response.getRewards()));
            
            // 5. Send response back
            sendResponse(session, 1616, builder.build()); // MT_BOX_INFO_SC
        }
    }
}
```

---

### **Bước 8: Feign Client Call (Backend)**

**File**: `BoxServiceClient.java` (Feign Interface)
```java
@FeignClient(name = "box-service", url = "http://localhost:8290")
public interface BoxServiceClient {
    
    @PostMapping("/api/box/open")
    BoxOpenResponse openBox(@RequestBody BoxOpenRequest request);
}
```

**HTTP Request**:
```http
POST http://localhost:8290/api/box/open
Content-Type: application/json

{
    "userId": "user123",
    "mode": "SINGLE"
}
```

---

### **Bước 9: Business Service Xử Lý (Backend)**

**File**: `BoxController.java` (Port 8290)
```java
@RestController
@RequestMapping("/api/box")
public class BoxController {
    
    @PostMapping("/open")
    public BoxOpenResponse openBox(@RequestBody BoxOpenRequest request) {
        return boxService.openBox(request);
    }
}
```

**File**: `BoxServiceImpl.java`
```java
@Service
public class BoxServiceImpl implements BoxService {
    
    @Autowired
    private DropServiceClient dropServiceClient;
    
    @Autowired
    private BagServiceClient bagServiceClient;
    
    @Autowired
    private WalletServiceClient walletServiceClient;
    
    @Override
    @Transactional
    public BoxOpenResponse openBox(BoxOpenRequest request) {
        String userId = request.getUserId();
        
        // 1. Check wallet balance
        WalletBalanceDTO wallet = walletServiceClient.getBalance(userId);
        if (wallet.getDiamond() < BOX_COST) {
            throw new InsufficientFundsException();
        }
        
        // 2. Deduct cost
        walletServiceClient.debit(DebitRequest.builder()
            .userId(userId)
            .diamond(BOX_COST)
            .reason("open_box")
            .build());
        
        // 3. Roll rewards from drop-service
        DropRollResponse dropResult = dropServiceClient.rollDropTable(
            DropRollRequest.builder()
                .tableId(101) // box drop table
                .userId(userId)
                .build()
        );
        
        // 4. Grant items to bag
        bagServiceClient.grantItems(GrantItemsRequest.builder()
            .userId(userId)
            .items(dropResult.getItems())
            .source("box_open")
            .build());
        
        // 5. Save record
        BoxRecord record = BoxRecord.builder()
            .userId(userId)
            .mode(request.getMode())
            .rewards(dropResult.getItems())
            .openTime(LocalDateTime.now())
            .build();
        boxRepository.save(record);
        
        // 6. Return response
        return BoxOpenResponse.builder()
            .result("SUCCESS")
            .rewards(dropResult.getItems())
            .build();
    }
}
```

---

### **Bước 10: Trả Response Về Client (Backend)**

**File**: `BoxHandler.java`
```java
private void sendResponse(
    PlayerSession session, 
    int msgId, 
    MessageLite protobuf
) {
    // 1. Serialize protobuf
    byte[] protoBytes = protobuf.toByteArray();
    
    // 2. Encode packet: [4B bodyLen][4B msgId][protobuf]
    byte[] packet = PacketCodec.encode(msgId, protoBytes);
    
    // 3. Send via WebSocket
    BinaryMessage message = new BinaryMessage(packet);
    session.getWs().sendMessage(message);
}
```

**Packet Structure (Response)**:
```
Offset | Size | Field       | Example Value
-------|------|-------------|---------------
0      | 4B   | bodyLen     | 0x00000154 (340 bytes)
4      | 4B   | msgId       | 0x00000650 (1616)
8      | N    | protobuf    | [serialized PB_SCBoxInfo]
```

---

### **Bước 11: Frontend Nhận Response**

**File**: `NetNode.ts`
```typescript
protected onMessage(msg: NetData): void {
    // 1. Receive binary data
    msg = new Uint8Array(msg);
    // msg = [00 00 01 54][00 00 06 50][protobuf bytes...]
    
    // 2. Check package integrity
    if (!this._protocolHelper.checkPackage(msg)) {
        console.error(`NetNode checkHead Error`);
        return;
    }
    
    // 3. Process received packet
    this.processRecvPacket(new Uint8Array(msg));
}

protected processRecvPacket(data: Uint8Array) {
    // 4. Decode header
    let msgId = this._protocolHelper.getMsgId(data); // 1616
    
    // 5. Extract payload
    let payload = this._protocolHelper.getPayload(data);
    
    // 6. Deserialize protobuf
    let ProtoClass = this._protocolHelper.getProtoClass(msgId);
    let message = ProtoClass.decode(payload);
    // message = PB_SCBoxInfo instance
    
    // 7. Dispatch to listeners
    this.notifyListeners(msgId, message);
}
```

---

### **Bước 12: Controller Nhận & Update Data (Frontend)**

**File**: `BoxCtrl.ts`
```typescript
// This method was registered in MsgCfg()
private recvBoxInfo(data: PB_SCBoxInfo) {
    // 1. Extract data from protobuf
    let result = data.result;           // "SUCCESS"
    let rewards = data.rewardsList;     // Array of items
    
    // 2. Update data model
    BoxData.Inst().setBoxLevel(data);
    
    // 3. Trigger UI update events
    SMDTriggerNotify(BoxData.Inst().getBoxResultData(), "box_level_data");
    
    // 4. Show rewards in UI
    console.log("Box opened successfully!");
    console.log("Rewards:", rewards);
}
```

---

### **Bước 13: View Update UI (Frontend)**

**File**: `BoxView.ts`
```typescript
export class BoxView {
    
    onShow() {
        // Listen to data changes
        this.handleCollector.Add(
            SMDHandle.Create(
                BoxData.Inst().getBoxResultData(),
                this.refreshBoxUI.bind(this),
                "box_level_data"
            )
        );
    }
    
    // Called when BoxData changes
    private refreshBoxUI() {
        let rewards = BoxData.Inst().getRewards();
        
        // Show rewards animation
        this.playRewardsAnimation(rewards);
        
        // Update UI labels
        this.updateGoldLabel();
        this.updateDiamondLabel();
        this.updateItemList();
    }
    
    private playRewardsAnimation(rewards: Item[]) {
        // Play particle effects
        // Show reward popup
        // Animate items flying to inventory
    }
}
```

---

## 🔧 CÁC COMPONENT CHÍNH

### **1. Frontend Components**

| Component | File | Vai Trò |
|-----------|------|---------|
| **View** | `*View.ts` | UI components, user input |
| **Controller** | `*Ctrl.ts` | Business logic, message handling |
| **Data** | `*Data.ts` | Data model, state management |
| **NetworkMgr** | `NetNode.ts` | WebSocket connection management |
| **ProtocolHelper** | `ProtocolHelper.ts` | Protobuf serialization/deserialization |

### **2. Backend Components**

| Component | File | Port | Vai Trò |
|-----------|------|------|---------|
| **WebSocket Gateway** | `WsGatewayHandler.java` | 8094 | Accept WebSocket connections |
| **Message Dispatcher** | `MessageDispatcher.java` | 8094 | Route messages to handlers |
| **Handlers** | `*Handler.java` | 8094 | Process messages, call services |
| **Feign Clients** | `*ServiceClient.java` | 8094 | REST clients to microservices |
| **Business Services** | `*Service.java` | 8210-8599 | Business logic |
| **Controllers** | `*Controller.java` | 8210-8599 | REST endpoints |
| **Repositories** | `*Repository.java` | 8210-8599 | Database access |

---

## 📦 PROTOCOL BUFFERS

### **Message Definition**

**File**: `proto/cs/msgbox.proto`
```protobuf
syntax = "proto3";

package cs;

// Client → Server: Open box request
message PB_CSBoxReq {
    int32 reqType = 1;  // BoxReqType enum
    int32 param = 2;    // mode or itemId
}

// Server → Client: Box info response
message PB_SCBoxInfo {
    string result = 1;              // "SUCCESS" or "ERROR"
    repeated PB_ItemInfo rewards = 2; // List of rewards
    int32 totalGold = 3;            // Total gold earned
    int32 totalExp = 4;             // Total exp earned
}

message PB_ItemInfo {
    int32 itemId = 1;
    int32 count = 2;
    int32 quality = 3;
}
```

### **Compilation**

```bash
# Frontend (TypeScript)
protoc --ts_out=./assets/script/proto proto/**/*.proto

# Backend (Java)
protoc --java_out=./src/main/java proto/**/*.proto
```

---

## 🔢 MESSAGE ID MAPPING

### **Message ID Convention**

```typescript
// Frontend: MsgIdManager.ts
export class MsgIds {
    // Format: MT_{MODULE}_{DIRECTION}
    // CS = Client → Server
    // SC = Server → Client
    
    // Box module (1610-1619)
    static MT_BOX_REQ_CS = 1610;        // Open box request
    static MT_BOX_INFO_SC = 1616;       // Box info response
    static MT_BOX_EQUIP_SC = 1617;      // Equipment from box
    static MT_BOX_SELL_SC = 1618;       // Sell result
    
    // Bag module (1500-1509)
    static MT_BAG_INFO_SC = 1505;
    static MT_BAG_CHANGE_SC = 1506;
    static MT_BAG_USE_CS = 1507;
    
    // ... more modules
}
```

### **Category Mapping**

```java
// Backend: MessageIds.java
public class MessageIds {
    public static String getCategory(int msgId) {
        if (msgId >= 1610 && msgId <= 1619) return "BOX";
        if (msgId >= 1500 && msgId <= 1509) return "BAG";
        if (msgId >= 1600 && msgId <= 1609) return "EQUIP";
        if (msgId >= 1620 && msgId <= 1639) return "SHOP";
        if (msgId >= 7000 && msgId <= 7199) return "LOGIN";
        // ...
        return "UNKNOWN";
    }
}
```

---

## ⚠️ ERROR HANDLING & RETRY

### **Frontend Retry Logic**

```typescript
// NetNode.ts
protected onError(event: Event) {
    console.error("WebSocket error:", event);
    
    if (this._autoReconnect > 0 || this._autoReconnect === -1) {
        this.reconnect();
    }
}

protected reconnect() {
    if (this._reconnectTimer) {
        clearTimeout(this._reconnectTimer);
    }
    
    this._reconnectTimer = setTimeout(() => {
        console.log("Attempting to reconnect...");
        this.Connect(this._connectOptions);
        
        if (this._autoReconnect > 0) {
            this._autoReconnect--;
        }
    }, this._reconnetTimeOut);
}
```

### **Backend Error Response**

```java
// BoxHandler.java
try {
    // Process message
    BoxOpenResponse response = boxServiceClient.openBox(request);
    sendResponse(session, 1616, convertToProto(response));
    
} catch (InsufficientFundsException e) {
    // Send error response
    PB_SCBoxInfo error = PB_SCBoxInfo.newBuilder()
        .setResult("ERROR_INSUFFICIENT_FUNDS")
        .build();
    sendResponse(session, 1616, error);
    
} catch (Exception e) {
    log.error("Error opening box", e);
    PB_SCBoxInfo error = PB_SCBoxInfo.newBuilder()
        .setResult("ERROR_INTERNAL")
        .build();
    sendResponse(session, 1616, error);
}
```

---

## 🎯 BEST PRACTICES

### **1. Message ID Management**

✅ **DO**:
- Dùng enum hoặc constants cho message IDs
- Group theo module (1610-1619 = Box, 1500-1509 = Bag)
- Document rõ ràng CS/SC direction

❌ **DON'T**:
- Hardcode magic numbers
- Tái sử dụng message IDs
- Thay đổi IDs của messages đã deploy

### **2. Protobuf Best Practices**

✅ **DO**:
- Dùng `required` cho fields bắt buộc
- Thêm field number khi thêm fields mới
- Version control proto files

❌ **DON'T**:
- Xóa fields (dùng deprecated thay vì xóa)
- Thay đổi field numbers
- Dùng same field number cho different types

### **3. Error Handling**

✅ **DO**:
- Return error codes trong protobuf
- Log errors với context
- Retry idempotent operations
- Timeout cho network calls

❌ **DON'T**:
- Expose internal errors to client
- Retry non-idempotent operations
- Ignore errors silently

### **4. Performance**

✅ **DO**:
- Batch operations khi có thể
- Cache frequently accessed data (Redis)
- Use connection pooling
- Monitor message throughput

❌ **DON'T**:
- Send large payloads via WebSocket
- Make N+1 database queries
- Block WebSocket thread

---

## 📚 TÀI LIỆU THAM KHẢO

| Document | Location | Purpose |
|----------|----------|---------|
| **WEBSOCKET_INTEGRATION_PLAN.md** | `GameServer/` | WebSocket integration details |
| **AGENT_DEVELOPMENT_GUIDE.md** | Root | Service catalog & implementation guide |
| **Protocol Files** | `client/LineR/proto/` | Protobuf definitions |
| **Frontend Controllers** | `client/LineR/assets/script/modules/` | TypeScript controllers |
| **Backend Handlers** | `GameServer/webSocket-server/src/.../handler/` | Java message handlers |
| **Business Services** | `GameServer/{service-name}/` | Microservice implementations |

---

## 🔗 LUỒNG HOÀN CHỈNH - SUMMARY

```
┌──────────────┐
│ User Click   │
│ Button       │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────┐
│ Frontend (Cocos Creator TypeScript)  │
│                                      │
│ 1. View → Controller                │
│ 2. Create Protobuf Message          │
│ 3. Serialize + Add Header           │
│ 4. Send via WebSocket               │
└──────┬───────────────────────────────┘
       │ Binary Message [8B Header + Protobuf]
       ▼
┌──────────────────────────────────────┐
│ WebSocket-Server (Port 8094)        │
│                                      │
│ 5. WsGatewayHandler receives        │
│ 6. PacketCodec decodes header       │
│ 7. MessageDispatcher routes         │
│ 8. Handler deserializes protobuf    │
│ 9. Feign client calls service       │
└──────┬───────────────────────────────┘
       │ HTTP REST (JSON)
       ▼
┌──────────────────────────────────────┐
│ Business Service (Port 8210-8599)   │
│                                      │
│ 10. Controller receives request     │
│ 11. Service executes business logic │
│ 12. Call other services (Feign)     │
│ 13. Repository saves to database    │
│ 14. Return JSON response            │
└──────┬───────────────────────────────┘
       │ HTTP Response (JSON)
       ▼
┌──────────────────────────────────────┐
│ WebSocket-Server (Port 8094)        │
│                                      │
│ 15. Handler converts DTO→Protobuf   │
│ 16. PacketCodec encodes packet      │
│ 17. Send via WebSocket              │
└──────┬───────────────────────────────┘
       │ Binary Message [8B Header + Protobuf]
       ▼
┌──────────────────────────────────────┐
│ Frontend (Cocos Creator TypeScript)  │
│                                      │
│ 18. NetNode receives binary         │
│ 19. Decode header, extract msgId    │
│ 20. Deserialize protobuf            │
│ 21. Controller updates data model   │
│ 22. View refreshes UI               │
└──────────────────────────────────────┘
```

---

**Tổng thời gian**: ~100-300ms (tùy network latency + business logic)

**Key Points**:
- ✅ WebSocket cho real-time bidirectional communication
- ✅ Protocol Buffers cho compact binary serialization
- ✅ Microservices architecture cho scalability
- ✅ Feign clients cho inter-service communication
- ✅ Clear separation: Gateway (8094) ↔ Business Services (8210+)

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-19  
**Author**: AI Development Team

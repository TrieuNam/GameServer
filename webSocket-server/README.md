# WebSocket Server

**Version**: 1.0.0  
**Phase**: P0 (Core Infrastructure)  
**Port**: 8094  
**Protocol**: WebSocket + Binary Protobuf  
**Database**: N/A (Redis-backed session)

---

## 📋 Overview

WebSocket Server là **cầu nối real-time** giữa game clients và toàn bộ hệ thống business services. Client kết nối bằng WebSocket với binary Protobuf protocol, server nhận message → decode → route đến service tương ứng qua Feign/gRPC → trả kết quả về client.

### Core Features
- ✅ WebSocket server với binary Protobuf protocol
- ✅ Route ~50 loại game message đến đúng service
- ✅ Player session registry (Redis-backed)
- ✅ Cross-server session management
- ✅ Kafka consumer (BagChangedEvent → push về client)
- ✅ Handlers cho ~50 game modules
- ✅ Gọi services qua Feign (REST) và gRPC

---

## 🎯 Flow Hoạt Động

```
Client (Mobile/Web)
        │  WebSocket + Protobuf binary
        ▼
[WebSocket Server :8094]
        │
        ├── Decode Protobuf message
        ├── Extract MSGID từ header
        │
        ├── MSGID_0101 ──► RoleHandler ──► role-service:8410 (gRPC)
        ├── MSGID_0201 ──► BagHandler  ──► bag-service:9230 (gRPC)
        ├── MSGID_0301 ──► ShopHandler ──► shop-service:9260 (gRPC)
        ├── MSGID_1401 ──► MailHandler ──► mail-service:8470 (Feign)
        ├── MSGID_1501 ──► ChatHandler ──► chat-service:8460 (Feign)
        └── ... (~50 handlers)
        │
        ▼
[Kafka Consumer] ── BagChangedEvent ──► Push update về client
```

---

## 🎮 Message ID Ranges

| Range | Module | Protocol |
|-------|---------|---------|
| 0101-0199 | Role | gRPC |
| 0201-0299 | Bag | gRPC |
| 0301-0399 | Shop | gRPC |
| 0401-0499 | Equip | gRPC |
| 0501-0599 | Combat/Battle | gRPC |
| 0601-0699 | Arena | Feign |
| 0701-0799 | Trial | gRPC |
| 0801-0899 | Task | Feign |
| 0901-0999 | Starmap | gRPC |
| 1001-1099 | Territory | gRPC |
| 1101-1199 | Escort | Feign |
| 1201-1299 | World | Feign |
| 1301-1399 | Guild | Feign |
| 1401-1499 | Mail | Feign |
| 1501-1599 | Chat | Feign |
| 1601-1699 | Friend | Feign |
| 1701-1799 | Leaderboard | gRPC |
| 2100-2199 | Pet | Feign |
| 2200-2299 | Mount | Feign |
| 2300-2399 | Rune | gRPC |
| 2400-2499 | Angel | gRPC |
| 2500-2599 | Artifact | gRPC |

---

## 🔌 WebSocket Endpoint

```
ws://localhost:8080/websocket-server/ws/game?token={jwt-token}
```

> Kết nối qua Gateway trên port 8080 (proxy đến 8094)

---

## 📦 Connection Flow

### 1. Connect
```
Client ──► ws://gateway:8080/websocket-server/ws/game?token={jwt}
Server ──► Validate JWT → Lấy userId/roleId → Đăng ký session Redis
```

### 2. Send Message
```
Client ──► Binary Protobuf: { msgId: 1401, payload: {...} }
Server ──► Decode → MailHandler.handle() → mail-service:8470
       ──► mail-service trả kết quả
       ──► Encode Protobuf response → Gửi về client
```

### 3. Push from Server
```
bag-service ──► Kafka: BagChangedEvent { roleId, items }
WebSocket Server ──► Kafka consumer nhận event
               ──► Tìm WebSocket session của roleId
               ──► Push Protobuf message về client
```

---

## ⚙️ Configuration

```yaml
server:
  port: 8094

spring:
  redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: websocket-server-group
      topics: bag-changed-events
```

---

## 🔧 Business Logic

### Session Management
- Mỗi player có WebSocket session được lưu trong Redis
- Key: `ws:session:{roleId}` → nodeId + connectionId
- Cross-server: nếu player đang kết nối server khác → route qua Redis pub/sub

### Disconnect Handling
- Xóa session khỏi Redis khi disconnect
- Gửi logout event đến session-service
- Timeout: 60 giây không heartbeat → force disconnect

---

## 🚀 Running

```bash
cd GameServer/webSocket-server
mvn clean install
mvn spring-boot:run
```

> ⚠️ Khởi động **SAU TẤT CẢ business services** để handlers hoạt động đúng

---

## 🔗 Integration Points

### Feign (REST) clients
- mail-service, chat-service, friend-service, guild-service, arena-service,
  task-service, escort-service, world-service, leaderboard-service, pet-service,
  mount-service, item-service, activity-service, notification-service, và nhiều hơn
- config-service (`GET /api/config/file?path=...`) cho các trường hợp cần tải game config động

### gRPC clients
- role(9410), bag(9230), equip(9240), crafting(9280), trial(9300),
  starmap(9092), territory(9086), angel(9090), artifact(9087), rune(9093),
  leaderboard(9088), gameworld(9105), main-fb(9128), localization(9560),
  battleserver(9082), shop(9260)

### Kafka (Consumer)
- **bag-changed-events**: Push cập nhật túi đồ về client real-time

---

## 📊 Statistics

```
Handlers:        ~50 message handlers
Feign Clients:   ~30 services
gRPC Clients:    ~16 services
Kafka Consumers: 1 topic
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~5,000+ lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


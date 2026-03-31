# Chat Service Memory

Service-specific operational memory cho `chat-service` trong GameServer.

## Identity
- Service name: `chat-service`
- Path: `GameServer/chat-service`
- Main port: 9018
- Database: chat_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Send/receive chat messages (private, group)
- Message history retrieval
- Chat room management
- Message persistence
- WebSocket integration for real-time chat

## Key Files & Anchors
- Controller: `chat-service/src/main/java/com/SouthMillion/chat_service/controller/ChatController.java`
- Service: `chat-service/src/main/java/com/SouthMillion/chat_service/service/ChatDomainService.java`
- Entity: `chat-service/src/main/java/com/SouthMillion/chat_service/entity/ChatMessage.java`
- WebSocket: `chat-service/src/main/java/com/SouthMillion/chat_service/websocket/ChatWebSocketHandler.java`

## Important APIs
```
POST   /api/chat/message/send       - Send message
GET    /api/chat/messages/{userId}  - Get message history
GET    /api/chat/room/{roomId}      - Get room messages
POST   /api/chat/room/create        - Create chat room
DELETE /api/chat/message/{msgId}    - Delete message
```

## Database Schema
```sql
CREATE TABLE chat_messages (
  id VARCHAR(36) PRIMARY KEY,
  sender_id VARCHAR(36) NOT NULL,
  recipient_id VARCHAR(36),
  room_id VARCHAR(36),
  content TEXT NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  INDEX idx_sender (sender_id),
  INDEX idx_room (room_id),
  INDEX idx_created (created_at)
);

CREATE TABLE chat_rooms (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  owner_id VARCHAR(36) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Common Bugs & Patterns
- **Bug 1**: Messages not marked as read
  - Fix: Update is_read flag when message viewed
- **Bug 2**: WebSocket connection drops, messages lost
  - Fix: Implement reconnection logic, store undelivered messages
- **Bug 3**: User receives others' private messages
  - Fix: Validate sender/recipient before storing

## Cross-Service Dependencies
- **user-service**: Validate users before sending message
- **webSocket-server**: Real-time message delivery

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_service_db
    username: root
    password: root

server:
  port: 9018
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\chat-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Message encryption for sensitive data?
- [ ] Rate limiting on send message?
- [ ] User cannot read others' private messages?
- [ ] WebSocket timeout handling?
- [ ] Message persistence before broadcasting?

## Update Log
- 2026-03-21 | Scope: chat-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/CHAT_SERVICE_MEMORY.md`


# Chat Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8460  
**Database**: `game_chat`

---

## 📋 Overview

Chat Service manages all chat functionality including world chat, guild chat, team chat, private messages, system announcements, mute system, and chat history.

### Core Features
- ✅ World chat (global)
- ✅ Guild chat (guild members)
- ✅ Team chat (party members)
- ✅ Private chat (1-on-1)
- ✅ System announcements
- ✅ Chat history
- ✅ Mute/unmute players
- ✅ Auto-cleanup old messages

---

## 🎯 Chat Channels

| Channel | ID | Description | Visibility |
|---------|----|----|-----------|
| **World** | 1 | Global chat | All players |
| **Guild** | 2 | Guild chat | Guild members only |
| **Team** | 3 | Party chat | Team members only |
| **Private** | 4 | Direct message | Sender + Receiver |
| **System** | 5 | Announcements | All players |

---

## 🗄️ Database Schema

### chat_message
```sql
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel INT NOT NULL, -- 1=World, 2=Guild, 3=Team, 4=Private, 5=System
    sender_id VARCHAR(50) NOT NULL,
    sender_name VARCHAR(50) NOT NULL,
    receiver_id VARCHAR(50), -- For private chat
    receiver_name VARCHAR(50),
    channel_id VARCHAR(50), -- Guild ID or Team ID
    content VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL
);
```

### muted_player
```sql
CREATE TABLE muted_player (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    reason VARCHAR(200),
    mute_until DATETIME NOT NULL,
    muted_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
POST   /api/chat/send           - Send message
POST   /api/chat/history        - Get chat history
POST   /api/chat/mute           - Mute player
DELETE /api/chat/unmute/{roleId} - Unmute player
GET    /api/chat/health         - Health check
```

---

## 📦 API Examples

### Send World Message
```bash
curl -X POST http://localhost:8460/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "channel": 1,
    "senderId": "player123",
    "senderName": "Hero",
    "content": "Hello world!"
  }'
```

### Send Private Message
```bash
curl -X POST http://localhost:8460/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "channel": 4,
    "senderId": "player123",
    "senderName": "Hero",
    "receiverId": "player456",
    "receiverName": "Warrior",
    "content": "Hi friend!"
  }'
```

### Get Chat History
```bash
curl -X POST http://localhost:8460/api/chat/history \
  -H "Content-Type: application/json" \
  -d '{
    "channel": 1,
    "count": 50
  }'
```

### Mute Player
```bash
curl -X POST http://localhost:8460/api/chat/mute \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "player999",
    "roleName": "Spammer",
    "durationMinutes": 60,
    "reason": "Spam"
  }'
```

---

## 🚀 Running

```bash
cd GameServer/chat-service
mvn clean install
mvn spring-boot:run
```

---

## 📊 Statistics

```
Entities:        2 classes
Repositories:    2 interfaces
DTOs:            1 file
Services:        1 class
Controllers:     1 class
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          7 files ~1,200 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


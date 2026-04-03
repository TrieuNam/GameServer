# Chat Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8460  
**Database**: `game_chat`

---

## 📋 Tổng quan

Chat Service quản lý toàn bộ chức năng chat bao gồm world chat, guild chat, team chat, tin nhắn riêng, thông báo hệ thống, hệ thống mute, và lịch sử chat.

### Core Features
- ✅ World chat (toàn cầu)
- ✅ Guild chat (thành viên guild)
- ✅ Team chat (thành viên nhóm)
- ✅ Private chat (1-on-1)
- ✅ Thông báo hệ thống
- ✅ Lịch sử chat
- ✅ Mute/unmute người chơi
- ✅ Tự động dọn dẹp tin nhắn cũ

---

## 🎯 Kênh Chat

| Channel | ID | Mô tả | Hiển thị |
|---------|----|----|-----------|
| **World** | 1 | Chat toàn cầu | Tất cả người chơi |
| **Guild** | 2 | Chat guild | Chỉ thành viên guild |
| **Team** | 3 | Chat nhóm | Chỉ thành viên team |
| **Private** | 4 | Tin nhắn trực tiếp | Người gửi + Người nhận |
| **System** | 5 | Thông báo | Tất cả người chơi |

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

### Gửi Tin Nhắn World
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

### Gửi Tin Nhắn Riêng
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

### Lấy Lịch Sử Chat
```bash
curl -X POST http://localhost:8460/api/chat/history \
  -H "Content-Type: application/json" \
  -d '{
    "channel": 1,
    "count": 50
  }'
```

### Mute Người Chơi
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


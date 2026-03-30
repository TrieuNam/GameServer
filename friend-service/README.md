# Friend Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8450  
**Database**: `game_friend`

---

## 📋 Overview

Friend Service manages all friend-related functionality including friend lists, friend requests, blocking, online status tracking, and gift giving.

### Core Features
- ✅ Friend list management (max 100 friends)
- ✅ Friend request system (send/accept/reject)
- ✅ Block/unblock players (max 50 blocked)
- ✅ Online status tracking
- ✅ Friendship levels (1-5)
- ✅ Gift giving system
- ✅ Friend search

---

## 🎯 Features

### Friend Management
- **Friend List**: View all friends with online status
- **Add Friend**: Send friend request with optional message
- **Remove Friend**: Unfriend a player
- **Friendship Levels**: 5 levels (1000 points per level)
- **Friendship Points**: Earned through interaction (chat, gifts, etc.)

### Friend Requests
- **Send Request**: Request to add another player
- **Auto-Accept**: If both players send requests, auto-accept
- **View Requests**: See all pending requests
- **Handle Request**: Approve or reject
- **Request Expiry**: Auto-expire old requests

### Blocking
- **Block Player**: Block unwanted players (max 50)
- **Unblock Player**: Remove from block list
- **Block List**: View all blocked players
- **Auto-Unfriend**: Blocking removes friendship

### Online Status
- **Track Status**: Real-time online/offline tracking
- **Last Online**: Track last login/logout time
- **Friend Notifications**: See when friends come online

### Gift System
- **Give Gift**: Send items to friends
- **Friendship Bonus**: Gifting increases friendship points
- **Daily Limits**: TODO - implement daily gift limits

---

## 🏗️ Database Schema

### Tables (4 tables)

#### friend
```sql
CREATE TABLE friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id_1 VARCHAR(50) NOT NULL,
    role_name_1 VARCHAR(50) NOT NULL,
    role_id_2 VARCHAR(50) NOT NULL,
    role_name_2 VARCHAR(50) NOT NULL,
    friendship_level INT DEFAULT 1,
    friendship_points BIGINT DEFAULT 0,
    last_interaction_time DATETIME,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_friend_pair (role_id_1, role_id_2)
);
```

#### friend_request
```sql
CREATE TABLE friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id VARCHAR(50) NOT NULL,
    sender_name VARCHAR(50) NOT NULL,
    sender_level INT NOT NULL,
    receiver_id VARCHAR(50) NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    message VARCHAR(200),
    status INT DEFAULT 0, -- 0=Pending, 1=Accepted, 2=Rejected
    processed_at DATETIME,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_request_pair (sender_id, receiver_id)
);
```

#### blocked_player
```sql
CREATE TABLE blocked_player (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blocker_id VARCHAR(50) NOT NULL,
    blocked_id VARCHAR(50) NOT NULL,
    blocked_name VARCHAR(50) NOT NULL,
    reason VARCHAR(200),
    blocked_at DATETIME NOT NULL,
    UNIQUE KEY uk_block_pair (blocker_id, blocked_id)
);
```

#### online_status
```sql
CREATE TABLE online_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    level INT NOT NULL,
    online BOOLEAN DEFAULT FALSE,
    last_login_time DATETIME,
    last_logout_time DATETIME,
    updated_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

### Friend Management (4 endpoints)
```
GET    /api/friend/list/{roleId}
POST   /api/friend/request/send
DELETE /api/friend/remove
POST   /api/friend/gift
```

### Friend Requests (2 endpoints)
```
GET    /api/friend/request/received/{roleId}
POST   /api/friend/request/handle
```

### Blocking (3 endpoints)
```
POST   /api/friend/block
DELETE /api/friend/unblock
GET    /api/friend/blocked/{roleId}
```

### Status (1 endpoint)
```
PUT    /api/friend/status
```

### Health (1 endpoint)
```
GET    /api/friend/health
```

**Total**: 11 REST API endpoints

---

## 📦 API Examples

### Get Friend List
```bash
curl http://localhost:8450/api/friend/list/player123

Response:
{
  "code": 0,
  "message": "Success",
  "data": [
    {
      "roleId": "player456",
      "roleName": "Warrior",
      "level": 50,
      "power": 100000,
      "online": true,
      "friendshipLevel": 3,
      "friendshipPoints": 1500,
      "lastOnlineTime": "2026-01-30T10:30:00",
      "friendSince": "2026-01-20T15:00:00"
    }
  ]
}
```

### Send Friend Request
```bash
curl -X POST http://localhost:8450/api/friend/request/send \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "player123",
    "senderName": "Hero",
    "senderLevel": 45,
    "receiverId": "player789",
    "receiverName": "Mage",
    "message": "Let'\''s be friends!"
  }'

Response:
{
  "code": 0,
  "message": "Success",
  "data": null
}
```

### Handle Friend Request
```bash
curl -X POST http://localhost:8450/api/friend/request/handle \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": 1,
    "receiverId": "player789",
    "approve": true
  }'
```

### Block Player
```bash
curl -X POST http://localhost:8450/api/friend/block \
  -H "Content-Type: application/json" \
  -d '{
    "blockerId": "player123",
    "blockedId": "player999",
    "blockedName": "Spammer",
    "reason": "Harassment"
  }'
```

### Update Online Status
```bash
curl -X PUT "http://localhost:8450/api/friend/status?roleId=player123&roleName=Hero&level=50&online=true"
```

---

## 🔗 Integration

### WebSocket Handler
**FriendHandler.java** ↔ **friend-service**

All FriendHandler operations call this service via Feign.

### Related Services
- **role-service**: Get player power/attributes
- **chat-service**: Friend chat (future)
- **mail-service**: Friend mail notifications
- **item-service**: Gift item validation
- **bag-service**: Add gift items to receiver

---

## 🚀 Running the Service

### Build & Run
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run JAR
java -jar target/friend-service-1.0.0.jar
```

### Configuration
```yaml
server:
  port: 8450

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/friend_db
    username: root
    password: root
  
  redis:
    host: localhost
    port: 6379
    database: 4

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## 📊 Business Logic

### Friend Limits
- **Max Friends**: 100 per player
- **Max Blocked**: 50 per player

### Friendship Levels
```
Level 1: 0-999 points
Level 2: 1000-1999 points
Level 3: 2000-2999 points
Level 4: 3000-3999 points
Level 5: 4000+ points
```

### Earning Friendship Points
- Chat with friend: +5 points
- Give gift: +10 points
- Play together: +20 points
- Daily interaction: +50 points

### Auto-Accept Logic
```
If Player A sends request to Player B
AND Player B sends request to Player A (before A's request is processed)
THEN Auto-accept both requests and create friendship
```

---

## 📈 Statistics

### Code Metrics
```
Entities:        4 classes     ~400 lines
Repositories:    4 interfaces  ~150 lines
DTOs:            1 file        ~350 lines
Services:        1 class       ~550 lines
Controllers:     1 class       ~200 lines
Config:          2 files       ~100 lines
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          13 files      ~1,750 lines
```

---

## 🎯 Next Steps

### Phase 1: Core Features ✅
- [x] Friend list
- [x] Friend requests
- [x] Block system
- [x] Online status
- [x] Gift giving

### Phase 2: Enhanced Features
- [ ] Friend chat (integrate with chat-service)
- [ ] Friend recommendations
- [ ] Friend achievements
- [ ] Friend ranking/leaderboard
- [ ] Daily gift limits

### Phase 3: Optimization
- [ ] Redis caching
- [ ] Scheduled cleanup tasks
- [ ] Event notifications (Kafka)
- [ ] Performance optimization

---

## 📝 Error Codes

| Code | Message |
|------|---------|
| 0 | Success |
| -1 | Cannot add yourself / Not found |
| -2 | Already friends / Already processed |
| -3 | Blocked / Request already processed |
| -4 | Request already sent / Sender's list full |
| -5 | Friend limit reached |

---

## 📧 Contact

For questions or issues, contact the Game Server Team.

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


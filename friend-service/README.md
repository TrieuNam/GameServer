# Friend Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8450  
**Database**: `game_friend`

---

## 📋 Tổng quan

Friend Service quản lý toàn bộ chức năng liên quan đến bạn bè bao gồm danh sách bạn bè, lời mời kết bạn, chặn người chơi, theo dõi trạng thái online, và tặng quà.

### Core Features
- ✅ Quản lý danh sách bạn bè (tối đa 100 bạn)
- ✅ Hệ thống lời mời kết bạn (gửi/chấp nhận/từ chối)
- ✅ Chặn/bỏ chặn người chơi (tối đa 50 người bị chặn)
- ✅ Theo dõi trạng thái online
- ✅ Cấp độ tình bạn (1-5)
- ✅ Hệ thống tặng quà
- ✅ Tìm kiếm bạn bè

---

## 🎯 Tính Năng

### Quản Lý Bạn Bè
- **Danh sách bạn bè**: Xem tất cả bạn bè với trạng thái online
- **Thêm bạn**: Gửi lời mời kết bạn với tin nhắn tùy chọn
- **Xóa bạn**: Hủy kết bạn với người chơi
- **Cấp độ tình bạn**: 5 cấp độ (1000 điểm mỗi cấp)
- **Điểm tình bạn**: Nhận qua tương tác (chat, quà, v.v.)

### Lời Mời Kết Bạn
- **Gửi lời mời**: Gửi yêu cầu kết bạn với người chơi khác
- **Tự động chấp nhận**: Nếu cả hai người gửi lời mời, tự động chấp nhận
- **Xem lời mời**: Xem tất cả lời mời đang chờ
- **Xử lý lời mời**: Chấp nhận hoặc từ chối
- **Hết hạn lời mời**: Tự động hết hạn các lời mời cũ

### Chặn
- **Chặn người chơi**: Chặn người chơi không mong muốn (tối đa 50)
- **Bỏ chặn**: Xóa khỏi danh sách chặn
- **Danh sách chặn**: Xem tất cả người chơi bị chặn
- **Tự động hủy kết bạn**: Chặn sẽ xóa tình bạn

### Trạng Thái Online
- **Theo dõi trạng thái**: Theo dõi online/offline thời gian thực
- **Lần cuối online**: Theo dõi thời gian login/logout cuối
- **Thông báo bạn bè**: Xem khi bạn bè online

### Hệ Thống Quà
- **Tặng quà**: Gửi items cho bạn bè
- **Bonus tình bạn**: Tặng quà tăng điểm tình bạn
- **Giới hạn hàng ngày**: TODO - triển khai giới hạn quà hàng ngày

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

### Lấy Danh Sách Bạn Bè
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

### Gửi Lời Mời Kết Bạn
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

### Xử Lý Lời Mời Kết Bạn
```bash
curl -X POST http://localhost:8450/api/friend/request/handle \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": 1,
    "receiverId": "player789",
    "approve": true
  }'
```

### Chặn Người Chơi
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

### Cập Nhật Trạng Thái Online
```bash
curl -X PUT "http://localhost:8450/api/friend/status?roleId=player123&roleName=Hero&level=50&online=true"
```

---

## 🔗 Tích Hợp

### WebSocket Handler
**FriendHandler.java** ↔ **friend-service**

Tất cả thao tác FriendHandler gọi service này qua Feign.

### Services Liên Quan
- **role-service**: Lấy power/attributes của người chơi
- **chat-service**: Chat với bạn bè (tương lai)
- **mail-service**: Thông báo mail cho bạn bè
- **item-service**: Xác thực item quà
- **bag-service**: Thêm items quà cho người nhận

---

## 🚀 Running

```bash
cd GameServer/friend-service
mvn clean install
mvn spring-boot:run
```

### Configuration
```yaml
server:
  port: 8450

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:33085/game_friend?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
    username: tpnam
    password: 121831

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

### Giới Hạn Bạn Bè
- **Tối đa bạn bè**: 100 mỗi người chơi
- **Tối đa bị chặn**: 50 mỗi người chơi

### Cấp Độ Tình Bạn
```
Cấp 1: 0-999 điểm
Cấp 2: 1000-1999 điểm
Cấp 3: 2000-2999 điểm
Cấp 4: 3000-3999 điểm
Cấp 5: 4000+ điểm
```

### Nhận Điểm Tình Bạn
- Chat với bạn: +5 điểm
- Tặng quà: +10 điểm
- Chơi cùng: +20 điểm
- Tương tác hàng ngày: +50 điểm

### Logic Tự Động Chấp Nhận
```
Nếu Player A gửi lời mời cho Player B
VÀ Player B gửi lời mời cho Player A (trước khi lời mời của A được xử lý)
THÌ Tự động chấp nhận cả hai lời mời và tạo tình bạn
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


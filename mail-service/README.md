# Mail Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8470  
**Database**: `game_mail`

---

## 📋 Tổng quan

Mail Service quản lý toàn bộ hệ thống thư trong game bao gồm thư hệ thống, thư giữa người chơi, thư phần thưởng, và thư có đính kèm (items, gold, gems, exp).

### Core Features
- ✅ Thư hệ thống
- ✅ Thư giữa người chơi
- ✅ Thư phần thưởng có đính kèm
- ✅ Thư thông báo
- ✅ Thư có đính kèm gold/gems/items/exp
- ✅ Gửi thư hàng loạt
- ✅ Tự động hết hạn (mặc định 7 ngày)
- ✅ Trạng thái Đã đọc/Chưa đọc
- ✅ Hệ thống nhận đính kèm

---

## 🎯 Loại Thư

| Type | ID | Mô tả | Tính năng |
|------|----|----|----------|
| **System** | 1 | Từ hệ thống game | Không có người gửi, chính thức |
| **Player** | 2 | Người chơi gửi cho nhau | Có thông tin người gửi |
| **Reward** | 3 | Phần thưởng/bồi thường | Có đính kèm |
| **Notice** | 4 | Thông báo | Thông báo quan trọng |

---

## 🎁 Loại Đính Kèm

| Type | ID | Mô tả | Ví dụ |
|------|----|----|---------|
| **Gold** | 1 | Tiền tệ trong game | 10000 gold |
| **Gems** | 2 | Tiền tệ cao cấp | 500 gems |
| **Item** | 3 | Items thông thường | HP Potion x5 |
| **Equipment** | 4 | Items trang bị | Legendary Sword |
| **EXP** | 5 | Điểm kinh nghiệm | 5000 EXP |

---

## 🗄️ Database Schema

### mail
```sql
CREATE TABLE mail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type INT NOT NULL, -- 1=System, 2=Player, 3=Reward, 4=Notice
    sender_id VARCHAR(50),
    sender_name VARCHAR(50),
    receiver_id VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(1000),
    is_read BOOLEAN DEFAULT FALSE,
    is_claimed_attachment BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    claimed_at DATETIME
);
```

### mail_attachment
```sql
CREATE TABLE mail_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mail_id BIGINT NOT NULL,
    attachment_type INT NOT NULL, -- 1=Gold, 2=Gems, 3=Item, 4=Equipment, 5=EXP
    item_id VARCHAR(50),
    item_name VARCHAR(100),
    quantity INT DEFAULT 1,
    quality INT,
    created_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
POST   /api/mail/send              - Send mail
POST   /api/mail/send-bulk         - Send bulk mail (multiple receivers)
GET    /api/mail/list/{roleId}     - Get mail list
PUT    /api/mail/{mailId}/read     - Mark mail as read
POST   /api/mail/{mailId}/claim    - Claim attachments
DELETE /api/mail/{mailId}          - Delete mail
GET    /api/mail/health            - Health check
```

---

## 📦 API Examples

### Gửi Thư Hệ Thống Có Phần Thưởng
```bash
curl -X POST http://localhost:8470/api/mail/send \
  -H "Content-Type: application/json" \
  -d '{
    "type": 3,
    "senderId": "SYSTEM",
    "senderName": "Game Master",
    "receiverId": "player123",
    "title": "Daily Login Reward",
    "content": "Thank you for logging in today!",
    "expirationDays": 7,
    "attachments": [
      {
        "attachmentType": 1,
        "quantity": 10000
      },
      {
        "attachmentType": 2,
        "quantity": 500
      },
      {
        "attachmentType": 3,
        "itemId": "item_001",
        "itemName": "HP Potion",
        "quantity": 5,
        "quality": 3
      }
    ]
  }'
```

### Gửi Thư Hàng Loạt Cho Nhiều Người Chơi
```bash
curl -X POST http://localhost:8470/api/mail/send-bulk \
  -H "Content-Type: application/json" \
  -d '{
    "type": 4,
    "senderId": "ADMIN",
    "senderName": "Administrator",
    "receiverIds": ["player123", "player456", "player789"],
    "title": "Server Maintenance Notice",
    "content": "Server will be under maintenance tomorrow.",
    "expirationDays": 3
  }'
```

### Lấy Danh Sách Thư
```bash
curl http://localhost:8470/api/mail/list/player123
```

### Đọc Thư
```bash
curl -X PUT http://localhost:8470/api/mail/123/read
```

### Nhận Đính Kèm
```bash
curl -X POST http://localhost:8470/api/mail/123/claim
```

### Xóa Thư
```bash
curl -X DELETE http://localhost:8470/api/mail/123
```

---

## 🔧 Business Logic

### Hết Hạn Thư
- Mặc định: 7 ngày
- Cấu hình được: 1-30 ngày
- Thư hết hạn tự động xóa bởi scheduled task

### Nhận Đính Kèm
- Chỉ có thể nhận một lần mỗi thư
- Không thể xóa thư có đính kèm chưa nhận
- Đính kèm được thêm vào inventory/wallet của người chơi
- Tích hợp với: wallet-service, bag-service

### Bảo Mật
- Không thể xóa thư có đính kèm chưa nhận
- Thư hết hạn không thể đọc hoặc nhận
- Gửi thư hàng loạt bị giới hạn để tránh spam

---

## 🚀 Running

```bash
cd GameServer/mail-service
mvn clean install
mvn spring-boot:run
```

---

## 📊 Statistics

```
Entities:        2 classes
Repositories:    2 interfaces
DTOs:            1 file (7 DTO classes)
Services:        1 class
Controllers:     1 class
Feign clients:   3 (WalletServiceFeign, BagServiceFeign, RoleServiceFeign)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~1,600 lines
```

---

## 🔗 Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **wallet-service** | (Feign) | Cộng gold/gem khi claim mail reward |
| **bag-service** | `POST /api/bag/internal/add` | Phát item reward |
| **role-service** | `GET /api/role/{roleId}` | Cộng EXP reward |

### Được gọi bởi
- **role-service**: `POST /api/mail/list`, `GET /api/mail/{userId}/{mailId}`, `POST /api/mail/{userId}/{mailId}/fetch`
- **task-service**: Gửi mail reward sau khi hoàn task
- **webSocket-server**: MSGID_1401_MAIL_REQ

---

**Status**: ✅ Production Ready (Updated 2026-03-22)  
**Last Updated**: 2026-03-22

# Mail Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8470  
**Database**: `game_mail`

---

## 📋 Overview

Mail Service manages all in-game mail functionality including system mails, player-to-player mails, reward mails, and mails with attachments (items, gold, gems, exp).

### Core Features
- ✅ System mail
- ✅ Player-to-player mail
- ✅ Reward mail with attachments
- ✅ Notice mail
- ✅ Mail with gold/gems/items/exp attachments
- ✅ Bulk mail sending
- ✅ Auto-expiration (default 7 days)
- ✅ Read/Unread status
- ✅ Attachment claiming system

---

## 🎯 Mail Types

| Type | ID | Description | Features |
|------|----|----|----------|
| **System** | 1 | From game system | No sender, official |
| **Player** | 2 | Player to player | Has sender info |
| **Reward** | 3 | Rewards/compensation | With attachments |
| **Notice** | 4 | Announcements | Important notices |

---

## 🎁 Attachment Types

| Type | ID | Description | Example |
|------|----|----|---------|
| **Gold** | 1 | In-game currency | 10000 gold |
| **Gems** | 2 | Premium currency | 500 gems |
| **Item** | 3 | Regular items | HP Potion x5 |
| **Equipment** | 4 | Equipment items | Legendary Sword |
| **EXP** | 5 | Experience points | 5000 EXP |

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

### Send System Mail with Reward
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

### Send Bulk Mail to Multiple Players
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

### Get Mail List
```bash
curl http://localhost:8470/api/mail/list/player123
```

### Read Mail
```bash
curl -X PUT http://localhost:8470/api/mail/123/read
```

### Claim Attachments
```bash
curl -X POST http://localhost:8470/api/mail/123/claim
```

### Delete Mail
```bash
curl -X DELETE http://localhost:8470/api/mail/123
```

---

## 🔧 Business Logic

### Mail Expiration
- Default: 7 days
- Configurable: 1-30 days
- Expired mails auto-deleted by scheduled task

### Attachment Claiming
- Can only claim once per mail
- Cannot delete mail with unclaimed attachments
- Attachments given to player inventory/wallet
- Integration with: wallet-service, bag-service

### Security
- Cannot delete mail with unclaimed attachments
- Expired mails cannot be read or claimed
- Bulk mail limited to prevent spam

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

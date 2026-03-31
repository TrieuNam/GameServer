# IAP Verify Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8580  
**Database**: `game_iap_verify`

---

## 📋 Overview

IAP Verify Service xử lý **xác thực thanh toán In-App Purchase** từ App Store (Apple) và Google Play (Android). Chống gian lận thanh toán, lưu lịch sử giao dịch, và cấp phát phần thưởng nạp tiền cho người chơi.

### Core Features
- ✅ Verify receipt từ App Store (iOS)
- ✅ Verify receipt từ Google Play (Android)
- ✅ Ghi lịch sử giao dịch IAP
- ✅ Chống gian lận (duplicate receipt detection)
- ✅ Consume purchase để trao thưởng
- ✅ Refund request management
- ✅ Suspicious transaction detection

---

## 🎯 Flow Thanh Toán

```
[Player nạp tiền qua App Store / Google Play]
        │
        ▼
Client nhận receipt từ Apple/Google
        │
        ▼
POST /api/iap/verify { receipt, platform, userId }
        │
        ▼
iap-verify-service
├── Gửi receipt đến Apple/Google để verify (external API)
├── Kiểm tra receipt chưa được dùng (duplicate check)
├── Lưu purchase record vào iap_verify_db
│
        ▼
POST /api/iap/consume/{purchaseId}
        │
        ▼
├──► wallet-service: add diamonds
└──► role-service: update vip level / recharge total
```

---

## 🗄️ Database Schema

### iap_purchase
```sql
CREATE TABLE iap_purchase (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    platform VARCHAR(10) NOT NULL,     -- "ios" hoặc "android"
    product_id VARCHAR(100) NOT NULL,
    transaction_id VARCHAR(200) UNIQUE NOT NULL,
    receipt_data TEXT NOT NULL,
    status INT DEFAULT 0,              -- 0=pending, 1=verified, 2=consumed, 3=refunded
    amount_usd DECIMAL(10,2),
    diamonds_granted INT,
    verified_at DATETIME,
    consumed_at DATETIME,
    created_at DATETIME NOT NULL
);
```

### iap_refund
```sql
CREATE TABLE iap_refund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_id BIGINT NOT NULL,
    reason VARCHAR(500),
    status INT DEFAULT 0,              -- 0=pending, 1=approved, 2=rejected
    requested_at DATETIME NOT NULL,
    processed_at DATETIME
);
```

---

## 🔌 API Endpoints

```
POST  /api/iap/verify                       - Verify purchase receipt
POST  /api/iap/consume/{purchaseId}         - Consume purchase (grant rewards)
GET   /api/iap/purchases/{userId}           - Lịch sử mua của user
GET   /api/iap/purchases/{userId}/unconsumed - Purchases chờ consume
POST  /api/iap/refund/request               - Yêu cầu hoàn tiền
PUT   /api/iap/refund/{refundId}/process    - Xử lý refund (admin)
GET   /api/iap/refund/pending               - Danh sách refund chờ xử lý
GET   /api/iap/suspicious                  - Giao dịch đáng ngờ
GET   /api/iap/health                      - Health check
```

---

## 📦 API Examples

### Verify Purchase
```bash
curl -X POST http://localhost:8580/api/iap/verify \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "platform": "ios",
    "productId": "com.game.diamond_pack_1",
    "receiptData": "MIIT..."
  }'
# Response: { "purchaseId": 456, "status": "verified", "diamonds": 600 }
```

### Consume Purchase (Grant Rewards)
```bash
curl -X POST http://localhost:8580/api/iap/consume/456
```

---

## 🔧 Business Logic

### Product → Diamond Mapping
| Product ID | USD | Diamonds |
|-----------|-----|----------|
| diamond_pack_1 | $0.99 | 60 |
| diamond_pack_2 | $4.99 | 330 |
| diamond_pack_3 | $9.99 | 680 |
| diamond_pack_4 | $19.99 | 1380 |
| diamond_pack_5 | $49.99 | 3480 |
| diamond_pack_6 | $99.99 | 6980 |

### Anti-Fraud
- `transaction_id` phải unique (chống replay attack)
- Verify với Apple/Google server (server-side validation)
- Flag suspicious nếu cùng receipt verify > 1 lần

---

## 🚀 Running

```bash
cd GameServer/iap-verify-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### External APIs
- Apple App Store Server API
- Google Play Developer API

### Phụ thuộc (internal)
- **wallet-service**: Grant diamonds sau verify
- **role-service**: Update VIP level, total recharge

---

## 📊 Statistics

```
Entities:        2 classes (IapPurchase, IapRefund)
Repositories:    2 interfaces
Controllers:     1 class (IapController)
Services:        2 (IapVerifyService, IapFraudDetectionService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~700 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


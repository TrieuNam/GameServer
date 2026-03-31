# Moderation Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8570  
**Database**: game_moderation

---

## 📋 Overview

Moderation Service quản lý **nội dung và kiểm duyệt người chơi** — lọc chat không phù hợp, xử lý reports vi phạm, và quản lý danh sách mute/ban. Đảm bảo môi trường game lành mạnh.

### Core Features
- ✅ Lọc nội dung không phù hợp (tên nhân vật, chat)
- ✅ Player reports (báo cáo vi phạm)
- ✅ Xử lý reports (approve/reject)
- ✅ Mute/unmute players
- ✅ Ban/unban users
- ✅ Violations tracking

---

## 🎯 Flow Kiểm Duyệt

```
[Player đặt tên nhân vật]
role-service ──► moderation-service: POST /api/moderation/filter
                        │
                        ├── Check blacklist words
                        ├── Check inappropriate patterns
                        └── Trả về: { allowed: true/false, reason }

[Player report người khác]
POST /api/moderation/report { reporterId, targetId, reason, evidence }
        │
        ▼
moderation-service
├── Ghi report vào DB
├── Auto-check: nếu target có nhiều reports → auto flag
└── Queue cho admin review
```

---

## 🗄️ Database Schema

### moderation_report
```sql
CREATE TABLE moderation_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id VARCHAR(50) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    report_type INT NOT NULL,         -- 1=spam, 2=abuse, 3=cheat, 4=inappropriate
    evidence TEXT,
    status INT DEFAULT 0,             -- 0=pending, 1=handled
    handled_by VARCHAR(50),
    handle_action VARCHAR(100),
    created_at DATETIME NOT NULL,
    handled_at DATETIME
);
```

### violations
```sql
CREATE TABLE violations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    severity INT DEFAULT 1,           -- 1=warning, 2=mute, 3=ban
    is_active BOOLEAN DEFAULT TRUE,
    expires_at DATETIME,
    created_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
POST  /api/moderation/filter                    - Lọc nội dung text
POST  /api/moderation/report                    - Báo cáo vi phạm
PUT   /api/moderation/report/{reportId}/handle  - Xử lý report (admin)
GET   /api/moderation/reports/pending           - Reports chờ xử lý
GET   /api/moderation/violations/{userId}       - Vi phạm của user
GET   /api/moderation/check/{userId}/muted      - Kiểm tra user đang muted không
GET   /api/moderation/check/{userId}/banned     - Kiểm tra user đang banned không
POST  /api/moderation/mute/{userId}             - Mute user
POST  /api/moderation/ban/{userId}              - Ban user
```

---

## 📦 API Examples

### Lọc Nội Dung
```bash
curl -X POST http://localhost:8570/api/moderation/filter \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello World", "context": "chat"}'
# Response: { "allowed": true, "filtered": "Hello World" }
```

### Báo Cáo Vi Phạm
```bash
curl -X POST http://localhost:8570/api/moderation/report \
  -H "Content-Type: application/json" \
  -d '{
    "reporterId": "player123",
    "targetId": "player999",
    "reportType": 3,
    "evidence": "Player dùng hack để di chuyển bất thường"
  }'
```

---

## 🔧 Business Logic

### Blacklist Filter
- Danh sách từ ngữ cấm trong config
- Regex patterns cho tên không phù hợp
- Auto-censor: thay bằng `***`

### Auto-Moderation
- Nếu player bị report ≥ 5 lần trong 24h → auto-flag cho admin review
- Patterns bất thường trong analytics → flag sang anti-cheat

---

## 🚀 Running

```bash
cd GameServer/moderation-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **chat-service**: Lọc nội dung chat
- **role-service**: Kiểm tra tên nhân vật
- **anti-cheat-service**: Escalate violations

---

## 📊 Statistics

```
Entities:        2 classes (ModerationReport, Violations)
Repositories:    2 interfaces
Controllers:     1 class (ModerationController)
Services:        2 (ModerationService, FilterService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~500 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


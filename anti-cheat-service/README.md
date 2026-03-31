# Anti-Cheat Service

**Version**: 1.0.0  
**Phase**: P4 (Optional Features)  
**Port**: 8590  
**Database**: game_anticheat

---

## 📋 Overview

Anti-Cheat Service **phát hiện và xử lý hành vi gian lận** trong game — speed hacks, damage hacks, resource manipulation, và các hành vi bất thường khác. Phân tích dữ liệu từ nhiều services để detect anomalies.

### Core Features
- ✅ Phát hiện speed hack (movement reports)
- ✅ Phát hiện damage hack (damage reports)
- ✅ Resource manipulation detection
- ✅ Suspicious activity reporting
- ✅ Admin review workflow
- ✅ Severity scoring
- ✅ Integration với moderation-service

---

## 🎯 Flow Phát Hiện Gian Lận

```
[WebSocket Server nhận movement data]
webSocket-server ──► POST /api/anticheat/report/movement
                                │
                        anticheat-service
                        ├── So sánh speed với max allowed
                        ├── Check teleportation (distance/time)
                        └── Nếu anomaly → tạo report với severity

[Phân Tích Damage]
battleserver-service ──► POST /api/anticheat/report/damage
                                │
                        ├── So sánh damage với max possible (formula-based)
                        └── Nếu vượt quá 200% max → flag suspicious

[Admin Review]
GET /api/anticheat/reports/high-severity
PUT /api/anticheat/reports/{reportId}/review { action: "ban"/"dismiss" }
```

---

## 🗄️ Database Schema

### anticheat_report
```sql
CREATE TABLE anticheat_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    report_type VARCHAR(50) NOT NULL,  -- "movement", "damage", "resource"
    data JSON NOT NULL,               -- Suspicious data details
    severity INT NOT NULL,             -- 1=low, 2=medium, 3=high, 4=critical
    status INT DEFAULT 0,              -- 0=pending, 1=reviewed
    reviewed_by VARCHAR(50),
    review_action VARCHAR(50),         -- "banned", "warned", "dismissed"
    created_at DATETIME NOT NULL,
    reviewed_at DATETIME,
    INDEX idx_user_type (user_id, report_type),
    INDEX idx_severity_status (severity, status)
);
```

---

## 🔌 API Endpoints

```
POST  /api/anticheat/report/movement           - Report movement anomaly
POST  /api/anticheat/report/damage             - Report damage anomaly
POST  /api/anticheat/report/resource           - Report resource anomaly
GET   /api/anticheat/reports/user/{userId}     - Reports của user
GET   /api/anticheat/reports/pending           - Reports chờ review
GET   /api/anticheat/reports/high-severity     - Reports severity cao
PUT   /api/anticheat/reports/{reportId}/review - Review report
GET   /api/anticheat/suspicious/user/{userId}  - Check user có suspicious không
GET   /api/anticheat/suspicious/unresolved     - Tất cả unresolved suspicious
POST  /api/anticheat/analyze/{userId}          - Phân tích toàn diện 1 user
GET   /api/anticheat/health                    - Health check
```

---

## 📦 API Examples

### Report Movement Anomaly
```bash
curl -X POST http://localhost:8590/api/anticheat/report/movement \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "player999",
    "fromX": 100, "fromY": 100,
    "toX": 5000, "toY": 5000,
    "timeMs": 500,
    "mapId": "map_001"
  }'
```

### Phân Tích User
```bash
curl -X POST http://localhost:8590/api/anticheat/analyze/player999
# Response: { "riskScore": 85, "flags": ["speed_hack", "damage_abnormal"], "recommendation": "ban" }
```

---

## 🔧 Business Logic

### Thresholds
- **Speed hack**: speed > max_speed * 1.5
- **Teleport**: distance > 1000 units trong < 100ms
- **Damage hack**: damage > theoretical_max * 2.0
- **Resource hack**: gold > daily_earn_max * 3.0

### Auto-Action
- Severity 4 (Critical) + 3 reports trong 1 giờ → Auto temp-ban 24h
- Kết hợp với moderation-service để thực hiện ban

---

## 🚀 Running

```bash
cd GameServer/anti-cheat-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **webSocket-server**: Movement và action reports
- **battleserver-service**: Damage reports

### Gọi ra
- **moderation-service**: Execute ban/mute actions

---

## 📊 Statistics

```
Entities:        1 class (AnticheatReport)
Repositories:    1 interface
Controllers:     1 class (AnticheatController)
Services:        2 (AnticheatService, AnomalyAnalyzer)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


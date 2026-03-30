# Analytics Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8510  
**Database**: game_analytics

---

## 📋 Overview

Analytics Service **thu thập và phân tích dữ liệu gameplay** — tracking hành động người chơi, giao dịch, KPI metrics, và phân tích hành vi. Dùng cho business intelligence, game balancing, và phát hiện gian lận.

### Core Features
- ✅ Event tracking (player actions, purchases, v.v.)
- ✅ KPI reports (DAU, retention, revenue)
- ✅ Top spenders ranking
- ✅ Most active players
- ✅ Event query theo player/type
- ✅ Date range analytics

---

## 🎯 Flow Analytics

```
[Mọi action của player]
Bất kỳ service nào ──► POST /api/analytics/track
                                │
                        analytics-service
                        ├── Lưu event vào game_analytics
                        └── Aggregate vào KPI metrics

[Business team cần báo cáo]
GET /api/analytics/kpi/{playerId}/range  (params: from, to)
└── Trả về metrics: sessions, purchases, playtime, v.v.
```

---

## 🗄️ Database Schema

### analytics_event
```sql
CREATE TABLE analytics_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(50),
    event_type VARCHAR(100) NOT NULL,  -- "login", "purchase", "level_up", "pvp_win"
    event_data JSON,
    platform VARCHAR(20),
    session_id VARCHAR(100),
    created_at DATETIME NOT NULL,
    INDEX idx_player_type (player_id, event_type),
    INDEX idx_created_at (created_at)
);
```

---

## 🔌 API Endpoints

```
POST  /api/analytics/track                               - Track event
GET   /api/analytics/events/{playerId}                   - Events của player (params: start, end)
GET   /api/analytics/events/{playerId}/type/{eventType}  - Events theo loại
GET   /api/analytics/kpi/{playerId}                      - KPI của player (param: date)
GET   /api/analytics/kpi/{playerId}/range                - KPI theo khoảng thời gian (params: start, end)
GET   /api/analytics/top-spenders                        - Top players chi tiêu nhiều nhất
GET   /api/analytics/most-active                         - Players active nhất
```

---

## 📦 API Examples

### Track Event
```bash
curl -X POST http://localhost:8510/api/analytics/track \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "eventType": "purchase",
    "eventData": {"productId": "diamond_pack_3", "amount": 9.99},
    "platform": "ios"
  }'
```

### Lấy KPI Theo Thời Gian
```bash
curl "http://localhost:8510/api/analytics/kpi/player123/range?from=2026-03-01&to=2026-03-16"
```

### Top Spenders
```bash
curl "http://localhost:8510/api/analytics/top-spenders?limit=10&period=monthly"
```

---

## 🔧 Business Logic

### Event Types
| Event | Mô tả |
|-------|-------|
| `login` | Player login |
| `logout` | Player logout |
| `purchase` | IAP purchase |
| `level_up` | Character level up |
| `pvp_win/lose` | Arena battle result |
| `boss_kill` | World boss kill |
| `quest_complete` | Quest completed |

---

## 🚀 Running

```bash
cd GameServer/analytics-service
mvn clean install
mvn spring-boot:run
```

---

## 📊 Statistics

```
Entities:        1 class (AnalyticsEvent)
Repositories:    1 interface
Controllers:     1 class (AnalyticsController)
Services:        1 class (AnalyticsService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


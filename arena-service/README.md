# Arena Service

**Version**: 1.0.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8084  
**Database**: game_arena

---

## 📋 Overview

Arena Service quản lý **hệ thống đấu trường PvP** — xếp hạng, matching đối thủ, lịch sử trận đấu, và phát reward theo hạng. Hỗ trợ cross-server arena và real-time ranking updates.

### Core Features
- ✅ Xếp hạng đấu trường (ELO/Rating-based)
- ✅ Matching đối thủ tự động (gần rating)
- ✅ Cross-server arena
- ✅ Lịch sử trận đấu
- ✅ Thống kê (win rate, streak, v.v.)
- ✅ Phát reward theo hạng (daily/weekly)
- ✅ Pagination cho bảng xếp hạng

---

## 🎯 Flow PvP Arena

```
Client ──► POST /api/arena/{playerId}/enter
                │
                ▼
        arena-service
        ├── GET opponent: tìm đối thủ gần rating nhất
        │
        ▼
POST /api/arena/{playerId}/battle { opponentId }
        │
        ▼
├──► battleserver-service (gRPC): run battle simulation
│          │
│    ◄── battle result { winner, damage, skills, ... }
│
├── Update ratings (ELO formula)
├── Ghi lịch sử trận đấu
├── Ghi Kafka event (task-service consume cho quest)
└──► wallet/bag-service: trao reward
```

---

## 🗄️ Database Schema

### arena_player
```sql
CREATE TABLE arena_player (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id VARCHAR(50) NOT NULL UNIQUE,
    player_name VARCHAR(50),
    rating INT DEFAULT 1000,
    rank INT DEFAULT 0,
    win_count INT DEFAULT 0,
    lose_count INT DEFAULT 0,
    win_streak INT DEFAULT 0,
    season INT DEFAULT 1,
    updated_at DATETIME NOT NULL
);
```

### arena_battle_history
```sql
CREATE TABLE arena_battle_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attacker_id VARCHAR(50) NOT NULL,
    defender_id VARCHAR(50) NOT NULL,
    winner_id VARCHAR(50) NOT NULL,
    rating_change INT,
    attacker_rating_before INT,
    attacker_rating_after INT,
    battle_log JSON,
    created_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

```
POST  /api/arena/{playerId}/enter        - Vào đấu trường
POST  /api/arena/{playerId}/battle       - Tấn công đối thủ
GET   /api/arena/{playerId}/opponent     - Tìm đối thủ phù hợp
GET   /api/arena/rankings                - Bảng xếp hạng (top 100)
GET   /api/arena/rankings/page           - Bảng xếp hạng phân trang
GET   /api/arena/{playerId}/rank         - Hạng của player
GET   /api/arena/{playerId}/rating       - Rating hiện tại
GET   /api/arena/{playerId}/history      - Lịch sử trận đấu
GET   /api/arena/{playerId}/stats        - Thống kê
```

---

## 📦 API Examples

### Tìm Đối Thủ
```bash
curl http://localhost:8084/api/arena/player123/opponent
# Response: { "opponentId": "player456", "opponentName": "DragonSlayer", "rating": 1450 }
```

### Tấn Công
```bash
curl -X POST http://localhost:8084/api/arena/player123/battle \
  -H "Content-Type: application/json" \
  -d '{"opponentId": "player456"}'
# Response: { "winner": "player123", "ratingChange": +25, "newRating": 1475 }
```

### Bảng Xếp Hạng Top 10
```bash
curl "http://localhost:8084/api/arena/rankings?limit=10"
```

---

## 🔧 Business Logic

### ELO Rating System
- Rating ban đầu: 1000
- K-factor: 32 (có thể điều chỉnh theo tier)
- Formula: `newRating = oldRating + K * (actual - expected)`
- `expected = 1 / (1 + 10^((opponentRating - myRating) / 400))`

### Daily Reward
- Dựa trên hạng cuối ngày: Top 1 → 10 → 50 → 100
- Reset daily tại 00:00 server time

### Matching Algorithm
- Tìm đối thủ có rating trong khoảng ±200
- Nếu không tìm được → mở rộng ±400, ±600
- Ưu tiên online players

---

## 🚀 Running

```bash
cd GameServer/arena-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Phụ thuộc
- **battleserver-service** (gRPC): Simulate battle
- **leaderboard-service** (Feign): Sync arena ranking
- **wallet-service**: Trao reward

### Kafka Producer
- **arena-events**: Gửi kết quả trận đấu (task-service consume)

---

## 📊 Statistics

```
Entities:        2 classes (ArenaPlayer, ArenaBattleHistory)
Repositories:    2 interfaces
Controllers:     1 class (ArenaController)
Services:        2 (ArenaService, MatchingService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~700 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


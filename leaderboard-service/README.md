# Leaderboard Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8480  
**Database**: `game_leaderboard`

---

## 📋 Overview

Leaderboard Service manages all game rankings including power, level, arena, wealth, guild, pet, mount, and PVP rankings. Uses Redis for high-performance caching with auto-refresh every 5 minutes.

### Core Features
- ✅ 8 ranking types
- ✅ Top 100 players per ranking
- ✅ Real-time rank updates
- ✅ Rank change tracking (up/down)
- ✅ Redis caching (5 min TTL)
- ✅ Auto-refresh every 5 minutes
- ✅ Player's personal rank lookup

---

## 🏆 Ranking Types

| Type | ID | Description | Score Metric |
|------|----|----|-------------|
| **Power** | 1 | Combat power | Total power |
| **Level** | 2 | Character level | Level + EXP |
| **Arena** | 3 | Arena ranking | Arena points |
| **Wealth** | 4 | Richest players | Gold + Gems |
| **Guild** | 5 | Guild ranking | Guild power |
| **Pet** | 6 | Pet ranking | Pet power |
| **Mount** | 7 | Mount ranking | Mount power |
| **PVP Kills** | 8 | Kill count | Total kills |

---

## 🗄️ Database Schema

### ranking_entry
```sql
CREATE TABLE ranking_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ranking_type INT NOT NULL, -- 1-8
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_level INT NOT NULL,
    score BIGINT DEFAULT 0,
    current_rank INT,
    previous_rank INT,
    guild_name VARCHAR(50),
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_type_role (ranking_type, role_id)
);
```

---

## 🔌 API Endpoints

```
POST   /api/leaderboard/update              - Update player score
GET    /api/leaderboard/{rankingType}       - Get leaderboard (top 100); optional ?roleId= to include my rank
POST   /api/leaderboard/refresh             - Manual refresh all
GET    /api/leaderboard/health              - Health check
```

---

## 📦 API Examples

### Update Score
```bash
curl -X POST http://localhost:8480/api/leaderboard/update \
  -H "Content-Type: application/json" \
  -d '{
    "rankingType": 1,
    "roleId": "player123",
    "roleName": "DragonSlayer",
    "roleLevel": 85,
    "score": 125000,
    "guildName": "Legends"
  }'
```

### Get Power Leaderboard
```bash
curl http://localhost:8480/api/leaderboard/1
```

### Get Leaderboard with My Rank
```bash
curl "http://localhost:8480/api/leaderboard/1?roleId=player123"
```

### Manual Refresh
```bash
curl -X POST http://localhost:8480/api/leaderboard/refresh
```

---

## 🔧 Business Logic

### Ranking Calculation
- Top 100 players per ranking type
- Sorted by score DESC, updatedAt ASC
- Rank changes tracked (previous vs current)
- Real-time updates on score change

### Caching Strategy
- Redis cache: 5 minute TTL
- Auto-refresh: Every 5 minutes via @Scheduled
- Cache cleared on score update
- Hot data in memory for fast queries

### Rank Change Indicators
- **Positive number**: Rank improved (e.g., +5 means moved up 5 ranks)
- **Negative number**: Rank dropped (e.g., -3 means moved down 3 ranks)
- **Zero**: No change

---

## 🚀 Running

```bash
cd GameServer/leaderboard-service
mvn clean install
mvn spring-boot:run
```

---

## 📊 Statistics

```
Entities:        1 class
Repositories:    1 interface
DTOs:            1 file (4 DTO classes)
Services:        1 class
Controllers:     1 class
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          5 files ~1,000 lines
```

---

## 🔗 Integration Points

### Data Sources
- **role-service**: Power, level updates
- **arena-service**: Arena points
- **wallet-service**: Wealth calculation
- **guild-service**: Guild rankings
- **pet-service**: Pet power
- **mount-service**: Mount power

### WebSocket Handler
- MSGID_1501_RANK_REQ

---

## ⚡ Performance

- **Redis caching**: Sub-millisecond reads
- **Top 100 only**: Limited data set
- **Auto-refresh**: Background updates
- **Indexed queries**: Fast sorting by score

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


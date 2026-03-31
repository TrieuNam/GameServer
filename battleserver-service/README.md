# BattleServer Service

**Version**: 1.0.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8082 · **gRPC**: 9082  
**Database**: `game_battle`

---

## 📋 Overview

BattleServer Service là **engine tính toán chiến đấu** — nhận thông tin 2 bên chiến đấu, chạy simulation theo formula, và trả về kết quả chi tiết (winner, damage, skills used, round-by-round log). Service này chỉ giao tiếp qua gRPC với webSocket-server.

### Core Features
- ✅ Battle simulation engine (auto-battle)
- ✅ Tính toán damage, skills, buffs, debuffs
- ✅ Round-by-round combat log
- ✅ Support nhiều loại battle: PvE, PvP, Boss
- ✅ gRPC server (port 9082)

---

## 🎯 Flow Chiến Đấu

```
[Player bắt đầu trận đấu]
webSocket-server (gRPC Client)
        │
        ▼
battleserver-service (gRPC Server :9082)
        │
        ▼
BattleEngine.runBattle(BattleRequest)
├── Load fighter stats: player attributes + equip + buffs
├── Load enemy stats: from config-service
├── Run rounds:
│   └── Per round:
│       ├── Calculate damage: ATK * (1 - DEF_RATIO) ± random
│       ├── Check crit: CRIT_RATE vs random
│       ├── Check dodge: DODGE_RATE vs random
│       ├── Apply skill effects
│       └── Check death
│
└── Return BattleResult {
      winner, rounds, totalDamage,
      roundLogs, rewards
    }
```

---

## 🗄️ Database Schema

### battle_record
```sql
CREATE TABLE battle_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    battle_type INT NOT NULL,         -- 1=PvE, 2=PvP, 3=Boss, 4=Trial
    attacker_id VARCHAR(50) NOT NULL,
    defender_id VARCHAR(50) NOT NULL,
    winner_id VARCHAR(50),
    total_rounds INT,
    attacker_damage BIGINT,
    defender_damage BIGINT,
    battle_log JSON,                  -- Round-by-round details
    created_at DATETIME NOT NULL
);
```

---

## 🔌 gRPC Interface

```protobuf
service BattleServerService {
  rpc RunBattle(BattleRequest) returns (BattleResult);
  rpc GetBattleRecord(BattleId) returns (BattleRecord);
}

message BattleRequest {
  string battle_id = 1;
  int32 battle_type = 2;
  FighterStats attacker = 3;
  FighterStats defender = 4;
}

message BattleResult {
  string winner_id = 1;
  int32 total_rounds = 2;
  repeated RoundLog round_logs = 3;
  bool attacker_win = 4;
}
```

---

## 🔧 Battle Formula

### Damage Calculation
```
base_damage = ATK * (1 - DEF / (DEF + 1000))
crit_damage = isCrit ? base_damage * CRIT_DMG_RATE : base_damage
final_damage = crit_damage * (1 - DAMAGE_REDUCTION)
```

### Battle Types
| Type | ID | Mô tả |
|------|----|----|
| PvE | 1 | Player vs Monster |
| PvP | 2 | Player vs Player |
| Boss | 3 | Player vs Boss |
| Trial | 4 | Trial dungeon |

---

## 🚀 Running

```bash
cd GameServer/battleserver-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### gRPC Server (port 9082)
- **webSocket-server**: Gọi để run battles
- **arena-service**: PvP battles
- **trial-service**: Trial battles

---

## 📊 Statistics

```
Entities:        1 class (BattleRecord)
Repositories:    1 interface
gRPC:            BattleServerGrpcImpl
Battle Engine:   Core combat logic
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~800 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


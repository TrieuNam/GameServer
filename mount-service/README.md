# Mount Service

**Version:** 2.0.0 (Phase 3 Complete)
**Port:** 8089
**Database:** game_mount

Mount/Cavalry system microservice for the game server with comprehensive power calculation, skill/buff system, and exploration features.

## 📋 Features

### Core Mount System
- **Mount Management**: Unlock, level up, grade up mounts
- **Mount Equipment**: Ride/unequip mounts with power calculation
- **Cosmetics**: Appearance and skin system
- **Star System**: Additional upgrade tier with power multipliers

### Advanced Features (Phase 3) ✨
- **Skill & Buff System**: Passive bonuses (PERSONAL/PARTY/GUILD)
  - Speed boosts, HP/ATK/DEF bonuses
  - Critical rate/damage enhancements
  - EXP/Gold/Drop rate bonuses
- **Enhanced Power Calculation**: Config-driven formulas with harness integration
- **Exploration System**:
  - 3 exploration types (Quick/Normal/Epic)
  - Level and grade-based rewards
  - Milestone rewards (every 1000 progress)
  - Mount skill bonuses apply to rewards
- **Role-Service Integration**: Auto-sync player capability on equip/unequip
- **Config Integration**: Redis-first caching with config-service fallback

### Harness System
- **Equipment**: 4 harness slots per mount
- **Random Attributes**: Up to 8 entry types with lock/refresh system
- **Operations**: Wear, remove, decompose, refresh entries
- **Shop System**: Buy harness with gold/diamonds

## API Endpoints

### Mount Operations (roleId)
- `GET /api/mount/{roleId}` - Get all mounts
- `POST /api/mount/{roleId}/levelup` - Level up mount
- `POST /api/mount/{roleId}/gradeup/{mountId}` - Grade up mount
- `POST /api/mount/{roleId}/appearance` - Set appearance
- `POST /api/mount/{roleId}/explore` - Mount exploration for rewards
- `POST /api/mount/{roleId}/pifu/set` - Set skin
- `POST /api/mount/{roleId}/pifu/upgrade` - Upgrade skin

### Mount Harness (roleId)
- `POST /api/mount/{roleId}/harness/wear` - Wear harness
- `POST /api/mount/{roleId}/harness/decompose` - Decompose harness
- `POST /api/mount/{roleId}/harness/unlock` - Unlock harness

### Mount Shop (roleId)
- `POST /api/mount/{roleId}/shop/open` - Open shop
- `POST /api/mount/{roleId}/shop/buy` - Buy from shop
- `POST /api/mount/{roleId}/shop/refresh` - Refresh shop
- `POST /api/mount/{roleId}/shop/refreshbuy` - Refresh and buy

### Harness Controller (userId)
- `GET /api/mount/harness/{userId}` - Get all harness
- `GET /api/mount/harness/{userId}/{harnessIndex}` - Get specific harness
- `GET /api/mount/harness/{userId}/{harnessIndex}/bonus` - Get harness bonus
- `GET /api/mount/harness/{userId}/hasspace` - Check harness space
- `POST /api/mount/harness/{userId}/add` - Add harness
- `POST /api/mount/harness/{userId}/wear` - Equip harness
- `DELETE /api/mount/harness/{userId}/wear` - Remove harness
- `DELETE /api/mount/harness/{userId}/{harnessIndex}/decompose` - Decompose harness
- `POST /api/mount/harness/{userId}/{harnessIndex}/refresh` - Refresh attributes
- `POST /api/mount/harness/{userId}/buy` - Buy harness
- `PUT /api/mount/harness/{userId}/{harnessIndex}/lock` - Set lock flag

## Database

- **mount**: Mount/cavalry data (level, grade, equipment slots)
- **mount_harness**: Harness items in bag (attributes, lock flags)

## Configuration

Port: 8089
Database: game_mount

## Protocol Messages

MsgIDs: 2140-2149
- 2140: Mount request
- 2141: Mount info
- 2142: Mount operation result
- 2143: Harness list info
- 2144: Single harness info
- 2145: Mount harness info

## Operations

- LEVEL_UP, GRADE_UP
- EXPLORE
- SET_APP (appearance), PIFU_UP (skin upgrade), SET_PIFU
- WEAR (harness), DECOMPOSE
- UNLOCK, ENTRY_REFRESH
- BUY, REFRESH_BUY, OPEN_BUY
- SET_LOCK_FLAG

## 🎯 Phase 3 Implementation Details

### Mount Skills
Skills unlock based on mount grade and star level:
- **Grade 1**: Speed Boost (10% SPD)
- **Grade 3**: Mount Vigor (+500 HP)
- **Grade 5**: Cavalry Charge (5% ATK)
- **Grade 5 + Star 3**: Battle Aura (3% ATK - PARTY)
- **Grade 7 + Star 5**: Fortune's Blessing (10% EXP)
- **Grade 10 + Star 8**: Guild Banner (5% DEF - GUILD)

High-tier mounts unlock additional skills:
- **Rare Mounts (ID ≥3)**: Treasure Hunter (15% Drop Rate)
- **Epic Mounts (ID ≥4)**: Critical Strike Mastery (10% Crit Rate), Deadly Precision (20% Crit DMG)

### Power Calculation Formula
```
Total Power = (Base Stats + Harness Stats) × Grade Multiplier × Star Multiplier

Base Stats = (HP × 1) + (ATK × 5) + (DEF × 3) + (SPD × 2) + (Skin Level × 50)
Grade Multiplier = 1.0 + (grade - 1) × 0.2  (20% per grade)
Star Multiplier = 1.0 + starLevel × 0.1    (10% per star)
```

### Exploration Rewards
**Base Rewards by Type:**
- Quick (Type 1): 100 progress, 500 exp, 1000 gold, 10 stamina cost
- Normal (Type 2): 200 progress, 1000 exp, 2000 gold, 20 stamina cost
- Epic (Type 3): 300 progress, 1500 exp, 3000 gold, 30 stamina cost

**Bonuses:**
- Level Bonus: +5 progress per mount level
- Grade Bonus: +10 progress per mount grade
- Mount Skills: EXP/Gold bonuses apply from active skills

**Milestone Rewards (Every 1000 progress):**
- 5000 gold
- 5 mount exp stones (item 1001)
- 2 grade stones (item 1002)
- 30% chance: 1 star stone (item 1003)

## 🔧 Configuration Files

### mount.json
```json
{
  "mountList": [
    {
      "mountId": 1,
      "name": "Basic Horse",
      "unlockCost": 1000,
      "baseAttributes": { "hp": 100, "attack": 50, "defense": 30, "speed": 20 },
      "growthRate": { "hpPerLevel": 10, "attackPerLevel": 5, "defensePerLevel": 3, "speedPerLevel": 2 }
    }
  ]
}
```

### harness.json
```json
{
  "harnessList": [
    {
      "itemId": 101,
      "quality": "Common",
      "baseAttributes": { "hp": 50, "attack": 25, "defense": 15, "speed": 10 },
      "buyCost": { "gold": 1000 },
      "upgradeCost": { "gold": 500, "materials": [{"itemId": 10020, "count": 2}] }
    }
  ]
}
```

## TODO

- [x] ~~Feign clients (config-service, role-service, bag-service, wallet-service)~~
- [x] ~~Config file loading (mount.json, harness.json)~~
- [x] ~~Complete business logic formulas~~
- [x] ~~Phase 1: Config integration & Redis caching~~
- [x] ~~Phase 2: Enhanced power calculations & role-service integration~~
- [x] ~~Phase 3: Mount skills, buffs, and exploration rewards~~
- [ ] WebSocket handler integration (MountHandler - pre-existing in webSocket-server)
- [ ] Unit and integration tests
- [ ] Performance optimization for high-concurrency scenarios

## 📊 Status

**Phase 1 (Foundation):** ✅ Complete
**Phase 2 (Core Logic):** ✅ Complete
**Phase 3 (Advanced Features):** ✅ Complete

**Last Updated:** 2026-04-08


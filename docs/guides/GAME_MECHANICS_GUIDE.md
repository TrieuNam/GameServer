# 🎮 GAME MECHANICS GUIDE - Chi Tiết Cách Game Hoạt Động

> **Tài liệu**: Giải thích chi tiết các cơ chế game  
> **Ngày**: 2026-01-19  
> **Mục đích**: Hiểu rõ cách người chơi tương tác với game, từ nhận đồ, đánh quái, đến tham gia sự kiện

---

## 📋 MỤC LỤC

1. [Tổng Quan Game](#tổng-quan-game)
2. [Hệ Thống Nhân Vật](#hệ-thống-nhân-vật)
3. [Hệ Thống Gacha/Roll Box](#hệ-thống-gacharoll-box)
4. [Hệ Thống Chiến Đấu](#hệ-thống-chiến-đấu)
5. [Hệ Thống Nhiệm Vụ](#hệ-thống-nhiệm-vụ)
6. [Hệ Thống Sự Kiện](#hệ-thống-sự-kiện)
7. [Hệ Thống Pet & Mount](#hệ-thống-pet--mount)
8. [Hệ Thống Kinh Tế](#hệ-thống-kinh-tế)
9. [Hệ Thống Xã Hội](#hệ-thống-xã-hội)
10. [Flow Chi Tiết](#flow-chi-tiết)

---

## 🎯 TỔNG QUAN GAME

### **Game Là Gì?**
Đây là một **MMO RPG** với các tính năng:
- 🗡️ **Combat**: Chiến đấu PvE (quái) và PvP (người với người)
- 🎰 **Gacha**: Mở hộp để nhận trang bị ngẫu nhiên
- 🐾 **Companions**: Pet, Mount, Angel đồng hành
- 👥 **Social**: Guild, friend, chat
- 🏆 **Competition**: Arena, leaderboard, territory war
- 🎯 **Progression**: Level up, nhiệm vụ, sự kiện

### **Cấu Trúc Game Loop**

```
┌─────────────────────────────────────────────────────────────┐
│                    GAME LOOP CHÍNH                          │
│                                                             │
│  1. Login → Nhận nhân vật                                  │
│  2. Làm nhiệm vụ hàng ngày (Daily Tasks)                   │
│  3. Đánh quái → Nhận exp + items                           │
│  4. Mở hộp (Gacha) → Nhận trang bị                         │
│  5. Tăng cường nhân vật (Equip, Pet, Angel...)             │
│  6. Tham gia PvP Arena                                      │
│  7. Guild activities (Donate, build, war)                  │
│  8. Events & Activities                                     │
│  9. Shop → Mua items với tiền game                         │
│  10. Lặp lại...                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 👤 HỆ THỐNG NHÂN VẬT

### **1. Tạo Nhân Vật**

**Flow**:
```
User → Chọn tên + server
  → Frontend: CreateRoleCtrl.SendCreateRole()
  → Backend: role-service.createRole()
     ├─ Validate tên unique
     ├─ Random stats ban đầu (HP, ATK, DEF)
     ├─ Set level = 1, exp = 0
     └─ Tạo record trong database
```

**Thuộc tính nhân vật**:
```yaml
Role:
  roleId: ULID unique ID
  userId: User account
  name: Tên nhân vật (2-20 ký tự)
  level: Level (1-200)
  exp: Experience points
  vip: VIP level (0-15)
  
  Attributes:
    hp: Health points
    atk: Attack
    def: Defense
    crit: Critical rate
    dodge: Dodge rate
    speed: Speed
    
  Combat Power:
    capability: Tổng sức mạnh (tính từ stats + equip + pets)
```

### **2. Level Up System**

**Cách nhận EXP**:
1. Đánh quái/dungeon
2. Hoàn thành nhiệm vụ
3. Tham gia sự kiện
4. Mở hộp (một số loại)

**Auto Level Up**:
```java
// role-service: Tự động level up khi đủ exp
public void addExp(String roleId, int expToAdd) {
    Role role = roleRepository.findById(roleId);
    role.exp += expToAdd;
    
    // Auto level up nếu đủ exp
    while (role.exp >= getExpRequiredForNextLevel(role.level)) {
        role.exp -= getExpRequiredForNextLevel(role.level);
        role.level++;
        
        // Tăng stats theo level
        role.hp += 100 * role.level;
        role.atk += 10 * role.level;
        role.def += 8 * role.level;
    }
    
    // Recalculate combat power
    role.capability = calculateTotalPower(role);
}
```

**Scaling công thức**:
- EXP cần cho level N: `100 * N^2`
- HP mỗi level: `+100 * level`
- ATK mỗi level: `+10 * level`
- DEF mỗi level: `+8 * level`

---

## 🎰 HỆ THỐNG GACHA/ROLL BOX

### **1. Gacha Là Gì?**

**Gacha** (开箱 - Khai Tương) là hệ thống mở hộp để nhận trang bị/vật phẩm ngẫu nhiên.

### **2. Các Loại Hộp**

```yaml
Box Types:
  1. Normal Box (普通宝箱):
     Cost: 50 gold
     Rewards: Trang bị white/green (70%/30%)
     
  2. Silver Box (白银宝箱):
     Cost: 100 gold
     Rewards: Green/Blue (60%/40%)
     
  3. Gold Box (黄金宝箱):
     Cost: 200 gold hoặc 10 diamond
     Rewards: Blue/Purple (50%/50%)
     
  4. Diamond Box (钻石宝箱):
     Cost: 50 diamond
     Rewards: Purple/Orange (80%/20%)
     Pity: 10 lần → guarantee orange
```

### **3. Flow Mở Hộp Chi Tiết**

```
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 1: User Click "Open Box"                              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 2: Frontend (BoxCtrl.ts)                              │
│                                                              │
│  BoxCtrl.SendBoxReq(BoxReqType.OPEN_BOX, mode)             │
│    mode = 0: Mở 1 lần                                       │
│    mode = 1: Mở 5 lần (batch)                               │
│                                                              │
│  → Tạo protobuf: PB_CSBoxReq                                │
│  → Send via WebSocket                                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 3: WebSocket Gateway (BoxHandler.java)                │
│                                                              │
│  1. Decode message (msgId = 1610)                           │
│  2. Deserialize PB_CSBoxReq                                  │
│  3. Call box-service via Feign                              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 4: box-service Business Logic                         │
│                                                              │
│  BoxService.openBox():                                       │
│                                                              │
│  4.1. Check wallet balance                                   │
│       wallet-service.getBalance(userId)                      │
│       → Nếu không đủ tiền: throw InsufficientFundsException│
│                                                              │
│  4.2. Deduct cost                                            │
│       wallet-service.debit(userId, cost, "open_box")        │
│                                                              │
│  4.3. Roll rewards from drop-service                         │
│       DropRollRequest req = {                                │
│         tableId: 101, // box drop table                     │
│         userId: userId,                                      │
│         pityCounter: user's current pity                    │
│       }                                                      │
│       DropRollResponse = drop-service.rollDropTable(req)    │
│                                                              │
│       → drop-service làm gì?                                 │
│         • Load drop table từ config                         │
│         • Weighted random theo tỷ lệ                        │
│         • Check pity counter (guarantee)                    │
│         • Generate item list với quality/quantity           │
│                                                              │
│  4.4. Grant items to bag                                     │
│       bag-service.grantItems(userId, items, "box_open")     │
│       → Thêm items vào inventory của user                   │
│                                                              │
│  4.5. Save box opening record                                │
│       BoxRecord record = {                                   │
│         userId, mode, rewards, openTime                     │
│       }                                                      │
│       boxRepository.save(record)                             │
│                                                              │
│  4.6. Return response                                         │
│       return BoxOpenResponse {                               │
│         result: "SUCCESS",                                   │
│         rewards: List<Item>                                  │
│       }                                                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  BƯỚC 5: Response trở về Frontend                           │
│                                                              │
│  BoxHandler converts DTO → Protobuf (PB_SCBoxInfo)          │
│  → Send via WebSocket (msgId = 1616)                        │
│                                                              │
│  Frontend: BoxCtrl.recvBoxInfo()                            │
│  → Update BoxData model                                      │
│  → Trigger UI animation (show rewards)                      │
│  → Update inventory UI                                       │
└──────────────────────────────────────────────────────────────┘
```

### **4. Drop Table & RNG System**

**Drop Table Config** (`drop.xml`):
```xml
<DropTable id="101" name="Gold Box">
  <Group weight="50">
    <!-- 50% chance: Blue equipment -->
    <Item id="1001" quality="3" min="1" max="1" weight="100"/>
  </Group>
  
  <Group weight="40">
    <!-- 40% chance: Purple equipment -->
    <Item id="1002" quality="4" min="1" max="1" weight="80"/>
    <Item id="1003" quality="4" min="1" max="1" weight="20"/>
  </Group>
  
  <Group weight="10">
    <!-- 10% chance: Orange equipment (rare) -->
    <Item id="1004" quality="5" min="1" max="1" weight="100"/>
  </Group>
</DropTable>
```

**Pity System** (Guarantee):
```java
// drop-service: Pity counter logic
public DropRollResponse rollDropTable(DropRollRequest req) {
    UserPityCounter pity = pityRepository.findByUserId(req.userId);
    
    // Nếu đã roll 9 lần không có orange → lần 10 guarantee
    if (pity.count >= 9) {
        // Force drop orange item
        pity.count = 0; // Reset counter
        return guaranteeOrangeItem(req.tableId);
    }
    
    // Normal weighted random
    List<Item> items = weightedRandom(req.tableId);
    
    // Check if got orange
    boolean gotOrange = items.stream()
        .anyMatch(item -> item.quality == 5);
    
    if (gotOrange) {
        pity.count = 0; // Reset
    } else {
        pity.count++; // Increment
    }
    
    pityRepository.save(pity);
    return new DropRollResponse(items);
}
```

### **5. Item Quality Tiers**

```yaml
Quality Tiers:
  1 - White (Common):
     Drop rate: 60%
     Stats: +10 ATK, +50 HP
     
  2 - Green (Uncommon):
     Drop rate: 25%
     Stats: +20 ATK, +100 HP
     
  3 - Blue (Rare):
     Drop rate: 10%
     Stats: +40 ATK, +200 HP
     
  4 - Purple (Epic):
     Drop rate: 4%
     Stats: +80 ATK, +400 HP
     
  5 - Orange (Legendary):
     Drop rate: 1% (với pity guarantee)
     Stats: +160 ATK, +800 HP
```

---

## ⚔️ HỆ THỐNG CHIẾN ĐẤU

### **1. Các Loại Combat**

```yaml
Combat Types:
  PVE_NORMAL:      # Đánh quái thường
  PVE_DUNGEON:     # Đánh dungeon (副本)
  PVE_BOSS:        # Đánh boss
  PVP_ARENA:       # PvP arena (竞技场)
  PVP_TERRITORY:   # Tranh giành lãnh thổ
  PVP_GUILD_WAR:   # Chiến tranh guild
```

### **2. Flow Đánh Quái (PvE Dungeon)**

```
┌──────────────────────────────────────────────────────────────┐
│  STEP 1: User chọn dungeon và click "Enter"                │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 2: Frontend (DungeonCtrl.ts)                          │
│                                                              │
│  DungeonCtrl.SendCSLingZhuReq(type=Fight, stage)           │
│    → Gọi BattleCtrl.reqFight(HERO_BATTLE_TYPE_LINGZHU)     │
│                                                              │
│  BattleCtrl tạo battle request:                             │
│    PB_CSBattleReq {                                          │
│      type: LINGZHU_OP_TYPE.Fight,                           │
│      p1: stage (stage nào),                                  │
│      p2: userId                                              │
│    }                                                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 3: WebSocket → battle-service                         │
│                                                              │
│  Battle Service:                                             │
│  1. Load player stats (role-service)                        │
│     → HP, ATK, DEF, Skills, Equipment                       │
│                                                              │
│  2. Load monster stats (monster-service)                    │
│     → Monster HP, ATK, Skills, AI behavior                  │
│                                                              │
│  3. Load dungeon config                                      │
│     → Wave count, monster groups, rewards                   │
│                                                              │
│  4. Initialize battle session                                │
│     BattleSession {                                          │
│       battleId: UUID                                         │
│       players: [player stats]                               │
│       monsters: [monster stats]                             │
│       battleType: PVE_DUNGEON                               │
│       status: INITIALIZING                                  │
│     }                                                        │
│                                                              │
│  5. Return battle file URL                                   │
│     PB_SCBattleReport {                                      │
│       battleModeType: HERO_BATTLE_TYPE_LINGZHU              │
│       battleFileName: "1675231914_0_2"                      │
│       url: "http://server/fightdata/pve/..."               │
│     }                                                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 4: Frontend download battle file                      │
│                                                              │
│  BattleCtrl.recvResult(data: PB_SCBattleReport)            │
│    → Download battle JSON từ URL                            │
│    → Parse battle data                                       │
│    → Load BattleScene                                        │
│                                                              │
│  Battle JSON format:                                         │
│  {                                                           │
│    "frames": [                                               │
│      {                                                       │
│        "time": 0,                                            │
│        "actions": [                                          │
│          {"actor": "player", "skill": 1001, "target": "monster1"},│
│          {"actor": "monster1", "skill": 2001, "target": "player"}│
│        ],                                                    │
│        "damages": [                                          │
│          {"target": "monster1", "damage": 150, "crit": true},│
│          {"target": "player", "damage": 50, "blocked": 20}  │
│        ],                                                    │
│        "states": [                                           │
│          {"entity": "player", "hp": 950/1000},               │
│          {"entity": "monster1", "hp": 350/500}              │
│        ]                                                     │
│      },                                                      │
│      ... more frames ...                                     │
│    ],                                                        │
│    "result": "WIN",                                          │
│    "rewards": [                                              │
│      {"itemId": 1001, "count": 5},                          │
│      {"gold": 100},                                          │
│      {"exp": 500}                                            │
│    ]                                                         │
│  }                                                           │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 5: Play battle animation                              │
│                                                              │
│  BattleView renders:                                         │
│    → Hiển thị character và monsters                         │
│    → Play từng frame theo timeline                          │
│    → Show damage numbers (-150, CRIT!)                      │
│    → Show skill effects/animations                          │
│    → Update HP bars                                          │
│    → Show battle result (WIN/LOSE)                          │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 6: Grant rewards                                       │
│                                                              │
│  battle-service.completeBattle():                           │
│    1. Validate battle result                                 │
│    2. Call reward services:                                  │
│       → role-service.addExp(userId, 500)                    │
│       → wallet-service.credit(userId, gold: 100)            │
│       → bag-service.grantItems(userId, items)               │
│       → drop-service.rollDropTable(dungeonRewards)          │
│    3. Update dungeon progress                                │
│       → dungeon-service.updateProgress(stage)               │
│    4. Publish event:                                         │
│       → Kafka: gameh5.dungeon.cleared                       │
└──────────────────────────────────────────────────────────────┘
```

### **3. Combat Calculation (Server-Authoritative)**

**Damage Formula**:
```java
// battle-service: Damage calculation
public int calculateDamage(Attacker attacker, Defender defender, Skill skill) {
    // Base damage
    int baseDamage = attacker.atk * skill.damageMultiplier;
    
    // Defense reduction
    int defense = defender.def;
    double reduction = defense / (defense + 100.0);
    int damageAfterDef = (int)(baseDamage * (1 - reduction));
    
    // Critical hit (2x damage)
    boolean isCrit = random.nextDouble() < attacker.critRate;
    if (isCrit) {
        damageAfterDef *= 2;
    }
    
    // Dodge check
    boolean isDodge = random.nextDouble() < defender.dodgeRate;
    if (isDodge) {
        return 0; // Miss
    }
    
    // Minimum damage = 1
    return Math.max(1, damageAfterDef);
}
```

**Combat Turn System**:
```
1. Calculate turn order by speed
   → Faster character attacks first
   
2. Each turn:
   → Attacker selects skill
   → Calculate damage
   → Apply buffs/debuffs
   → Check death
   → Next turn
   
3. Win conditions:
   PvE: All monsters dead
   PvP: Enemy HP = 0 or timeout
   
4. Loss conditions:
   PvE: Player HP = 0
   PvP: Player HP = 0 or lower damage
```

### **4. PvP Arena System**

**Matchmaking**:
```java
// arena-service: ELO-based matchmaking
public ArenaMatch findOpponent(String roleId) {
    Role player = roleService.getRole(roleId);
    int playerElo = player.arenaRating; // e.g. 1500
    
    // Find opponent within ±200 ELO
    List<Role> candidates = roleRepository.findByEloRange(
        playerElo - 200, 
        playerElo + 200
    );
    
    // Filter: not in cooldown, not offline
    candidates = candidates.stream()
        .filter(c -> c.lastArenaTime + 60000 < now())
        .filter(c -> c.online)
        .collect(Collectors.toList());
    
    // Random select
    Role opponent = candidates.get(random.nextInt(candidates.size()));
    
    // Create match
    ArenaMatch match = new ArenaMatch(player, opponent);
    match.status = MatchStatus.STARTING;
    
    // Call battle-service
    BattleResult result = battleService.startPvpBattle(
        player.roleId, 
        opponent.roleId,
        BattleType.PVP_ARENA
    );
    
    // Update ELO
    updateEloRatings(player, opponent, result);
    
    // Grant rewards
    if (result.winner == player.roleId) {
        walletService.credit(player.userId, arenaWinReward);
    }
    
    return match;
}
```

**ELO Rating**:
```
Win: +20-30 ELO (tùy opponent)
Loss: -10-15 ELO
Draw: ±0 ELO

Season reset: Monthly
Rewards: Top 100 nhận quà đặc biệt
```

---

## 🎯 HỆ THỐNG NHIỆM VỤ

### **1. Loại Nhiệm Vụ**

```yaml
Task Types:
  MAIN_STORY:
    - Nhiệm vụ chính của game
    - Unlock theo level
    - Rewards: Exp, items, unlock features
    
  DAILY:
    - Reset mỗi ngày 00:00
    - Example: "Đánh 10 quái", "Mở 5 hộp"
    - Rewards: Gold, exp, daily points
    
  WEEKLY:
    - Reset mỗi Chủ Nhật
    - Example: "Tham gia 20 trận PvP"
    - Rewards: Lớn hơn daily
    
  ACHIEVEMENT:
    - Permanent, không reset
    - Example: "Reach level 100", "Collect 50 pets"
    - Rewards: Titles, rare items
```

### **2. Flow Làm Nhiệm Vụ**

```
┌──────────────────────────────────────────────────────────────┐
│  STEP 1: Auto-accept daily tasks on login                  │
│                                                              │
│  task-service.onPlayerLogin(roleId):                        │
│    → Load today's daily tasks from config                   │
│    → Auto-accept if not started                             │
│    → Send task list to client                               │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 2: Player thực hiện activities                        │
│                                                              │
│  Example: Task "Kill 10 monsters"                           │
│                                                              │
│  battle-service.onMonsterKilled(roleId, monsterId):        │
│    → Publish event: gameh5.monster.killed                   │
│                                                              │
│  task-service listens to Kafka event:                       │
│    onMonsterKilled(event) {                                 │
│      UserTaskProgress progress = getProgress(roleId, taskId);│
│      progress.count++;                                       │
│      progress.currentProgress = progress.count;              │
│                                                              │
│      if (progress.count >= task.requirement) {               │
│        progress.status = TaskStatus.COMPLETED;              │
│        // Notify client                                      │
│        websocket.send(PB_SCTaskUpdate);                      │
│      }                                                       │
│    }                                                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 3: Claim rewards                                       │
│                                                              │
│  User clicks "Claim" button                                  │
│  → Frontend: TaskCtrl.SendFetchTaskRewardReq(taskId)       │
│  → Backend: task-service.claimReward(taskId)                │
│                                                              │
│  task-service.claimReward():                                │
│    1. Validate task completed                                │
│    2. Check not already claimed                              │
│    3. Grant rewards:                                         │
│       for (Reward reward : task.rewards) {                  │
│         switch(reward.type) {                                │
│           case EXP:                                          │
│             role-service.addExp(roleId, reward.amount);     │
│             break;                                           │
│           case CURRENCY:                                     │
│             wallet-service.credit(userId, reward.amount);   │
│             break;                                           │
│           case ITEM:                                         │
│             bag-service.grantItems(userId, reward.items);   │
│             break;                                           │
│         }                                                    │
│       }                                                      │
│    4. Mark as claimed                                        │
│    5. Publish: gameh5.task.claimed                          │
└──────────────────────────────────────────────────────────────┘
```

### **3. Task Config Example**

```json
{
  "taskId": 1001,
  "type": "DAILY",
  "name": "Daily Grind",
  "description": "Kill 10 monsters",
  "requirements": [
    {
      "type": "KILL_MONSTER",
      "targetId": 0,
      "count": 10
    }
  ],
  "rewards": [
    {
      "type": "EXP",
      "amount": 500
    },
    {
      "type": "CURRENCY",
      "currencyType": "gold",
      "amount": 1000
    },
    {
      "type": "ITEM",
      "itemId": 1001,
      "count": 5
    }
  ],
  "autoAccept": true
}
```

---

## 🎊 HỆ THỐNG SỰ KIỆN

### **1. Loại Events**

```yaml
Event Types:
  TIME_LIMITED:
    - Có thời gian bắt đầu và kết thúc
    - Example: "Double EXP Weekend"
    - Duration: 3-7 days
    
  SEASONAL:
    - Events theo mùa (Tết, Giáng Sinh...)
    - Special rewards
    - Duration: 2-4 weeks
    
  PROGRESSION:
    - Login streak, level up milestones
    - "Login 7 days → nhận quà"
    - Persistent
    
  COMPETITIVE:
    - Ranking-based (top 100)
    - "Ai đánh nhiều quái nhất"
    - Leaderboard prizes
```

### **2. Flow Tham Gia Event**

```
┌──────────────────────────────────────────────────────────────┐
│  STEP 1: Event starts (scheduled)                           │
│                                                              │
│  event-service có Cron Job check mỗi 5 phút:                │
│    @Scheduled(cron = "0 */5 * * * *")                       │
│    public void checkEventSchedule() {                        │
│      List<Event> events = eventRepository                    │
│        .findByStartTimeBetween(now, now + 5min);            │
│                                                              │
│      for (Event event : events) {                           │
│        startEvent(event);                                    │
│      }                                                       │
│    }                                                         │
│                                                              │
│  startEvent():                                               │
│    1. Update event.status = ACTIVE                           │
│    2. Publish: gameh5.event.started                         │
│    3. Send notification to all online players               │
│       → websocket.broadcast(PB_SCEventNotice)              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 2: Player tham gia event                              │
│                                                              │
│  Frontend: ActivityCtrl.SendAngelReq(activity_type, ...)   │
│  Backend: event-service.participate(eventId, roleId)        │
│                                                              │
│  event-service.participate():                               │
│    1. Check event is active                                  │
│    2. Check player eligible (level, VIP...)                 │
│    3. Create participation record                            │
│    4. Initialize player's event progress                     │
│       EventProgress {                                        │
│         eventId, roleId,                                     │
│         points: 0,                                           │
│         milestones: [],                                      │
│         rank: 0                                              │
│       }                                                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 3: Earn event points                                  │
│                                                              │
│  Example Event: "Kill 100 monsters in 3 days"               │
│                                                              │
│  battle-service kills monster:                              │
│    → Publish: gameh5.monster.killed                         │
│                                                              │
│  event-service listens:                                     │
│    onMonsterKilled(event) {                                 │
│      EventProgress progress = getProgress(roleId, eventId); │
│      progress.points += 10; // 10 points per kill           │
│                                                              │
│      // Check milestones                                     │
│      for (Milestone m : eventConfig.milestones) {           │
│        if (progress.points >= m.requirement                 │
│            && !progress.claimed.contains(m.id)) {           │
│          // Notify milestone reached                        │
│          websocket.send(PB_SCEventMilestone);              │
│        }                                                     │
│      }                                                       │
│                                                              │
│      // Update leaderboard                                   │
│      leaderboard-service.updateRank(eventId, roleId, points);│
│    }                                                         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 4: Claim milestone rewards                            │
│                                                              │
│  User clicks milestone reward:                               │
│  → event-service.claimMilestone(eventId, milestoneId)      │
│                                                              │
│  claimMilestone():                                          │
│    1. Validate milestone reached                             │
│    2. Check not already claimed                              │
│    3. Grant rewards (similar to task rewards)               │
│    4. Mark milestone as claimed                              │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 5: Event ends (scheduled)                             │
│                                                              │
│  Cron job checks event end time:                             │
│    endEvent(eventId):                                       │
│      1. Update event.status = ENDED                          │
│      2. Calculate final rankings                             │
│      3. Distribute ranking rewards:                          │
│         List<EventParticipant> topPlayers =                 │
│           leaderboard.getTop100(eventId);                   │
│                                                              │
│         for (EventParticipant p : topPlayers) {             │
│           Reward reward = calculateRankReward(p.rank);      │
│           grantReward(p.roleId, reward);                    │
│           // Send mail with rewards                          │
│           mail-service.sendMail(p.userId, reward);          │
│         }                                                    │
│      4. Publish: gameh5.event.ended                         │
│      5. Archive event data                                   │
└──────────────────────────────────────────────────────────────┘
```

### **3. Event Config Example**

```json
{
  "eventId": 2001,
  "type": "TIME_LIMITED",
  "name": "Monster Slayer Weekend",
  "description": "Kill as many monsters as you can!",
  "startTime": "2026-01-20T00:00:00Z",
  "endTime": "2026-01-22T23:59:59Z",
  
  "eligibility": {
    "minLevel": 10,
    "vipLevel": 0
  },
  
  "scoring": {
    "monsterKill": 10,
    "bossKill": 50,
    "dungeonClear": 100
  },
  
  "milestones": [
    {
      "id": 1,
      "requirement": 100,
      "rewards": [
        {"type": "ITEM", "itemId": 1001, "count": 10}
      ]
    },
    {
      "id": 2,
      "requirement": 500,
      "rewards": [
        {"type": "ITEM", "itemId": 1002, "count": 1},
        {"type": "CURRENCY", "gold": 5000}
      ]
    },
    {
      "id": 3,
      "requirement": 1000,
      "rewards": [
        {"type": "ITEM", "itemId": 1003, "count": 1, "quality": 5}
      ]
    }
  ],
  
  "rankingRewards": {
    "rank1-10": [
      {"type": "ITEM", "itemId": 9999, "count": 1, "rarity": "legendary"}
    ],
    "rank11-50": [
      {"type": "ITEM", "itemId": 9998, "count": 1, "rarity": "epic"}
    ],
    "rank51-100": [
      {"type": "CURRENCY", "diamond": 500}
    ]
  }
}
```

---

## 🐾 HỆ THỐNG PET & MOUNT

### **1. Pet System**

**Pet là gì?**
- Thú cưng đồng hành chiến đấu
- Có stats riêng (HP, ATK, DEF)
- Có skill (4 slots)
- Có thể evolution (5 tiers)

**Flow Nhận Pet**:
```
1. Nhận pet từ:
   - Quest rewards
   - Gacha (pet box)
   - Event
   - Shop

2. pet-service.addPet(userId, petId):
   → Tạo Pet entity với stats ban đầu
   → Generate pet_index (unique)
   → Save to database

3. Frontend receives pet:
   → Show "New Pet" animation
   → Add to pet collection UI
```

**Flow Level Up Pet**:
```
User có materials → Click "Level Up"
  → pet-service.levelUp(petIndex):
     1. Check materials sufficient
     2. Consume materials (bag-service)
     3. Increase pet.level
     4. Increase pet stats:
        pet.hp += 50 * level
        pet.atk += 5 * level
     5. Recalculate combat power
     6. Save and return updated pet
```

**Pet Evolution**:
```
Pet có 5 tiers (orders):
  Order 1 → Order 2 → Order 3 → Order 4 → Order 5

Evolution requirements:
  - Max level current order
  - Special evolution materials
  - Gold cost

pet-service.evolve(petIndex):
  1. Validate max level
  2. Check materials
  3. Consume materials + gold
  4. Increase order
  5. Reset level to 1 (nhưng giữ base stats)
  6. Unlock new skill slot
  7. Tăng appearance (new model)
```

**Pet Combat**:
```
Player có thể set 2 pets làm "fighting pets"
  → pet-service.setFightPet(petIndex, slot)
  → slot = 0 or 1

Trong battle:
  - Pet stats cộng vào player stats
  - Pet skills available to use
  - Pet có thể chết (revive sau battle)
```

### **2. Mount System**

**Mount là gì?**
- Phương tiện di chuyển (ngựa, rồng...)
- Tăng speed + stats
- Có harness (yên ngựa) cho thêm buffs

**Flow:**
```
1. Nhận mount từ quest/shop
2. mount-service.addMount(userId, mountId)
3. User có thể:
   - Level up mount (tăng speed)
   - Grade up (tier)
   - Equip harness (4 attributes: ATK, DEF, HP, SPD)
4. Set active mount
   → Tăng player move speed
   → Buffs apply to player
```

---

## 💰 HỆ THỐNG KINH TẾ

### **1. Các Loại Tiền Tệ**

```yaml
Currencies:
  Gold (金币):
    - Soft currency
    - Earn: Quái, nhiệm vụ, bán đồ
    - Use: Shop, upgrade, crafting
    
  Diamond (钻石):
    - Hard currency (IAP - In-App Purchase)
    - Earn: Events, first charge, VIP
    - Use: Premium boxes, skip time, revive
    
  Points (积分):
    - Event currency
    - Earn: Event activities
    - Use: Event shop
    
  Pay Gold (付费金币):
    - Premium soft currency
    - Earn: Convert from diamond
    - Use: Special purchases
```

### **2. Flow Mua Hàng Trong Shop**

```
┌──────────────────────────────────────────────────────────────┐
│  STEP 1: User browse shop                                   │
│                                                              │
│  shop-service.getCatalog():                                 │
│    → Load shop config from config-service                   │
│    → Apply discounts/sales if active                        │
│    → Return shop items with prices                           │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 2: User clicks "Buy"                                  │
│                                                              │
│  Frontend: ShopCtrl.SendCSShopBuyReq(itemId, quantity)     │
│  Backend: shop-service.buy(userId, itemId, qty)            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 3: shop-service validates and processes               │
│                                                              │
│  shop-service.buy():                                        │
│    1. Get item info from item-service                       │
│    2. Calculate total price                                  │
│       totalPrice = itemPrice * quantity                     │
│                                                              │
│    3. Check purchase limits (daily/weekly)                   │
│       if (userPurchases.today >= dailyLimit)               │
│         throw PurchaseLimitException                         │
│                                                              │
│    4. Validate wallet balance                                │
│       WalletDTO wallet = wallet-service.getBalance(userId); │
│       if (wallet.gold < totalPrice)                         │
│         throw InsufficientFundsException                     │
│                                                              │
│    5. START TRANSACTION (critical!)                          │
│       try {                                                  │
│         // Deduct payment                                    │
│         wallet-service.debit(                                │
│           userId, totalPrice, "shop_purchase",              │
│           idempotencyKey                                    │
│         );                                                   │
│                                                              │
│         // Grant items                                       │
│         bag-service.grantItems(                              │
│           userId, itemId, qty, "shop_purchase",             │
│           idempotencyKey                                    │
│         );                                                   │
│                                                              │
│         // Update purchase record                            │
│         ShopPurchase record = new ShopPurchase(             │
│           userId, itemId, qty, totalPrice, now()            │
│         );                                                   │
│         shopRepository.save(record);                         │
│                                                              │
│         // Publish event                                     │
│         kafka.send("gameh5.shop.purchase", record);         │
│                                                              │
│         return PurchaseResponse.success(itemId, qty);       │
│                                                              │
│       } catch (Exception e) {                               │
│         // Rollback if any step fails                        │
│         log.error("Purchase failed", e);                    │
│         return PurchaseResponse.error(e.getMessage());      │
│       }                                                      │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  STEP 4: Update UI                                           │
│                                                              │
│  Frontend receives success:                                  │
│    → Deduct gold from wallet UI                             │
│    → Add items to bag UI                                     │
│    → Show "Purchase successful" toast                       │
│    → Play item acquisition animation                         │
└──────────────────────────────────────────────────────────────┘
```

### **3. Idempotency (Tránh Mua Trùng)**

```java
// wallet-service: Idempotent debit
public DebitResponse debit(DebitRequest req) {
    String idempotencyKey = req.getIdempotencyKey();
    
    // Check if already processed
    WalletTransaction existing = 
        transactionRepository.findByIdempotencyKey(idempotencyKey);
    
    if (existing != null) {
        // Already processed → return same result
        return DebitResponse.fromTransaction(existing);
    }
    
    // Process new transaction
    Wallet wallet = walletRepository.findByUserId(req.getUserId());
    wallet.gold -= req.getAmount();
    
    WalletTransaction tx = new WalletTransaction(
        req.getUserId(),
        req.getAmount(),
        req.getReason(),
        idempotencyKey,
        now()
    );
    transactionRepository.save(tx);
    walletRepository.save(wallet);
    
    return DebitResponse.success(tx);
}
```

---

## 👥 HỆ THỐNG XÃ HỘI

### **1. Guild System**

**Guild Features**:
- 50 members max
- Ranks: Leader, Officer, Member
- Shared resources (guild gold)
- Guild buildings (donate để upgrade)
- Guild wars (Territory)

**Flow Tạo Guild**:
```
User clicks "Create Guild"
  → guild-service.createGuild(name, roleId):
     1. Validate name unique (2-20 chars)
     2. Check cost (1000 gold)
     3. Deduct gold from wallet
     4. Create Guild entity:
        Guild {
          guildId: UUID
          name: unique
          leaderId: roleId
          level: 1
          members: 1
          gold: 0
          buildings: []
        }
     5. Add creator as leader
     6. Publish: gameh5.guild.created
```

**Guild Contribution**:
```
Member clicks "Donate"
  → guild-service.contribute(guildId, roleId, amount):
     1. Deduct from player wallet
     2. Add to guild.gold
     3. Add player.contributionPoints
     4. Update guild leaderboard
     5. Check building upgrade requirements
```

**Guild War**:
```
Guild leader declares war on territory
  → territory-service.declareWar(guildId, territoryId):
     1. Schedule war time (e.g. Saturday 20:00)
     2. Notify both guilds
     3. On war time:
        - All guild members can join
        - Capture territory = gain buffs
        - PvP combat between guilds
     4. Winner gets:
        - Territory control (1 week)
        - Guild buffs (+10% exp, +5% drop rate)
        - Weekly rewards
```

### **2. Friend System**

```
Add friend flow:
  1. Search player by name/ID
  2. Send invite: friend-service.sendInvite(roleId, targetId)
  3. Target receives notification
  4. Target accepts: friend-service.acceptInvite(inviteId)
  5. Both become friends
  
Friend features:
  - Chat directly
  - See online status
  - View profile/equipment
  - Send gifts
```

### **3. Chat System**

```yaml
Chat Channels:
  World:
    - Everyone can see
    - Rate limit: 1 msg/10 seconds
    - Profanity filter active
    
  Guild:
    - Only guild members
    - No rate limit
    
  Private:
    - 1-on-1 chat
    - Friends or recent players
    
  System:
    - Server announcements
    - Maintenance notices
```

---

## 🔄 FLOW CHI TIẾT - TỔNG HỢP

### **Typical Player Session Flow**

```
┌─────────────────────────────────────────────────────────────┐
│  1. LOGIN                                                   │
│     → session-service validates JWT                        │
│     → Load character from role-service                     │
│     → Load inventory from bag-service                      │
│     → Load wallet from wallet-service                      │
│     → Load active pets from pet-service                    │
│     → Load daily tasks from task-service                   │
│     → Auto-accept daily tasks                              │
│     → Check for new mail                                    │
│     → Display to UI                                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  2. DAILY ACTIVITIES                                        │
│                                                             │
│  Morning (08:00-12:00):                                     │
│    → Complete daily tasks                                   │
│    → Dungeon runs (3x daily limit)                         │
│    → Pet training                                           │
│                                                             │
│  Afternoon (12:00-18:00):                                   │
│    → Arena matches (5x daily limit)                        │
│    → Guild contribution                                     │
│    → Event participation                                    │
│                                                             │
│  Evening (18:00-22:00):                                     │
│    → Territory war (if scheduled)                           │
│    → World boss (20:00)                                     │
│    → Guild activities                                       │
│                                                             │
│  Night (22:00-00:00):                                       │
│    → Shop purchases                                         │
│    → Gacha rolls                                            │
│    → Equipment upgrade                                      │
│    → Claim rewards                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  3. PROGRESSION CYCLE                                       │
│                                                             │
│  Earn Resources:                                            │
│    ├─ Gold: Quái, nhiệm vụ, bán đồ                        │
│    ├─ Diamond: Events, nạp tiền                            │
│    ├─ Exp: Đánh quái, nhiệm vụ                            │
│    └─ Items: Drop, gacha, rewards                         │
│                                                             │
│  Spend Resources:                                           │
│    ├─ Upgrade equipment                                     │
│    ├─ Level up pets                                        │
│    ├─ Buy from shop                                        │
│    ├─ Gacha rolls                                          │
│    └─ Guild donations                                       │
│                                                             │
│  Get Stronger:                                              │
│    ├─ Higher combat power                                   │
│    ├─ Win more battles                                     │
│    ├─ Higher arena rank                                    │
│    └─ Unlock harder content                                │
└─────────────────────────────────────────────────────────────┘
```

### **Example: Full Gacha → Equip → Battle Flow**

```
┌─────────────────────────────────────────────────────────────┐
│  10:00 AM - Player has 500 diamond                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  10:01 AM - Mở Diamond Box (x10 batch)                     │
│                                                             │
│  box-service.openBox(mode=10x):                            │
│    1. Deduct 500 diamond                                    │
│    2. Roll 10 times from drop table                         │
│    3. Pity check (9th roll guarantee orange)               │
│    4. Results:                                              │
│       - 5x Blue weapons (+40 ATK each)                     │
│       - 3x Purple armor (+80 DEF each)                     │
│       - 2x Orange legendary sword (+160 ATK)               │
│    5. Grant all to bag                                      │
│    6. Show results to UI with fancy animation              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  10:02 AM - Equip orange sword                             │
│                                                             │
│  equip-service.wear(itemId=legendaryServSword):            │
│    1. Validate item is equipment                            │
│    2. Check slot (weapon slot)                              │
│    3. Unequip current weapon (if any)                       │
│    4. Equip new sword                                       │
│    5. Recalculate stats:                                    │
│       player.atk: 500 → 660 (+160)                         │
│       player.capability: 5000 → 5800 (+800)                │
│    6. Update UI (show new ATK)                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  10:05 AM - Enter dungeon with new gear                    │
│                                                             │
│  dungeon-service.enter(stage=10):                          │
│    1. Load player stats (with new sword)                   │
│    2. Load monsters for stage 10                            │
│    3. Generate battle                                       │
│    4. Send battle file to client                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  10:06 AM - Battle animation plays                         │
│                                                             │
│  Battle sequence:                                           │
│    Turn 1:                                                  │
│      Player attacks monster (660 ATK)                      │
│      → Damage: 450 (CRIT!)                                 │
│      Monster HP: 1000 → 550                                │
│                                                             │
│    Turn 2:                                                  │
│      Monster attacks (300 ATK)                             │
│      → Damage: 100 (after DEF)                             │
│      Player HP: 2000 → 1900                                │
│                                                             │
│    Turn 3:                                                  │
│      Player skill attack (1000 damage)                     │
│      → Monster dies                                         │
│                                                             │
│  Result: WIN                                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  10:07 AM - Receive rewards                                │
│                                                             │
│  Rewards:                                                   │
│    ├─ 500 EXP → Level 49 → 50 (level up!)                 │
│    ├─ 1000 Gold                                            │
│    ├─ 3x Blue potions                                      │
│    └─ Stage 11 unlocked                                    │
│                                                             │
│  Updates:                                                   │
│    - role-service: Level up stats                          │
│    - wallet-service: +1000 gold                            │
│    - bag-service: +3 potions                               │
│    - dungeon-service: Unlock stage 11                      │
│    - task-service: "Complete dungeon" +1                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 TỔNG KẾT GAME MECHANICS

### **Core Systems Summary**

```yaml
Game Loop:
  Login → Tasks → Combat → Loot → Upgrade → Repeat

Economy:
  Earn: Combat, tasks, events, sell items
  Spend: Shop, gacha, upgrades, guild

Combat:
  PvE: Dungeons, bosses, monsters
  PvP: Arena, territory, guild wars
  
Progression:
  Character: Level, exp, stats
  Equipment: Quality tiers, enchant
  Companions: Pets, mounts, angels
  Social: Guild, friends, leaderboard

Events:
  Daily: Tasks, dungeons
  Weekly: Arena season, guild wars
  Special: Time-limited events, festivals
```

### **Key Services Map**

```
Player Actions → Services Involved:

Login:
  → session-service, role-service, bag-service, wallet-service

Open Box:
  → box-service → drop-service → bag-service → wallet-service

Battle:
  → battle-service → role-service → monster-service → bag-service

Shop:
  → shop-service → wallet-service → bag-service → item-service

Tasks:
  → task-service → role-service → wallet-service → bag-service

Events:
  → event-service → leaderboard-service → mail-service

Guild:
  → guild-service → wallet-service → territory-service

Pets:
  → pet-service → bag-service → role-service
```

---

## 🎯 LỜI KẾT

Game này hoạt động dựa trên **vòng lặp** (loop):
1. **Earn resources** (combat, tasks)
2. **Get loot** (gacha, drops)
3. **Upgrade character** (equip, pets)
4. **Get stronger** (power increase)
5. **Tackle harder content** (higher dungeons, PvP)
6. **Repeat**

Tất cả các hệ thống (gacha, combat, tasks, events...) đều phục vụ vòng lặp này để giữ chân người chơi và tạo cảm giác progression liên tục.

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-19  
**Author**: AI Development Team  
**Next**: Xem [FRONTEND_BACKEND_FLOW.md](FRONTEND_BACKEND_FLOW.md) để hiểu technical implementation

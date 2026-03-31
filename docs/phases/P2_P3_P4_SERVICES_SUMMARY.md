# P2/P3/P4 Services và API Endpoints

> **Cập nhật lần cuối**: 2026-02-18 — Implement P4: @EnableDiscoveryClient on 5 main classes, notification sendEmail(), scheduler Feign clients + job wiring, gameworld RoleServiceClient + real player level, iap-verify real Google Play API

---

## 📊 **TỔNG QUAN PORT MAPPING**

| Service | Port | gRPC | Phase | Status |
|---------|------|------|-------|--------|
| task-service | 8420 | — | P2 | ✅ |
| leaderboard-service | 8480 | — | P2 | ✅ (fixed) |
| chat-service | 8460 | — | P2 | ✅ |
| mount-service | 8180 | — | P2 | ✅ (fixed) |
| angel-service | 8130 | — | P2 | ✅ (fixed) |
| rune-service | 8160 | — | P2 | ✅ (fixed) |
| shizhuang-service | 8190 | — | P2 | ✅ |
| artifact-service | 8091 | — | P2 | ✅ (fixed) |
| starmap-service | 8092 | — | P2 | ✅ (fixed) |
| trial-service | 8300 | — | P2 | ✅ (fixed) |
| main-fb-service | — | — | P2 | ✅ |
| friend-service | 8450 | — | P3 | ✅ (fixed) |
| guild-service | 8440 | — | P3 | ✅ (fixed) |
| arena-service | 8084 | 9084 | P3 | ✅ (fixed) |
| world-service | 8370 | — | P3 | ✅ (fixed) |
| escort-service | 8340 | — | P3 | ✅ (fixed) |
| territory-service | 8360 | — | P3 | ✅ (fixed) |
| anti-cheat-service | 8590 | — | P4 | ✅ (fixed) |
| iap-verify-service | 8580 | — | P4 | ✅ (fixed) |
| analytics-service | — | — | P4 | ✅ |
| notification-service | — | — | P4 | ✅ |
| moderation-service | — | — | P4 | ✅ |
| scheduler-service | — | — | P4 | ✅ |
| admin-service | — | — | P4 | ✅ |
| gm-service | — | — | P4 | ✅ |
| report-service | — | — | P4 | ✅ |
| localization-service | — | — | P4 | ✅ |
| file-service | — | — | P4 | ✅ |
| user-service | — | — | P4 | ✅ |
| serverinfo-service | — | — | P4 | ✅ |
| gameworld-service | — | — | P4 | ✅ |
| battleserver-service | — | — | P4 | ✅ |

---

## **PHASE P2 — Gameplay Extension Services**

### 1️⃣ **task-service** (Port: **8420**)
**Chức năng**: Task/Quest system, reward claiming

**API Endpoints:**
```java
GET    /task/list/{roleId}               // Get task list
POST   /task/claim/{roleId}              // Claim all available task rewards
GET    /task/progress/{roleId}/{taskId}  // Get specific task progress
```

**Architecture**: Kafka consumer; integrates BagFeign + WalletFeign for rewards
**Status**: ✅ **IMPLEMENTED**

---

### 2️⃣ **leaderboard-service** (Port: **8480**)
**Chức năng**: Ranking system (8 types), Redis sorted sets, auto-refresh every 5 minutes

**Ranking Types:**
```
1 = Power Ranking    5 = Guild Ranking
2 = Level Ranking    6 = Pet Ranking
3 = Arena Ranking    7 = Mount Ranking
4 = Wealth Ranking   8 = PVP Kills Ranking
```

**API Endpoints:**
```java
POST   /api/leaderboard/update           // Update player score
GET    /api/leaderboard/{rankingType}    // Get leaderboard (?roleId=xxx for my rank)
POST   /api/leaderboard/refresh          // Manual refresh all leaderboards
GET    /api/leaderboard/health           // Health check
```

**Kafka Topics Consumed**: `arena.match.end`, `trial.completed`

**Fixes Applied**:
- ✅ Spring Boot `2.7.18` → `3.5.3`
- ✅ Java `11` → `21`
- ✅ spring-cloud `2021.0.8` → `2025.0.0`
- ✅ `javax.*` → `jakarta.*` (controller, dto, entity)
- ✅ `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- ✅ `@EnableEurekaClient` → `@EnableDiscoveryClient`
- ✅ Added `scanBasePackages` for `com.SouthMillion.common`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 3️⃣ **chat-service** (Port: **8460**)
**Chức năng**: In-game chat (4 channels), mute system, history

**API Endpoints:**
```java
POST   /api/chat/send                    // Send message
GET    /api/chat/history                 // Get chat history
POST   /api/chat/mute                    // Mute player
POST   /api/chat/unmute                  // Unmute player
GET    /api/chat/player/{roleId}/info    // Get player chat info
```

**Channels**: World, Guild, Party, Private
**Status**: ✅ **IMPLEMENTED**

---

### 4️⃣ **mount-service** (Port: **8180**)
**Chức năng**: Mount/cavalry system — level-up, grade-up, harness equipment, appearance

**API Endpoints:**
```java
GET    /api/mount/{roleId}                    // Get mount data
POST   /api/mount/{roleId}/levelup            // Level up mount
POST   /api/mount/{roleId}/gradeup/{mountId}  // Grade up mount
POST   /api/mount/{roleId}/explore            // Start exploration
POST   /api/mount/{roleId}/appearance         // Set appearance
POST   /api/mount/{roleId}/pifu/upgrade       // Upgrade skin
POST   /api/mount/{roleId}/pifu/set           // Switch skin
POST   /api/mount/{roleId}/harness/wear       // Wear harness
POST   /api/mount/{roleId}/harness/decompose  // Decompose harness (body: {harnessIndex})
POST   /api/mount/{roleId}/harness/unlock     // Unlock harness slot
POST   /api/mount/{roleId}/shop/refresh       // Refresh shop
POST   /api/mount/{roleId}/shop/buy           // Buy from shop
POST   /api/mount/{roleId}/shop/refreshbuy    // Refresh & buy
POST   /api/mount/{roleId}/shop/open          // Open shop
```

**Fixes Applied**:
- ✅ Added `scanBasePackages = {"com.game.mount", "com.SouthMillion.common"}`
- ✅ `BagClient`: added `grantItems(GrantReq)` → `POST /api/bag/grant`
- ✅ `MountHarnessServiceImpl.addMaterial()`: now calls `bagClient.grantItems()` (was log-only stub)
- ✅ `MountController.decomposeHarness()`: injected `MountHarnessService`, calls `decomposeHarness(roleId, harnessIndex)`
- ✅ `MountController`: added `MountHarnessService` dependency injection

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 5️⃣ **angel-service** (Port: **8130**)
**Chức năng**: Angel/wing companion — upgrade, skill, appearance, evolution

**API Endpoints:**
```java
GET    /api/angel/{roleId}                    // Get angel data
POST   /api/angel/{roleId}/levelup            // Level up (body: {angelId})
POST   /api/angel/{roleId}/gradeup/{angelId}  // Grade up
POST   /api/angel/{roleId}/skill/upgrade      // Upgrade skill (body: {angelId, skillId})
POST   /api/angel/{roleId}/activate/{angelId} // Activate angel
POST   /api/angel/{roleId}/switch/{angelId}   // Switch active angel
POST   /api/angel/{roleId}/rename             // Rename (body: {angelId, newName}) — costs 10,000 gold
POST   /api/angel/{roleId}/blessing/{angelId} // Bless angel
POST   /api/angel/{roleId}/transform          // Transform appearance (body: {angelId, transformId})
```

**Rename flow**: validates name (1–12 chars), deducts 10,000 gold via WalletClient, saves new name to DB.

**Fixes Applied**:
- ✅ Added `scanBasePackages = {"com.game.angel", "com.SouthMillion.common"}`
- ✅ `Angel` entity: added `name` field (`VARCHAR 32`, auto-migrated via `ddl-auto: update`)
- ✅ `AngelService`: added `renameAngel(userId, angelIndex, newName)` to interface
- ✅ `AngelServiceImpl`: implemented `renameAngel()` with name validation + gold cost
- ✅ `AngelController.rename()`: wired to `angelService.renameAngel()` (was echoing name back, no service call)

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 6️⃣ **rune-service** (Port: **8160**)
**Chức năng**: Rune enhancement system for equipment power boost

**API Endpoints:**
```java
GET    /api/rune/{roleId}                // Get all runes
GET    /api/rune/{roleId}/{runeId}       // Get specific rune
POST   /api/rune/create                  // Create/unlock rune
POST   /api/rune/level-up               // Level up rune
POST   /api/rune/quality-up             // Upgrade quality
POST   /api/rune/star-up                // Upgrade star
POST   /api/rune/equip                  // Equip rune to slot
POST   /api/rune/unequip                // Unequip rune
GET    /api/rune/{roleId}/equipped       // Get equipped runes
```

**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.rune", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 7️⃣ **shizhuang-service** (Fashion service, Port: **8350**)
**Chức năng**: Fashion/costume system — activate, wear, level up with resource cost

**API Endpoints:**
```java
GET    /api/shizhuang/list/{roleId}      // Get role fashions
GET    /api/shizhuang/{roleId}/{shizhuangId} // Get single fashion
POST   /api/shizhuang/activate           // Activate fashion (body: {roleId, shizhuangId})
POST   /api/shizhuang/wear               // Wear fashion (body: {roleId, shizhuangId})
POST   /api/shizhuang/levelup            // Level up fashion (body: {roleId, shizhuangId}) — costs level*1000 gold
```

**Fixes Applied**:
- ✅ `WalletFeignClient`: fixed path `/wallet` → `/internal/wallet`
- ✅ `BagFeignClient`: fixed path `/bag` → `/api/bag`
- ✅ `ShizhuangService`: injected `WalletFeignClient`
- ✅ `ShizhuangService.levelUp()`: added gold deduction (`level * 1000`) via WalletFeignClient before level increment (was free, no resource check)

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 8️⃣ **artifact-service** (Port: **8091**)
**Chức năng**: Divine Artifact/ShenQi system — activate, upgrade, skills, evolve

**API Endpoints:**
```java
GET    /api/artifact/{roleId}            // Get role artifacts
POST   /api/artifact/activate            // Activate artifact
POST   /api/artifact/upgrade             // Upgrade artifact
POST   /api/artifact/skill/upgrade       // Upgrade artifact skill
POST   /api/artifact/set-active          // Set active artifact
POST   /api/artifact/evolve              // Evolve artifact
POST   /api/artifact/refine              // Refine artifact
```

**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.artifact", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 9️⃣ **starmap-service** (Port: **8092**)
**Chức năng**: Star Map/Constellation system — activate nodes, upgrades, presets

**API Endpoints:**
```java
GET    /api/starmap/{roleId}             // Get star map info
POST   /api/starmap/activate             // Activate node
POST   /api/starmap/upgrade              // Upgrade node
POST   /api/starmap/reset                // Reset star map
GET    /api/starmap/config               // Get star map config
POST   /api/starmap/preset               // Apply preset
```

**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.starmap", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 🔟 **trial-service** (Port: **8300**)
**Chức năng**: Trial/Challenge dungeon — stage progression, daily limits, rewards

**API Endpoints:**
```java
GET    /api/trial/{roleId}               // Get trial data
GET    /api/trial/{roleId}/record        // Get trial record
POST   /api/trial/start                  // Start trial
POST   /api/trial/complete               // Complete trial
POST   /api/trial/fail                   // Fail trial
POST   /api/trial/advance                // Advance stage
POST   /api/trial/claim                  // Claim reward
POST   /api/trial/reset                  // Reset progress
GET    /api/trial/{roleId}/best          // Get best record
GET    /api/trial/{roleId}/claimed       // Get claimed rewards
```

**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.trial", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 1️⃣1️⃣ **main-fb-service** (Main Dungeon/Copy)
**Chức năng**: Main story dungeon/instance system for PvE content

**Status**: ✅ **IMPLEMENTED** — JPA + Redis + gRPC

---

## **PHASE P3 — Social & World Services**

### 1️⃣ **friend-service** (Port: **8450**)
**Chức năng**: Friend list management, requests, block/unblock, online status, gifts

**API Endpoints:**
```java
GET    /api/friend/{roleId}/list         // Get friend list (max 100)
POST   /api/friend/request/send          // Send friend request
GET    /api/friend/{roleId}/requests     // Get received requests
POST   /api/friend/request/handle        // Approve/reject request
DELETE /api/friend/{roleId}/remove       // Remove friend
POST   /api/friend/block                 // Block player
POST   /api/friend/unblock              // Unblock player
GET    /api/friend/search               // Search players
GET    /api/friend/{roleId}/online       // Get online status
```

**Fixes Applied**:
- ✅ `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- ✅ Added `scanBasePackages = {"com.lhp.game.friend", "com.SouthMillion.common"}`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 2️⃣ **guild-service** (Port: **8440**)
**Chức năng**: Guild creation, member management (50 max), tech upgrades (5 branches), warehouse (100 slots)

**API Endpoints:**
```java
GET    /api/guild/{guildId}              // Get guild info
GET    /api/guild/player/{roleId}        // Get player's guild
POST   /api/guild/create                 // Create guild
POST   /api/guild/join                   // Join guild
POST   /api/guild/leave                  // Leave guild
POST   /api/guild/donate                 // Donate to guild
POST   /api/guild/tech/upgrade           // Upgrade guild tech
GET    /api/guild/{guildId}/members      // Get members
GET    /api/guild/search                 // Search guilds
POST   /api/guild/apply                  // Apply to guild
POST   /api/guild/approve               // Approve application
GET    /api/guild/list                   // Get guild list
```

**Fixes Applied**:
- ✅ `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`
- ✅ Added `scanBasePackages = {"com.lhp.game.guild", "com.SouthMillion.common"}`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 3️⃣ **arena-service** (Port: **8084**, gRPC: **9084**)
**Chức năng**: PvP Arena — ELO rating, daily challenge limits, rank rewards, Kafka events

**gRPC Endpoints** (via `ArenaServiceGrpc`):
```
GetArenaInfo        // Player rating, rank, challenges remaining (real daily tracking)
GetOpponents        // ELO-based matchmaking (±200 rating range)
StartBattle         // Process battle, update ELO, publish Kafka match.end event
GetRankings         // Paginated leaderboard (Redis cached)
ClaimRewards        // Grant gold reward by rank tier via WalletFeignClient
GetBattleHistory    // Paginated battle history
BuyChallengeCount   // Buy extra challenges at 50g each via WalletFeignClient
```

**Rank Reward Tiers**: Top 10 = 10,000g | 11-50 = 5,000g | 51-100 = 2,000g | Others = 500g

**Fixes Applied**:
- ✅ `ArenaServiceApplication`: added `scanBasePackages = {"com.SouthMillion.arenaservice", "com.SouthMillion.common"}`
- ✅ `ArenaPlayer` entity: added `challengesUsedToday` + `lastResetDate` fields for daily limit tracking
- ✅ Created `WalletFeignClient` in `com.SouthMillion.arenaservice.client`
- ✅ `ArenaService`: added `getChallengesRemaining()`, `consumeChallenge()`, `addBoughtChallenges()`, `calculateRankReward()`
- ✅ `getArenaInfo()`: `setChallengesRemaining(10)` hardcode → real daily tracking
- ✅ `startBattle()`: `setNewRank(0)` hardcode → `arenaService.calculatePlayerRank(playerId)`
- ✅ `claimRewards()`: fully implemented — grants gold by rank tier via `walletFeignClient.addCurrency()`
- ✅ `buyChallengeCount()`: fully implemented — deducts 50g/challenge via `walletFeignClient.deductCurrency()`, calls `arenaService.addBoughtChallenges()`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 4️⃣ **world-service** (Port: **8370**)
**Chức năng**: Scene/zone management, AOI (Area of Interest), player movement, item pickup → bag integration

**API Endpoints:**
```java
POST   /api/world/enter                  // Enter scene (returns nearby players)
POST   /api/world/leave                  // Leave scene
POST   /api/world/position              // Update position (AOI radius=50, anti-cheat speed check)
POST   /api/world/pickup                // Pick up item → grants via bag-service
POST   /api/world/interact              // Interact with NPC (dialog/shop/task)
GET    /api/world/scene/{sceneId}        // Get scene info (player/item counts)
```

**Architecture**: Redis-based scene state + JPA for world boss/events + Flyway schema migration

**Fixes Applied**:
- ✅ `world_service.WorldServiceApplication` (pom mainClass): added `scanBasePackages = {"com.SouthMillion.worlds_ervice", "com.SouthMillion.world_service", "com.SouthMillion.common"}`, `@EnableDiscoveryClient`, `@EnableScheduling`, `@EnableCaching`, `@EnableJpaRepositories`, `@EnableFeignClients`
- ✅ `worldservice.WorldServiceApplication` (duplicate): converted to empty placeholder — was `@SpringBootApplication` incorrectly excluding DataSource/JPA despite using JPA repositories
- ✅ Created `BagFeignClient` in `com.SouthMillion.worlds_ervice.client` → `POST /api/bag/grant`
- ✅ `SceneManagementService`: injected `BagFeignClient`
- ✅ `SceneManagementService.pickupItem()`: now calls `bagFeignClient.grantItems()` after removing item from scene (was mock — only logged, no bag-service call)

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 5️⃣ **escort-service** (Port: **8340**)
**Chức năng**: Escort/convoy missions — quality tiers, daily limits, rob mechanics

**API Endpoints:**
```java
GET    /api/escort/{roleId}/info         // Get escort info
POST   /api/escort/start                 // Start escort
POST   /api/escort/complete              // Complete escort
POST   /api/escort/rob                   // Rob other player's escort
GET    /api/escort/targets               // Get rob targets
POST   /api/escort/speedup              // Speed up escort
GET    /api/escort/history              // Get escort history
```

**Note**: Stateless service (no DB). Excludes DataSource auto-config.
**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.escort", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 6️⃣ **territory-service** (Port: **8360**)
**Chức năng**: Territory/base management — land control, building construction, resource production

**API Endpoints:**
```java
GET    /api/territory/list               // Get all territories
GET    /api/territory/{id}/info          // Get territory info
GET    /api/territory/player/{roleId}    // Get player's territories
POST   /api/territory/occupy            // Occupy territory
POST   /api/territory/attack            // Attack territory
POST   /api/territory/defend            // Defend territory
POST   /api/territory/claim             // Claim production rewards
GET    /api/territory/battles           // Battle records
```

**Fix Applied**: ✅ Added `scanBasePackages = {"com.game.territory", "com.SouthMillion.common"}`
**Status**: ✅ **IMPLEMENTED & FIXED**

---

## **PHASE P4 — Infrastructure & Support Services**

### 1️⃣ **anti-cheat-service** (Port: **8590**)
**Chức năng**: Anti-cheat detection, statistical analysis, violation tracking

**API Endpoints:**
```java
POST   /api/anticheat/report             // Report suspicious activity
GET    /api/anticheat/player/{roleId}    // Get player violation history
POST   /api/anticheat/ban                // Issue ban
GET    /api/anticheat/stats              // Detection statistics
```

**Fixes Applied**:
- ✅ `mysql:mysql-connector-java:8.0.33` → `com.mysql:mysql-connector-j`
- ✅ Added `scanBasePackages = {"com.SouthMillion.anti_cheat_service", "com.SouthMillion.common"}`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 2️⃣ **iap-verify-service** (Port: **8580**)
**Chức năng**: In-App Purchase verification — Google Play + Apple AppStore receipt validation

**API Endpoints:**
```java
POST   /api/iap/verify/google            // Verify Google Play purchase
POST   /api/iap/verify/apple             // Verify Apple AppStore purchase
GET    /api/iap/history/{roleId}         // Get purchase history
POST   /api/iap/grant                    // Grant IAP rewards
```

**Dependencies**: google-api-services-androidpublisher, google-auth-library-oauth2-http, spring-webflux (Apple HTTP), Caffeine cache
**Fixes Applied**:
- ✅ `mysql:mysql-connector-java:8.0.33` → `com.mysql:mysql-connector-j`
- ✅ Added `scanBasePackages = {"com.SouthMillion.iap_verify_service", "com.SouthMillion.common"}`
- ✅ Added `google-auth-library-oauth2-http:1.20.0` dependency to `pom.xml`
- ✅ `verifyGooglePlayPurchase()`: replaced mock (always returned `valid=true`) with real **Google Play Developer API** call using `ServiceAccountCredentials` + `AndroidPublisher.purchases().products().get()` — purchase validity now verified against Google's servers

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 3️⃣ **analytics-service**
**Chức năng**: Player KPIs, event tracking, behavior analytics

**API Endpoints:**
```java
POST   /api/analytics/event              // Track game event
GET    /api/analytics/player/{roleId}/events // Get player events
GET    /api/analytics/player/{roleId}/kpi    // Get player KPIs
```

**Status**: ✅ **IMPLEMENTED** — JPA + Redis + Kafka + gRPC

---

### 4️⃣ **notification-service**
**Chức năng**: Push notifications, email, in-game alerts

**API Endpoints:**
```java
POST   /api/notification/send            // Send notification
GET    /api/notification/player/{roleId} // Get player notifications
GET    /api/notification/player/{roleId}/unread // Get unread
POST   /api/notification/read           // Mark as read
```

**Fixes Applied**:
- ✅ `sendEmail()`: now uses `JavaMailSender` — extracts `email` field from `notification.getData()` JSON, sends real SMTP email via `SimpleMailMessage`

**Status**: ✅ **IMPLEMENTED & FIXED** — Kafka consumer, SMTP mail, gRPC, Redis

---

### 5️⃣ **moderation-service**
**Chức năng**: Chat content moderation, player reporting

**Status**: ✅ **IMPLEMENTED** — Kafka consumer, JPA, Redis

---

### 6️⃣ **scheduler-service**
**Chức năng**: Cron jobs — daily reset, event scheduling, timed tasks

**API Endpoints:**
```java
GET    /api/scheduler/jobs               // List active jobs
POST   /api/scheduler/trigger/daily-reset  // Manually trigger daily reset
POST   /api/scheduler/trigger/weekly-reset // Manually trigger weekly reset
```

**Note**: Stateless (Redis-based, no DB)

**Fixes Applied**:
- ✅ `SchedulerServiceApplication`: added `scanBasePackages`, `@EnableFeignClients(basePackages = "com.SouthMillion.scheduler_service.client")`
- ✅ Created 5 Feign clients: `TaskServiceClient` (reset-daily/weekly), `ShopServiceClient` (reset/daily), `GiftServiceClient` (send-daily-rewards), `GuildServiceClient` (reset/weekly), `LeaderboardServiceClient` (refresh)
- ✅ `DailyResetJob`: all job methods now call Feign clients (was stub — only logged)
- ✅ `WeeklyResetJob`: all job methods now call Feign clients (was stub — only logged)
- ✅ `SchedulerController`: inject `DailyResetJob` + `WeeklyResetJob`; trigger endpoints now actually execute jobs (was returning hardcoded success)

**Status**: ✅ **IMPLEMENTED & FIXED** — `@EnableScheduling`, Redis, Feign clients

---

### 7️⃣ **admin-service**
**Chức năng**: Spring Boot Admin server — service health monitoring, log viewing

**Fixes Applied**: ✅ Added `@EnableDiscoveryClient` (was missing — admin server could not self-register with Eureka)
**Status**: ✅ **IMPLEMENTED & FIXED** — Spring Boot Admin Server, Security, Prometheus metrics

---

### 8️⃣ **gm-service** (Game Master)
**Chức năng**: GM tools — player management, grant items, ban, god mode

**Fixes Applied**: ✅ Added `@EnableDiscoveryClient` (was missing)
**Status**: ✅ **IMPLEMENTED & FIXED** — Security, JPA, MySQL, Redis, Feign

---

### 9️⃣ **report-service**
**Chức năng**: Player reports (bug reports, player abuse), notice board, world boss tracking

**Controllers**: ReportController, NoticeController, BossController
**Fixes Applied**: ✅ Added `@EnableDiscoveryClient` (was missing)
**Status**: ✅ **IMPLEMENTED & FIXED** — JPA + Kafka

---

### 🔟 **localization-service**
**Chức năng**: Game text localization (multi-language support)

**API Endpoints:**
```java
POST   /api/localization/translate       // Translate text key
GET    /api/localization/all             // Get all translations
```

**Note**: Stateless (Redis cache, no DB)
**Status**: ✅ **IMPLEMENTED**

---

### 1️⃣1️⃣ **file-service**
**Chức năng**: File download/streaming for game assets

**Note**: Stateless (no DB)
**Status**: ✅ **IMPLEMENTED** — gRPC support

---

### 1️⃣2️⃣ **user-service**
**Chức năng**: User account management, authentication bridge

**Controllers**: UserController, AuthController, InternalAuthController
**Fixes Applied**: ✅ Added `@EnableDiscoveryClient` (was missing)
**Status**: ✅ **IMPLEMENTED & FIXED** — JPA + Redis + Flyway

---

### 1️⃣3️⃣ **serverinfo-service**
**Chức năng**: Server information and status API

**Fixes Applied**: ✅ Added `@EnableDiscoveryClient` (was missing)
**Status**: ✅ **IMPLEMENTED & FIXED** — JPA + Redis + MySQL

---

### 1️⃣4️⃣ **gameworld-service**
**Chức năng**: Game world coordination — nearby player info with real role data, entity management

**Note**: Stateless (no DB). Uses Redis + Kafka + gRPC

**Fixes Applied**:
- ✅ `GameworldServiceApplication`: added `@EnableDiscoveryClient`, `scanBasePackages`, `@EnableFeignClients(basePackages=...)`
- ✅ Created `RoleServiceClient` in `service.client` → `GET /api/role/{roleId}`
- ✅ `GameWorldServiceGrpcImpl`: replaced `setLevel(50)` hardcode with `fetchPlayerLevel(playerId)` → calls `RoleServiceClient` with graceful fallback to `1`

**Status**: ✅ **IMPLEMENTED & FIXED**

---

### 1️⃣5️⃣ **battleserver-service**
**Chức năng**: Battle server (stateless combat processing)

**Note**: Stateless (no DB). Uses Kafka + gRPC + JWT auth
**Status**: ✅ **IMPLEMENTED**

---

## 🔧 **FIXES APPLIED IN THIS SESSION**

| Service | Fix Type | Details |
|---------|----------|---------|
| leaderboard-service | Version Upgrade | Spring Boot 2.7.18→3.5.3, Java 11→21, spring-cloud 2021→2025 |
| leaderboard-service | Import Fix | `javax.*` → `jakarta.*` (controller, dto, entity) |
| leaderboard-service | MySQL | `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j` |
| leaderboard-service | Discovery | `@EnableEurekaClient` → `@EnableDiscoveryClient` |
| leaderboard-service | Scan | Added `scanBasePackages` |
| mount-service | Scan | Added `scanBasePackages = {"com.game.mount", "com.SouthMillion.common"}` |
| mount-service | BagClient | Added `grantItems(GrantReq)` method → `POST /api/bag/grant` |
| mount-service | addMaterial() | Fixed stub → calls `bagClient.grantItems()` to deliver decompose rewards |
| mount-service | decomposeHarness() | Fixed stub → injected `MountHarnessService`, calls `decomposeHarness(roleId, idx)` |
| angel-service | Scan | Added `scanBasePackages = {"com.game.angel", "com.SouthMillion.common"}` |
| angel-service | Entity | Added `name` field to `Angel` entity (VARCHAR 32, auto-migrated) |
| angel-service | Service | Added `renameAngel()` to `AngelService` + `AngelServiceImpl` with gold cost (10,000) |
| angel-service | Controller | `rename()` now calls `angelService.renameAngel()` (was echoing input, no DB save) |
| rune-service | Scan | Added `scanBasePackages = {"com.game.rune", "com.SouthMillion.common"}` |
| artifact-service | Scan | Added `scanBasePackages = {"com.game.artifact", "com.SouthMillion.common"}` |
| escort-service | Scan | Added `scanBasePackages = {"com.game.escort", "com.SouthMillion.common"}` |
| starmap-service | Scan | Added `scanBasePackages = {"com.game.starmap", "com.SouthMillion.common"}` |
| territory-service | Scan | Added `scanBasePackages = {"com.game.territory", "com.SouthMillion.common"}` |
| trial-service | Scan | Added `scanBasePackages = {"com.game.trial", "com.SouthMillion.common"}` |
| friend-service | Scan + MySQL | `scanBasePackages` + `com.mysql:mysql-connector-j` |
| guild-service | Scan + MySQL | `scanBasePackages` + `com.mysql:mysql-connector-j` |
| anti-cheat-service | Scan + MySQL | `scanBasePackages` + `com.mysql:mysql-connector-j` |
| iap-verify-service | Scan + MySQL | `scanBasePackages` + `com.mysql:mysql-connector-j` |
| shizhuang-service | Feign Paths | `WalletFeignClient`: `/wallet` → `/internal/wallet`; `BagFeignClient`: `/bag` → `/api/bag` |
| shizhuang-service | levelUp() | Added gold deduction (`level * 1000`) via `WalletFeignClient` before DB update |
| arena-service | Scan | Added `scanBasePackages = {"com.SouthMillion.arenaservice", "com.SouthMillion.common"}` |
| arena-service | Entity | Added `challengesUsedToday` + `lastResetDate` to `ArenaPlayer` for daily limit |
| arena-service | WalletFeign | Created `WalletFeignClient` in `com.SouthMillion.arenaservice.client` |
| arena-service | Service | Added `getChallengesRemaining()`, `consumeChallenge()`, `addBoughtChallenges()`, `calculateRankReward()` |
| arena-service | gRPC getArenaInfo | `setChallengesRemaining(10)` hardcode → real daily tracking |
| arena-service | gRPC startBattle | `setNewRank(0)` hardcode → `arenaService.calculatePlayerRank(playerId)` |
| arena-service | gRPC claimRewards | Implemented: grants gold by rank tier via `walletFeignClient.addCurrency()` |
| arena-service | gRPC buyChallengeCount | Implemented: deducts 50g/challenge, calls `arenaService.addBoughtChallenges()` |
| world-service | Main class | `world_service.WorldServiceApplication`: added `scanBasePackages`, `@EnableDiscoveryClient`, `@EnableScheduling`, `@EnableCaching`, `@EnableJpaRepositories` |
| world-service | Duplicate removed | `worldservice.WorldServiceApplication`: converted from `@SpringBootApplication` (with wrong JPA excludes) to empty placeholder |
| world-service | BagFeignClient | Created `BagFeignClient` in `com.SouthMillion.worlds_ervice.client` |
| world-service | pickupItem() | Now calls `bagFeignClient.grantItems()` after removing item from Redis scene (was mock) |
| docs | rank-service | Removed — service does not exist (was incorrect documentation) |
| admin-service | Discovery | Added `@EnableDiscoveryClient` (was missing — admin couldn't register with Eureka) |
| gm-service | Discovery | Added `@EnableDiscoveryClient` (was missing) |
| report-service | Discovery | Added `@EnableDiscoveryClient` (was missing) |
| user-service | Discovery | Added `@EnableDiscoveryClient` (was missing) |
| serverInfo-service | Discovery | Added `@EnableDiscoveryClient` (was missing) |
| notification-service | sendEmail() | Now uses `JavaMailSender` — extracts email from `notification.getData()` JSON, sends real SMTP email |
| scheduler-service | Scan + Feign | Added `scanBasePackages`, `@EnableFeignClients`; created 5 Feign clients |
| scheduler-service | DailyResetJob | All job methods now call Feign clients (was stub — only logged, no remote calls) |
| scheduler-service | WeeklyResetJob | All job methods now call Feign clients (was stub — only logged) |
| scheduler-service | Controller | Inject `DailyResetJob` + `WeeklyResetJob`; trigger endpoints now execute real jobs (was hardcoded success) |
| gameworld-service | Discovery + Feign | Added `@EnableDiscoveryClient`, `scanBasePackages`, `@EnableFeignClients` |
| gameworld-service | RoleServiceClient | Created Feign client → `GET /api/role/{roleId}` |
| gameworld-service | getPlayerInfo gRPC | `setLevel(50)` hardcode → real `fetchPlayerLevel()` via `RoleServiceClient` with graceful fallback |
| iap-verify-service | pom.xml | Added `google-auth-library-oauth2-http:1.20.0` |
| iap-verify-service | verifyGooglePlayPurchase() | Replaced mock (always `valid=true`) with real Google Play Developer API via `ServiceAccountCredentials` + `AndroidPublisher` |

---

## 📌 **WHY scanBasePackages IS NEEDED**

All services with `com.game.*` or `com.lhp.game.*` main packages (and even `com.SouthMillion.*` sub-packages) have shared config files under `com.SouthMillion.common.*`:

```
com.SouthMillion.common.config.VirtualThreadsConfig    ← Virtual Threads (Java 21)
com.SouthMillion.common.config.MemoryOptimizationConfig
com.SouthMillion.common.config.DataSourceOptimizationConfig
com.SouthMillion.common.config.DatabaseUserInitializer
com.SouthMillion.common.config.RedisOptimizationConfig
com.SouthMillion.common.listener.MemoryMonitorListener
com.SouthMillion.common.optimization.JvmArgumentsSuggester
```

Without `scanBasePackages`, Spring Boot only scans the application class package and its sub-packages. So `com.game.mount.MountServiceApplication` would NOT scan `com.SouthMillion.common.*`.

**Fix**: Add `@SpringBootApplication(scanBasePackages = {"com.game.xxx", "com.SouthMillion.common"})`

---

## 🎯 **TỔNG IMPLEMENTATION STATUS**

```
P0 (Infrastructure): 3/3  ✅ session, config, role
P1 (Economy):        7/7  ✅ bag, equip, shop, box, wallet, item, gift
P1+:                 5/5  ✅ drop, crafting, pet, globalserver, mail

P2 (Gameplay):      11/11 ✅ task, leaderboard, chat, mount, angel, rune,
                           shizhuang, artifact, starmap, trial, main-fb
                           [mount: BagClient+decomposeHarness fixed]
                           [angel: rename() fully implemented]
                           [shizhuang: Feign paths + levelUp cost fixed]

P3 (Social/World):   6/6  ✅ friend, guild, escort, territory, arena, world
                           [arena: gRPC stubs fully implemented, daily tracking]
                           [world: main class + BagFeign + pickupItem fixed]
                           [rank-service: removed — never existed, was doc error]

P4 (Support):       15/15 ✅ anti-cheat, iap-verify, analytics, notification,
                           moderation, scheduler, admin, gm, report,
                           localization, file, user, serverinfo,
                           gameworld, battleserver

Infrastructure:      3/3  ✅ eureka-server, gateway-service, webSocket-server

TOTAL: 50 services — 50 fully implemented ✅
```

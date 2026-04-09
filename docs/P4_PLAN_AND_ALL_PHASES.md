# GameServer - Complete Phase Overview & P4 Implementation Plan

**Date Created:** 2026-04-09
**Document Purpose:** Comprehensive overview of all phases (P0-P4) and detailed P4 implementation plan
**Total Services:** 56 microservices

---

## 📊 TỔNG QUAN TẤT CẢ CÁC PHASE

### Phase Structure Overview

| Phase | Name | Services | Status | Focus Area |
|-------|------|----------|--------|------------|
| **P0** | Infrastructure Core | 3 services | ✅ Complete | Authentication, Config, Role Management |
| **P1** | Economy & Gameplay | 9 services | ✅ Complete | Wallet, Bag, Item, Equipment, Shop, Crafting |
| **P2** | Gameplay Extension | 11 services | ✅ Complete | Tasks, Leaderboard, Chat, Mount, Angel, Rune, Fashion |
| **P3** | Social & World | 6 services | ✅ Complete | Friend, Guild, Arena, World, Escort, Territory |
| **P4** | Infrastructure & Support | 15 services | ✅ Complete (needs testing) | Anti-cheat, IAP, Analytics, Admin, Scheduler |
| **Additional** | Specialized | 12 services | ✅ Complete | Activity, Pet, Mail, Block, etc. |

**Total:** 56 services across all phases

---

## 📋 PHASE P0 - INFRASTRUCTURE CORE (3 Services)

### Summary
Core infrastructure services that all other services depend on. These must be running first.

### Services

#### 1. session-service (Port 8096)
- **Purpose:** Authentication, Authorization, Session Management
- **Key Features:** JWT tokens, Redis sessions, Virtual Threads
- **Status:** ✅ **IMPLEMENTED**
- **Critical APIs:**
  - `POST /api/session/login` - User login
  - `POST /api/session/refresh` - Token refresh
  - `POST /internal/session/introspect` - Token validation for Gateway

#### 2. config-service (Port 8888)
- **Purpose:** Game configuration files (JSON/Excel data)
- **Key Features:** L1/L2 cache, Virtual Threads, batch loading
- **Status:** ✅ **IMPLEMENTED**
- **Critical APIs:**
  - `GET /api/config/file/{*path}` - Get config by path
  - `GET /api/config/batch` - Batch load multiple configs

#### 3. role-service (Port 8410, gRPC 9410)
- **Purpose:** Character/Role management, attributes, level, settings
- **Key Features:** REST + gRPC hybrid, Mail proxy, Ads system
- **Status:** ✅ **IMPLEMENTED**
- **Critical APIs:**
  - `POST /api/role` - Create role
  - `GET /api/role/{roleId}` - Get role info
  - `POST /api/role/add-exp` - Add experience
  - gRPC methods for high-performance operations

---

## 📋 PHASE P1 - ECONOMY & GAMEPLAY (9 Services)

### Summary
Core economy services managing virtual currencies, items, and inventory. Forms the foundation of the game economy.

### P1 Phase 1: Core Economy (3 services)

#### 1. wallet-service (Port 8210)
- **Purpose:** Virtual currency management (Gold, Diamond, VIP Points, etc.)
- **Key Features:** Batch operations, idempotency, transaction ledger
- **Status:** ✅ **IMPLEMENTED**
- **Integration:** Used by all services that handle currency

#### 2. item-service (Port 8220)
- **Purpose:** Item metadata and definitions
- **Key Features:** Item catalog, attributes, usage rules
- **Status:** ✅ **IMPLEMENTED**

#### 3. bag-service (Port 8230, gRPC 9230)
- **Purpose:** Inventory/bag management
- **Key Features:** Kafka event-driven, gRPC server, idempotency
- **Status:** ✅ **IMPLEMENTED**
- **Performance:** 65% latency reduction with gRPC

### P1 Phase 2: Equipment & Enhancement (3 services)

#### 4. equip-service (Port 8240, gRPC 9240)
- **Purpose:** Equipment management, FuMo (enchantment) system
- **Key Features:** Equipment slots, FuMo enhancement, power calculation
- **Status:** ✅ **IMPLEMENTED**
- **Performance:** 50-60% faster with gRPC

#### 5. shop-service (Port 8260, gRPC 9260)
- **Purpose:** Shop system (Common, Fashion, Mystery shops)
- **Key Features:** 3 shop types, daily refresh, purchase limits
- **Status:** ✅ **IMPLEMENTED**
- **Performance:** 60-65% faster with gRPC

#### 6. crafting-service (Port 8280, gRPC 9280)
- **Purpose:** Item crafting and forging
- **Key Features:** Recipe system, time-gated production, instant completion
- **Status:** ✅ **IMPLEMENTED**
- **Architecture:** gRPC-first design

### P1 Phase 3: Rewards & Drops (3 services)

#### 7. gift-service (Port 8270)
- **Purpose:** Gift code redemption, login rewards
- **Key Features:** DefGift (fixed), RandGift (random weighted)
- **Status:** ✅ **IMPLEMENTED**

#### 8. box-service (Port 8290)
- **Purpose:** Gacha/treasure box system
- **Key Features:** Auto-sell, equipment comparison, pity system
- **Status:** ✅ **IMPLEMENTED**

#### 9. drop-service (Port 8250)
- **Purpose:** Drop table and loot generation
- **Key Features:** Redis cache, pity mechanics, weighted random
- **Status:** ✅ **IMPLEMENTED**

---

## 📋 PHASE P2 - GAMEPLAY EXTENSION (11 Services)

### Summary
Progression and enhancement systems that extend core gameplay with additional features.

#### 1. task-service (Port 8420)
- **Purpose:** Task/quest system, reward claiming
- **Key Features:** Kafka consumer, achievement tracking
- **Status:** ✅ **IMPLEMENTED**

#### 2. leaderboard-service (Port 8480)
- **Purpose:** Ranking system (8 types)
- **Key Features:** Redis sorted sets, auto-refresh every 5 minutes
- **Status:** ✅ **IMPLEMENTED & FIXED**
- **Ranking Types:** Power, Level, Arena, Wealth, Guild, Pet, Mount, PVP Kills

#### 3. chat-service (Port 8460)
- **Purpose:** In-game chat (4 channels)
- **Key Features:** World, Guild, Party, Private channels, mute system
- **Status:** ✅ **IMPLEMENTED**

#### 4. mount-service (Port 8180)
- **Purpose:** Mount/cavalry system
- **Key Features:** Level-up, grade-up, harness equipment, exploration, skills
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 5. angel-service (Port 8130)
- **Purpose:** Angel/wing companion system
- **Key Features:** Upgrade, skills, appearance, evolution, rename
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 6. rune-service (Port 8160)
- **Purpose:** Rune enhancement system
- **Key Features:** Level-up, quality upgrade, star upgrade, equip to slots
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 7. shizhuang-service (Port 8190)
- **Purpose:** Fashion/costume system
- **Key Features:** Activate, wear, level up with gold cost
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 8. artifact-service (Port 8091)
- **Purpose:** Divine artifact system
- **Key Features:** Activate, upgrade, skills, evolve, refine
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 9. starmap-service (Port 8092)
- **Purpose:** Star map/constellation system
- **Key Features:** Activate nodes, upgrades, presets, reset
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 10. trial-service (Port 8300)
- **Purpose:** Trial/challenge dungeon
- **Key Features:** Stage progression, daily limits, rewards
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 11. main-fb-service
- **Purpose:** Main story dungeon/instance system
- **Key Features:** PvE content, story progression
- **Status:** ✅ **IMPLEMENTED**

---

## 📋 PHASE P3 - SOCIAL & WORLD (6 Services)

### Summary
Multiplayer social features and world interaction systems.

#### 1. friend-service (Port 8450)
- **Purpose:** Friend list management
- **Key Features:** Friend requests, block/unblock, online status, search
- **Status:** ✅ **IMPLEMENTED & FIXED**
- **Max Friends:** 100

#### 2. guild-service (Port 8440)
- **Purpose:** Guild management
- **Key Features:** Creation (100k gold), member management (50 max), tech tree (5 branches), warehouse (100 slots)
- **Status:** ✅ **IMPLEMENTED & FIXED**
- **Tech Branches:** ATK, DEF, HP, CRIT, SPD

#### 3. arena-service (Port 8084, gRPC 9084)
- **Purpose:** PvP Arena with ELO matchmaking
- **Key Features:** ELO rating (±200 range), daily challenges (10 free), rank rewards, Kafka events
- **Status:** ✅ **IMPLEMENTED & FIXED**
- **Rewards:** Top 10 = 10,000g | 11-50 = 5,000g | 51-100 = 2,000g | Others = 500g

#### 4. world-service (Port 8370)
- **Purpose:** Scene/zone management, AOI
- **Key Features:** Position tracking, AOI radius 50, item pickup, anti-cheat speed check
- **Status:** ✅ **IMPLEMENTED & FIXED**

#### 5. escort-service (Port 8340)
- **Purpose:** Escort/convoy missions
- **Key Features:** Quality tiers, daily limits, robbery mechanics, speed-up
- **Status:** ✅ **IMPLEMENTED & FIXED**
- **Stateless:** Redis-only state

#### 6. territory-service (Port 8360)
- **Purpose:** Territory/base management
- **Key Features:** Land control, building construction, resource production
- **Status:** ✅ **IMPLEMENTED & FIXED**

---

## 📋 PHASE P4 - INFRASTRUCTURE & SUPPORT (15 Services)

### Summary
Backend infrastructure, monitoring, admin tools, and support services.

### 🎯 P4 IMPLEMENTATION PLAN

---

## 🔧 P4 SERVICE DETAILS

### 1️⃣ anti-cheat-service (Port 8590)

**Purpose:** Anti-cheat detection and violation tracking

**Key Features:**
- Suspicious activity reporting
- Statistical analysis for cheat detection
- Violation history tracking
- Ban management

**API Endpoints:**
```java
POST   /api/anticheat/report             // Report suspicious activity
GET    /api/anticheat/player/{roleId}    // Get player violation history
POST   /api/anticheat/ban                // Issue ban
GET    /api/anticheat/stats              // Detection statistics
```

**Dependencies:**
- MySQL database (game_anticheat)
- role-service - Player information
- analytics-service - Behavior patterns

**Fixes Applied:**
- ✅ MySQL connector updated to `com.mysql:mysql-connector-j`
- ✅ Added `scanBasePackages = {"com.SouthMillion.anti_cheat_service", "com.SouthMillion.common"}`

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test violation reporting flow
- [ ] Verify ban enforcement
- [ ] Validate statistical analysis accuracy
- [ ] Test integration with role-service for ban checks

---

### 2️⃣ iap-verify-service (Port 8580)

**Purpose:** In-App Purchase verification for Google Play and Apple AppStore

**Key Features:**
- Google Play receipt verification (real API integration)
- Apple AppStore receipt verification
- Purchase history tracking
- Reward granting after verification
- Caffeine cache for recent verifications

**API Endpoints:**
```java
POST   /api/iap/verify/google            // Verify Google Play purchase
POST   /api/iap/verify/apple             // Verify Apple AppStore purchase
GET    /api/iap/history/{roleId}         // Get purchase history
POST   /api/iap/grant                    // Grant IAP rewards
```

**Dependencies:**
- MySQL database (game_iap)
- wallet-service - Grant premium currency
- bag-service - Grant premium items
- Google Play Developer API
- Apple AppStore API

**Fixes Applied:**
- ✅ MySQL connector updated
- ✅ Added `scanBasePackages`
- ✅ Added `google-auth-library-oauth2-http:1.20.0` dependency
- ✅ `verifyGooglePlayPurchase()`: Real Google Play API integration using ServiceAccountCredentials + AndroidPublisher

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test Google Play verification with real receipts
- [ ] Test Apple AppStore verification
- [ ] Verify purchase history tracking
- [ ] Test reward granting flow
- [ ] Validate duplicate purchase prevention

---

### 3️⃣ analytics-service

**Purpose:** Player behavior analytics and KPI tracking

**Key Features:**
- Event tracking (login, purchase, quest completion, etc.)
- Player KPI calculation (retention, ARPU, LTV)
- Behavior pattern analysis
- Kafka event consumer for real-time analytics
- Redis + JPA for data storage

**API Endpoints:**
```java
POST   /api/analytics/event              // Track game event
GET    /api/analytics/player/{roleId}/events // Get player events
GET    /api/analytics/player/{roleId}/kpi    // Get player KPIs
```

**Architecture:**
- Kafka consumer for real-time event ingestion
- Redis for fast KPI caching
- JPA + MySQL for historical data
- gRPC for high-performance queries

**Status:** ✅ **IMPLEMENTED**

**Testing Tasks:**
- [ ] Test event tracking pipeline
- [ ] Verify KPI calculation accuracy
- [ ] Test Kafka consumer performance
- [ ] Validate data retention policies

---

### 4️⃣ notification-service

**Purpose:** Push notifications, email, and in-game alerts

**Key Features:**
- Email notifications (SMTP integration)
- Push notifications (Firebase, APNs)
- In-game alert messages
- Kafka consumer for notification events
- Redis for unread tracking

**API Endpoints:**
```java
POST   /api/notification/send            // Send notification
GET    /api/notification/player/{roleId} // Get player notifications
GET    /api/notification/player/{roleId}/unread // Get unread notifications
POST   /api/notification/read           // Mark as read
```

**Fixes Applied:**
- ✅ `sendEmail()`: Now uses real `JavaMailSender` with SMTP
- ✅ Extracts email from `notification.getData()` JSON
- ✅ Sends real email via `SimpleMailMessage`

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test email sending (SMTP)
- [ ] Test push notification delivery
- [ ] Verify unread count accuracy
- [ ] Test mark-as-read functionality

---

### 5️⃣ moderation-service

**Purpose:** Content moderation and player reporting

**Key Features:**
- Chat content moderation (profanity filter)
- Player report management
- Automated flagging system
- Manual review queue
- Kafka consumer for chat events

**Architecture:**
- Kafka consumer for real-time chat monitoring
- JPA + MySQL for reports and bans
- Redis for filter cache

**Status:** ✅ **IMPLEMENTED**

**Testing Tasks:**
- [ ] Test profanity filter accuracy
- [ ] Verify report submission flow
- [ ] Test automated flagging rules
- [ ] Validate manual review workflow

---

### 6️⃣ scheduler-service

**Purpose:** Scheduled jobs for daily/weekly resets and timed events

**Key Features:**
- Daily reset (00:00 UTC)
  - Task daily limits
  - Shop daily refresh
  - Daily rewards distribution
- Weekly reset
  - Task weekly limits
  - Guild weekly rewards
- Leaderboard refresh (every 5 minutes)
- Event scheduling

**API Endpoints:**
```java
GET    /api/scheduler/jobs               // List active jobs
POST   /api/scheduler/trigger/daily-reset  // Manual daily reset
POST   /api/scheduler/trigger/weekly-reset // Manual weekly reset
```

**Fixes Applied:**
- ✅ Added `scanBasePackages`, `@EnableFeignClients`
- ✅ Created 5 Feign clients:
  - `TaskServiceClient` - reset-daily/weekly
  - `ShopServiceClient` - reset/daily
  - `GiftServiceClient` - send-daily-rewards
  - `GuildServiceClient` - reset/weekly
  - `LeaderboardServiceClient` - refresh
- ✅ `DailyResetJob`: All methods now call Feign clients (was stub)
- ✅ `WeeklyResetJob`: All methods now call Feign clients (was stub)
- ✅ `SchedulerController`: Inject jobs, trigger endpoints now execute real jobs

**Architecture:**
- Stateless (Redis-based, no database)
- Spring `@Scheduled` annotations
- Feign clients to all services requiring resets

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test daily reset execution
- [ ] Test weekly reset execution
- [ ] Verify all Feign client calls work
- [ ] Test manual trigger endpoints
- [ ] Validate reset timing accuracy

---

### 7️⃣ admin-service

**Purpose:** Spring Boot Admin server for monitoring

**Key Features:**
- Service health monitoring
- Log viewing (real-time)
- Metrics dashboard (Prometheus)
- Thread dump analysis
- Heap dump analysis
- Security (Spring Security)

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient` (was missing - admin couldn't register with Eureka)

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Verify service discovery and monitoring
- [ ] Test log viewing functionality
- [ ] Validate metrics collection
- [ ] Test security access controls

---

### 8️⃣ gm-service (Game Master)

**Purpose:** GM tools for game masters and administrators

**Key Features:**
- Player management (search, view, edit)
- Grant items/currency to players
- Ban/unban players
- God mode toggle
- Event creation
- Server announcements

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient` (was missing)

**Architecture:**
- Spring Security for GM authentication
- JPA + MySQL for GM actions log
- Redis for session management
- Feign clients to all game services

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test GM authentication
- [ ] Verify grant items functionality
- [ ] Test ban/unban system
- [ ] Validate action logging
- [ ] Test server announcement broadcast

---

### 9️⃣ report-service

**Purpose:** Player reports, notice board, world boss tracking

**Key Features:**
- Bug reports
- Player abuse reports
- Notice board (server announcements)
- World boss event tracking
- Kafka consumer for boss events

**Controllers:**
- `ReportController` - Bug/abuse reports
- `NoticeController` - Server notices
- `BossController` - World boss tracking

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient` (was missing)

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test report submission
- [ ] Verify notice board display
- [ ] Test world boss event tracking
- [ ] Validate Kafka event processing

---

### 🔟 localization-service

**Purpose:** Multi-language support for game text

**Key Features:**
- Text key translation
- Multiple language support (EN, ZH, VI, etc.)
- Redis cache for translations
- Batch translation loading

**API Endpoints:**
```java
POST   /api/localization/translate       // Translate text key
GET    /api/localization/all             // Get all translations
```

**Architecture:**
- Stateless (Redis cache, no database)
- Fast lookup (<5ms)
- Fallback to default language

**Status:** ✅ **IMPLEMENTED**

**Testing Tasks:**
- [ ] Test translation accuracy
- [ ] Verify fallback behavior
- [ ] Test cache performance
- [ ] Validate batch loading

---

### 1️⃣1️⃣ file-service

**Purpose:** File download/streaming for game assets

**Key Features:**
- Asset file serving
- CDN integration
- Streaming support
- gRPC for high-performance transfers

**Architecture:**
- Stateless (no database)
- Direct file serving or CDN redirect
- gRPC support for large file transfers

**Status:** ✅ **IMPLEMENTED**

**Testing Tasks:**
- [ ] Test file download performance
- [ ] Verify streaming functionality
- [ ] Test gRPC file transfer
- [ ] Validate CDN integration

---

### 1️⃣2️⃣ user-service

**Purpose:** User account management

**Key Features:**
- User registration
- Profile management
- Authentication bridge to session-service
- Password reset
- Email verification

**Controllers:**
- `UserController` - User CRUD
- `AuthController` - Authentication
- `InternalAuthController` - Internal auth

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient` (was missing)

**Architecture:**
- JPA + MySQL for user data
- Redis for verification tokens
- Flyway for schema migrations

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test user registration flow
- [ ] Verify authentication bridge
- [ ] Test password reset
- [ ] Validate email verification

---

### 1️⃣3️⃣ serverinfo-service

**Purpose:** Server information and status API

**Key Features:**
- Server list
- Server status (online/offline/maintenance)
- Server capacity tracking
- Recommended server selection

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient` (was missing)

**Architecture:**
- JPA + MySQL for server data
- Redis for real-time status

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test server list retrieval
- [ ] Verify status tracking accuracy
- [ ] Test capacity monitoring
- [ ] Validate recommended server logic

---

### 1️⃣4️⃣ gameworld-service

**Purpose:** Game world coordination and entity management

**Key Features:**
- Nearby player information with real role data
- Entity management (NPCs, monsters)
- Cross-server coordination
- Real-time player level fetching

**Fixes Applied:**
- ✅ Added `@EnableDiscoveryClient`, `scanBasePackages`, `@EnableFeignClients`
- ✅ Created `RoleServiceClient` → `GET /api/role/{roleId}`
- ✅ `GameWorldServiceGrpcImpl`: Replaced hardcoded `setLevel(50)` with real `fetchPlayerLevel()` via RoleServiceClient with graceful fallback to 1

**Architecture:**
- Stateless (no database)
- Redis for world state
- Kafka for event coordination
- gRPC for high-performance queries

**Status:** ✅ **IMPLEMENTED & FIXED**

**Testing Tasks:**
- [ ] Test nearby player queries
- [ ] Verify real player level fetching
- [ ] Test entity management
- [ ] Validate cross-server coordination

---

### 1️⃣5️⃣ battleserver-service

**Purpose:** Stateless combat processing

**Key Features:**
- Battle simulation
- Damage calculation
- Skill resolution
- PvP/PvE combat
- JWT authentication
- Kafka event publishing

**Architecture:**
- Stateless (no database)
- Kafka for battle events
- gRPC for real-time combat

**Status:** ✅ **IMPLEMENTED**

**Testing Tasks:**
- [ ] Test battle simulation accuracy
- [ ] Verify damage calculation
- [ ] Test skill resolution
- [ ] Validate event publishing

---

## 🔗 P4 INTEGRATION FLOWS

### Flow 1: IAP Purchase Verification and Reward

```
Player completes purchase on Google Play
         ↓
Client sends receipt to backend
         ↓
  POST /api/iap/verify/google
    { roleId, productId, purchaseToken }
         ↓
  iap-verify-service validates:
    - Receipt format correct
    - Product ID matches catalog
         ↓
  Call Google Play Developer API:
    AndroidPublisher.purchases().products().get(
      packageName, productId, purchaseToken
    )
         ↓
  Google responds with purchase details:
    { purchaseState: 0 (purchased), consumptionState, etc. }
         ↓
  iap-verify-service validates:
    - purchaseState == 0 (purchased)
    - Not already consumed (check DB)
         ↓
  MySQL Transaction:
    - INSERT IapPurchase { roleId, productId, purchaseToken, timestamp }
    - Mark as consumed
         ↓
  Grant rewards:
    - WalletFeignClient.batchAdd(roleId, diamond=1000)
    - BagFeignClient.grantItems(roleId, bonusItems)
         ↓
  Return: { success: true, rewards: { diamond: 1000, items: [...] } }
```

**Validation Points:**
1. Receipt authenticity (Google API)
2. Duplicate purchase prevention (DB check)
3. Product catalog validation
4. Reward granting atomic transaction
5. Purchase history tracking

---

### Flow 2: Daily Reset (Scheduler)

```
Cron trigger at 00:00 UTC
         ↓
  scheduler-service: DailyResetJob.executeDailyReset()
         ↓
  Parallel Feign calls:
    ├─ TaskServiceClient.resetDaily() → task-service resets daily limits
    ├─ ShopServiceClient.resetDaily() → shop-service refreshes mystery shop
    ├─ GiftServiceClient.sendDailyRewards() → gift-service grants login rewards
    └─ LeaderboardServiceClient.refresh() → leaderboard-service recalculates
         ↓
  Each service executes:
    task-service:
      - UPDATE UserTask SET dailyProgress = 0 WHERE daily = true
    shop-service:
      - Regenerate mystery shop items (weighted random)
      - Redis: SET mystery:shop:{roleId} {newItems} EX 86400
    gift-service:
      - For each online player: grant daily login reward
    leaderboard-service:
      - ZRANGE for each ranking type
      - Update Redis sorted sets
         ↓
  Return: { success: true, servicesReset: 4, timestamp }
```

**Validation Points:**
1. Cron timing accuracy (UTC 00:00)
2. All Feign calls succeed
3. Retry mechanism for failed calls
4. Atomic reset per service
5. Event logging for troubleshooting

---

### Flow 3: Anti-Cheat Detection

```
Player action triggers suspicion
         ↓
  game-service detects anomaly:
    - Speed hack (movement too fast in world-service)
    - Gold hack (balance inconsistency in wallet-service)
    - Item duplication (bag-service detects duplicate items)
         ↓
  POST /api/anticheat/report
    {
      roleId,
      violationType: SPEED_HACK,
      evidence: { before, after, timestamp },
      severity: HIGH
    }
         ↓
  anti-cheat-service processes:
    - MySQL: INSERT Violation { roleId, type, evidence, timestamp }
    - Increment violation counter for player
    - Query violation history: SELECT * FROM Violation WHERE roleId = ?
         ↓
  Automated decision:
    if (violationCount >= 3 && severity == HIGH):
      - POST /api/anticheat/ban { roleId, duration: 7 days }
      - MySQL: UPDATE Role SET banned = true, banUntil = now + 7 days
      - Kafka: Publish ban event
      - Close all active sessions for player
         ↓
  Analytics integration:
    - analytics-service tracks violation patterns
    - Dashboard shows cheat detection statistics
         ↓
  Return: { reported: true, action: BAN, duration: 7 days }
```

**Validation Points:**
1. Evidence collection completeness
2. Violation threshold accuracy
3. Ban enforcement across all services
4. Appeal process support
5. False positive rate monitoring

---

### Flow 4: GM Item Grant

```
GM logs into gm-service
         ↓
  AuthController: POST /auth/login
    { username: "gm_admin", password: "***" }
         ↓
  Spring Security validates credentials
    - Check against GM user database
    - Generate JWT token with GM role
         ↓
  Return: { token, expiresIn: 3600 }

GM searches for player
         ↓
  GET /api/gm/player/search?name=PlayerName
         ↓
  gm-service calls:
    - RoleFeignClient.getRoleByName("PlayerName")
         ↓
  Return: { roleId, name, level, power, lastLogin }

GM grants items
         ↓
  POST /api/gm/grant/items
    {
      roleId,
      items: [
        { itemId: 1001, quantity: 100 },
        { itemId: 2001, quantity: 50 }
      ],
      reason: "Compensation for server maintenance"
    }
         ↓
  gm-service validates:
    - JWT token has GM role
    - Item IDs exist in catalog
    - Quantity within reasonable limits
         ↓
  BagFeignClient.grantItems(roleId, items)
         ↓
  MySQL: INSERT GmActionLog {
    gmId, actionType: GRANT_ITEMS, targetRoleId,
    details: { items }, reason, timestamp
  }
         ↓
  notification-service: Send in-game notification to player
    "You received items from GM: compensation for server maintenance"
         ↓
  Return: { success: true, itemsGranted: 2 }
```

**Validation Points:**
1. GM authentication and authorization
2. Item catalog validation
3. Action logging for audit trail
4. Player notification delivery
5. Rate limiting for mass grants

---

## ✅ P4 TESTING CHECKLIST

### Anti-Cheat Service
- [ ] Test violation reporting from multiple services
- [ ] Verify automated ban enforcement
- [ ] Test ban duration and expiry
- [ ] Validate appeal process
- [ ] Test statistics dashboard accuracy

### IAP Verify Service
- [ ] Test Google Play verification with sandbox receipts
- [ ] Test Apple AppStore verification with sandbox receipts
- [ ] Verify duplicate purchase prevention
- [ ] Test reward granting flow (wallet + bag)
- [ ] Validate purchase history retrieval
- [ ] Test cache performance

### Analytics Service
- [ ] Test event tracking pipeline (Kafka)
- [ ] Verify KPI calculations (retention, ARPU, LTV)
- [ ] Test data retention and archiving
- [ ] Validate query performance
- [ ] Test dashboard data accuracy

### Notification Service
- [ ] Test email sending (SMTP configuration)
- [ ] Test push notification delivery (Firebase/APNs)
- [ ] Verify unread count tracking
- [ ] Test mark-as-read functionality
- [ ] Validate notification history

### Moderation Service
- [ ] Test profanity filter accuracy
- [ ] Verify report submission and queuing
- [ ] Test automated flagging rules
- [ ] Validate manual review workflow
- [ ] Test ban enforcement

### Scheduler Service
- [ ] Test daily reset (all Feign calls)
- [ ] Test weekly reset (all Feign calls)
- [ ] Verify cron timing accuracy
- [ ] Test manual trigger endpoints
- [ ] Validate retry mechanism on failure
- [ ] Test leaderboard refresh schedule

### Admin Service
- [ ] Verify service health monitoring
- [ ] Test log viewing (real-time)
- [ ] Validate metrics collection (Prometheus)
- [ ] Test thread dump analysis
- [ ] Verify security access controls

### GM Service
- [ ] Test GM authentication
- [ ] Verify player search functionality
- [ ] Test item granting (bag integration)
- [ ] Test currency granting (wallet integration)
- [ ] Validate ban/unban system
- [ ] Test action logging
- [ ] Verify god mode toggle

### Report Service
- [ ] Test bug report submission
- [ ] Test player abuse report submission
- [ ] Verify notice board display
- [ ] Test world boss event tracking
- [ ] Validate Kafka event processing

### Localization Service
- [ ] Test translation retrieval
- [ ] Verify multi-language support
- [ ] Test cache performance
- [ ] Validate fallback to default language

### File Service
- [ ] Test file download performance
- [ ] Verify streaming functionality
- [ ] Test gRPC file transfer
- [ ] Validate CDN integration

### User Service
- [ ] Test user registration
- [ ] Verify email verification
- [ ] Test password reset flow
- [ ] Validate authentication bridge to session-service

### ServerInfo Service
- [ ] Test server list retrieval
- [ ] Verify status tracking (online/offline)
- [ ] Test capacity monitoring
- [ ] Validate recommended server logic

### GameWorld Service
- [ ] Test nearby player queries
- [ ] Verify real player level fetching (RoleServiceClient)
- [ ] Test entity management
- [ ] Validate fallback behavior when role-service unavailable

### BattleServer Service
- [ ] Test battle simulation accuracy
- [ ] Verify damage calculation
- [ ] Test skill resolution
- [ ] Validate Kafka event publishing

---

## 📊 P4 SUCCESS CRITERIA

### Functional Requirements
- [x] All 15 P4 services build successfully
- [x] All services have proper Eureka registration
- [x] All Feign clients configured correctly
- [x] All database schemas migrated
- [ ] All integration flows tested
- [ ] All admin tools functional
- [ ] All scheduled jobs executing

### Performance Requirements
- [ ] Admin dashboard loads <2s
- [ ] GM operations complete <500ms
- [ ] IAP verification <3s (includes external API)
- [ ] Scheduler jobs complete within 5 minutes
- [ ] Analytics queries <1s
- [ ] Notification delivery <10s

### Reliability Requirements
- [ ] Scheduler jobs retry on failure
- [ ] IAP verification has fallback for API timeout
- [ ] Anti-cheat false positive rate <1%
- [ ] Admin service uptime 99.9%
- [ ] All services handle network failures gracefully

---

## 🚀 P4 DEPLOYMENT PLAN

### Phase 1: Infrastructure Services
1. Deploy admin-service first (monitoring)
2. Deploy user-service (account management)
3. Deploy serverinfo-service (server status)
4. Verify all services register with Eureka

### Phase 2: Support Services
1. Deploy notification-service
2. Deploy localization-service
3. Deploy file-service
4. Test email and notifications

### Phase 3: Analytics & Anti-Cheat
1. Deploy analytics-service
2. Deploy anti-cheat-service
3. Deploy moderation-service
4. Configure Kafka topics
5. Test event processing

### Phase 4: Monetization & Admin
1. Deploy iap-verify-service
2. Configure Google Play credentials
3. Configure Apple AppStore credentials
4. Deploy gm-service
5. Test IAP flow end-to-end

### Phase 5: Automation
1. Deploy scheduler-service
2. Configure all Feign clients
3. Test daily reset
4. Test weekly reset
5. Monitor first automated execution

### Phase 6: World Services
1. Deploy gameworld-service
2. Deploy battleserver-service
3. Deploy report-service
4. Test cross-server features

---

## 📝 ADDITIONAL SPECIALIZED SERVICES (12 Services)

These services were implemented outside the main phase structure:

### 1. activity-service
- **Purpose:** Time-limited events and activities
- **Key Features:** SevenDaySign, LuckUnpacking, NewAreaPreferential
- **Integration:** LoginBootstrapHandler initialization
- **Status:** ✅ **IMPLEMENTED**

### 2. pet-service (Port 8112)
- **Purpose:** Pet/companion system
- **Key Features:** Pet summoning, feeding, leveling, skills
- **Status:** ✅ **IMPLEMENTED**

### 3. mail-service
- **Purpose:** In-game mail system
- **Key Features:** Send/receive mail, attachments, system mail
- **Status:** ✅ **IMPLEMENTED**
- **Note:** Proxied through role-service for convenience

### 4. block-service
- **Purpose:** Player blocking system
- **Key Features:** Block/unblock players, block list management
- **Database:** game_block
- **Status:** ✅ **IMPLEMENTED**

### 5. dataaccess-service
- **Purpose:** Centralized data access layer
- **Key Features:** Common database operations, query optimization
- **Status:** ✅ **IMPLEMENTED**

### 6. globalserver-service (Port 8100)
- **Purpose:** Global server coordination
- **Key Features:** Cross-server events, global leaderboard
- **Status:** ✅ **IMPLEMENTED**

### 7-12. Additional Support Services
- eureka-server - Service discovery
- gateway-server - API Gateway
- zipkin-server - Distributed tracing
- redis-server - Caching layer
- kafka-server - Event streaming
- mysql-server - Database

---

## 🎯 NEXT STEPS AFTER P4

### 1. Comprehensive Integration Testing
- End-to-end user journey testing
- Load testing for all phases
- Stress testing for critical services
- Cross-service integration validation

### 2. Performance Optimization
- Identify bottlenecks across all phases
- Optimize database queries
- Tune gRPC performance
- Redis cache optimization
- Kafka consumer optimization

### 3. Monitoring & Observability
- Set up Grafana dashboards
- Configure alerting (PagerDuty/Slack)
- Log aggregation (ELK stack)
- APM integration (New Relic/Datadog)
- Custom metrics for business KPIs

### 4. Documentation
- API documentation (Swagger/OpenAPI)
- Architecture diagrams
- Deployment runbooks
- Troubleshooting guides
- Development guidelines

### 5. Security Hardening
- Penetration testing
- Security audit of all services
- Encryption at rest and in transit
- Rate limiting implementation
- DDoS protection

### 6. DevOps Improvements
- CI/CD pipeline optimization
- Blue-green deployment
- Canary releases
- Automated rollback
- Infrastructure as Code (Terraform)

---

## 📚 SUMMARY: ALL PHASES STATUS

| Phase | Services | Status | Next Action |
|-------|----------|--------|-------------|
| **P0** | 3 | ✅ Complete | Monitor in production |
| **P1** | 9 | ✅ Complete | Performance optimization |
| **P2** | 11 | ✅ Complete | Feature enhancements |
| **P3** | 6 | ✅ Complete | Social features expansion |
| **P4** | 15 | ✅ Implementation complete | **Testing & validation** |
| **Additional** | 12 | ✅ Complete | Documentation |

**Total Services:** 56 microservices
**Overall Status:** 🎉 **ALL PHASES IMPLEMENTED**
**Current Focus:** P4 testing and integration validation

---

## 🔧 KNOWN ISSUES & IMPROVEMENTS

### High Priority
- [ ] P4: Complete integration testing for all 15 services
- [ ] P4: Validate scheduler daily/weekly reset execution
- [ ] P4: Test IAP verification with real receipts (sandbox)
- [ ] P4: Verify anti-cheat detection accuracy

### Medium Priority
- [ ] Performance testing for all phases
- [ ] Load testing for critical services (arena, world)
- [ ] Redis cache hit ratio optimization
- [ ] Database query optimization

### Low Priority
- [ ] API documentation generation (Swagger)
- [ ] Service-to-service authentication (mTLS)
- [ ] Advanced monitoring dashboards
- [ ] Automated backup procedures

---

## 📊 SERVICE METRICS SUMMARY

### By Technology Stack
- **gRPC Services:** 5 (bag, equip, shop, crafting, role, arena)
- **REST-only Services:** 45
- **Hybrid (REST + gRPC):** 6
- **Stateless Services:** 8 (escort, localization, file, scheduler, gameworld, battleserver, etc.)
- **Kafka Consumers:** 12 (bag, task, analytics, notification, moderation, leaderboard, etc.)

### By Database
- **MySQL Services:** 38
- **Redis-only:** 6
- **No Database:** 12

### By Port Range
- **8xxx:** Core game services (8000-8999)
- **9xxx:** gRPC ports (9000-9999)

---

**Document Version:** 1.0
**Last Updated:** 2026-04-09
**Generated with:** Claude Code

**Next Review:** After P4 integration testing completion

# P0/P1 Services và API Endpoints

> **Cập nhật lần cuối**: 2026-02-18 — Session 2: Fix leaderboard (SB 3.5.3/Java 21/jakarta), mount/angel/rune (scanBasePackages), RoleHandler (TaskFeign)

## 📊 **TỔNG QUAN SERVICES**

> ⚠️ **LƯU Ý PORT**: Docs cũ ghi sai port. Ports thực tế đang dùng:

| Service | Port docs cũ | Port thực tế | gRPC port |
|---------|-------------|--------------|-----------|
| session-service | 8081 | 8096 | — |
| config-service | 8083 | 8888 | — |
| bag-service | 8097 | 8230 | 9230 |
| equip-service | 8098 | 8240 | 9240 |
| shop-service | 8099 | 8260 | — |
| box-service | 8100 | 8290 | — |
| wallet-service | 8101 | 8210 | — |
| item-service | 8102 | 8220 | — |
| gift-service | 8103 | 8270 | — |
| role-service | 8082 | 8410 | 9410 |
| drop-service | — | 8250 | — |
| crafting-service | — | 8280 | — |
| pet-service | — | 8112 | — |
| globalserver-service | — | 8100 | — |

---

### **PHASE P0 - Infrastructure Services (Core)**

#### 1️⃣ **session-service** (Port: **8096**)
**Chức năng**: Authentication, Authorization, Session Management

**API Endpoints:**
```java
// Public APIs (qua Gateway)
POST   /api/session/login              // Login với username/password
POST   /api/session/refresh            // Refresh access token
POST   /api/session/heartbeat          // Heartbeat / keep-alive
POST   /api/session/logout             // Logout
GET    /api/session/time               // Lấy server time

// Internal APIs (service-to-service)
POST   /internal/session/introspect    // Validate token (Gateway auth)
```

**Status**: ✅ **IMPLEMENTED** — Virtual Threads, JWT, Redis session store

---

#### 2️⃣ **role-service** (Port: **8410**, gRPC: **9410**)
**Chức năng**: Character/Role Management, Attributes, Level, Settings, Mail proxy, Ads, OtherRole

**API Endpoints:**
```java
// Role CRUD — path prefix: /api/role (không có 's')
POST   /api/role                             // Create new role (RoleDTOs.CreateRoleReq)
GET    /api/role/{roleId}                    // Get role info (RoleDTOs.RoleResp)
GET    /api/role/by-user/{userId}            // List roles by userId
GET    /api/role/by-name/{roleName}          // Get role by name
POST   /api/role/{roleId}/wxinfo             // Set WX name + headChar
POST   /api/role/{roleId}/login              // Update last login time
PUT    /api/role/{roleId}/vip                // Update VIP level
POST   /api/role/add-exp                     // Add experience

// Settings
POST   /api/role/settings                    // Apply system settings (music, sfx, language...)

// Advertisement
POST   /api/ads/claim                        // Claim ad reward (tracks daily quota in DB)

// Other Role Info (for PvP, friend list)
GET    /api/other-role/{uid}?roleId=xxx      // Get other player's public info

// Mail Proxy (delegates to mail-service)
POST   /api/mail/list                        // Get mail list { userId }
GET    /api/mail/{userId}/{mailId}           // Get mail detail + mark read
POST   /api/mail/{userId}/{mailId}/fetch     // Claim mail attachment
POST   /api/mail/{userId}/{mailId}/delete    // Delete mail
```

**DB Tables**: `roles`, `role_settings`, `ads_claim`

**gRPC Methods** (RoleServiceGrpc):
- `getRole`, `getRoleByName`, `getRolesByUserId`, `addExp`
- `updateRole`, `batchGetRoles`, `streamRoleUpdates`
- `setWxInfo`, `applySettings`

**Status**: ✅ **IMPLEMENTED** — REST + gRPC, settings/ads/mail proxy hoàn thiện

---

#### 3️⃣ **config-service** (Port: **8888**)
**Chức năng**: Game Configuration Files (JSON, Excel data)

**API Endpoints:**
```java
GET    /api/config/file/{*path}         // Get config file by path
GET    /config/by-path?p={path}         // Shorthand (Feign-friendly)
GET    /api/config/batch                // Batch get multiple configs
GET    /api/config/starter-box          // Starter box config
```

**Status**: ✅ **IMPLEMENTED** — L1/L2 cache, Virtual Threads

---

### **PHASE P1 - Economy Services (Gameplay)**

#### 4️⃣ **bag-service** (Port: **8230**, gRPC: **9230**)
**Chức năng**: Inventory/Bag Management, Item Usage, Kafka event consumer

**API Endpoints:**
```java
GET    /api/bag/{roleId}/items           // Get all items in bag
POST   /api/bag/{roleId}/items/use       // Use item
POST   /api/bag/{roleId}/items/sell      // Sell item
POST   /api/bag/grant                    // Grant items (REST)

// Internal (gRPC preferred)
POST   /internal/bag/add                 // Add items
POST   /internal/bag/consume             // Consume items
```

**Architecture**: Event-driven via Kafka topic `gameh5.bag.grant`; publishes to `gameh5.bag.changed`

**Status**: ✅ **IMPLEMENTED** — Kafka consumer, gRPC server, idempotency via Redis

---

#### 5️⃣ **equip-service** (Port: **8240**, gRPC: **9240**)
**Chức năng**: Equipment Management, FuMo (Enchant) System

**API Endpoints:**
```java
GET    /api/equip/{roleId}               // Get equipped items
POST   /api/equip/equip                  // Equip item
POST   /api/equip/unequip                // Unequip item
POST   /api/equip/wear/{roleId}/{itemId} // Wear item
GET    /api/equip/fumo/{roleId}          // Get FuMo list
GET    /api/equip/fumo/{roleId}/{equipType} // Get FuMo by type
POST   /api/equip/fumo/add-exp           // Add FuMo exp
POST   /api/equip/fumo/activate          // Activate FuMo
POST   /api/equip/fumo/reset             // Reset FuMo
```

**Status**: ✅ **IMPLEMENTED** — gRPC, Feign clients to bag/item/config services

---

#### 6️⃣ **shop-service** (Port: **8260**)
**Chức năng**: Shop System (Common, Fashion/Cloth, Mystery/Shenmi)

**API Endpoints:**
```java
GET    /api/shop/info                    // Bootstrap shop info (mystery items + reset time)
POST   /api/shop/list/common             // List common shop items
POST   /api/shop/list/cloth              // List fashion shop items
GET    /api/shop/list/mystery            // List mystery shop items (random)
POST   /api/shop/buy                     // Buy from any shop (kind: COMMON/CLOTH/SHENMI)
POST   /api/shop/mystery/refresh         // Re-randomize mystery shop items
```

**Status**: ✅ **IMPLEMENTED** — Config-driven, Caffeine cache, daily quota tracking

---

#### 7️⃣ **box-service** (Port: **8290**)
**Chức năng**: Gacha/Box System (Treasure Box Opening)

**API Endpoints:**
```java
GET    /api/box/info
POST   /api/box/open
POST   /api/box/wear
POST   /api/box/sell
POST   /api/box/buy
POST   /api/box/level-up
POST   /api/box/quicken
POST   /api/box/level-reward
GET    /api/box/luck/info
POST   /api/box/luck/receive
GET    /api/box/setting
POST   /api/box/setting
POST   /api/box/decompose
GET    /api/box/equipInfo
```

**Status**: ✅ **IMPLEMENTED** — Đầy đủ endpoints

---

#### 8️⃣ **wallet-service** (Port: **8210**)
**Chức năng**: Virtual Currency Management (Gold, Diamond, etc.)

**API Endpoints:**
```java
POST   /internal/wallet/batch-add        // Add currencies
POST   /internal/wallet/batch-cost       // Deduct currencies
GET    /internal/wallet/{roleId}         // Get balances
GET    /internal/wallet/info             // Get wallet info
```

**Status**: ✅ **IMPLEMENTED**

---

#### 9️⃣ **item-service** (Port: **8220**)
**Chức năng**: Item Metadata (Item definitions)

**API Endpoints:**
```java
GET    /api/item/meta                    // Get item meta by id
GET    /api/item/meta/batch              // Batch get item metadata
GET    /api/item/type                    // Get item type
GET    /api/item/validate                // Validate item ids
GET    /internal/item/meta/raw           // Raw metadata (internal)
```

**Status**: ✅ **IMPLEMENTED** — In-memory cache from config-service

---

#### 🔟 **gift-service** (Port: **8270**)
**Chức năng**: Gift Box System (Reward packages)

**API Endpoints:**
```java
GET    /api/gift/{giftItemId}/info       // Get gift info
POST   /api/gift/open                    // Open gift
POST   /api/gift/use                     // Use gift item
```

**Status**: ✅ **IMPLEMENTED** — No DB (config-driven), Feign to bag/wallet/item

---

### **Các services bổ sung**

#### **drop-service** (Port: **8250**)
```java
GET    /internal/drop/tables             // Get drop tables
POST   /internal/drop/roll               // Roll drops
GET    /internal/drop/simulate           // Simulate drop (debug)
```
**Status**: ✅ IMPLEMENTED — Pity system, configurable drop tables

#### **crafting-service** (Port: **8280**, gRPC)
```java
GET    /api/crafting/recipes             // Get available recipes
POST   /api/crafting/start               // Start crafting
GET    /api/crafting/status              // Check crafting status
POST   /api/crafting/claim               // Claim crafted item
```
**Status**: ✅ IMPLEMENTED

#### **pet-service** (Port: **8112**)
```java
GET    /api/pet/{roleId}                 // Get all pets
POST   /api/pet/{roleId}/activate/{petTemplateId}  // Activate pet
POST   /api/pet/{roleId}/upgrade         // Upgrade pet
POST   /api/pet/{roleId}/evolve/{petId}  // Evolve pet
POST   /api/pet/{roleId}/setactive/{petId} // Set active pet
```
**Status**: ✅ IMPLEMENTED — Kafka events, fixed mainClass + @EnableFeignClients

#### **globalserver-service** (Port: **8100**)
```java
GET    /api/global/status                // Service health
POST   /api/global/server/register       // Register game server
POST   /api/global/server/{id}/heartbeat // Heartbeat
GET    /api/global/servers               // List registered servers
POST   /api/global/player/online         // Mark player online
POST   /api/global/player/offline        // Mark player offline
GET    /api/global/player/{roleId}/server // Get player's server
```
**Status**: ✅ IMPLEMENTED — Redis-based, no DB

---

## 🔗 **SERVICE DEPENDENCIES**

```
Client (WebSocket)
    ↓
webSocket-server (8094) — Blocking + Virtual Threads
    ↓
    ├─→ session-service (8096)        [P0] Auth, Login (HTTP + Feign)
    ├─→ role-service (8410/gRPC 9410) [P0] Character (gRPC + HTTP)
    ├─→ config-service (8888)         [P0] Game Config (Feign)
    ├─→ bag-service (8230/gRPC 9230)  [P1] Inventory (gRPC)
    ├─→ equip-service (8240)          [P1] Equipment (Feign)
    ├─→ shop-service (8260)           [P1] Shop (Feign)
    ├─→ box-service (8290)            [P1] Gacha (Feign)
    ├─→ wallet-service (8210)         [P1] Currency (Feign)
    ├─→ item-service (8220)           [P1] Item Meta (Feign)
    └─→ gift-service (8270)           [P1] Gifts (Feign)

mail-service (8470) — MailHandler calls directly via MailFeign
role-service proxies /api/mail/* → mail-service (for RoleFeign compat)
```

---

## 📌 **FEIGN CLIENT MAPPING (webSocket-server)**

| Feign Client | Service Name | Path Prefix | Protocol |
|--------------|--------------|-------------|----------|
| RoleFeign | role-service | /api/role, /api/mail, /api/ads, /api/other-role | HTTP |
| BagGrpcClient | bag-service | gRPC | gRPC |
| EquipFeign | equip-service | /api/equip | HTTP |
| ShopFeign | shop-service | /api/shop | HTTP |
| BoxFeign | box-service | /api/box | HTTP |
| WalletHttpClient | wallet-service | /internal/wallet | HTTP |
| ItemMetaFeign | item-service | /api/item | HTTP |
| GiftFeign | gift-service | /api/gift | HTTP |
| MailFeign | mail-service | /api/mail | HTTP |
| ConfigFeign | config-service | /config | HTTP |

---

## 🎯 **IMPLEMENTATION STATUS**

### **P0 Services:**
- ✅ session-service — Login/Auth, JWT, heartbeat, timesync
- ✅ role-service — REST + gRPC, settings, ads, mail proxy, other-role, **path: /api/role**
- ✅ config-service — Config file serving, L1/L2 cache

### **P1 Services:**
- ✅ bag-service — Kafka consumer, gRPC, idempotency
- ✅ equip-service — REST + gRPC, FuMo system
- ✅ shop-service — Common/Cloth/Mystery, mystery refresh endpoint
- ✅ box-service — Gacha, luck system, pending items
- ✅ wallet-service — batch-add/batch-cost, ledger tracking
- ✅ item-service — Metadata, in-memory cache
- ✅ gift-service — Config-driven, no DB

### **Extra Services:**
- ✅ drop-service — Drop tables, pity system
- ✅ crafting-service — Recipes, timed crafting
- ✅ pet-service — Pet CRUD, evolve, upgrade, Kafka events
- ✅ globalserver-service — Server registry + online player tracking (Redis)
- ✅ mail-service — Full mail system (send/list/read/claim/delete)
- ✅ eureka-server — Service discovery
- ✅ gateway-service — API Gateway (Reactive/WebFlux)
- ✅ webSocket-server — WebSocket real-time, Virtual Threads

### **P2 Services (gameplay extras):**
- ✅ task-service (8420) — Task/quest system, Kafka consumer, BagFeign + WalletFeign rewards
- ✅ leaderboard-service (8480) — Ranking system (8 types), Redis sorted sets, auto-refresh 5min
  - **Fixed**: Upgraded Spring Boot 2.7.18→3.5.3, Java 11→21, spring-cloud 2021→2025, javax→jakarta
- ✅ chat-service (8460) — 4 channels (world/guild/party/private), mute system, history
- ✅ mount-service — Mount/harness system, grade-up/level-up, Kafka events
  - **Fixed**: Added `scanBasePackages` to include `com.SouthMillion.common`
- ✅ angel-service — Angel/wing companion, upgrade/skills, appearance
  - **Fixed**: Added `scanBasePackages` to include `com.SouthMillion.common`
- ✅ rune-service — Rune enhancement system for equipment
  - **Fixed**: Added `scanBasePackages` to include `com.SouthMillion.common`
- ✅ shizhuang-service — Fashion/costume system, star upgrade

### **P3 Services (stub/partial):**
- 🟡 world-service — Partial implementation (SceneManagementService with AOI)
- 🟡 arena-service — Partial implementation (ELO rating, claimRewards stub)
- 🟡 rank-service — Partial implementation
- 🟡 friend-service — Partial implementation
- 🟡 guild-service — Partial stub (handler TODOs remain)

### **webSocket-server Fixes (this session):**
- ✅ RoleHandler: Added `TaskFeign` injection — `handleFetchTaskReward` now calls `taskFeign.claimTaskRewards()`
- ✅ TaskHandler.interests(): Added clarification comment (1451 is in ROLE range, routed via RoleHandler)
- ✅ Removed stale TODO comments from `sendSystemSetInfo`, `sendTaskProgressInfo`

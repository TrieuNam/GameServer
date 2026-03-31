# Service Memory Index

Danh sach toan bo services trong GameServer + status cua service memory.

## Status Key
- ✅ **Complete** — Full memory file with APIs, DB, tests, bugs
- 📝 **Template** — Ready to fill using SERVICE_MEMORY_TEMPLATE.md
- ⏳ **Todo** — Need to create

---

## Core Services (✅ Complete)

| Service | Port | Database | Status | Memory File |
|---------|------|----------|--------|-------------|
| **task-service** | 9015 | task_service_db | ✅ | TASK_SERVICE_MEMORY.md |
| **user-service** | 9016 | user_service_db | ✅ | USER_SERVICE_MEMORY.md |
| **gateway-service** | 9001 | N/A | ✅ | GATEWAY_SERVICE_MEMORY.md |
| **guild-service** | 9017 | guild_service_db | ✅ | GUILD_SERVICE_MEMORY.md |
| **chat-service** | 9018 | chat_service_db | ✅ | CHAT_SERVICE_MEMORY.md |
| **notification-service** | 9025 | notification_service_db | ✅ | NOTIFICATION_SERVICE_MEMORY.md |
| **role-service** | 9019 | role_service_db | ✅ | ROLE_SERVICE_MEMORY.md |
| **session-service** | 9020 | session_db | ✅ | SESSION_SERVICE_MEMORY.md |
| **item-service** | 9035 | item_service_db | ✅ | ITEM_SERVICE_MEMORY.md |
| **world-service** | 9040 | world_service_db | ✅ | WORLD_SERVICE_MEMORY.md |

---

## Data Services (📝 Template)

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **wallet-service** | 9021 | wallet_db | User currency, balance, transactions |
| **shop-service** | 9022 | shop_db | Shop management, item selling |
| **pet-service** | 9023 | pet_service_db | Pet ownership, care, battles |
| **friend-service** | 9024 | friend_service_db | Friend list, friend requests |
| **leaderboard-service** | 9026 | leaderboard_db | Ranking, stats, leaderboards |
| **mail-service** | 9027 | mail_service_db | In-game mail, messages |
| **equipment-service** | 9028 | equip_service_db | Equipment management |
| **skill-service** | 9029 | skill_service_db | Character skills, learning |
| **inventory-service** | 9030 | inventory_db | User inventory management |
| **activity-service** | 9002 | activity_db | Daily/weekly activities |
| **quest-service** | 9003 | quest_db | Quest management |

---

## Feature Services (📝 Template)

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **arena-service** | 9031 | arena_db | PvP arena, matchmaking |
| **dungeon-service** | 9032 | dungeon_db | Dungeon runs, drops |
| **territory-service** | 9033 | territory_db | Territory wars, occupation |
| **trial-service** | 9034 | trial_db | Trial/challenge modes |
| **gift-service** | 9036 | gift_db | Gift system, gifting |
| **crafting-service** | 9037 | crafting_db | Crafting, recipes |
| **drop-service** | 9038 | drop_db | Item drops, loot tables |
| **artifact-service** | 9039 | artifact_db | Artifact management |
| **rune-service** | 9041 | rune_db | Rune system |
| **scroll-service** | 9042 | scroll_db | Scroll/buff items |
| **mount-service** | 9043 | mount_db | Mount system |
| **gem-service** | 9044 | gem_db | Gem enhancement |
| **shizhuang-service** | 9045 | shizhuang_db | Cosmetics system |
| **pagoda-service** | 9046 | pagoda_db | Tower/pagoda challenge |
| **escort-service** | 9047 | escort_db | Escort missions |
| **box-service** | 9048 | box_db | Treasure boxes, rewards |

---

## Admin/System Services (📝 Template)

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **admin-service** | 9050 | admin_db | Admin panel, user management |
| **gm-service** | 9051 | gm_db | Game master tools |
| **moderation-service** | 9052 | moderation_db | Moderation, reports |
| **report-service** | 9053 | report_db | Bug/player reports |
| **localization-service** | 9054 | localization_db | Multi-language support |
| **file-service** | 9055 | N/A | File upload/download |
| **config-service** | 9056 | config_db | Service configuration |
| **analytics-service** | 9057 | analytics_db | Game analytics |
| **iap-verify-service** | 9058 | iap_db | In-app purchase verification |
| **anti-cheat-service** | 9059 | cheat_db | Anti-cheat checks |
| **scheduler-service** | 9060 | N/A | Scheduled tasks |
| **globalserver-service** | 9061 | global_db | Global server stats |
| **serverInfo-service** | 9062 | N/A | Server information |

---

## Other Services (📝 Template)

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **angel-service** | 9064 | angel_db | Angel/helper system |
| **knights-service** | 9065 | knights_db | Knights order system |
| **lingzhu-service** | 9066 | lingzhu_db | Lingzhu system |
| **game-world-service** | 9067 | gameworld_db | Game world state |
| **battleserver-service** | 9068 | N/A | Battle server |
| **webSocket-server** | 9100 | N/A | WebSocket gateway |
| **eureka-server** | 8761 | N/A | Service discovery |
| **main-fb-service** | 9070 | fb_db | Facebook integration |

---

## Utility (📝 Template)

| Library | Location | Description |
|---------|----------|-------------|
| **common-lib** | N/A | Shared utilities, DTOs |

---

## How to Create Missing Service Memories

### Option 1: Manual (Detailed)
1. Copy `SERVICE_MEMORY_TEMPLATE.md`
2. Examine service folder structure
3. Read pom.xml for dependencies
4. Read application.yml for port/database
5. Read Controller for APIs
6. Read Entity for DB schema
7. Save as `{SERVICE_NAME}_MEMORY.md`

### Option 2: Use Script (Automated)
```powershell
# Scan all services and generate stub memories
.\generate-service-memories.ps1

# Or for specific service
.\generate-service-memories.ps1 -ServiceName "wallet-service"
```

---

## Usage

### In Skill
When you ask skill question about a service:
```
@gameserver Show WALLET_SERVICE_MEMORY
@gameserver Review wallet-service code
@gameserver How to test item-service?
```

Agent will load the memory file automatically.

### Updating Memory Files
1. Edit file in `service-memories/{SERVICE_NAME}_MEMORY.md`
2. Test: `@gameserver Show {SERVICE_NAME}_MEMORY`
3. Sync to .github and .claude folders (see HOW_TO_USE_SKILLS.md)

---

## Service Dependencies Map

```
gateway-service (9001)
  ├─> task-service (9015)
  ├─> user-service (9016)
  ├─> guild-service (9017)
  ├─> chat-service (9018)
  ├─> role-service (9019)
  ├─> session-service (9020)
  ├─> wallet-service (9021)
  ├─> shop-service (9022)
  └─> [other services...]

task-service ─> user-service (validate ownership)
             ─> event-bus (publish events)

chat-service ─> user-service (validate users)
             ─> webSocket-server (real-time delivery)

notification-service ─> event-bus (listen events)
                     ─> mail-service (send emails)
```

---

## Priority Levels

### P1 (Critical - Already Complete)
- gateway-service
- user-service
- session-service
- role-service
- task-service

### P2 (High - Complete Next)
- wallet-service
- friend-service
- mail-service
- chat-service
- guild-service

### P3 (Medium)
- item-service
- shop-service
- notification-service
- arena-service

### P4 (Low - As Needed)
- All other services

---

## Update Checklist

- [ ] All critical services (P1) have memory
- [ ] Top 10 services have detailed memory
- [ ] Template available for remaining services
- [ ] Index updated when new service added
- [ ] Memory files synced to .github and .claude
- [ ] Skill agent can load all memories

---

## Notes

- Service ports check: `GameServer/docs/SERVICE-PORT-DB-MAPPING.md`
- Service details check: `GameServer/SERVICES_SUMMARY.md`
- Add new service memory whenever working with a new service
- Update log each change in service memory


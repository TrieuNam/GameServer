# GameServer - Service Port & Database Mapping (Canonical Reference)

> **Last Updated:** 2026-02-12  
> **Status:** ✅ ALL CONFLICTS RESOLVED  
> **Purpose:** Single source of truth for all service ports, DB ports, and DB names

---

## 📋 COMPLETE SERVICE MAPPING

### Infrastructure Services (No DB)

| # | Service | HTTP Port | gRPC Port | DB | Notes |
|---|---------|-----------|-----------|-----|-------|
| 1 | eureka-server | 8761 | - | - | Service Discovery |
| 2 | config-service | 8888 | - | - | Config Server |
| 3 | gateway-service | 8080 | - | - | API Gateway |
| 4 | session-service | 8096 | - | Redis only | Auth/Session |
| 5 | webSocket-server | 8094 | - | - | Real-time Communication |
| 6 | scheduler-service | 8550 | - | - | Job Scheduler |
| 7 | localization-service | 8560 | 9560 | - | i18n |
| 8 | file-service | 8540 | 9540 | - | File Upload |
| 9 | item-service | 8220 | - | - | Item metadata (stateless) |

### P0 — Core Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 10 | user-service | 8110 | - | 33061 | `user_db` | gameserver-userdb |
| 11 | role-service | 8410 | 9410 | 33062 | `db_role` | gameserver-roledb |
| 12 | serverInfo-service | 8095 | - | 33063 | `serverinfo_db` | gameserver-serverinfodb |

### P1 — Economy Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 13 | wallet-service | 8210 | - | 33064 | `wallet_db` | gameserver-walletdb |
| 14 | report-service | 8098 | - | 33065 | `report_db` | gameserver-reportdb |
| 15 | iap-verify-service | 8580 | - | 33066 | `iap_verify_db` | gameserver-iapverifydb |
| 16 | bag-service | 8230 | 9230 | 33067 | `bag_db` | gameserver-bagdb |
| 17 | equip-service | 8240 | 9240 | 33068 | `equip_db` | gameserver-equipdb |
| 18 | shop-service | 8260 | 9260 | 33069 | `shop_db` | gameserver-shopdb |
| 19 | box-service | 8290 | - | 33070 | `box_db` | gameserver-boxdb |
| 20 | crafting-service | 8280 | 9280 | 33071 | `crafting_db` | gameserver-craftingdb |
| 21 | arena-service | 8084 | 9084 | 33072 | `game_arena` | gameserver-arenadb |

### P2 — Combat & Progression Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 22 | trial-service | 8300 | 9300 | 33073 | `game_trial` | gameserver-trialdb |
| 23 | task-service | 8097 | - | 33074 | `game_task` | gameserver-taskdb |
| 24 | starmap-service | 8092 | - | 33075 | `game_starmap` | gameserver-starmapdb |
| 25 | territory-service | 8360 | - | 33076 | `game_territory` | gameserver-territorydb |
| 26 | pet-service | 8112 | - | 33077 | `game_pet` | gameserver-petdb |
| 27 | mount-service | 8089 | - | 33078 | `game_mount` | gameserver-mountdb |
| 28 | rune-service | 8093 | - | 33079 | `game_rune` | gameserver-runedb |
| 29 | chat-service | 8460 | - | 33080 | `chat_db` | gameserver-chatdb |
| 30 | shizhuang-service | 8350 | - | 33081 | `game_shizhuang` | gameserver-shizhuangdb |
| 31 | angel-service | 8090 | - | 33082 | `game_angel` | gameserver-angeldb |
| 32 | artifact-service | 8091 | - | 33083 | `game_artifact` | gameserver-artifactdb |

### P3 — Social Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 33 | guild-service | 8440 | - | 33084 | `guild_db` | gameserver-guilddb |
| 34 | friend-service | 8450 | - | 33085 | `friend_db` | gameserver-frienddb |
| 35 | mail-service | 8470 | - | 33086 | `mail_db` | gameserver-maildb |
| 36 | leaderboard-service | 8480 | - | 33087 | `leaderboard_db` | gameserver-leaderboarddb |

### P4 — Supporting & Admin Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 37 | gm-service | 9093 | - | 33089 | `game_gm` | gameserver-gmdb |
| 38 | notification-service | 8520 | 9520 | 33090 | `game_notification` | gameserver-notificationdb |
| 39 | moderation-service | 8570 | - | 33091 | `game_moderation` | gameserver-moderationdb |
| 40 | analytics-service | 8510 | 9510 | 33092 | `game_analytics` | gameserver-analyticsdb |
| 41 | anti-cheat-service | 8590 | - | 33093 | `game_anticheat` | gameserver-anticheatdb |
| 42 | admin-service | 9091 | - | 9088 | `game_admin` | gameserver-admindb |
| 43 | main-fb-service | 8128 | 9128 | 33094 | `game_mainfb` | gameserver-mainfbdb |
| 44 | escort-service | 8340 | - | 33095 | `game_escort` | gameserver-escortdb |

### P5 — Game World Services

| # | Service | HTTP Port | gRPC Port | DB Port | DB Name | Docker Container |
|---|---------|-----------|-----------|---------|---------|-----------------|
| 45 | world-service | 8370 | - | 33096 | `game_world` | gameserver-worlddb |
| 46 | globalserver-service | 8100 | - | 3317 | `globalserver_service_db` | gameserver-globalserverdb |
| 47 | gameworld-service | 8105 | 9105 | 3308 | `gameworld_db` | gameserver-gameworlddb |
| 48 | battleserver-service | 8082 | 9082 | 3328 | `db_battle_service` | gameserver-battledb |
| 49 | drop-service | 8250 | - | ~~3313~~ | ~~`drop_db`~~ | ~~gameserver-dropdb~~ | **Không dùng DB** - load XML từ config-service (Feign) |
| 50 | gift-service | 8270 | - | ~~3315~~ | ~~`gift_db`~~ | ~~gameserver-giftdb~~ | **Không dùng DB** - load gift.json từ config-service (Feign) |

---

## ⚠️ Legacy DB Ports (not in 33xxx range)

Các service dưới đây dùng port DB không theo chuẩn 33xxx. Chúng vẫn hoạt động nhưng nên migrate dần sang 33xxx để đồng nhất.

| Service | DB Port | DB Name | Recommended New Port |
|---------|---------|---------|---------------------|
| admin-service | 9088 | `game_admin` | 33088 |
| gameworld-service | 3308 | `gameworld_db` | 33097 |
| drop-service | 3313 | `drop_db` | 33098 |
| gift-service | 3315 | `gift_db` | 33099 |
| globalserver-service | 3317 | `globalserver_service_db` | 33100 |
| battleserver-service | 3328 | `db_battle_service` | 33101 |

---

## 🔧 Infrastructure Services (Non-DB)

| Service | Port | Purpose |
|---------|------|---------|
| Redis | 6379 | Cache & Session |
| Kafka | 9092 (internal) / 29092 (external) | Event Streaming |
| Kafdrop | 9000 | Kafka UI |
| Prometheus | 9090 | Metrics Collection |
| Grafana | 3000 | Monitoring Dashboard |

---

## 📊 Port Range Convention

| Range | Usage |
|-------|-------|
| 8080–8099 | Infrastructure & legacy game services |
| 8100–8199 | Core services (user, global, gameworld, main-fb) |
| 8200–8299 | Economy services (wallet, item, bag, equip, drop, shop, gift, crafting, box) |
| 8300–8399 | Combat & progression (trial, escort, shizhuang, territory, world) |
| 8400–8499 | Social services (role, guild, friend, chat, mail, leaderboard) |
| 8500–8599 | Supporting services (analytics, notification, file, scheduler, localization, moderation, iap-verify, anti-cheat) |
| 8761 | Eureka Server |
| 8888 | Config Service |
| 9088 | Admin DB (legacy) |
| 9091–9093 | Admin/GM services |
| 9084–9560 | gRPC ports |
| 33061–33096 | Standard MySQL DB ports (one per service) |

---

## 🔐 Database Credentials

| Environment | Username | Password | Root Password |
|-------------|----------|----------|---------------|
| Local (default in application.yml) | `root` | `1234` | `root` |
| Local (application-local.yml) | `root` | `1234` | - |
| docker-compose-databases.yml | `tpnam` | `121831` | `root` |

> **⚠️ Lưu ý:** Tất cả services đã set `spring.profiles.active: local` mặc định.  
> Default credentials trong application.yml đã đổi từ `tpnam/121831` sang `root/1234`.

---

## 📝 Services KHÔNG dùng Database

| Service | Lý do | Data Source |
|---------|-------|-------------|
| drop-service | Load drop tables từ XML | config-service (Feign) + Caffeine cache |
| gift-service | Load gift data từ JSON | config-service (Feign) + Redis cache |

> DB containers `gameserver-dropdb` (port 3313) và `gameserver-giftdb` (port 3315) trong docker-compose **không cần thiết** và có thể xóa để tiết kiệm tài nguyên.

---

## 🐳 Docker Compose Files

| File | Location | Purpose |
|------|----------|---------|
| `docker-compose.yml` | `GameServer/docker/` | Full stack (DBs + infra + services) |
| `docker-compose-databases.yml` | `GameServer/docker/` | All databases only (35 instances) |
| `docker-compose-databases-optimized.yml` | `GameServer/docker/` | Memory-optimized DBs with profiles |

### Quick Start

```bash
# Start ONLY databases (recommended for local dev)
cd GameServer/docker
docker-compose -f docker-compose-databases.yml up -d

# Start with profile (optimized version)
docker-compose -f docker-compose-databases-optimized.yml --profile core up -d
docker-compose -f docker-compose-databases-optimized.yml --profile core --profile economy up -d
docker-compose -f docker-compose-databases-optimized.yml --profile full up -d

# Start everything (main compose)
docker-compose up -d

# Check / Stop
docker-compose ps
docker-compose down
```

---

## ✅ Resolved Conflicts (2026-02-12)

### HTTP Port Conflicts Fixed

| Service | Old Port | New Port | Conflicted With |
|---------|----------|----------|-----------------|
| territory-service | 8095 | **8360** | serverInfo-service (8095) |
| trial-service | 8094 | **8300** | webSocket-server (8094) |
| escort-service | 8096 | **8340** | session-service (8096) |
| world-service | 8084 | **8370** | arena-service (8084) |
| gm-service | 9092 | **9093** | Kafka internal (9092) |

### DB Port Conflicts Fixed (docker-compose.yml)

| DB | Old Port | New Port |
|----|----------|----------|
| walletdb | 3342 | **33064** |
| arenadb | 3327 | **33072** |
| craftingdb | 3316 | **33071** |

### DB Port Conflicts Fixed (docker-compose-databases.yml)

| DB | Old Port | New Port |
|----|----------|----------|
| userdb | 33062 | **33061** |
| roledb | 3319 | **33062** |
| serverinfodb | 3318 | **33063** |
| bagdb | 33065 | **33067** |
| equipdb | 33066 | **33068** |
| shopdb | 33068 | **33069** |
| boxdb | 33071 | **33070** |
| iapverifydb | 3357 | **33066** |
| reportdb | 33063 | **33065** |
| craftingdb | 33070 | **33071** |

### DB Name Conflicts Fixed

| Location | Old Name | New Name |
|----------|----------|----------|
| docker-compose.yml (bagdb) | `db_bag` | **`bag_db`** |
| docker-compose.yml (reportdb) | `report_game_h2` | **`report_db`** |
| init.sql | `db_bag` | **`bag_db`** |
| init.sql | `report_game_h2` | **`report_db`** |

### Missing DBs Added to docker-compose-databases.yml

| DB | Port | DB Name |
|----|------|---------|
| dropdb | 3313 | `drop_db` |
| giftdb | 3315 | `gift_db` |
| globalserverdb | 3317 | `globalserver_service_db` |
| battledb | 3328 | `db_battle_service` |
| gameworlddb | 3308 | `gameworld_db` |

### Service application.yml Fixed

| Service | Change |
|---------|--------|
| crafting-service | DB port 33070 → **33071** |

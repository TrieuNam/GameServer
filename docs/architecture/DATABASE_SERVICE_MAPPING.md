# DATABASE & SERVICE MAPPING

**Date:** 01/02/2026  
**Total Services:** 51 (36 with database + 15 stateless/infrastructure)  
**Database User:** `tpnam`  
**Database Password:** `121831`

---

## SERVICES WITH DATABASE (36 services)

### P0 - Core Services

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| user-service | 8400 | - | **33062** | `user_db` | tpnam | ✅ Active |
| role-service | 8410 | **9090** | **3319** | `db_role` | tpnam | ✅ Active |
| serverInfo-service | 8420 | - | **3318** | `serverinfo_db` | tpnam | ✅ Active |

---

### P1 - Economy Services

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| wallet-service | 8200 | - | **33064** | `wallet_db` | tpnam | ✅ Active |
| shop-service | 8260 | **9089** | **33068** | `shop_db` | tpnam | ✅ Active |
| bag-service | 8230 | **9087** | **33065** | `bag_db` | tpnam | ✅ Active |
| equip-service | 8240 | **9088** | **33066** | `equip_db` | tpnam | ✅ Active |
| box-service | 8290 | - | **33071** | `box_db` | tpnam | ✅ Active |
| iap-verify-service | 8220 | - | **3357** | `iap_verify_db` | tpnam | ✅ Active |
| report-service | 8210 | - | **33063** | `report_db` | tpnam | ✅ Active |

---

### P2 - Combat & Progression Services

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| trial-service | 8094 | **9094** | **33073** | `game_trial` | tpnam | ✅ Active |
| arena-service | 8084 | **9370** | **33072** | `game_arena` | tpnam | ✅ Active |
| task-service | 8095 | - | **33074** | `game_task` | tpnam | ✅ Active |
| starmap-service | 8120 | - | **33075** | `game_starmap` | tpnam | ✅ Active |
| territory-service | 8122 | - | **33076** | `game_territory` | tpnam | ✅ Active |
| pet-service | 8300 | - | **33077** | `game_pet` | tpnam | ✅ Active |
| mount-service | 8310 | - | **33078** | `game_mount` | tpnam | ✅ Active |
| rune-service | 8320 | - | **33079** | `game_rune` | tpnam | ✅ Active |
| shizhuang-service | 8350 | - | **33081** | `game_shizhuang` | tpnam | ✅ Active |
| angel-service | 8360 | - | **33082** | `game_angel` | tpnam | ✅ Active |
| artifact-service | 8370 | - | **33083** | `game_artifact` | tpnam | ✅ Active |
| crafting-service | 8280 | **9099** | **33070** | `crafting_db` | tpnam | ✅ Active |

---

### P3 - Social Services

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| guild-service | 8440 | - | **33084** | `guild_db` | tpnam | ✅ Active |
| friend-service | 8450 | - | **33085** | `friend_db` | tpnam | ✅ Active |
| mail-service | 8460 | - | **33086** | `mail_db` | tpnam | ✅ Active |
| chat-service | 8470 | - | **33080** | `chat_db` | tpnam | ✅ Active |
| leaderboard-service | 8480 | - | **33087** | `leaderboard_db` | tpnam | ✅ Active |

---

### P4 - Supporting Services

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| admin-service | 9091 | - | **33088** | `game_admin` | tpnam | ✅ Active |
| gm-service | 8500 | - | **33089** | `game_gm` | root | ✅ Active |
| notification-service | 8520 | **9520** | **33090** | `game_notification` | tpnam | ✅ Active |
| moderation-service | 8530 | - | **33091** | `game_moderation` | tpnam | ✅ Active |
| analytics-service | 8510 | **9510** | **33092** | `game_analytics` | tpnam | ✅ Active |
| anti-cheat-service | 8093 | - | **33093** | `game_anticheat` | tpnam | ✅ Active |

---

### P5 - Additional Game Features (3 services with database)

| Service | HTTP Port | gRPC Port | MySQL Port | Database Name | Username | Status |
|---------|-----------|-----------|------------|---------------|----------|--------|
| main-fb-service | 8128 | **9096** | **33094** | `game_mainfb` | tpnam | ✅ Active |
| escort-service | 8129 | - | **33095** | `game_escort` | tpnam | ✅ Active |
| world-service | 8084 | - | **33096** | `game_world` | tpnam | ✅ Active |

---

### Stateless Services (No Database Needed - 5 services)

**These services are stateless and use only Redis/Kafka or load config from JSON/XML:**

| Service | HTTP Port | gRPC Port | Purpose | Storage | Status | POM Status |
|---------|-----------|-----------|---------|---------|--------|------------|
| drop-service | 8250 | - | Drop table calculator | Loads config from XML | ✅ Implemented | ✅ MySQL removed |
| gift-service | 8270 | - | Gift code redemption | Loads config from JSON | ✅ Implemented | ✅ No MySQL |
| gameworld-service | 8105 | **9095** | Player position tracking | **Redis (30min TTL)** | ✅ Implemented | ✅ MySQL removed |
| globalserver-service | 8100 | - | Global server coordinator | Redis/Kafka only | ✅ Implemented | ✅ MySQL removed |
| battleserver-service | 8082 | **9092** | Battle logic engine | Redis/Kafka only | ✅ Implemented | ✅ MySQL removed |

**Note:** Removed `spring-boot-starter-data-jpa`, `mysql-connector-j`, and `flyway` dependencies from these services.

---

## SERVICES WITHOUT DATABASE (11 infrastructure + support services)

**These services only use Redis or no persistence:**

| Service | HTTP Port | gRPC Port | Storage | Purpose | Status | POM Status |
|---------|-----------|-----------|---------|---------|--------|------------|
| session-service | 8096 | - | Redis only | JWT session management | ✅ Implemented | ✅ No MySQL |
| websocket-server | 8094 | - | Redis + Kafka | Real-time communication | ✅ Implemented | ✅ No MySQL |
| scheduler-service | 8550 | - | Redis only | Scheduled tasks | ✅ Implemented | ✅ No MySQL |
| gateway-service | 8083 | - | None | API Gateway routing | ✅ Implemented | ✅ No MySQL |
| eureka-server | 8761 | - | None | Service discovery | ✅ Implemented | ✅ No MySQL |
| config-service | 8888 | - | Git/File | Configuration management | ✅ Implemented | ✅ No MySQL |
| item-service | 8330 | - | Cache only | Item templates (read-only) | ✅ Implemented | ✅ No MySQL |
| dataaccess-service | 8340 | - | None | Data access layer | ✅ Implemented | ✅ No MySQL |
| localization-service | 8560 | **9560** | Redis | Multi-language support | ✅ Implemented | ✅ No MySQL |
| file-service | 8540 | **9540** | File system | File upload/storage | ✅ Implemented | ✅ No MySQL |

---

## PORT SUMMARY

### Dedicated MySQL Instances - All Services (33 active services)

**Each service has its own MySQL instance on unique port:**

#### P0 - Core Services (3 ports)
- **3318** - serverInfo-service (serverinfo_db)
- **3319** - role-service (db_role)
- **33062** - user-service (user_db)

#### P1 - Economy Services (7 ports)
- **3357** - iap-verify-service (iap_verify_db)
- **33063** - report-service (report_db)
- **33064** - wallet-service (wallet_db)
- **33065** - bag-service (bag_db)
- **33066** - equip-service (equip_db)
- **33068** - shop-service (shop_db)
- **33071** - box-service (box_db)

#### P2 - Combat & Progression (12 ports)
- **33070** - crafting-service (crafting_db)
- **33072** - arena-service (game_arena)
- **33073** - trial-service (game_trial)
- **33074** - task-service (game_task)
- **33075** - starmap-service (game_starmap)
- **33076** - territory-service (game_territory)
- **33077** - pet-service (game_pet)
- **33078** - mount-service (game_mount)
- **33079** - rune-service (game_rune)
- **33081** - shizhuang-service (game_shizhuang)
- **33082** - angel-service (game_angel)
- **33083** - artifact-service (game_artifact)

#### P3 - Social Services (5 ports)
- **33080** - chat-service (chat_db)
- **33084** - guild-service (guild_db)
- **33085** - friend-service (friend_db)
- **33086** - mail-service (mail_db)
- **33087** - leaderboard-service (leaderboard_db)

#### P4 - Supporting Services (6 ports)
- **33088** - admin-service (game_admin)
- **33089** - gm-service (game_gm)
- **33090** - notification-service (game_notification)
- **33091** - moderation-service (game_moderation)
- **33092** - analytics-service (game_analytics)
- **33093** - anti-cheat-service (game_anticheat)

#### P5 - Additional Game Features (2 ports)
- **33094** - main-fb-service (game_mainfb)
- **33095** - escort-service (game_escort)

### Port Range Summary
- **3318-3357**: Legacy ports (3 services)
- **33062-33095**: New standardized ports (32 services)

---

## DATABASE ARCHITECTURE

### Dedicated MySQL Instance per Service (Microservices Pattern)
**Count:** 35 active services  
**Ports:** 3318-3357, 33062-33095  
**Pattern:** Each service has its own MySQL container and port

```
user-service:8110 → MySQL:33062 (user_db)
wallet-service:8330 → MySQL:33064 (wallet_db)
shop-service:8099 → MySQL:33068 (shop_db)
guild-service:8440 → MySQL:33084 (guild_db)
friend-service:8450 → MySQL:33085 (friend_db)
trial-service:? → MySQL:33073 (game_trial)
main-fb-service:8128 → MySQL:33094 (game_mainfb)
escort-service:8096 → MySQL:33095 (game_escort)
... (35 independent MySQL instances)
```

### Benefits:
- ✅ True microservices isolation
- ✅ Independent scaling per service
- ✅ No cross-service database dependencies
- ✅ Better fault isolation
- ✅ Each service can have different MySQL versions/configs

### Resource Requirements:
- **35 MySQL containers** (approximately 200-500MB RAM each)
- **Total estimated RAM:** 7GB - 17.5GB for databases alone
- **Ports used:** 3318-3357 (4 ports) + 33062-33095 (34 ports)

---

## DOCKER COMPOSE MAPPING

### ✅ NEW: GameServer/docker/docker-compose-databases.yml

**Complete setup with 35 MySQL instances + Redis + Kafka**

All databases now use correct ports matching application configs:

#### P0 - Core Services (3 databases)
- **userdb** → `33062:3306` → user_db
- **roledb** → `3319:3306` → db_role
- **serverinfodb** → `3318:3306` → serverinfo_db

#### P1 - Economy Services (7 databases)
- **walletdb** → `33064:3306` → wallet_db
- **shopdb** → `33068:3306` → shop_db
- **bagdb** → `33065:3306` → bag_db
- **equipdb** → `33066:3306` → equip_db
- **boxdb** → `33071:3306` → box_db
- **iapverifydb** → `3357:3306` → iap_verify_db
- **reportdb** → `33063:3306` → report_db

#### P2 - Combat & Progression (12 databases)
- **craftingdb** → `33070:3306` → crafting_db
- **arenadb** → `33072:3306` → game_arena
- **trialdb** → `33073:3306` → game_trial
- **taskdb** → `33074:3306` → game_task
- **starmapdb** → `33075:3306` → game_starmap
- **territorydb** → `33076:3306` → game_territory
- **petdb** → `33077:3306` → game_pet
- **mountdb** → `33078:3306` → game_mount
- **runedb** → `33079:3306` → game_rune
- **shizhuangdb** → `33081:3306` → game_shizhuang
- **angeldb** → `33082:3306` → game_angel
- **artifactdb** → `33083:3306` → game_artifact

#### P3 - Social Services (5 databases)
- **chatdb** → `33080:3306` → chat_db
- **guilddb** → `33084:3306` → guild_db
- **frienddb** → `33085:3306` → friend_db
- **maildb** → `33086:3306` → mail_db
- **leaderboarddb** → `33087:3306` → leaderboard_db

#### P4 - Supporting Services (6 databases)
- **admindb** → `33088:3306` → game_admin
- **gmdb** → `33089:3306` → game_gm
- **notificationdb** → `33090:3306` → game_notification
- **moderationdb** → `33091:3306` → game_moderation
- **analyticsdb** → `33092:3306` → game_analytics
- **anticheatdb** → `33093:3306` → game_anticheat

#### P5 - Additional Game Features (2 databases)
- **mainfbdb** → `33094:3306` → game_mainfb
- **escortdb** → `33095:3306` → game_escort

### Quick Start

```powershell
cd D:\project\serverGame\GameServer\docker
docker-compose -f docker-compose-databases.yml up -d
```

**See [DATABASE_DOCKER_GUIDE.md](GameServer/docker/DATABASE_DOCKER_GUIDE.md) for detailed instructions.**

### ⚠️ OLD: GameServer/docker/docker-compose.yml

**Status:** Contains port conflicts and missing containers. Use `docker-compose-databases.yml` instead.

### ✅ COMPLETED: Application Config Updates

**All 35 services already have correct port configurations in application.yml**

- ✅ P0 services (3): Using ports 3318, 3319, 33062
- ✅ P1 services (7): Using ports 3357, 33063-33068, 33071
- ✅ P2 services (12): Using ports 33070, 33072-33083
- ✅ P3 services (5): Using ports 33080, 33084-33087
- ✅ P4 services (6): Using ports 33088-33093
- ✅ P5 services (2): Using ports 33094-33095

**Note:** gm-service port already updated to 33089, but credentials still need update from root/1234 → tpnam/121831

---

### ✅ COMPLETED: Docker Compose Setup

**New file created:** `GameServer/docker/docker-compose-databases.yml`

- ✅ 35 MySQL containers with correct ports
- ✅ All port conflicts resolved
- ✅ Redis + Kafka + Kafdrop included
- ✅ Healthchecks configured
- ✅ Volumes for data persistence

**No remaining conflicts!** All services match Docker ports.

---

---

---

## ADMIN SERVICE - SERVICE MANAGEMENT

### Database Schema

**Admin Service** manages all 51 services through database:
- **Table:** `service_config`
- **Database:** `game_admin` on port `33088`
- **Migrations:**
  - `V1__Init_complete_service_config.sql` - Initial schema with 53 services
  - `V2__Complete_50_services.sql` - Cleanup to 50 services
  - `V3__Update_51_services.sql` - **Latest: 51 services** (added dataaccess-service)

### Service Breakdown in Admin Database

**P0 - Infrastructure (4):**
1. eureka-server
2. gateway-service
3. config-service
4. websocket-server

**P1 - Database + Core Gameplay (13):**
5. session-service
6. user-service
7. report-service
8. wallet-service
9. item-service
10. bag-service
11. equip-service
12. drop-service
13. shop-service
14. gift-service
15. crafting-service
16. box-service
17. **dataaccess-service** *(added in V3)*

**P2 - Combat + Social (13):**
18. arena-service
19. battleserver-service
20. world-service
21. gameworld-service
22. trial-service
23. territory-service
24. escort-service
25. globalserver-service
26. chat-service
27. friend-service
28. guild-service
29. leaderboard-service
30. mail-service

**P3 - Enhancement + Support (17):**
31. role-service
32. task-service
33. pet-service
34. mount-service
35. angel-service
36. starmap-service
37. artifact-service
38. rune-service
39. shizhuang-service
40. analytics-service
41. notification-service
42. scheduler-service
43. file-service
44. localization-service
45. moderation-service
46. iap-verify-service
47. anti-cheat-service

**P4 - Optional (2):**
48. serverInfo-service
49. main-fb-service

**SPECIAL - Admin (2):**
50. admin-service
51. gm-service

### Admin Service Features

- ✅ **Service Registry:** All 51 services tracked in database
- ✅ **Startup Order:** Phase-based sequential startup (P0→P1→P2→P3→P4→SPECIAL)
- ✅ **Docker Integration:** Tracks which services need Docker containers
- ✅ **Health Monitoring:** Status tracking (STOPPED/RUNNING/ERROR)
- ✅ **Process Management:** JVM args, app args, process IDs
- ✅ **Control Panel:** Web UI on port 9091

---

## KAFKA INTEGRATION (15 services)

### Kafka Infrastructure

**Single Kafka cluster shared by all services:**
- **Internal Port**: `9092` (for Docker network communication)
- **External Port**: `29092` (for localhost/host machine access)
- **Image**: `confluentinc/cp-kafka:7.5.0`
- **Mode**: KRaft (no Zookeeper)
- **UI**: Kafdrop on port `9000` (http://localhost:9000)

### Services Using Kafka (15 total)

| Service | HTTP Port | Bootstrap Server | Role | Topics | Status |
|---------|-----------|------------------|------|--------|--------|
| **analytics-service** | 8510 | localhost:9092 | 🔵 Consumer | wallet.transaction, shop.purchase, battle.*, role.level.up, task.completed, guild.*, pet.* | ✅ Configured |
| **notification-service** | 8520 | localhost:9092 | 🟢 Producer | notification.sent, notification.read, notification.failed | ✅ Configured |
| **anti-cheat-service** | 8093 | localhost:9092 | 🟣 Both | cheat-detected, player-ban, battle.ended | ✅ Configured |
| **arena-service** | 8084 | localhost:29092 | 🟢 Producer | arena.match.* | ✅ Configured |
| **task-service** | 8095 | localhost:29092 | 🔵 Consumer | arena.match.ended, trial.completed, trial.failed | ✅ Configured |
| **leaderboard-service** | 8480 | localhost:29092 | 🔵 Consumer | arena.match.ended, trial.completed | ✅ Configured |
| **bag-service** | 8230 | localhost:29092 | 🔵 Consumer | gameh5.bag.grant | ✅ Configured |
| **trial-service** | 8094 | localhost:29092 | 🟢 Producer | trial.completed, trial.failed | ✅ Configured |
| **battleserver-service** | 8082 | localhost:29092 | 🟢 Producer | battle.started, battle.ended | ✅ Configured |
| **pet-service** | 8300 | - | 🟢 Producer | pet.activated, pet.level.up, pet.evolved | ⚠️ Missing config |
| **starmap-service** | 8120 | - | 🟢 Producer | star.activated, constellation.completed | ⚠️ Missing config |
| **rune-service** | 8320 | - | 🟢 Producer | rune.equipped, rune.upgraded | ⚠️ Missing config |
| **mount-service** | 8310 | - | 🟢 Producer | mount.activated, mount.level.up, mount.evolved | ⚠️ Missing config |
| **angel-service** | 8360 | - | 🟢 Producer | angel.activated, angel.level.up, angel.evolved | ⚠️ Missing config |
| **artifact-service** | 8370 | - | 🟢 Producer | artifact.equipped, artifact.upgraded | ⚠️ Missing config |

### Port Usage Analysis

**✅ Services using `localhost:9092` (3):**
- analytics-service
- notification-service
- anti-cheat-service

**✅ Services using `localhost:29092` (6):**
- arena-service
- task-service
- leaderboard-service
- bag-service
- trial-service
- battleserver-service

**⚠️ Services missing Kafka config (6):**
- pet-service
- starmap-service
- rune-service
- mount-service
- angel-service
- artifact-service

### Port Explanation

**Kafka exposes 2 ports but it's the SAME Kafka instance:**
- **Port 9092**: Internal Docker network address (`kafka:9092`) - used by services running INSIDE Docker
- **Port 29092**: External localhost address (`localhost:29092`) - used by services running ON HOST MACHINE

**Both ports connect to the same Kafka cluster!**

### Implementation Requirements

**6 services cần thêm Kafka configuration trong `application.yml`:**

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
```

**Services cần update:**
1. pet-service/src/main/resources/application.yml
2. starmap-service/src/main/resources/application.yml
3. rune-service/src/main/resources/application.yml
4. mount-service/src/main/resources/application.yml
5. angel-service/src/main/resources/application.yml
6. artifact-service/src/main/resources/application.yml

---

## RECOMMENDATIONS

### ✅ Recommended: Fix Docker to Match Application Ports

Update `GameServer/docker/docker-compose.yml`:
- userdb: `3307:3306` → `33062:3306`
- reportdb: `3309:3306` → `33063:3306`
- walletdb: `3342:3306` → `33064:3306`
- bagdb: `3311:3306` → `33065:3306`
- equipdb: `3312:3306` → `33066:3306`
- boxdb: `3310:3306` → `33071:3306`
- shopdb: `3343:3306` → `33068:3306`

**Then add 19 new MySQL containers** for services currently on port 3306.

### Alternative: Update All Application Configs

Update all 19 application.yml files with new ports 33073-33093, but this is more work than fixing Docker.

**Root Cause:** Application configs don't match Docker exposed ports!

### ⚠️ Add Kafka Configuration to 6 Services

6 services có KafkaTemplate code nhưng thiếu config trong application.yml. Cần thêm:
- pet-service
- starmap-service
- rune-service
- mount-service
- angel-service
- artifact-service

---

## CREDENTIALS SUMMARY

AllDEPLOYMENT GUIDE

### Step 1: Start Database Containers

```powershell
cd D:\project\serverGame\GameServer\docker
docker-compose -f docker-compose-databases.yml up -d
```

### Step 2: Verify Databases

```powershell
# Check all containers running
docker-compose -f docker-compose-databases.yml ps

# Test connection to a database
mysql -h localhost -P 33062 -u tpnam -p121831 user_db
```

### Step 3: Start Services

Services will auto-run Flyway migrations on startup. Start in order:

1. **Infrastructure:** eureka-server, config-service
2. **P0 (Core):** user-service, role-service, serverInfo-service
3. **P1 (Economy):** wallet-service, shop-service, bag-service, equip-service, box-service
4. **P2-P5:** Other services

### Step 4: Monitor

- Kafka UI: http://localhost:9000
- Service health: Check Eureka dashboard
- Logs: `docker-compose -f docker-compose-databases.yml logs -f`
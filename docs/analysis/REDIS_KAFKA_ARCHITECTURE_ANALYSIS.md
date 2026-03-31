# 📊 Redis & Kafka Architecture Analysis

**Date**: February 1, 2026  
**Purpose**: Analyze Redis and Kafka usage across all microservices

---

## Executive Summary

### Overall Statistics

| Resource | Configuration | Services Using |
|----------|--------------|----------------|
| **Redis** | 1 shared server, multiple DBs | 29 services |
| **Kafka** | 1-2 shared clusters | 19 services |
| **MySQL** | Separate DB per service | 22 services |
| **No Infrastructure** | Pure logic/utility | 21 services |

---

## 🔴 REDIS ARCHITECTURE

### Configuration: **SHARED SERVER with ISOLATED DATABASES**

**Redis Server**: `localhost:6379` (Single shared instance)

#### Database Allocation Strategy

```
┌─────────────────────────────────────────────────┐
│  Redis Server: localhost:6379                   │
├─────────────────────────────────────────────────┤
│  DB 0: General Purpose (18 services)            │
│  DB 3: Guild System (1 service)                 │
│  DB 4: Analytics & Friends (2 services)         │
│  DB 5: Chat & Scheduler (2 services)            │
│  DB 6: Localization & Mail (2 services)         │
│  DB 7: Leaderboard & Moderation (2 services)    │
│  DB 8: IAP Verification (1 service)             │
│  DB 9: Anti-Cheat (1 service)                   │
└─────────────────────────────────────────────────┘
```

### Redis DB 0 - General Purpose (18 services)
**Most services use DB 0 for general caching**

1. **arena-service** - Arena rankings cache
2. **bag-service** - Inventory cache
3. **battleserver-service** - Battle state cache
4. **box-service** - Loot box cache
5. **config-service** - Configuration cache
6. **drop-service** - Drop rates cache
7. **equip-service** - Equipment cache
8. **gameworld-service** - World state cache
9. **gift-service** - Gift codes cache
10. **globalserver-service** - Global data cache
11. **gm-service** - GM session cache
12. **main-fb-service** - Facebook integration cache
13. **role-service** - Player data cache
14. **serverInfo-service** - Server info cache
15. **session-service** - Session data **PRIMARY USE**
16. **shizhuang-service** - Costume cache
17. **task-service** - Task progress cache
18. **user-service** - User profile cache

### Redis DB 3 - Guild System (1 service)
- **guild-service** - Guild data, member lists, activities

### Redis DB 4 - Analytics & Social (2 services)
- **analytics-service** - Metrics aggregation, temporary stats
- **friend-service** - Friend lists, online status

### Redis DB 5 - Communication & Jobs (2 services)
- **chat-service** - Chat history, online users, rate limiting
- **scheduler-service** - Job locks, scheduled task state

### Redis DB 6 - Content (2 services)
- **localization-service** - Translation cache (20+ languages)
- **mail-service** - Mail queue, notification cache

### Redis DB 7 - Rankings & Moderation (2 services)
- **leaderboard-service** - Rankings, scores (sorted sets)
- **moderation-service** - Ban cache, spam detection

### Redis DB 8 - IAP (1 service)
- **iap-verify-service** - Purchase verification cache

### Redis DB 9 - Anti-Cheat (1 service)
- **anti-cheat-service** - Behavior patterns, anomaly detection

---

## 🟡 KAFKA ARCHITECTURE

### Configuration: **SHARED CLUSTER with SEPARATE TOPICS**

Kafka is configured with **2 endpoint variations** (same cluster):
- `localhost:9092` - Standard Kafka port
- `localhost:29092` - Docker-exposed port (default for most services)

### Kafka Usage by Service (19 services)

#### Group 1: Using `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}` (8 services)
**Pattern**: Configurable with environment variable, defaults to Docker port

1. **arena-service** - Arena match results, rating updates
2. **bag-service** - Inventory changes
3. **box-service** - Loot box openings
4. **equip-service** - Equipment changes
5. **gift-service** - Gift code redemptions
6. **role-service** - Player level/stat changes
7. **trial-service** - Dungeon completion events
8. **webSocket-server** - Real-time events broadcast

#### Group 2: Using `localhost:29092` (6 services)
**Pattern**: Hardcoded Docker port

9. **battleserver-service** - Battle events, PvP matches
10. **gameworld-service** - World events, boss spawns
11. **globalserver-service** - Cross-server events
12. **leaderboard-service** - Ranking updates
13. **report-service** - Analytics events
14. **task-service** - Task progress events

#### Group 3: Using `localhost:9092` (5 services)
**Pattern**: Standard Kafka port

15. **analytics-service** - Event consumption, metrics
16. **anti-cheat-service** - Suspicious activity monitoring
17. **iap-verify-service** - Purchase events
18. **moderation-service** - Report events
19. **notification-service** - Notification triggers

### Kafka Topics (Estimated)

Common topics across services:
- `player.events` - General player actions
- `battle.events` - Combat and PvP
- `inventory.events` - Item/equipment changes
- `economy.events` - Currency transactions
- `social.events` - Chat, friend, guild
- `analytics.events` - Metrics and tracking
- `notification.events` - Push notifications

---

## 🔵 SERVICES BY INFRASTRUCTURE TYPE

### Type 1: MySQL + Redis + Kafka (15 services)
**Full infrastructure stack**

1. analytics-service (MySQL 3333, Redis DB4, Kafka 9092)
2. anti-cheat-service (MySQL 3358, Redis DB9, Kafka 9092)
3. arena-service (MySQL 3323, Redis DB0, Kafka 29092)
4. bag-service (MySQL 3311, Redis DB0, Kafka 29092)
5. battleserver-service (MySQL 3317, Redis DB0, Kafka 29092)
6. box-service (MySQL 3310, Redis DB0, Kafka 29092)
7. equip-service (MySQL 3312, Redis DB0, Kafka 29092)
8. gameworld-service (MySQL 3344, Redis DB0, Kafka 29092)
9. gift-service (MySQL 3315, Redis DB0, Kafka 29092)
10. globalserver-service (MySQL 3318, Redis DB0, Kafka 29092)
11. iap-verify-service (MySQL 3357, Redis DB8, Kafka 9092)
12. leaderboard-service (MySQL 3330, Redis DB7, Kafka 29092)
13. moderation-service (MySQL 3356, Redis DB7, Kafka 9092)
14. role-service (MySQL 3308, Redis DB0, Kafka 29092)
15. task-service (MySQL 3326, Redis DB0, Kafka 29092)

### Type 2: MySQL + Redis (14 services)
**Database + cache, no events**

16. chat-service (MySQL 3327, Redis DB5)
17. config-service (None, Redis DB0)
18. drop-service (MySQL 3313, Redis DB0)
19. friend-service (MySQL 3328, Redis DB4)
20. gm-service (MySQL 3346, Redis DB0)
21. guild-service (MySQL 3329, Redis DB3)
22. localization-service (None, Redis DB6)
23. mail-service (MySQL 3331, Redis DB6)
24. main-fb-service (MySQL 3355, Redis DB0)
25. scheduler-service (None, Redis DB5)
26. serverInfo-service (MySQL 3354, Redis DB0)
27. session-service (None, Redis DB0) **Primary session store**
28. shizhuang-service (MySQL 3353, Redis DB0)
29. user-service (MySQL 3307, Redis DB0)

### Type 3: MySQL + Kafka (4 services)
**Database + events, no cache**

30. notification-service (MySQL 3334, Kafka 9092)
31. report-service (MySQL 3309, Kafka 29092)
32. trial-service (MySQL 3320, Kafka 29092)
33. webSocket-server (None, Kafka 29092)

### Type 4: MySQL Only (7 services)
**Database only, no cache or events**

34. admin-service (MySQL 3306/game_admin)
35. crafting-service (MySQL 3316)
36. pet-service (MySQL 3347)
37. shop-service (MySQL 3314)
38. wallet-service (MySQL 3342)
39. world-service (MySQL 3325)
40. (More services without Redis/Kafka)

### Type 5: No Infrastructure (21 services)
**Pure logic/utility/gateway services**

41. eureka-server (Service registry)
42. gateway-service (API gateway)
43. dataaccess-service (Data abstraction layer)
44. item-service (Item metadata, read-only)
45. file-service (File uploads, S3/local storage)
46. (16 more utility services)

---

## 🎯 ARCHITECTURE INSIGHTS

### Redis Strategy: ✅ **OPTIMAL**
**Shared server with isolated databases**

**Pros:**
- ✅ Single Redis instance - easier management
- ✅ Isolated databases prevent data collision
- ✅ Logical grouping (DB0 for general, DB3-9 for specialized)
- ✅ Cost-effective (1 server vs 29 servers)
- ✅ Easy to scale vertically

**Cons:**
- ⚠️ Single point of failure (needs Redis Sentinel/Cluster for HA)
- ⚠️ DB0 shared by too many services (18 services)
- ⚠️ No data isolation between services in same DB

**Recommendations:**
1. Consider Redis Cluster for high availability
2. Split DB0 further if memory limits reached
3. Use key prefixes: `{serviceName}:{resource}:{id}`

### Kafka Strategy: ✅ **GOOD**
**Shared cluster with separate topics**

**Pros:**
- ✅ Single Kafka cluster - easier management
- ✅ Topic-based isolation
- ✅ Configurable endpoints (environment variables)
- ✅ Supports multi-datacenter (9092 internal, 29092 Docker)

**Cons:**
- ⚠️ Port confusion (9092 vs 29092)
- ⚠️ Some services hardcoded, others configurable
- ⚠️ Need clear topic naming convention

**Recommendations:**
1. Standardize on environment variable: `${KAFKA_BOOTSTRAP_SERVERS:localhost:29092}`
2. Document topic schema and ownership
3. Implement topic naming convention: `{domain}.{entity}.{event}`
   - Example: `player.inventory.changed`, `battle.match.completed`

### MySQL Strategy: ✅ **EXCELLENT**
**Separate database per service**

**Pros:**
- ✅ Perfect microservice isolation
- ✅ Independent scaling
- ✅ No cross-service queries
- ✅ Easy to migrate/shard individual services

**Cons:**
- ⚠️ More databases to manage (22 databases)
- ⚠️ Backup complexity

---

## 📋 SUMMARY TABLE

| Infrastructure | Pattern | Services Count | Assessment |
|----------------|---------|----------------|------------|
| **Redis** | 1 shared server, 10 databases | 29 | ✅ Optimal |
| **Kafka** | 1 shared cluster, multiple topics | 19 | ✅ Good |
| **MySQL** | 1 DB per service | 22 | ✅ Excellent |

### Resource Requirements

**Minimum Infrastructure:**
- 1 Redis server (localhost:6379) with 10 databases
- 1 Kafka cluster (localhost:9092, exposed as 29092)
- 22 MySQL databases (ports 3306-3358)
- 1 Eureka server (service registry)
- 1 Gateway (API gateway)

**Total: 27 infrastructure components** for 50 microservices

---

## 🚀 DEPLOYMENT RECOMMENDATIONS

### Development Environment
```yaml
# docker-compose.yml
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    
  kafka:
    image: confluentinc/cp-kafka:latest
    ports: ["9092:9092", "29092:29092"]
    
  mysql:
    image: mysql:8
    # Use Docker network for multiple DB instances
```

### Production Environment
1. **Redis**: Use Redis Sentinel (3 nodes) or Redis Cluster (6+ nodes)
2. **Kafka**: 3-node cluster minimum (replication factor 3)
3. **MySQL**: Master-slave replication per service DB

---

**Architecture Status**: ✅ **WELL-DESIGNED**  
**Shared Infrastructure**: Optimal balance between cost and isolation  
**Ready for**: Development, Testing, Production deployment

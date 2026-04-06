# Redis Architecture for Game Config Data

> **Tác giả**: Claude Code Assistant
> **Ngày tạo**: 2026-04-06
> **Mục đích**: Tài liệu kiến trúc Redis cache cho game configuration data

---

## 📋 Tổng quan

GameServer sử dụng **kiến trúc Redis tập trung** (Centralized Redis) để cache game configuration data (JSON files). Redis instance được quản lý bởi **websocket-server** và được chia sẻ với tất cả backend services.

### Ưu điểm của kiến trúc này:

✅ **Single source of truth**: Chỉ 1 nơi quản lý config cache
✅ **Giảm memory usage**: Không duplicate cache giữa các services
✅ **Consistency cao**: Tất cả services đọc cùng 1 version
✅ **Giảm load config-service**: Preload 1 lần lúc startup
✅ **Fast lookup**: Redis-first strategy cho tất cả requests

---

## 🎯 Kiến trúc Tổng quát

```
┌─────────────────────────────────────────────────────────────────┐
│                     WEBSOCKET-SERVER (Gateway)                   │
│                                                                  │
│  Startup: StartupConfigRedisPreloader                           │
│    ↓                                                             │
│  Load P0 configs (task_cfg.json, skill JSONs, roleexp.json)    │
│    ↓                                                             │
│  Load P1 configs async (equipment.json, angel.json, etc.)       │
│    ↓                                                             │
│  Cache vào Redis: cfg:file:{path-with-colon}                    │
│    ↓                                                             │
│  TTL: 24 giờ + Scheduled refresh mỗi 30 phút                    │
│                                                                  │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Shared Redis Instance
                         │
         ┌───────────────┼───────────────┬──────────────┐
         │               │               │              │
         ▼               ▼               ▼              ▼
┌─────────────┐  ┌─────────────┐  ┌──────────┐  ┌──────────┐
│ task-service│  │equip-service│  │box-service│  │Other     │
│             │  │             │  │           │  │services  │
│ Redis-first │  │ Redis-first │  │Redis-first│  │          │
│   lookup    │  │   lookup    │  │  lookup   │  │          │
└─────────────┘  └─────────────┘  └──────────┘  └──────────┘
```

---

## 🔧 Chi tiết Implementation

### 1. WebSocket-Server (Master/Owner)

**File**: `/webSocket-server/src/main/java/com/southMillion/webSocket_server/config/StartupConfigRedisPreloader.java`

**Trách nhiệm**:
- Preload tất cả game config JSON từ config-service vào Redis lúc startup
- Scheduled refresh mỗi 30 phút (configurable)
- Đảm bảo Redis cache luôn fresh

**Config P0 (Critical - must load sync)**:
```yaml
gameworld/logicconfig/task_cfg.json       # Task definitions + indexed conditions
gameworld/logicconfig/roleexp.json        # Role level experience table
gameworld/logicconfig/role_name.json      # Role name templates
gameworld/skill/single_skill.json         # Single/active skills
gameworld/skill/passive_skill.json        # Passive skills
```

**Config P1 (Secondary - can load async)**:
```yaml
gameworld/item/equipment.json             # Equipment templates
gameworld/item/other.json                 # Other items
gameworld/item/expense.json               # Consumable items
gameworld/item/gift.json                  # Gift items
gameworld/logicconfig/shop_cfg.json       # Shop config
gameworld/logicconfig/angel.json          # Angel config
gameworld/logicconfig/unpack.json         # Box unpack rules
gameworld/logicconfig/kaixiangdaji.json   # Box opening config
# ... etc
```

**Redis Key Pattern**:
```
cfg:file:{path-with-colon}
```

**Example**:
```
gameworld/logicconfig/task_cfg.json  →  cfg:file:gameworld:logicconfig:task_cfg.json
gameworld/item/equipment.json        →  cfg:file:gameworld:item:equipment.json
```

**Scheduled Refresh**:
```java
@Scheduled(
    initialDelayString = "${app.redis-preload.scheduled-initial-delay-ms:1800000}",  // 30 min
    fixedDelayString = "${app.redis-preload.scheduled-interval-ms:1800000}"          // 30 min
)
public void scheduledRefresh() {
    if (!enabled) return;
    log.info("[redis-preload] scheduled refresh triggered");
    reloadNow();
}
```

**Configuration** (`application.yml`):
```yaml
app:
  redis-preload:
    enabled: true
    ttl-hours: 24
    scheduled-initial-delay-ms: 1800000  # 30 minutes
    scheduled-interval-ms: 1800000       # 30 minutes
    p0-paths: "gameworld/logicconfig/task_cfg.json,..."
    p1-paths: "gameworld/item/equipment.json,..."
    p1-async: true
    p1-timeout-ms: 15000
```

---

### 2. Task-Service (Consumer)

**File**: `/task-service/src/main/java/com/SouthMillion/task_service/service/TaskDefinitionProvider.java`

**Strategy**: Redis-first lookup

```java
// 1. Try Redis first
String redisKey = toRedisKey(taskConfigPath);
String cached = redis.opsForValue().get(redisKey);
if (cached != null && !cached.isBlank()) {
    log.debug("[TaskDefinitionProvider] Redis HIT path={}", taskConfigPath);
    parseConfigs(cached);
    touchRedisKey(redisKey);
    return;
}

// 2. Redis miss → call config-service
ResponseEntity<byte[]> response = configServiceClient.getFile(taskConfigPath, ifNoneMatch);
String payload = new String(response.getBody(), StandardCharsets.UTF_8);
parseConfigs(payload);

// 3. Cache in Redis for next time
redis.opsForValue().set(redisKey, payload, redisTtlHours, TimeUnit.HOURS);
```

**Config**:
```yaml
task:
  config:
    path: gameworld/logicconfig/task_cfg.json
    redis-enabled: true
    redis-ttl-hours: 24
```

---

### 3. Equip-Service (Consumer)

**File**: `/equip-service/src/main/java/com/southMillion/equip_service/config/EquipmentConfigCache.java`

**Strategy**: Redis-first lookup

```java
// 1. Try Redis first (if enabled)
if (redisEnabled) {
    json = redis.opsForValue().get(redisKey);
    if (json != null && !json.isBlank()) {
        log.debug("[EquipmentConfigCache] Redis HIT path={}", equipmentPath);
        parseAndCache(json);
        touchRedisKey(redisKey);
        return;
    }
}

// 2. Redis miss → call config-service
ResponseEntity<byte[]> resp = cfg.getFile(equipmentPath, cur);
json = new String(resp.getBody(), StandardCharsets.UTF_8);
parseAndCache(json);

// 3. Cache in Redis for next time
if (redisEnabled && json != null) {
    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
}
```

**Config**:
```yaml
equip:
  config:
    redis-enabled: true
    redis-ttl-hours: 24
    equipment-path: gameworld/item/equipment.json
    unpack-path: gameworld/logicconfig/unpack.json
```

---

### 4. Box-Service (Consumer)

**File**: `/box-service/src/main/java/com/SouthMillion/box_service/config/UnpackConfigCache.java`

**Strategy**: Redis-first lookup

```java
// 1. Try Redis first (if enabled and not force reload)
if (redisEnabled && force == 0) {
    String redisKey = toRedisKey(path);
    String cached = redis.opsForValue().get(redisKey);
    if (cached != null && !cached.isBlank()) {
        log.debug("[UnpackConfigCache] Redis HIT path={}", path);
        Map<String, Object> parsed = om.readValue(cached, new TypeReference<>() {});
        raw = parsed;
        touchRedisKey(redisKey);
        return;
    }
}

// 2. Call config-service
ResponseEntity<byte[]> resp = cfg.getFile(path, ifNoneMatch);
String json = new String(resp.getBody(), StandardCharsets.UTF_8);
Map<String, Object> parsed = om.readValue(json, new TypeReference<>() {});
raw = parsed;

// 3. Cache in Redis
if (redisEnabled) {
    String redisKey = toRedisKey(path);
    redis.opsForValue().set(redisKey, json, redisTtlHours, TimeUnit.HOURS);
}
```

**Config**:
```yaml
box:
  config:
    redis-enabled: true
    redis-ttl-hours: 24
    allow-remote-fallback-on-miss: false
```

---

## 📊 Luồng dữ liệu Runtime

### Scenario: 100 users login

**TRƯỚC KHI cải thiện** (mỗi service tự load config):
```
100 users login
  → 100 x task-service calls config-service
  → 100 x equip-service calls config-service
  → 100 x box-service calls config-service
  = 300 unnecessary config-service requests
```

**SAU KHI cải thiện** (Redis-first lookup):
```
Startup:
  WebSocket-server preload → config-service (1 lần)
  → Cache vào Redis

Runtime (100 users login):
  → 100 x task-service reads from Redis (fast)
  → 100 x equip-service reads from Redis (fast)
  → 100 x box-service reads from Redis (fast)
  = 0 config-service requests during login
```

**Kết quả**:
- ✅ Giảm 300 requests → 0 requests tới config-service
- ✅ Latency giảm từ ~50-100ms (HTTP call) → ~1-5ms (Redis lookup)
- ✅ Config-service CPU/memory usage giảm đáng kể

---

## 🔍 Redis Key Examples

| Config Path | Redis Key |
|------------|-----------|
| `gameworld/logicconfig/task_cfg.json` | `cfg:file:gameworld:logicconfig:task_cfg.json` |
| `gameworld/item/equipment.json` | `cfg:file:gameworld:item:equipment.json` |
| `gameworld/skill/single_skill.json` | `cfg:file:gameworld:skill:single_skill.json` |
| `gameworld/logicconfig/unpack.json` | `cfg:file:gameworld:logicconfig:unpack.json` |

**Pattern**:
```java
private String toRedisKey(String path) {
    return "cfg:file:" + path.replace('/', ':');
}
```

---

## ⚙️ Configuration Summary

### WebSocket-Server (Master)

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}

app:
  redis-preload:
    enabled: true
    ttl-hours: 24
    scheduled-initial-delay-ms: 1800000  # 30 min
    scheduled-interval-ms: 1800000       # 30 min
    p0-paths: "gameworld/logicconfig/task_cfg.json,..."
    p1-paths: "gameworld/item/equipment.json,..."
```

### Backend Services (Consumers)

**Task-Service**:
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

task:
  config:
    redis-enabled: true
    redis-ttl-hours: 24
```

**Equip-Service**:
```yaml
spring:
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

equip:
  config:
    redis-enabled: true
    redis-ttl-hours: 24
```

**Box-Service**:
```yaml
spring:
  cache:
    type: redis
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

box:
  config:
    redis-enabled: true
    redis-ttl-hours: 24
```

---

## 🚀 Deployment Checklist

### Khi deploy mới hoặc update config:

1. ✅ **Ensure Redis is running và accessible** từ tất cả services
2. ✅ **WebSocket-server starts first** để preload config vào Redis
3. ✅ **Backend services start sau** và sẽ tự động đọc từ Redis
4. ✅ **Monitor Redis memory usage** (tất cả config ~10-50MB)
5. ✅ **Check logs** cho Redis HIT/MISS metrics

### Khi update config JSON files:

**Option 1: Scheduled refresh (recommended)**
- Wait 30 phút → scheduled job sẽ tự động refresh
- Hoặc trigger manual reload qua admin endpoint

**Option 2: Manual refresh**
- Call admin endpoint: `POST /admin/config/reload`
- WebSocket-server sẽ reload tất cả configs

**Option 3: Restart websocket-server**
- Restart websocket-server → startup preloader runs
- Backend services không cần restart

---

## 📈 Monitoring

### Metrics to track:

**Redis**:
- Memory usage for `cfg:file:*` keys
- Hit/miss ratio
- TTL expiration events

**WebSocket-Server**:
- Preload success/failure count
- Scheduled refresh execution time
- P0 vs P1 load time

**Backend Services**:
- Redis HIT count (should be >95%)
- Config-service fallback calls (should be minimal)

### Log patterns:

```
[redis-preload] start, ttlHours=24, p0=5, p1=11
[redis-preload] completed in 2345ms, ok=16, fail=0
[redis-preload] scheduled refresh triggered
[TaskDefinitionProvider] Redis HIT path=gameworld/logicconfig/task_cfg.json
[EquipmentConfigCache] Redis HIT path=gameworld/item/equipment.json
```

---

## 🔧 Troubleshooting

### Problem: Backend service getting "Config not preloaded in Redis" error

**Cause**: WebSocket-server chưa preload config hoặc Redis key đã expire

**Solution**:
1. Check websocket-server logs cho preload status
2. Verify Redis contains keys: `KEYS cfg:file:*`
3. Restart websocket-server để force reload
4. Enable fallback: `allow-remote-fallback-on-miss: true`

### Problem: Config updates không apply

**Cause**: Redis cache chưa refresh

**Solution**:
1. Wait cho scheduled refresh (30 phút)
2. Hoặc trigger manual reload
3. Hoặc restart websocket-server

### Problem: High config-service load

**Cause**: Backend services đang fallback sang config-service

**Solution**:
1. Check Redis connectivity từ backend services
2. Verify `redis-enabled: true` trong config
3. Check logs cho "Redis MISS" messages
4. Ensure websocket-server preload succeeded

---

## ✅ Best Practices

1. **Always start websocket-server first** khi deploy cluster
2. **Use scheduled refresh** thay vì rely on TTL expiration
3. **Monitor Redis memory** và adjust TTL nếu cần
4. **Log Redis HIT/MISS** để track cache effectiveness
5. **Enable fallback** cho production để tránh downtime
6. **Use consistent Redis key pattern** across all services
7. **Test config updates** trong staging trước khi deploy production

---

## 📝 Related Files

- WebSocket-Server preloader: `/webSocket-server/src/main/java/com/southMillion/webSocket_server/config/StartupConfigRedisPreloader.java`
- Config lookup service: `/webSocket-server/src/main/java/com/southMillion/webSocket_server/service/ConfigSnapshotLookupService.java`
- Task-service config cache: `/task-service/src/main/java/com/SouthMillion/task_service/service/TaskDefinitionProvider.java`
- Equip-service config cache: `/equip-service/src/main/java/com/southMillion/equip_service/config/EquipmentConfigCache.java`
- Box-service config cache: `/box-service/src/main/java/com/SouthMillion/box_service/config/UnpackConfigCache.java`

---

**Last Updated**: 2026-04-06
**Status**: ✅ Active

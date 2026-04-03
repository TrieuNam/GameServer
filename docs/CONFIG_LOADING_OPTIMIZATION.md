# Hướng dẫn Tối ưu Tải Config Trước Login

## Vấn đề

Hiện tại khi user login, **mỗi service** (role, equip, bag, skill, etc.) đều gọi **riêng lẻ** tới `config-service` để lấy các file config JSON (roleexp.json, equipment.json, skill.json...).

Kết quả:
- ❌ Hàng chục HTTP calls tới config-service mỗi lần login
- ❌ Tổng thời gian chờ config: 200-500ms
- ❌ Config-service bị overload trong login peak

## Giải pháp: Redis-First Config Loading

### Cơ chế

1. **WebSocket-server khởi động** → Preload tất cả static configs vào Redis (thực hiện 1 lần)
2. **Services khởi động** → Đọc config từ Redis thay vì gọi config-service
3. **Login** → 0 calls tới config-service (tất cả đã có trong Redis)

### Lợi ích

✅ **Nhanh hơn 10-50x**: Redis < 1ms vs HTTP 10-50ms
✅ **Giảm load config-service**: Từ N*M calls → 1 call (N=users, M=services)
✅ **Login nhanh hơn**: Giảm 200-500ms thời gian bootstrap
✅ **Chia sẻ cache**: Tất cả services dùng chung cache Redis

## Cách Sử dụng

### 1. Thêm Redis dependency (nếu chưa có)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. Cấu hình Redis connection

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

### 3. Sử dụng RedisFirstConfigLoader

#### Option A: Sử dụng utility class có sẵn

```java
@Component
@RequiredArgsConstructor
public class MyConfigCache {

    private final StringRedisTemplate redis;
    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();

    private RedisFirstConfigLoader loader;
    private final AtomicReference<String> etag = new AtomicReference<>();
    private final AtomicReference<MyConfig> config = new AtomicReference<>();

    @PostConstruct
    public void init() {
        loader = new RedisFirstConfigLoader(redis, configFeign, om, 24);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void warmup() {
        refreshConfig();
    }

    @Scheduled(fixedDelay = 60000)
    public void refreshConfig() {
        String json = loader.loadConfig("gameworld/myconfig.json", etag.get());
        if (json != null) {
            MyConfig parsed = om.readValue(json, MyConfig.class);
            config.set(parsed);

            String newETag = loader.getCachedETag("gameworld/myconfig.json");
            if (newETag != null) etag.set(newETag);
        }
    }

    public MyConfig getConfig() {
        return config.get();
    }
}
```

#### Option B: Manual Redis-first lookup

```java
@Component
@RequiredArgsConstructor
public class ManualConfigCache {

    private final StringRedisTemplate redis;
    private final ConfigFeign configFeign;

    public String loadConfig(String path) {
        String redisKey = "cfg:file:" + path.replace('/', ':');

        // 1. Try Redis first
        String cached = redis.opsForValue().get(redisKey);
        if (cached != null) {
            return cached; // HIT - fast path
        }

        // 2. Redis miss → call config-service
        ResponseEntity<byte[]> resp = configFeign.getFile(path, null);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            String json = new String(resp.getBody(), StandardCharsets.UTF_8);

            // 3. Cache in Redis for 24h
            redis.opsForValue().set(redisKey, json, 24, TimeUnit.HOURS);

            return json;
        }

        throw new RuntimeException("Cannot load config: " + path);
    }
}
```

## Danh sách Config Files Đã Preload

Các file sau **đã được preload** vào Redis khi websocket-server khởi động:

### P0 (Critical - loaded đồng bộ):
- ✅ `gameworld/logicconfig/task_cfg.json` - Task definitions
- ✅ `gameworld/logicconfig/roleexp.json` - Level exp table
- ✅ `gameworld/logicconfig/role_name.json` - Name pool
- ✅ `gameworld/skill/single_skill.json` - Active skills
- ✅ `gameworld/skill/passive_skill.json` - Passive skills
- ✅ `gameworld/item/equipment.json` - Equipment templates

### P1 (Important - loaded bất đồng bộ):
- ✅ `gameworld/item/other.json` - Other items
- ✅ `gameworld/item/expense.json` - Consumables
- ✅ `gameworld/item/gift.json` - Gift boxes
- ✅ `gameworld/logicconfig/shop_cfg.json` - Shop items
- ✅ `gameworld/logicconfig/shop_shenmi.json` - Mystery shop
- ✅ `gameworld/logicconfig/cloth_shop.json` - Fashion shop
- ✅ `gameworld/logicconfig/unpack.json` - Box configs
- ✅ `gameworld/logicconfig/kaixiangdaji.json` - Lucky box

## Lưu ý quan trọng

### ⚠️ Static Config vs User Data

**Static Config** (nên cache Redis):
- Task templates, equipment stats, skill configs
- Shop items, drop rates, level tables
- ✅ Dùng chung cho tất cả users
- ✅ Ít thay đổi

**User Data** (KHÔNG cache Redis, phải load từ DB):
- Role attributes (hp, level, exp của user)
- Bag items, equipment instances
- Task progress, skill levels của user
- ❌ Riêng từng user
- ❌ Thay đổi liên tục

### 🔧 Khi nào refresh cache?

**Tự động refresh**:
- Scheduler mỗi 60s (hoặc config)
- Sử dụng ETag để chỉ tải khi có thay đổi

**Manual refresh** (khi deploy config mới):
```bash
# Clear Redis cache
redis-cli DEL "cfg:file:gameworld:item:equipment.json"

# Hoặc restart services để reload
kubectl rollout restart deployment websocket-server
```

## Migration Checklist

Cho mỗi service cần migrate:

- [ ] 1. Thêm Redis dependency vào pom.xml
- [ ] 2. Cấu hình Redis connection trong application.yml
- [ ] 3. Inject `StringRedisTemplate` vào ConfigCache class
- [ ] 4. Thay đổi logic load config: Check Redis → Miss → ConfigFeign
- [ ] 5. Test local: Verify config được đọc từ Redis
- [ ] 6. Monitor logs: Đảm bảo không còn gọi config-service trong login

## Monitoring & Debugging

### Kiểm tra config đã được preload chưa

```bash
# Check Redis keys
redis-cli KEYS "cfg:file:*"

# Get specific config
redis-cli GET "cfg:file:gameworld:item:equipment.json"

# Check preload status
curl http://localhost:8094/api/admin/config/preload/status
```

### Logs cần chú ý

```log
# Good - config loaded from Redis
[RedisFirstConfig] HIT path=gameworld/item/equipment.json

# Bad - Redis miss, calling config-service
[RedisFirstConfig] MISS path=gameworld/item/equipment.json, calling config-service

# Verify preload success
[redis-preload] completed in 234ms, ok=14, fail=0
```

## Kết quả mong đợi

### Trước khi optimize:
```
Login time: 800-1200ms
Config-service calls per login: 26+ calls
Config loading time: 200-500ms
```

### Sau khi optimize:
```
Login time: 400-600ms  (giảm 40-50%)
Config-service calls per login: 0 calls  (giảm 100%)
Config loading time: < 10ms  (giảm 95%+)
```

---

**Tài liệu này được tạo tự động bởi Claude Code Agent**
Last Updated: 2026-04-03

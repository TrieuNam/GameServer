# Tối Ưu Hiệu Suất: Thao Tác Wear (Mặc Đồ)

## Tổng Quan

Tài liệu này mô tả các cải tiến hiệu suất được triển khai để giảm độ trễ của thao tác "wear" trong box-service và equip-service.

### Vấn Đề Ban Đầu

Client gặp độ trễ đáng kể (200-300ms) khi gửi lệnh "wear" để mặc trang bị. Nguyên nhân chính:

1. **HTTP calls tuần tự chặn (blocking)**: 5 lời gọi service thực hiện tuần tự
   - BoxService → EquipService: `wearFromBox()`
   - EquipService → RoleService: `applyStatDelta()`
   - EquipService → ConfigService: Metadata lookup (khi Redis miss)
   - BoxService → EquipService: `computeSell()` (khi thay đồ cũ)
   - BoxService → WalletService: `batchAdd()` (cộng coin bán đồ)

2. **JSON serialization overhead**: ObjectMapper deserialize compare state từ Redis ở mỗi request

3. **Thiếu caching**: Kết quả `computeSell()` không được cache, gây ra tính toán lặp lại

## Các Cải Tiến Đã Triển Khai

### 1. Async Auto-Sell Flow với Virtual Threads (Tác động cao nhất)

**File**: `box-service/src/main/java/com/SouthMillion/box_service/service/BoxService.java`
**File**: `box-service/src/main/java/com/SouthMillion/box_service/config/AsyncConfig.java`

**Thay đổi**:
- Tạo `AsyncConfig` sử dụng **Virtual Threads** (JDK 21+) thay vì ThreadPoolTaskExecutor
  - Sử dụng `Executors.newVirtualThreadPerTaskExecutor()`
  - Virtual threads tự động scale theo nhu cầu, không cần cấu hình pool size
  - Nhẹ hơn và hiệu quả hơn cho I/O operations
- Chuyển phương thức `autoSellWearItem()` từ `private void` thành `@Async public void`
- Auto-sell operation bây giờ chạy bất đồng bộ trên virtual threads, không chặn phản hồi wear()

**Lợi ích Virtual Threads**:
- **Lightweight**: Hàng triệu virtual threads có thể chạy đồng thời (vs hàng nghìn platform threads)
- **Auto-scaling**: Không cần tune pool size - tự động tạo thread mới khi cần
- **Better I/O**: Tối ưu cho HTTP calls, database queries, Redis operations
- **Simplified**: Không cần lo về queue capacity hay thread starvation

**Lợi ích chung**:
- Giảm thời gian phản hồi wear() xuống ~50-100ms (loại bỏ 2 HTTP calls khỏi critical path)
- Client nhận phản hồi ngay lập tức mà không cần đợi sell/wallet operations
- Auto-sell vẫn hoàn thành trong background với virtual threads

**Code ví dụ**:
```java
// AsyncConfig.java - Sử dụng Virtual Threads
@Bean(name = "boxAsyncExecutor")
public Executor getAsyncExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

// BoxService.java - Auto-sell chạy trên virtual thread
@Async("boxAsyncExecutor")
public void autoSellWearItem(Long roleId, EquipDTOs.WearFromBoxItem sellItem, String source) {
    // Auto-sell logic chạy async trên virtual thread
}
```

### 2. Caching cho computeSell() Results

**File**: `equip-service/src/main/java/com/southMillion/equip_service/service/EquipService.java`
**File**: `equip-service/src/main/resources/application.yml`

**Thay đổi**:
- Thêm `@Cacheable` annotation cho phương thức `computeSell()`
- Cache key: `itemId_quality_equipLevel_businessmanPermyriad`
- TTL: 10 phút (600,000ms)
- Cache backend: Redis (đã có sẵn)
- Không cache null values

**Lợi ích**:
- Loại bỏ một HTTP call đến equip-service cho các sell tính toán lặp lại
- Giảm CPU usage cho equipment metadata lookups
- Hit rate cao vì cùng loại trang bị thường được sell nhiều lần

**Cấu hình**:
```yaml
spring:
  cache:
    type: redis
    cache-names: equipSellPrice
    redis:
      time-to-live: 600000  # 10 minutes
      cache-null-values: false
```

### 3. Performance Metrics

**File**: `box-service/src/main/java/com/SouthMillion/box_service/service/BoxService.java`

**Metrics mới được thêm**:

| Metric | Loại | Mô tả |
|--------|------|-------|
| `box.wear.duration` | Timer | Thời gian hoàn thành toàn bộ wear operation |
| `box.autosell.duration` | Timer | Thời gian hoàn thành auto-sell operation (async) |
| `box.autosell.success` | Counter | Số lần auto-sell thành công |
| `box.autosell.failure` | Counter | Số lần auto-sell thất bại |

**Cách sử dụng metrics**:
- Truy cập: `http://localhost:8290/actuator/metrics/box.wear.duration`
- Dashboard: Grafana/Prometheus (nếu có cấu hình)
- Monitoring: Theo dõi p50, p95, p99 latencies

**Code ví dụ**:
```java
public BoxDTOs.OkResp wear(Long roleId) {
    return wearOperationTimer.record(() -> {
        // Wear logic được đo thời gian tự động
    });
}
```

## Kết Quả Kỳ Vọng

### Trước Tối Ưu
- **Thời gian phản hồi**: 200-300ms
- **Breakdown**:
  - wearFromBox: 50ms
  - computeSell: 40ms
  - walletFeign.batchAdd: 40ms
  - Database operations: 40ms
  - Overhead: 30-130ms

### Sau Tối Ưu
- **Thời gian phản hồi**: 100-150ms
- **Cải thiện**: ~50%
- **Breakdown**:
  - wearFromBox: 50ms
  - Database operations: 40ms
  - Overhead: 10-60ms
  - (computeSell + wallet async, không tính vào response time)

## Hướng Dẫn Triển Khai

### Yêu Cầu
- Java 21+
- Spring Boot 3.5.3+
- Redis server (đang chạy)
- Maven 3.8+

### Bước Triển Khai

1. **Build services**:
```bash
# Build common-lib trước (nếu cần)
cd common-lib
mvn clean install -DskipTests

# Build box-service
cd ../box-service
mvn clean install -DskipTests

# Build equip-service
cd ../equip-service
mvn clean install -DskipTests
```

2. **Cấu hình Virtual Threads**:
Đảm bảo Virtual Threads được bật trong `application.yml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

3. **Khởi động services với JDK 21**:
```bash
# Đảm bảo sử dụng JDK 21+
java -version  # Phải là "openjdk version "21" hoặc cao hơn

# Khởi động theo thứ tự
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar config-service/target/config-service-1.0.0.jar
java -jar equip-service/target/equip-service-1.0.0.jar
java -jar box-service/target/box-service-1.0.0.jar
```

4. **Kiểm tra Virtual Thread executor**:
Xem logs khi box-service khởi động:
```
[AsyncConfig] Initialized Virtual Thread executor for async operations
```

## Monitoring và Troubleshooting

### Kiểm Tra Metrics

**Xem wear operation duration**:
```bash
curl http://localhost:8290/actuator/metrics/box.wear.duration
```

Response mẫu:
```json
{
  "name": "box.wear.duration",
  "measurements": [
    {"statistic": "COUNT", "value": 150},
    {"statistic": "TOTAL_TIME", "value": 18.5},
    {"statistic": "MAX", "value": 0.245}
  ],
  "availableTags": [{"tag": "operation", "values": ["wear"]}]
}
```

**Xem auto-sell success rate**:
```bash
curl http://localhost:8290/actuator/metrics/box.autosell.success
curl http://localhost:8290/actuator/metrics/box.autosell.failure
```

### Kiểm Tra Cache

**Redis cache keys**:
```bash
redis-cli
> KEYS equipSellPrice*
> TTL equipSellPrice::40001_3_10_0
> GET equipSellPrice::40001_3_10_0
```

### Debug Async Operations với Virtual Threads

**Kiểm tra Virtual Thread**:
Virtual threads không cần monitoring như platform threads vì chúng được quản lý tự động bởi JVM.

Tuy nhiên, bạn có thể xem thông tin về virtual threads:
```java
// Thêm vào BoxService để debug (optional)
@Scheduled(fixedRate = 60000)
public void logVirtualThreadInfo() {
    Thread currentThread = Thread.currentThread();
    log.info("Current thread info: name={}, virtual={}, id={}",
        currentThread.getName(),
        currentThread.isVirtual(),
        currentThread.threadId());
}
```

**JVM flags hữu ích cho Virtual Threads**:
```bash
# Xem virtual thread diagnostics
java -Djdk.tracePinnedThreads=full -jar box-service.jar

# Monitor virtual threads (JDK 21+)
jcmd <pid> Thread.dump_to_file -format=json /tmp/threads.json
```

### Các Vấn Đề Thường Gặp

**1. Auto-sell không chạy**:
- Kiểm tra logs: `[box] auto sold old equip roleId=...`
- Verify `@EnableAsync` có trong `AsyncConfig`
- Đảm bảo phương thức được gọi từ bên ngoài class (Spring proxy)
- Kiểm tra JDK version: `java -version` (phải là 21+)

**2. Cache không hoạt động**:
- Kiểm tra Redis connection
- Verify cache config trong `application.yml`
- Xem logs: `[spring-cache]` entries

**3. Performance không cải thiện**:
- Check metrics để xác định bottleneck thực tế
- Virtual threads scale tự động - không cần tune
- Monitor database connection pool usage
- Kiểm tra xem có thread pinning không (synchronized blocks, native calls)

## Tối Ưu Tiếp Theo (Phase 2)

Các cải tiến có thể thêm vào sau:

### 1. In-Memory Cache Layer
- Thêm Caffeine cache trước Redis để giảm network calls
- TTL: 30 giây cho hot data

### 2. Batch Role Stat Updates
- Tích lũy stat deltas và flush theo batch
- Giảm calls đến role-service

### 3. Pre-warm Equipment Config
- Background job refresh equipment.json vào Redis
- Tăng TTL lên 24h và refresh định kỳ

### 4. Event-Driven Wallet Updates
- Publish "item_sold" events vào Kafka
- Wallet-service consume async
- Loại bỏ hoàn toàn wallet HTTP call

## Best Practices

1. **Monitoring**: Luôn theo dõi metrics sau khi deploy
2. **Gradual Rollout**: Deploy từng service một, theo dõi trước khi tiếp tục
3. **Cache Invalidation**: Khi thay đổi sell price logic, clear cache:
   ```bash
   redis-cli KEYS "equipSellPrice*" | xargs redis-cli DEL
   ```
4. **Virtual Threads**: Tận dụng JDK 21+ để có performance tốt nhất
   - Tránh synchronized blocks (có thể gây thread pinning)
   - Ưu tiên ReentrantLock thay vì synchronized khi cần locking
   - Monitor thread pinning với `-Djdk.tracePinnedThreads=full`
5. **Async Error Handling**: Monitor `box.autosell.failure` để phát hiện vấn đề sớm

## Tài Liệu Tham Khảo

- [Spring Async Documentation](https://spring.io/guides/gs/async-method/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Micrometer Metrics](https://micrometer.io/docs)
- [Redis Caching Best Practices](https://redis.io/docs/manual/client-side-caching/)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Virtual Threads Best Practices](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)

## Changelog

### Version 1.1.0 (2026-04-06)
- ✅ **BREAKING**: Chuyển sang Virtual Threads (JDK 21+)
- ✅ Loại bỏ ThreadPoolTaskExecutor
- ✅ Simplified async configuration
- ✅ Cập nhật documentation cho Virtual Threads

### Version 1.0.0 (2026-04-06)
- ✅ Async auto-sell flow với ThreadPoolTaskExecutor
- ✅ Redis caching for computeSell results
- ✅ Performance metrics (timers and counters)
- ✅ Documentation

---

**Tác giả**: Claude Code Agent
**Ngày cập nhật**: 2026-04-06

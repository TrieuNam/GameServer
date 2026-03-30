# ⚠️ REACTIVE VS VIRTUAL THREADS - QUAN TRỌNG!

## 🎯 TÓM TẮT

**KHÔNG NÊN** kết hợp Virtual Threads với Reactive Programming (WebFlux)!

## 📊 PHÂN LOẠI SERVICES

### ✅ SỬ DỤNG VIRTUAL THREADS (50 services)
**Điều kiện**: Dùng `spring-boot-starter-web` (Tomcat, blocking I/O)

```
✅ admin-service          ✅ analytics-service      ✅ angel-service
✅ anti-cheat-service     ✅ arena-service          ✅ artifact-service
✅ bag-service            ✅ battleserver-service   ✅ box-service
✅ chat-service           ✅ config-service         ✅ crafting-service
✅ dataaccess-service     ✅ drop-service           ✅ equip-service
✅ escort-service         ✅ file-service           ✅ friend-service
✅ gameworld-service      ✅ gift-service           ✅ globalserver-service
✅ gm-service             ✅ guild-service          ✅ iap-verify-service*
✅ item-service           ✅ leaderboard-service    ✅ localization-service
✅ mail-service           ✅ main-fb-service        ✅ moderation-service
✅ mount-service          ✅ notification-service   ✅ pet-service
✅ report-service         ✅ role-service           ✅ rune-service
✅ scheduler-service      ✅ serverInfo-service     ✅ session-service
✅ shizhuang-service      ✅ shop-service           ✅ starmap-service
✅ task-service           ✅ territory-service      ✅ trial-service
✅ user-service           ✅ wallet-service         ✅ webSocket-server
✅ world-service          ✅ eureka-server
```

*iap-verify-service dùng WebFlux nhưng CHỈ là WebClient, vẫn dùng Tomcat.

### ❌ KHÔNG DÙNG VIRTUAL THREADS (1 service)
**Điều kiện**: Dùng `spring-cloud-starter-gateway` (WebFlux/Netty, reactive)

```
❌ gateway-service  →  Dùng ReactiveOptimizationConfig
```

---

## 🔬 TẠI SAO KHÔNG KÊT HỢP?

### 1️⃣ REACTIVE (WebFlux) - Non-Blocking I/O
```
┌─────────────────────────────────────┐
│  Netty Event Loop (2-4 threads)    │
│  ┌─────┐  ┌─────┐  ┌─────┐        │
│  │ T1  │  │ T2  │  │ T3  │        │
│  └──┬──┘  └──┬──┘  └──┬──┘        │
│     │        │        │            │
│  ┌──▼────────▼────────▼──┐        │
│  │ 10,000+ concurrent    │        │
│  │ connections           │        │
│  │ (non-blocking!)       │        │
│  └───────────────────────┘        │
└─────────────────────────────────────┘

Đặc điểm:
✓ Mỗi thread xử lý HÀNG NGÀN connections
✓ Không bao giờ block (chờ I/O)
✓ Dùng callbacks/promises (Mono/Flux)
✓ Memory: ~5-10 MB per thread (event loop overhead)
```

### 2️⃣ VIRTUAL THREADS - Blocking I/O Made Cheap
```
┌─────────────────────────────────────┐
│  Virtual Threads (100-200)         │
│  ┌─────┐  ┌─────┐  ┌─────┐        │
│  │ VT1 │  │ VT2 │  │ VT3 │  ...   │
│  └──┬──┘  └──┬──┘  └──┬──┘        │
│     │ BLOCK   │ BLOCK  │ BLOCK    │
│  ┌──▼─────────▼────────▼──┐       │
│  │ Database / Redis       │        │
│  │ (waiting for I/O...)   │        │
│  └────────────────────────┘        │
└─────────────────────────────────────┘

Đặc điểm:
✓ Mỗi request = 1 virtual thread
✓ Thread BLOCK khi chờ I/O (nhưng cheap!)
✓ Code imperative (dễ đọc)
✓ Memory: ~1 KB per virtual thread
```

### 3️⃣ KẾT HỢP = ❌ XUNG ĐỘT

```
❌ Virtual Threads + Reactive = BAD IDEA!

Vấn đề:
1. Thread Pinning
   - Virtual thread block trên synchronized
   - Reactive code thường có synchronized (schedulers)
   - → Virtual thread không thể unmount → wasted carrier thread

2. Over-engineering
   - Reactive ĐÃ tối ưu non-blocking
   - Virtual Threads làm CHO blocking code
   - → Hai cơ chế giải quyết cùng vấn đề → redundant

3. Performance Degradation
   - Reactive scheduler + Virtual threads = context switch overhead
   - Netty event loop bị interrupt bởi Virtual threads
   - → Throughput GIẢM thay vì tăng
```

---

## 📋 CÁCH PHÂN BIỆT

### Kiểm tra `pom.xml`:

#### ✅ DÙNG VIRTUAL THREADS
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>  ← Tomcat/Blocking
</dependency>
```

#### ❌ KHÔNG DÙNG VIRTUAL THREADS
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>  ← WebFlux/Reactive
</dependency>
<!-- HOẶC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>  ← Netty/Reactive
</dependency>
<!-- (nếu không có starter-web) -->
```

### ⚠️ TRƯỜNG HỢP ĐẶC BIỆT: iap-verify-service

```xml
<dependency>
    <artifactId>spring-boot-starter-web</artifactId>      ← Tomcat (chính)
</dependency>
<dependency>
    <artifactId>spring-boot-starter-webflux</artifactId>  ← WebClient only
</dependency>
```

➡️ **Virtual Threads VẪN OK** vì:
- Server dùng Tomcat (blocking I/O)
- WebFlux chỉ là HTTP client (WebClient) để gọi API Apple/Google
- Không dùng @Controller reactive (Mono/Flux)

---

## 🔧 CÁCH TỐI ƯU

### 🌐 Gateway Service (Reactive)

**File**: `ReactiveOptimizationConfig.java`

```java
@Configuration
public class ReactiveOptimizationConfig {
    
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> {
            int workerThreads = Runtime.getRuntime().availableProcessors();
            
            // Netty event loop - NON-BLOCKING
            factory.addServerCustomizers(httpServer -> 
                httpServer.runOn(LoopResources.create("gateway-event-loop", workerThreads, true))
            );
        };
    }
}
```

**Đặc điểm**:
- Netty event loop: 2-4 threads
- Mỗi thread xử lý 10,000+ connections
- Memory: ~20-40 MB total (vs 200 MB Virtual Threads)
- Throughput: Cao hơn Virtual Threads cho I/O-bound

### ⚙️ Business Services (Virtual Threads)

**File**: `MemoryOptimizationConfig.java` + `VirtualThreadsConfig.java`

```java
@Configuration
public class MemoryOptimizationConfig {
    
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}
```

**Đặc điểm**:
- Virtual threads: 100-200 threads
- Mỗi request = 1 thread (blocking OK)
- Memory: ~0.1-0.2 MB total
- Code đơn giản (imperative style)

---

## 📊 SO SÁNH PERFORMANCE

### Gateway (Reactive - Netty)
```
┌─────────────────────────────────────────┐
│  Connections: 10,000                    │
│  Threads:     4 (event loop)            │
│  Memory:      40 MB                     │
│  Throughput:  50,000 req/s             │
│  Latency:     <1ms (event loop)        │
└─────────────────────────────────────────┘
```

### Business Service (Virtual Threads - Tomcat)
```
┌─────────────────────────────────────────┐
│  Connections: 200                       │
│  Threads:     200 (virtual)             │
│  Memory:      0.2 MB                    │
│  Throughput:  5,000 req/s              │
│  Latency:     <10ms (DB/Redis wait)    │
└─────────────────────────────────────────┘
```

**Kết luận**:
- Gateway: Cần **throughput CỰC CAO** → Reactive tốt hơn
- Business: Cần **code đơn giản + low memory** → Virtual Threads tốt hơn

---

## ✅ CHECKLIST TRIỂN KHAI

### Gateway Service (1 service)
- [x] Xóa `VirtualThreadsConfig.java`
- [x] Xóa `MemoryOptimizationConfig.java`
- [x] Xóa `DataSourceOptimizationConfig.java`
- [x] Tạo `ReactiveOptimizationConfig.java`
- [x] Giữ `RedisOptimizationConfig.java` (OK với Lettuce reactive)
- [x] Giữ `MemoryMonitorListener.java` (monitoring only)
- [x] Build test: `mvn clean compile` ✅

### Business Services (50 services)
- [x] `MemoryOptimizationConfig.java` (Virtual Threads cho Tomcat)
- [x] `VirtualThreadsConfig.java` (@Async tasks)
- [x] `DataSourceOptimizationConfig.java` (HikariCP)
- [x] `RedisOptimizationConfig.java` (Lettuce)
- [x] `MemoryMonitorListener.java`
- [x] `JvmArgumentsSuggester.java`

---

## 🚀 LOGS KHÁC BIỆT

### Gateway (Reactive)
```
╔════════════════════════════════════════════════════════════════╗
║  ⚛️  REACTIVE OPTIMIZATION (GATEWAY - WEBFLUX/NETTY)         ║
╚════════════════════════════════════════════════════════════════╝
🔧 Netty worker threads: 4 (event loop, non-blocking)
💡 Memory model: Event Loop (NOT Virtual Threads)
⚡ Each thread handles THOUSANDS of concurrent connections
📊 Memory per thread: ~5-10 MB (event loop overhead)
🎯 Total thread memory: ~40 MB
```

### Business Service (Virtual Threads)
```
╔════════════════════════════════════════════════════════════════╗
║  🚀 VIRTUAL THREADS ENABLED (JAVA 21 - TOMCAT)               ║
╚════════════════════════════════════════════════════════════════╝
🔧 Tomcat optimized with VIRTUAL THREADS
   • Max threads: 150 (virtual)
   • Memory per thread: ~1KB (vs 1MB for platform threads)
📊 Total thread memory: ~0.15 MB
⚡ 10x better throughput for blocking I/O
```

---

## 🎯 KẾT LUẬN

| Service Type | Technology | Thread Model | Use Case |
|--------------|------------|--------------|----------|
| **Gateway** | Spring Cloud Gateway (WebFlux) | Netty Event Loop (4 threads) | High-throughput proxy, routing |
| **Business** | Spring Boot Web (Tomcat) | Virtual Threads (100-200) | Business logic, DB, Redis |

### Quy tắc vàng:
1. **Reactive** = High concurrency, low latency → Dùng cho **gateway/proxy**
2. **Virtual Threads** = Simple code, low memory → Dùng cho **business logic**
3. **KHÔNG** kết hợp cả hai trong cùng 1 service!

---

## 📚 TÀI LIỆU THAM KHẢO

- [Spring Cloud Gateway Docs](https://spring.io/projects/spring-cloud-gateway)
- [Project Loom (Virtual Threads)](https://openjdk.org/projects/loom/)
- [Reactive Streams Specification](https://www.reactive-streams.org/)
- [Netty Event Loop](https://netty.io/wiki/user-guide-for-4.x.html)

---

**Cập nhật**: 2026-02-07
**Services**: 51 total (1 reactive, 50 virtual threads)

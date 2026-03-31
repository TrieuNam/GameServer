# 💾 RAM OPTIMIZATION GUIDE - GIẢM RAM CHO 51 SERVICES

## 📊 PHÂN TÍCH HIỆN TẠI

### Default JVM Memory Model
```
┌─────────────────────────────────────────┐
│  Spring Boot Service (1 service)       │
├─────────────────────────────────────────┤
│ Heap Memory (Xms/Xmx)      512-1024 MB │
│ Metaspace                   100-150 MB │
│ Thread Stacks                  50 MB   │
│ Direct Memory                  50 MB   │
│ Native Memory (JIT, GC...)    50 MB    │
├─────────────────────────────────────────┤
│ TOTAL PER SERVICE:         700-1300 MB │
│ 51 SERVICES TOTAL:         35-66 GB!!! │
└─────────────────────────────────────────┘
```

**Vấn đề:** 51 services × 1 GB = 51 GB RAM!

---

## ⚡ SOLUTION 1: JVM TUNING (Đơn giản - Khuyến nghị)

### 1.1 Giảm Heap Size

**Hiện tại:**
```java
-Xms512m -Xmx1024m
```

**Tối ưu:**
```java
// Small services (user, role, wallet...)
-Xms128m -Xmx256m

// Medium services (gateway, admin...)
-Xms256m -Xmx512m

// Large services (analytics...)
-Xms512m -Xmx1024m
```

### 1.2 Tối ưu Garbage Collector

**Hiện tại:** ZGC (tốt cho low latency, nhưng tốn RAM)
```java
-XX:+UseZGC
```

**Tối ưu cho RAM thấp:** Serial GC (dùng ít RAM nhất)
```java
-XX:+UseSerialGC
```

**Hoặc G1GC (cân bằng):**
```java
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=4m
```

### 1.3 Giảm Metaspace

```java
-XX:MetaspaceSize=64m
-XX:MaxMetaspaceSize=128m
```

### 1.4 Tối ưu Thread Stacks

```java
-Xss256k  # Giảm từ 1MB default xuống 256KB
```

### 1.5 Tắt JIT Compiler Tiers (cho services ít traffic)

```java
-XX:TieredStopAtLevel=1  # Chỉ dùng C1 compiler, bỏ C2
```

### 🎯 RECOMMENDED JVM ARGS - SMALL SERVICES

```java
java -Dthin.root=../repository \
  -Xms128m \
  -Xmx256m \
  -XX:+UseSerialGC \
  -XX:MetaspaceSize=64m \
  -XX:MaxMetaspaceSize=128m \
  -Xss256k \
  -XX:TieredStopAtLevel=1 \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  -jar service.jar
```

**Kết quả:** 150-200 MB RAM/service
**51 services:** 7.5-10 GB RAM (giảm 80%!)

---

## ⚡ SOLUTION 2: SPRING BOOT OPTIMIZATION

### 2.1 Lazy Initialization

**application.yml:**
```yaml
spring:
  main:
    lazy-initialization: true
```

**Lợi ích:** 
- Giảm 20-30% startup memory
- Chỉ load beans khi cần

### 2.2 Disable Unused Features

```yaml
spring:
  jmx:
    enabled: false  # Tắt JMX nếu không dùng
  devtools:
    restart:
      enabled: false
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Chỉ expose cần thiết
```

### 2.3 Tối ưu Connection Pools

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5       # Giảm từ 10
      minimum-idle: 2             # Giảm từ 5
      connection-timeout: 20000
      
  redis:
    lettuce:
      pool:
        max-active: 8             # Giảm từ 16
        max-idle: 4               # Giảm từ 8
```

### 2.4 Tối ưu Tomcat (cho web services)

```yaml
server:
  tomcat:
    threads:
      max: 50                     # Giảm từ 200
      min-spare: 5                # Giảm từ 10
    max-connections: 500          # Giảm từ 10000
    accept-count: 50              # Giảm từ 100
```

**Kết quả:** Kết hợp với JVM tuning = 120-180 MB/service

---

## ⚡ SOLUTION 3: GRAALVM NATIVE IMAGE (RAM thấp nhất)

### 3.1 Cài đặt GraalVM

```bash
# Download GraalVM JDK 21
# https://www.graalvm.org/downloads/

# Set JAVA_HOME
$env:JAVA_HOME = "C:\graalvm-jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### 3.2 Build Native Image

**pom.xml:**
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

**Build:**
```bash
mvn -Pnative native:compile
```

### 3.3 Kết quả

```
┌─────────────────────────────────────┐
│ Native Image vs JVM                 │
├─────────────────────────────────────┤
│ Startup Time:  0.1s vs 3-5s        │
│ Memory:        50-100 MB vs 700 MB │
│ Binary Size:   50 MB vs 100+ MB    │
└─────────────────────────────────────┘
```

**Nhược điểm:**
- Build time lâu (5-10 phút/service)
- Không support reflection/dynamic loading
- Khó debug
- Cần config metadata cho reflection

**51 services với Native Image:** 2.5-5 GB RAM!

---

## 📋 SO SÁNH CÁC GIẢI PHÁP

| Giải pháp | RAM/service | 51 services | Độ khó | Thời gian | Performance |
|-----------|-------------|-------------|--------|-----------|-------------|
| **Default (hiện tại)** | 700-1300 MB | 35-66 GB | Easy | 0 | Good ⭐⭐⭐⭐⭐ |
| **JVM Tuning** | 150-250 MB | 7.5-12.5 GB | Easy | 30 phút | Good ⭐⭐⭐⭐ |
| **JVM + Spring Opt** | 120-200 MB | 6-10 GB | Medium | 2 giờ | OK ⭐⭐⭐ |
| **EXTREME Mode** | 80-120 MB | 4-6 GB | Medium | 3 giờ | Slower ⭐⭐ |
| **GraalVM Native** | 50-100 MB | 2.5-5 GB | Hard | 2-3 ngày | Fast ⭐⭐⭐⭐⭐ |

### 🎯 Service RAM Tiers (EXTREME Strategy)

| Tier | Services | RAM | Strategy | Reason |
|------|----------|-----|----------|--------|
| **CRITICAL** | eureka, gateway, config | 256-512 MB | Optimized | Always-on, high traffic |
| **NORMAL** | user, role, bag, shop, task, arena, pet... | 128-256 MB | Low Memory | Medium traffic |
| **EXTREME** | analytics, scheduler, file, localization... | 64-128 MB | Ultra-Low | Background, low traffic |

**Tính toán cho 51 services:**
```
3 Critical × 384 MB (avg) = 1,152 MB
30 Normal  × 192 MB (avg) = 5,760 MB  
18 Extreme × 96 MB (avg)  = 1,728 MB
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:                      8,640 MB ≈ 8.5 GB
vs Default (51 GB):         83% saved!
```

---

## ⚡ SOLUTION 4: EXTREME MODE - ULTRA-LOW MEMORY (5 GB cho 51 services!)

### 4.1 Chiến thuật "Giảm thật nhiều, mở thất nhiều"

**Ý tưởng:**
- Giảm RAM từng service xuống MỨC CỰC THẤP (64-128 MB)
- Trade-off: Performance chậm hơn, GC nhiều hơn
- Phù hợp: Services ít traffic, background services

### 4.2 EXTREME JVM Args

```java
// EXTREME MODE - 80-120 MB/service
-Xms64m                    // Initial heap: chỉ 64 MB!
-Xmx128m                   // Max heap: 128 MB
-XX:+UseSerialGC           // Serial GC (lowest memory)
-XX:MetaspaceSize=32m      // Metaspace cực thấp
-XX:MaxMetaspaceSize=64m   
-Xss128k                   // Thread stack: 128 KB
-XX:TieredStopAtLevel=1    // No C2 JIT
-XX:+UseStringDeduplication
-XX:MaxGCPauseMillis=500   // Accept longer GC pauses
-XX:GCTimeRatio=4          // Allow 20% time in GC
-Djava.awt.headless=true
-Dfile.encoding=UTF-8
```

### 4.3 Service Classification (Phân loại services)

**TIER 1: Critical (Optimized 256 MB)**
- eureka-server, gateway-service, config-service
- Cần performance tốt, uptime cao

**TIER 2: Normal (Low 128 MB)**
- user-service, role-service, bag-service, shop-service...
- Services có traffic trung bình

**TIER 3: Background (Extreme 64 MB)**
- analytics-service, scheduler-service, file-service...
- Services ít traffic, chạy background

### 4.4 Dynamic Memory Profiles

```java
// ServiceManager.java - Profile-based JVM args
private List<String> getJvmArgsForService(ServiceConfig config) {
    String profile = config.getMemoryProfile(); // CRITICAL, NORMAL, EXTREME
    
    switch (profile) {
        case "CRITICAL":
            return Arrays.asList(
                "-Xms256m", "-Xmx512m", 
                "-XX:+UseG1GC", 
                "-XX:MetaspaceSize=128m"
            );
        case "EXTREME":
            return Arrays.asList(
                "-Xms64m", "-Xmx128m",
                "-XX:+UseSerialGC",
                "-XX:MetaspaceSize=32m",
                "-XX:MaxMetaspaceSize=64m",
                "-Xss128k",
                "-XX:TieredStopAtLevel=1"
            );
        default: // NORMAL
            return Arrays.asList(
                "-Xms128m", "-Xmx256m",
                "-XX:+UseSerialGC",
                "-XX:MetaspaceSize=64m",
                "-XX:MaxMetaspaceSize=128m",
                "-Xss256k"
            );
    }
}
```

### 4.5 On-Demand Service Management

**Chiến lược: Start khi cần, Stop khi idle**

```java
// Auto-stop services after X minutes idle
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void stopIdleServices() {
    for (ServiceConfig service : getBackgroundServices()) {
        if (isIdle(service) && service.isAutoStopEnabled()) {
            stopService(service.getServiceName());
            log.info("🛑 Auto-stopped idle service: {}", service.getServiceName());
        }
    }
}

// Auto-start on first request
public void ensureServiceRunning(String serviceName) {
    if (!isServiceRunning(serviceName)) {
        startService(serviceName);
        waitForHealthy(serviceName);
    }
}
```

### 4.6 Kết quả EXTREME Mode

```
┌─────────────────────────────────────────────┐
│ RAM Usage - 51 Services                     │
├─────────────────────────────────────────────┤
│ TIER 1 (Critical): 3 × 256 MB =  768 MB    │
│ TIER 2 (Normal):  30 × 128 MB = 3840 MB    │
│ TIER 3 (Extreme): 18 × 64 MB  = 1152 MB    │
├─────────────────────────────────────────────┤
│ TOTAL:                          5.76 GB     │
│ vs Default (51 GB):             89% saved!  │
└─────────────────────────────────────────────┘
```

### 4.7 Trade-offs (Nhược điểm)

⚠️ **Performance:**
- Startup chậm hơn 20-30%
- Response time tăng 10-20%
- GC pause thường xuyên hơn

⚠️ **Risk:**
- OutOfMemoryError nếu traffic tăng đột ngột
- Cần monitor cẩn thận

⚠️ **Best for:**
- Development/Testing environment
- Low-traffic production
- Background/scheduled services

### 4.8 Monitoring EXTREME Mode

```bash
# Watch memory usage real-time
while ($true) { 
    Get-Process java | Select ProcessName,
        @{N='PID';E={$_.Id}},
        @{N='RAM(MB)';E={[math]::Round($_.WS/1MB,2)}},
        @{N='CPU(%)';E={$_.CPU}} | 
    Sort RAM -Descending | 
    Format-Table -AutoSize
    Start-Sleep 5
}
```

### 4.9 Emergency Memory Boost

```java
// Nếu service bị OOM, tự động restart với higher memory
if (service.getRestartCount() > 3 && service.getLastError().contains("OutOfMemory")) {
    log.warn("⚠️ Service {} has OOM errors, boosting memory", serviceName);
    service.setMemoryProfile("NORMAL"); // Upgrade từ EXTREME → NORMAL
    restartService(serviceName);
}
```

---

## 🎯 KHUYẾN NGHỊ

### Phase 1: JVM Tuning (Làm ngay)
✅ **Dễ implement**  
✅ **Giảm 75-80% RAM**  
✅ **Không thay đổi code**

```java
// ServiceManager.java - Sửa default JVM args
if (config.getJvmArgs() != null && !config.getJvmArgs().isEmpty()) {
    command.addAll(Arrays.asList(config.getJvmArgs().split("\\s+")));
} else {
    // OPTIMIZED JVM ARGS FOR LOW MEMORY
    command.add("-Xms128m");
    command.add("-Xmx256m");
    command.add("-XX:+UseSerialGC");
    command.add("-XX:MetaspaceSize=64m");
    command.add("-XX:MaxMetaspaceSize=128m");
    command.add("-Xss256k");
    command.add("-XX:TieredStopAtLevel=1");
    command.add("-XX:+UseStringDeduplication");
}
```

### Phase 2: Spring Boot Optimization (Tuần sau)
✅ **Giảm thêm 20-30 MB**  
✅ **Cải thiện startup time**

### Phase 3: GraalVM Native (Nếu cần thiết)
⚠️ **Chỉ làm nếu:**
- RAM vẫn không đủ sau Phase 1+2
- Có thời gian test kỹ (1-2 tuần)
- Cần startup time cực nhanh (<1s)

---

## 🚀 QUICK START - ÁP DỤNG NGAY

### Bước 1: Sửa ServiceManager.java

```bash
cd D:\project\serverGame\GameServer\admin-service
code src/main/java/com/southMillion/admin/service/ServiceManager.java
```

Tìm dòng 388-391, sửa thành:
```java
} else {
    // OPTIMIZED for low memory microservices
    command.add("-Xms128m");
    command.add("-Xmx256m");
    command.add("-XX:+UseSerialGC");
    command.add("-XX:MetaspaceSize=64m");
    command.add("-XX:MaxMetaspaceSize=128m");
    command.add("-Xss256k");
    command.add("-XX:TieredStopAtLevel=1");
}
```

### Bước 2: Rebuild admin-service

```bash
mvn clean package -DskipTests
```

### Bước 3: Test với 1 service

```bash
cd ../user-service
java -Dthin.root=../repository \
  -Xms128m -Xmx256m \
  -XX:+UseSerialGC \
  -jar target/user-service-1.0.0.jar
```

**Kiểm tra RAM:**
```bash
# PowerShell
Get-Process java | Select-Object Id,ProcessName,@{Name="RAM(MB)";Expression={[math]::Round($_.WorkingSet64/1MB,2)}}
```

### Bước 4: Apply cho tất cả services

Start từ Admin UI - tự động dùng optimized JVM args!

---

## 📊 EXPECTED RESULTS

### Trước:
```
51 services × 1 GB = 51 GB RAM
❌ Không thể chạy trên máy thường
```

### Sau (JVM Tuning):
```
51 services × 200 MB = 10.2 GB RAM
✅ Chạy được trên máy 16-32 GB RAM
✅ Giảm 80% RAM usage
✅ Không thay đổi code
```

---

## 🔍 MONITORING & TROUBLESHOOTING

### Check memory usage

```bash
# Tất cả Java processes
Get-Process java | Select ProcessName,@{N='RAM(MB)';E={[math]::Round($_.WS/1MB,2)}} | Sort RAM -Descending

# Chi tiết 1 service
jcmd <PID> VM.native_memory summary
```

### Nếu service bị OutOfMemoryError

1. Tăng heap: `-Xmx256m` → `-Xmx512m`
2. Kiểm tra memory leak: jvisualvm
3. Disable lazy-init cho service đó

---

## 📚 TÀI LIỆU THAM KHẢO

- [JVM Memory Model](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Spring Boot Memory Optimization](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)

---

**🎉 Kết luận:** Với JVM Tuning đơn giản, bạn có thể giảm RAM từ **51 GB → 10 GB** (80%)!

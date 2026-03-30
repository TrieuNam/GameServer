# 🔥 ULTRA-LOW MEMORY MODE - Chạy 51 Services trên Máy Yếu

## 🎯 MỤC TIÊU
Chạy **full 51 services** trên máy có **RAM 8-16 GB** và **CPU yếu**.

---

## 📊 CHIẾN LƯỢC 3-TIER ULTRA-LOW

### Tier 1: CRITICAL (3 services) - 150-250 MB/service
```
eureka-server, gateway-service, config-service
JVM: -Xms128m -Xmx256m
Total: 3 × 200 MB = 600 MB
```

### Tier 2: MINIMAL (30 services) - 70-120 MB/service  
```
user, role, bag, shop, task, arena, pet, mount...
JVM: -Xms48m -Xmx96m
Total: 30 × 95 MB = 2,850 MB
```

### Tier 3: ULTRA-LOW (18 services) - 50-80 MB/service
```
analytics, scheduler, file, localization, moderation...
JVM: -Xms32m -Xmx64m
Total: 18 × 65 MB = 1,170 MB
```

### 📈 TỔNG RAM CẦN:
```
600 + 2,850 + 1,170 = 4,620 MB (4.5 GB)
+ Docker containers (MySQL, Redis...): ~2-3 GB
+ OS + others: ~2 GB
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL: 8.5-10 GB
```

**→ Có thể chạy trên máy 12-16 GB RAM!**

---

## ⚠️ TRADE-OFFS (Đánh đổi)

| Aspect | Impact |
|--------|--------|
| **Startup time** | +50-100% (10-15s thay vì 5s) |
| **Response time** | +20-50% (có thể chậm hơn) |
| **GC frequency** | 5-10x nhiều hơn |
| **OutOfMemory risk** | Cao - cần monitor |
| **Throughput** | Giảm 30-50% |

**Kết luận:** Chỉ dùng cho **development/testing local**, KHÔNG dùng production!

---

## 🚀 IMPLEMENTATION (ĐÃ LÀM)

### 1. ServiceManager.java - Updated!
```java
// ULTRA-LOW profile: 32-64 MB
-Xms32m -Xmx64m
-XX:MetaspaceSize=16m
-XX:MaxMetaspaceSize=48m

// MINIMAL profile: 48-96 MB  
-Xms48m -Xmx96m
-XX:MetaspaceSize=24m
-XX:MaxMetaspaceSize=64m

// CRITICAL profile: 128-256 MB
-Xms128m -Xmx256m
-XX:MetaspaceSize=64m
-XX:MaxMetaspaceSize=128m
```

### 2. Spring Boot Optimization
Thêm vào `application.yml` của MỌI services:

```yaml
spring:
  main:
    lazy-initialization: true
    banner-mode: off
  jmx:
    enabled: false
    
server:
  tomcat:
    threads:
      max: 20          # Giảm từ 200
      min-spare: 2     # Giảm từ 10
    max-connections: 100
    
spring:
  datasource:
    hikari:
      maximum-pool-size: 3   # Giảm từ 10
      minimum-idle: 1
      
  redis:
    lettuce:
      pool:
        max-active: 4        # Giảm từ 8
        max-idle: 2
```

### 3. Tăng Windows Pagefile (QUAN TRỌNG!)

**Khi RAM vật lý thiếu, Windows sẽ dùng pagefile (swap trên disk).**

#### Tăng Pagefile ngay:
```powershell
# Run as Administrator
# Set pagefile 16-32 GB (gấp đôi RAM)
$computerSystem = Get-WmiObject Win32_ComputerSystem -EnableAllPrivileges
$computerSystem.AutomaticManagedPagefile = $false
$computerSystem.Put()

$pageFile = Get-WmiObject Win32_PageFileSetting
if ($pageFile) {
    $pageFile.InitialSize = 16384  # 16 GB
    $pageFile.MaximumSize = 32768  # 32 GB
    $pageFile.Put()
} else {
    Set-WmiInstance -Class Win32_PageFileSetting -Arguments @{
        Name = "C:\\pagefile.sys"
        InitialSize = 16384
        MaximumSize = 32768
    }
}

Write-Host "✅ Pagefile set to 16-32 GB. Restart required!"
```

**Sau khi set, RESTART máy!**

---

## 📋 SCRIPT QUẢN LÝ SERVICE GROUPS

Nếu vẫn quá nặng, bật theo nhóm thay vì cả 51 cùng lúc:

### start-group-p0.ps1 (Core services)
```powershell
# Core infrastructure + essential services
$services = @(
    "eureka-server",
    "gateway-service", 
    "config-service",
    "user-service",
    "role-service",
    "session-service"
)

foreach ($svc in $services) {
    Write-Host "Starting $svc..."
    # Call Admin API to start
    Invoke-RestMethod -Uri "http://localhost:9091/api/services/$svc/start" -Method POST
}
```

### start-group-game.ps1 (Game features)
```powershell
$services = @(
    "bag-service",
    "shop-service",
    "item-service",
    "equip-service",
    "pet-service",
    "mount-service",
    "arena-service",
    "trial-service"
)

foreach ($svc in $services) {
    Invoke-RestMethod -Uri "http://localhost:9091/api/services/$svc/start" -Method POST
}
```

### stop-all.ps1
```powershell
# Stop all non-critical services
Invoke-RestMethod -Uri "http://localhost:9091/api/services/stop-all" -Method POST
```

---

## 🔍 MONITORING

### Check RAM Usage Real-time
```powershell
# Run continuously
while ($true) {
    Clear-Host
    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
    Write-Host " JAVA PROCESSES - RAM USAGE" -ForegroundColor Yellow
    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
    
    Get-Process java -ErrorAction SilentlyContinue | 
        Select-Object Id, 
            @{N='RAM(MB)';E={[math]::Round($_.WS/1MB,1)}},
            @{N='CPU(%)';E={$_.CPU}},
            @{N='Threads';E={$_.Threads.Count}} |
        Sort-Object 'RAM(MB)' -Descending |
        Format-Table -AutoSize
    
    $total = (Get-Process java -ErrorAction SilentlyContinue | 
              Measure-Object WS -Sum).Sum / 1GB
    Write-Host "TOTAL RAM: $([math]::Round($total, 2)) GB" -ForegroundColor Green
    
    Start-Sleep 3
}
```

### Auto-Restart on OOM
Thêm vào ServiceManager.java (đã có sẵn):
```java
// Monitor logs for OutOfMemoryError
// Auto-restart with higher memory tier
```

---

## 📈 KẾT QUẢ DỰ KIẾN

### Trước:
```
Default JVM args: 51 × 1 GB = 51 GB ❌
→ Không thể chạy trên máy thường
```

### Sau ULTRA-LOW mode:
```
Tier 1 (Critical):  3 × 200 MB =  600 MB
Tier 2 (Minimal):  30 × 95 MB  = 2,850 MB
Tier 3 (Ultra):    18 × 65 MB  = 1,170 MB
─────────────────────────────────────────
Total Services:                  4,620 MB
Docker (MySQL/Redis):            2,000 MB
OS + Others:                     2,000 MB
─────────────────────────────────────────
TOTAL:                           8.6 GB ✅

→ Chạy được trên máy 12-16 GB RAM!
→ Với pagefile 32 GB: Ổn định!
```

---

## ⚡ BƯỚC TIẾP THEO

### 1. Rebuild admin-service (ĐÃ SỬA)
```bash
cd admin-service
mvn clean package -DskipTests
```

### 2. Tăng Pagefile
```powershell
# Run script ở trên
# Restart máy
```

### 3. Thêm lazy-init vào services
Sửa `application.yml` trong common hoặc config-service:
```yaml
spring:
  main:
    lazy-initialization: true
```

### 4. Start services
```bash
cd admin-service
.\quick-start.ps1

# Hoặc start từ Admin UI: http://localhost:9091
```

### 5. Monitor RAM
```powershell
# Run monitoring script
.\monitor-ram.ps1
```

---

## 🎯 NẾU VẪN QUÁ NẶNG

### Option A: Start theo groups
Chỉ bật nhóm services đang dev, nhóm khác tắt.

### Option B: Disable scheduled tasks
Tắt @Scheduled trong analytics, scheduler, notification...

### Option C: Remote services
Một số services chạy trên server khác, local chỉ chạy core.

### Option D: Mock services
Mock một số services không cần thiết (analytics, moderation...).

---

## 🚨 CẢN BÁO

**ULTRA-LOW mode có thể gây:**
- ❌ OutOfMemoryError thường xuyên
- ❌ Service crash và restart
- ❌ Response time chậm (>1s)
- ❌ Throughput thấp

**Giải pháp:**
1. Monitor liên tục
2. Auto-restart on crash
3. Tăng pagefile đủ lớn
4. Accept performance hit

**CHỈ DÙNG CHO DEVELOPMENT LOCAL!**

---

## 📚 TÓM TẮT

✅ **Đã làm:**
- Sửa ServiceManager.java → ULTRA-LOW profiles
- Giảm RAM: 250 MB → 50-120 MB/service
- Total: 51 GB → 4.6 GB (91% reduction!)

⏳ **Cần làm:**
- Rebuild admin-service
- Tăng Windows pagefile → 16-32 GB
- Thêm lazy-init vào application.yml
- Monitor và điều chỉnh

🎉 **Kết quả:**
**Chạy full 51 services trên máy 12-16 GB RAM + pagefile 32 GB!**

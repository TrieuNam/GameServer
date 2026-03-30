# 📋 DANH SÁCH CÁC SERVICE CẦN SỬA

## 📊 Tổng Quan
- **Tổng số services**: 51
- **Tổng dung lượng**: 5,155.82 MB (~5.16 GB)
- **Trung bình**: 101.09 MB/service
- **Mục tiêu**: Giảm xuống ~1 GB (tiết kiệm 80%)

---

## 🔴 PRIORITY 1: TOP 10 NẶNG NHẤT (1,542 MB = 30% total)

**Áp dụng Thin Launcher trước tiên để có impact lớn nhất:**

| # | Service | Size (MB) | Target (MB) | Savings |
|---|---------|-----------|-------------|---------|
| 1 | gateway-service | 289.75 | 10 | 279.75 MB |
| 2 | role-service | 132.14 | 8 | 124.14 MB |
| 3 | analytics-service | 131.95 | 8 | 123.95 MB |
| 4 | notification-service | 131.89 | 8 | 123.89 MB |
| 5 | bag-service | 130.81 | 8 | 122.81 MB |
| 6 | task-service | 127.97 | 8 | 119.97 MB |
| 7 | arena-service | 127.61 | 8 | 119.61 MB |
| 8 | trial-service | 122.75 | 7 | 115.75 MB |
| 9 | pet-service | 122.55 | 7 | 115.55 MB |
| 10 | mount-service | 122.51 | 7 | 115.51 MB |

**Tổng tiết kiệm P1**: 1,360 MB (88%)

---

## 🟡 PRIORITY 2: SERVICES TRUNG BÌNH (2,193 MB = 43% total)

**30 services từ 100-122 MB:**

| Service | Size | Service | Size | Service | Size |
|---------|------|---------|------|---------|------|
| angel-service | 122.49 | rune-service | 122.48 | starmap-service | 122.47 |
| artifact-service | 122.47 | report-service | 116.86 | shizhuang-service | 112.03 |
| main-fb-service | 110.04 | world-service | 109.47 | iap-verify-service | 108.84 |
| shop-service | 108.53 | box-service | 108.07 | user-service | 107.62 |
| serverInfo-service | 106.80 | equip-service | 106.69 | leaderboard-service | 105.53 |
| wallet-service | 105.31 | anti-cheat-service | 104.77 | moderation-service | 102.77 |
| escort-service | 102.67 | territory-service | 102.67 | gameworld-service | 100.06 |
| webSocket-server | 100.05 | battleserver-service | 99.92 | globalserver-service | 99.55 |

**Tổng tiết kiệm P2**: ~1,950 MB (89%)

---

## 🟢 PRIORITY 3: SERVICES NHỎ (1,420 MB = 27% total)

**11 services dưới 100 MB:**

| Service | Size (MB) | Service | Size (MB) |
|---------|-----------|---------|-----------|
| crafting-service | 97.27 | mail-service | 90.42 |
| gm-service | 90.14 | config-service | 87.19 |
| file-service | 85.17 | session-service | 85.12 |
| drop-service | 82.52 | scheduler-service | 82.48 |
| localization-service | 78.85 | admin-service | 77.99 |
| gift-service | 74.61 | item-service | 69.93 |
| eureka-server | 56.93 | dataaccess-service | 48.92 |

**Tổng tiết kiệm P3**: ~1,250 MB (88%)

---

## ⚠️ SERVICES CÓ VẤN ĐỀ (Need Investigation)

| Service | Size | Issue |
|---------|------|-------|
| guild-service | 0.09 MB | JAR bị lỗi/chưa build đúng |
| friend-service | 0.07 MB | JAR bị lỗi/chưa build đúng |
| chat-service | 0.03 MB | JAR bị lỗi/chưa build đúng |

**Action**: Rebuild 3 services này trước khi apply Thin Launcher

---

## 🎯 IMPLEMENTATION PLAN

### Phase 1: Demo & Validation (30 phút)
```bash
# Step 1: Apply Thin Launcher to user-service
# Step 2: Build & test
# Step 3: Verify size reduction (107.62 MB → ~8 MB)
# Step 4: Test run with shared repository
```

### Phase 2: Top 10 Heaviest (1 giờ)
```bash
# Apply to: gateway, role, analytics, notification, bag, 
#          task, arena, trial, pet, mount
# Expected: 1,542 MB → 100 MB (tiết kiệm 1,442 MB)
```

### Phase 3: Remaining 38 Services (2 giờ)
```bash
# Apply to all remaining services
# Expected: 3,613 MB → 380 MB (tiết kiệm 3,233 MB)
```

### Phase 4: Fix Problem Services (30 phút)
```bash
# Rebuild guild, friend, chat services
# Apply Thin Launcher
```

---

## 📊 EXPECTED RESULTS

| Metric | Before | After | Savings |
|--------|--------|-------|---------|
| **Total Size** | 5,155 MB | ~550 MB | **4,605 MB (89%)** |
| **Gateway** | 290 MB | 10 MB | 280 MB |
| **Average** | 101 MB | 11 MB | 90 MB |
| **Docker Image** | 5.2 GB | 1 GB | 4.2 GB |
| **Deploy Time** | 10 min | 2 min | 80% faster |
| **Startup** | 8s/service | 6s/service | 25% faster |

---

## 🚀 COMMANDS TO APPLY

### Apply to All Services (Batch)
```bash
# Script tự động apply cho tất cả 51 services
cd d:\project\serverGame\GameServer
.\apply-thin-launcher-all.ps1
```

### Apply to Specific Service
```bash
# Apply cho 1 service cụ thể
.\apply-thin-launcher.ps1 -service user-service
```

### Build All with Thin Launcher
```bash
# Build tất cả services
mvn clean package -DskipTests
```

### Download Shared Dependencies (Once)
```bash
# Download dependencies 1 lần duy nhất
java -Dthin.root=./repository -jar user-service.jar --thin.dryrun
```

---

## 📞 NEXT STEPS

**Bạn muốn:**
1. **Demo user-service trước** (10 phút) - Xem kết quả ngay
2. **Apply TOP 10 ngay** (1 giờ) - Tiết kiệm 1.4 GB
3. **Apply ALL 51 services** (3 giờ) - Full implementation
4. **Tôi tạo script tự động** - Chạy 1 lệnh xong hết

Chọn số nào? 😊

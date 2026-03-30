# Activity Service

**Version**: 1.0.0
**Phase**: P5 (New Gameplay Systems)
**Port**: 8382
**Database**: `game_activity`

---

## 📋 Overview

Activity Service quản lý **hệ thống sự kiện game (Activities/Events)** — sự kiện khai server, sự kiện ngẫu nhiên, daily rewards, thị trường đặc biệt, và hệ thống quảng cáo reward. Là module mới nhất (P5).

### Core Features
- ✅ Sự kiện khai server (OpenServerActivity) — 7 ngày đầu
- ✅ Random activity events (RandActivity) — sự kiện ngẫu nhiên
- ✅ Hệ thống 7-day sign-in trong context sự kiện
- ✅ Thị trường sự kiện (Market) với refresh
- ✅ Đa bảo (Duobao) — fortune wheel
- ✅ Ad reward (phần thưởng xem quảng cáo)
- ✅ Recharge config

---

## 🎯 Flow Sự Kiện

```
[Ngày khai server — tự động kích hoạt]
OpenServerActivity (auto-start khi server khởi động)
├── Kích hoạt 7-day sign-in reward
├── Kích hoạt bonus EXP/Drop
└── Unlock special market items

[Player tham gia sự kiện]
GET /api/activity/{roleId}/sevenday
    ├── Lấy trạng thái 7 ngày
    └── Xem phần thưởng mỗi ngày

POST /api/activity/{roleId}/sevenday/claim
    └── Nhận phần thưởng ngày hôm nay

[Thị trường sự kiện]
GET /api/activity/{roleId}/market
POST /api/activity/{roleId}/market/buy { itemId, quantity }
POST /api/activity/{roleId}/market/refresh  (dùng diamond)
```

---

## 🗄️ Database Schema

### activity_progress
```sql
CREATE TABLE activity_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,  -- "sevenday", "luck", "market"
    progress_data JSON,                   -- Tiến độ và trạng thái
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_activity (role_id, activity_type)
);
```

---

## 🔌 API Endpoints

```
GET   /api/activity                             - Danh sách activities đang active
GET   /api/activity/{roleId}/sevenday           - 7-day sign-in status
POST  /api/activity/{roleId}/sevenday/claim     - Nhận reward ngày hôm nay
GET   /api/activity/{roleId}/luck               - Vận may status
POST  /api/activity/{roleId}/luck/claim         - Nhận luck reward
GET   /api/activity/{roleId}/newarea            - New area activity
POST  /api/activity/{roleId}/newarea/buy        - Mua trong new area
GET   /api/activity/{roleId}/market             - Market sự kiện
POST  /api/activity/{roleId}/market/buy         - Mua item market
POST  /api/activity/{roleId}/market/refresh     - Refresh market
POST  /api/activity/{roleId}/duobao             - Fortune wheel operation (body: opType, param1, param2)
POST  /api/activity/{roleId}/rand               - Random activity dispatch (body: activityType, operaType, params)
POST  /api/activity/{roleId}/ad-reward          - Nhận ad reward
GET   /api/activity/recharge-config             - Config nạp tiền
```

---

## 📦 API Examples

### Nhận 7-Day Reward
```bash
curl -X POST http://localhost:8382/api/activity/player123/sevenday/claim
```

### Xem Thị Trường Sự Kiện
```bash
curl http://localhost:8382/api/activity/player123/market
```

---

## ⚙️ Port Configuration

```yaml
# application-local.yml
server:
  port: 8382

# application-prod.yml  
server:
  port: 8382
```

> ⚠️ Port cấu hình trong profile file, không phải application.yml chính

---

## 🚀 Running

```bash
cd GameServer/activity-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 🔗 Integration Points

### Phụ thuộc
- **bag-service**: Grant item rewards
- **wallet-service**: Grant currency rewards

---

## 📊 Statistics

```
Entities:        1 class (ActivityProgress)
Repositories:    1 interface
Controllers:     1 class (ActivityController)
Services:        3 (OpenServerActivity, RandActivity, MarketService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


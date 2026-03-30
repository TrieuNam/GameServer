# Lingzhu Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8380 (config trong application-local.yml)  
**Database**: `game_lingzhu`

---

## 📋 Overview

Lingzhu Service quản lý **hệ thống Linh Châu (靈珠)** — đá quý/ngọc đặc biệt tăng lực mạnh. Khác với gem-service (đá thông thường), Linh Châu là hệ thống cao cấp hơn với challenge mechanics và sweep mode.

### Core Features
- ✅ Thu thập và quản lý Linh Châu
- ✅ Challenge (thử thách để nâng cấp)
- ✅ Sweep mode (quét nhanh)
- ✅ Buff stats cho nhân vật

---

## 🎯 Flow Linh Châu

```
[Player thử thách Linh Châu]
POST /api/lingzhu/{roleId}/challenge { lingzhuId }
        │
        ▼
lingzhu-service
├── Check điều kiện (level, materials)
├──► battleserver-service: Run challenge battle
└── Nếu thắng → nâng cấp lingzhu, buff stats

[Sweep nhiều lần]
POST /api/lingzhu/{roleId}/sweep { lingzhuId, count }
└── Auto-clear và nhận rewards
```

---

## 🗄️ Database Schema

### lingzhu
```sql
CREATE TABLE lingzhu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    lingzhu_id INT NOT NULL,
    lingzhu_name VARCHAR(100),
    level INT DEFAULT 0,
    is_active BOOLEAN DEFAULT FALSE,
    attrs JSON,                       -- Buffed attributes
    obtained_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_lingzhu (role_id, lingzhu_id)
);
```

---

## 🔌 API Endpoints

```
GET   /api/lingzhu/{roleId}              - Lấy tất cả linh châu
POST  /api/lingzhu/{roleId}/challenge    - Thử thách linh châu
POST  /api/lingzhu/{roleId}/sweep        - Sweep mode
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/lingzhu-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 📊 Statistics

```
Entities:        1 class (Lingzhu)
Repositories:    1 interface
Controllers:     1 class (LingzhuController)
Services:        1 class (LingzhuService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


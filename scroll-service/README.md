# Scroll Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8330 (config trong application-local.yml)  
**Database**: `game_scroll`

---

## 📋 Overview

Scroll Service quản lý **hệ thống Cuộn (Scroll)** — items đặc biệt/hiếm với các hiệu ứng đặc biệt (buff lớn, skill enhancement, v.v.). Quản lý ScrollItem instances và ScrollMeta definitions.

### Core Features
- ✅ Scroll item management
- ✅ Scroll metadata (ScrollMeta) definitions
- ✅ Sử dụng scroll để nhận hiệu ứng
- ✅ Danh sách scrolls của player

---

## 🎯 Flow Scroll

```
[Player nhận Scroll từ boss/event]
bag-service ──► Scroll item vào inventory

[Player dùng Scroll]
GET /api/scroll/{roleId}/info     ← Xem chi tiết scroll
GET /api/scroll/{roleId}/list     ← Danh sách scrolls
POST /api/scroll/{roleId}/draw    ← Rút/dùng scroll
        │
        ▼
scroll-service
├── Consume scroll item từ bag
└── Apply scroll effect (buff, skill unlock, attribute bonus)
```

---

## 🗄️ Database Schema

### scroll_item
```sql
CREATE TABLE scroll_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    scroll_type INT NOT NULL,
    scroll_meta_id INT NOT NULL,    -- Reference to ScrollMeta
    is_used BOOLEAN DEFAULT FALSE,
    used_at DATETIME,
    obtained_at DATETIME NOT NULL
);
```

### scroll_meta
```sql
CREATE TABLE scroll_meta (
    id INT PRIMARY KEY AUTO_INCREMENT,
    scroll_type INT NOT NULL,
    scroll_name VARCHAR(100),
    description VARCHAR(500),
    rarity INT,                     -- 1=Common, 2=Rare, 3=Epic, 4=Legendary
    effect_data JSON,               -- Effect definition
    is_active BOOLEAN DEFAULT TRUE
);
```

---

## 🔌 API Endpoints

```
GET   /api/scroll/{roleId}/info    - Thông tin chi tiết scroll của player
GET   /api/scroll/{roleId}/list    - Danh sách scrolls
POST  /api/scroll/{roleId}/draw    - Dùng scroll
```

---

## 📦 API Examples

### Dùng Scroll
```bash
curl -X POST http://localhost:8330/api/scroll/player123/draw \
  -H "Content-Type: application/json" \
  -d '{"scrollItemId": 456}'
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/scroll-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 📊 Statistics

```
Entities:        2 classes (ScrollItem, ScrollMeta)
Repositories:    2 interfaces
Controllers:     1 class (ScrollController)
Services:        1 class (ScrollService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


# Pagoda Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8320 (config trong application-local.yml)  
**Database**: `game_pagoda`

---

## 📋 Overview

Pagoda Service quản lý **hệ thống Tháp (Pagoda/Tower Dungeons)** — leo tháp nhiều tầng (floors), mỗi tầng có thử thách riêng, nhận phần thưởng khi qua mỗi tầng. Hỗ trợ 2 loại tháp: Shilian (修炼) và Gumo (古魔).

### Core Features
- ✅ Shilian Tower (修炼塔) — tháp tu luyện
- ✅ Gumo Tower (古魔塔) — tháp cổ ma
- ✅ Multi-floor progression
- ✅ Challenge mechanics
- ✅ Reward claiming per floor
- ✅ Sweep/auto-clear unlocked floors

---

## 🎯 Flow Leo Tháp

```
[Player thử thách tầng mới]
POST /api/pagoda/{roleId}/shilian/challenge { floor }
        │
        ▼
pagoda-service
├── Kiểm tra player đã clear floor trước chưa
├──► battleserver-service (gRPC): Run battle với floor boss
│          │
│    ◄── Battle result
│
├── Nếu thắng:
│   ├── Unlock floor tiếp theo
│   └── Mark reward as claimable
│
POST /api/pagoda/{roleId}/shilian/claim { floor }
└── Grant floor reward
```

---

## 🗄️ Database Schema

### pagoda_progress
```sql
CREATE TABLE pagoda_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    pagoda_type INT NOT NULL,        -- 1=Shilian, 2=Gumo
    current_floor INT DEFAULT 0,
    max_floor INT DEFAULT 0,
    claimed_floors JSON,             -- Bit array: floors đã claim reward
    last_challenge_at DATETIME,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_type (role_id, pagoda_type)
);
```

---

## 🔌 API Endpoints

```
# Shilian Tower
GET   /api/pagoda/{roleId}/shilian              - Tiến độ Shilian Tower
POST  /api/pagoda/{roleId}/shilian/challenge    - Thử thách tầng
POST  /api/pagoda/{roleId}/shilian/claim        - Nhận reward tầng

# Gumo Tower
GET   /api/pagoda/{roleId}/gumo                 - Tiến độ Gumo Tower
POST  /api/pagoda/{roleId}/gumo/challenge       - Thử thách tầng
POST  /api/pagoda/{roleId}/gumo/claim           - Nhận reward tầng
```

---

## 📦 API Examples

### Thử Thách Tầng Tiếp Theo
```bash
curl -X POST http://localhost:8320/api/pagoda/player123/shilian/challenge \
  -H "Content-Type: application/json" \
  -d '{"floor": 15}'
```

### Nhận Reward Tầng
```bash
curl -X POST http://localhost:8320/api/pagoda/player123/shilian/claim \
  -H "Content-Type: application/json" \
  -d '{"floor": 15}'
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/pagoda-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 📊 Statistics

```
Entities:        1 class (PagodaProgress)
Repositories:    1 interface
Controllers:     1 class (PagodaController)
Services:        2 (ShilianService, GumoService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~500 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


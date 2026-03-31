# Knights Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8310 (config trong application-local.yml)  
**Database**: `game_knights`

---

## 📋 Overview

Knights Service quản lý **hệ thống Hiệp Sĩ/Tướng (Knights)** — thu thập tướng, nâng cấp cấp độ và chất lượng, deploy vào đội hình chiến đấu, và nhận thưởng từ sequences/levels.

### Core Features
- ✅ Thu thập hiệp sĩ mới
- ✅ Nâng cấp level và chất lượng (gradeup)
- ✅ Deploy hiệp sĩ vào đội hình (conditions)
- ✅ Claim sequence rewards
- ✅ Claim level-based rewards
- ✅ Combat power contribution từ knights

---

## 🎯 Flow Knights System

```
[Unlock Hiệp Sĩ Mới]
bag-service ──► knights-service: Trigger khi nhận knight shard/item
                      │
        knights-service.unlockKnight()
        ├── Tạo knight record
        └── Update combat power

[Deploy Hiệp Sĩ]
GET /api/knights/{roleId}/conditions
    └── Lấy điều kiện đội hình tốt nhất
        (ví dụ: slot 1=ATK knight, slot 2=DEF knight, ...)
```

---

## 🗄️ Database Schema

### knight
```sql
CREATE TABLE knight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    knight_id INT NOT NULL,           -- Knight type ID từ config
    knight_name VARCHAR(100),
    level INT DEFAULT 1,
    grade INT DEFAULT 1,              -- 1=Normal, 2=Rare, 3=Epic, 4=Legendary
    is_deployed BOOLEAN DEFAULT FALSE,
    deploy_slot INT DEFAULT 0,
    star INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_knight (role_id, knight_id)
);
```

---

## 🔌 API Endpoints

```
GET   /api/knights/{roleId}              - Lấy tất cả hiệp sĩ
GET   /api/knights/{roleId}/conditions   - Điều kiện đội hình hiện tại
POST  /api/knights/{roleId}/claim-seq    - Nhận reward theo sequence
POST  /api/knights/{roleId}/claim-level  - Nhận reward theo level
```

---

## 📦 API Examples

### Lấy Danh Sách Hiệp Sĩ
```bash
curl http://localhost:8310/api/knights/player123
```

### Claim Level Reward
```bash
curl -X POST http://localhost:8310/api/knights/player123/claim-level \
  -H "Content-Type: application/json" \
  -d '{"knightId": 5, "rewardLevel": 10}'
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/knights-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 🔗 Integration Points

### Phụ thuộc
- **bag-service**: Consume materials, grant rewards

---

## 📊 Statistics

```
Entities:        1 class (Knight)
Repositories:    1 interface
Controllers:     1 class (KnightsController)
Services:        1 class (KnightsService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


# Gem Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8381 (config trong application-local.yml)  
**Database**: `game_gem`

---

## 📋 Overview

Gem Service quản lý **hệ thống Đá Quý (Gem Enhancement)** — thu thập, nâng cấp, và gắn đá quý vào các slot trang bị để tăng sức mạnh nhân vật. Là tầng enhancement bổ sung cho equipment system.

### Core Features
- ✅ Thu thập và quản lý đá quý
- ✅ Nâng cấp cấp độ gem
- ✅ Gắn gem vào slot trang bị
- ✅ Tháo gem
- ✅ Tổng hợp gem (compose)
- ✅ Nâng cấp tất cả gem cùng lúc
- ✅ Mua gem bằng currency

---

## 🎯 Flow Gem System

```
[Player có gem trong bag]
        │
        ▼
POST /api/gem/{roleId}/inlay { gemId, slotIndex }
        │
        ▼
gem-service
├── Verify gem tồn tại trong bag
├── Check equipment slot hỗ trợ gem type
├── Tháo gem cũ nếu có (trả về bag)
├── Gắn gem mới vào slot
└── Recalculate combat power

[Nâng Cấp Gem]
POST /api/gem/{roleId}/upgrade-all
├── Tìm tất cả gems trong bag đủ điều kiện upgrade
└── Nâng cấp đồng loạt
```

---

## 🗄️ Database Schema

### gem_slot
```sql
CREATE TABLE gem_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    equipment_slot INT NOT NULL,    -- Slot trang bị (1-6)
    gem_slot_index INT NOT NULL,    -- Vị trí gem trong slot (1-4)
    gem_id VARCHAR(50),             -- NULL = trống
    gem_level INT DEFAULT 1,
    gem_type INT,
    attrs JSON,                     -- Attributes từ gem
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_slot_index (role_id, equipment_slot, gem_slot_index)
);
```

---

## 🎮 Gem Types

| Loại | ID | Attribute |
|------|----|-----------|
| **Attack Gem** | 1 | ATK bonus |
| **Defense Gem** | 2 | DEF bonus |
| **HP Gem** | 3 | HP bonus |
| **Speed Gem** | 4 | SPD bonus |
| **Crit Gem** | 5 | CRIT bonus |
| **Special Gem** | 6 | Multiple random attrs |

---

## 🔌 API Endpoints

```
GET   /api/gem/{roleId}              - Lấy tất cả gem slots
POST  /api/gem/{roleId}/inlay        - Gắn gem vào slot
POST  /api/gem/{roleId}/remove       - Tháo gem
POST  /api/gem/{roleId}/compose      - Tổng hợp gems
POST  /api/gem/{roleId}/upgrade-all  - Nâng cấp tất cả
POST  /api/gem/{roleId}/buy          - Mua gem
```

---

## 📦 API Examples

### Gắn Gem
```bash
curl -X POST http://localhost:8381/api/gem/player123/inlay \
  -H "Content-Type: application/json" \
  -d '{
    "gemItemId": "gem_atk_lv3_001",
    "equipmentSlot": 1,
    "gemSlotIndex": 1
  }'
```

### Tổng Hợp Gem
```bash
curl -X POST http://localhost:8381/api/gem/player123/compose \
  -H "Content-Type: application/json" \
  -d '{"gemType": 1, "fromLevel": 3, "count": 3}'
# 3 gems level 3 → 1 gem level 4
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/gem-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 🔗 Integration Points

### Phụ thuộc
- **bag-service**: Consume/grant gems
- **wallet-service**: Buy gems

---

## 📊 Statistics

```
Entities:        1 class (GemSlot)
Repositories:    1 interface
Controllers:     1 class (GemController)
Services:        1 class (GemService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~500 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


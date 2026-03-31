# Shizhuang Service

**Version**: 1.0.0  
**Phase**: P5 (New Gameplay Systems)  
**Port**: 8350 (config trong application-local.yml)  
**Database**: game_shizhuang

---

## 📋 Overview

Shizhuang Service quản lý **hệ thống Thời Trang / Trang Phục (时装 — Costume/Fashion)** — quản lý trang phục nhân vật, skin thiên thần, và mô hình trang phục. Cho phép player tùy chỉnh ngoại hình mà không ảnh hưởng combat stats.

### Core Features
- ✅ Quản lý trang phục nhân vật (ShiZhuang)
- ✅ Skin thiên thần (Angel skins — PlayerAngelEntity)
- ✅ Trang phục mô hình (PlayerClothesEntity)
- ✅ Hệ thống skin thiên thần
- ✅ Mặc/tháo trang phục
- ✅ Level up trang phục (bonus minor stats)
- ✅ Mua trang phục bằng diamond

---

## 🎯 Flow Thời Trang

```
[Player mở shop trang phục]
GET /api/shizhuang/list
    └── Danh sách trang phục + lock/unlock status

[Player mua trang phục]
POST /api/shizhuang/buy { costumeId }
├──► wallet-service: consume diamond
└── Grant PlayerClothesEntity

[Mặc trang phục]
POST /api/shizhuang/wear { costumeId }
├── Update current costume
└── Cập nhật visual data cho client

[Nâng cấp trang phục]
POST /api/shizhuang/levelup { costumeId }
├──► bag-service: consume materials
└── Tăng level → bonus stats nhỏ
```

---

## 🗄️ Database Schema

### player_clothes
```sql
CREATE TABLE player_clothes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    clothes_id INT NOT NULL,
    clothes_name VARCHAR(100),
    level INT DEFAULT 1,
    is_wearing BOOLEAN DEFAULT FALSE,
    obtained_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_clothes (role_id, clothes_id)
);
```

### player_angel_skin
```sql
CREATE TABLE player_angel_skin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    angel_id INT NOT NULL,
    skin_id INT NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    obtained_at DATETIME NOT NULL,
    UNIQUE KEY uk_role_angel_skin (role_id, angel_id, skin_id)
);
```

---

## 🔌 API Endpoints

```
GET   /api/shizhuang/get          - Lấy thông tin trang phục đang mặc
GET   /api/shizhuang/list         - Danh sách tất cả trang phục
POST  /api/shizhuang/add          - Thêm trang phục (admin)
DELETE /api/shizhuang/delete      - Xóa trang phục (admin)
POST  /api/shizhuang/buy          - Mua trang phục
POST  /api/shizhuang/wear         - Mặc trang phục
POST  /api/shizhuang/levelup      - Nâng cấp trang phục
GET   /api/shizhuang/list/{roleId} - Trang phục của player cụ thể

# Angel skins
GET   /api/angel/{playerId}           - Angel skins của player
POST  /api/angel/{playerId}/levelup   - Nâng cấp angel skin
GET   /api/angel/{playerId}/stage-cfg - Config stages
GET   /api/angel/{playerId}           - Get angel info
POST  /api/angel/{playerId}/gradeup   - Nâng phẩm angel
```

---

## 📦 API Examples

### Mua Trang Phục
```bash
curl -X POST http://localhost:8350/api/shizhuang/buy \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "clothesId": 5001}'
```

### Mặc Trang Phục
```bash
curl -X POST http://localhost:8350/api/shizhuang/wear \
  -H "Content-Type: application/json" \
  -d '{"roleId": "player123", "clothesId": 5001}'
```

---

## ⚙️ Port Configuration

> ⚠️ Port cấu hình trong `application-local.yml` / `application-prod.yml`

---

## 🚀 Running

```bash
cd GameServer/shizhuang-service
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 🔗 Integration Points

### Phụ thuộc (Feign)
- **bag-service**: Consume materials khi nâng cấp
- **role-service**: Query role info
- **wallet-service**: Consume diamond khi mua
- **item-service**: Validate item metadata
- **config-service**: Load `model_clothes.json`, `cloth_shop.json`, `angel.json`

Shizhuang-service gọi config bằng `GET /api/config/file?path=gameworld/logicconfig/*.json`.

---

## 📊 Statistics

```
Entities:        2 classes (PlayerClothes, PlayerAngelSkin)
Repositories:    2 interfaces
Controllers:     2 (ShizhuangController, AngelController)
Services:        2 (ShizhuangService, AngelSkinService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~600 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


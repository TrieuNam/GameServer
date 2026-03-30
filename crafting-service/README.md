# Crafting Service

**Version**: 1.0.0  
**Phase**: P1 (Database & Core Gameplay)  
**Port**: 8280 · **gRPC**: 9280  
**Database**: `game_crafting`

---

## 📋 Overview

Crafting Service quản lý **hệ thống chế tạo vật phẩm (crafting/synthesis)** — kết hợp nguyên liệu để tạo ra items mới. Công thức chế tạo được load từ config-service, service tiêu thụ nguyên liệu từ bag và cấp item mới.

### Core Features
- ✅ Chế tạo items theo công thức từ config
- ✅ Tiêu thụ nguyên liệu (consume từ bag)
- ✅ Tạo item mới (grant vào bag)
- ✅ Kiểm tra đủ nguyên liệu trước khi craft
- ✅ gRPC server (port 9280)

---

## 🎯 Flow Chế Tạo

```
Client ──► POST /crafting/craft { recipeId, roleId }
                │
                ▼
        crafting-service
        ├── Load recipe từ config-service
        ├── Kiểm tra materials trong bag-service
        ├──► bag-service: consume materials
        ├── Tính output (có thể có bonus quality)
        └──► bag-service: grant crafted items
```

---

## 🗄️ Database Schema

### crafting_record
```sql
CREATE TABLE crafting_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id VARCHAR(50) NOT NULL,
    recipe_id VARCHAR(50) NOT NULL,
    materials_used JSON,            -- Materials consumed
    result_item_id VARCHAR(50),
    result_quality INT,
    crafted_at DATETIME NOT NULL
);
```

---

## 🔌 API Endpoints

(Chủ yếu qua gRPC từ webSocket-server)

```
gRPC: CraftingServiceGrpc
  - craftItem(CraftRequest) → CraftResponse
  - getRecipes(RoleId) → RecipeList
  - checkMaterials(CheckRequest) → CheckResponse
```

---

## 📦 API Examples

### Chế Tạo Item (qua gRPC)
```protobuf
CraftRequest {
  role_id: "player123",
  recipe_id: "recipe_iron_sword",
  count: 1
}
CraftResponse {
  success: true,
  item_id: "item_iron_sword",
  quality: 2
}
```

---

## 🔧 Business Logic

### Recipe Structure (từ config)
```json
{
  "id": "recipe_iron_sword",
  "name": "Iron Sword",
  "materials": [
    {"itemId": "iron_ore", "quantity": 5},
    {"itemId": "wood_plank", "quantity": 2}
  ],
  "result": {
    "itemId": "item_iron_sword",
    "quantity": 1,
    "baseQuality": 2
  },
  "levelRequired": 10,
  "successRate": 100
}
```

### Quality Roll
- Một số recipe có chance craft được quality cao hơn
- Dùng luck stat của player để tăng bonus quality chance

---

## 🚀 Running

```bash
cd GameServer/crafting-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### gRPC Server (port 9280)
- webSocket-server gọi trực tiếp

### Phụ thuộc
- **bag-service**: Consume materials & grant results
- **config-service**: Load recipes

---

## 📊 Statistics

```
Entities:        1 class (CraftingRecord)
Repositories:    1 interface
Controllers:     N/A (gRPC only)
Services:        1 class
gRPC:            CraftingServiceGrpcImpl
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22


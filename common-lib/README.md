# Common Lib

**Version**: 1.0.0  
**Type**: Shared Maven Library (không phải service)  
**Group ID**: `org.SouthMillion`  
**Artifact ID**: `common-lib`

---

## 📋 Overview

`common-lib` là **thư viện dùng chung** cho toàn bộ hệ thống microservices. Chứa các DTO (Data Transfer Object), gRPC Protobuf definitions, API utilities và exception classes được tất cả services import.

> ⚠️ Đây là **library**, không phải service — không có REST controller, không có port, không cần chạy độc lập.

---

## 📦 Cấu Trúc

```
common-lib/
├── src/main/java/org/SouthMillion/
│   ├── api/
│   │   ├── ApiError.java          - Chuẩn hóa lỗi HTTP response
│   │   └── GenericResult<T>.java  - Generic result wrapper (code, message, data)
│   ├── dto/                       - Data Transfer Objects
│   │   ├── bag/                   - BagDTOs, BagAddItemReq/Resp, BagConsumeReq, GrantReq, ItemInfo, UseItemReq
│   │   ├── battle/                - Battle-related DTOs
│   │   ├── box/                   - Box/Loot DTOs
│   │   ├── config/                - ConfigEnvelope, config DTOs
│   │   ├── crafting/              - Crafting request/response DTOs
│   │   ├── drop/                  - RollRequest, RollResult
│   │   ├── equip/                 - EquipDTOs, EquipFumoDTOs
│   │   ├── event/                 - Event DTOs
│   │   ├── gift/                  - Gift DTOs
│   │   ├── item/                  - Item metadata DTOs
│   │   ├── main_fb/               - Main-FB (福缘) DTOs
│   │   ├── pet/                   - Pet DTOs
│   │   ├── report/                - Report DTOs
│   │   ├── role/                  - RoleDTOs, CreateRoleReq, mail, advertisement, settings, other
│   │   ├── serverInfor/           - ServerInfoDto
│   │   ├── session/               - Session DTOs
│   │   ├── ShiZhuang/             - Appearance/Fashion DTOs
│   │   ├── shop/                  - Shop DTOs
│   │   ├── task/                  - Task DTOs
│   │   ├── user/                  - User DTOs
│   │   └── wallet/                - WalletDTOs, ResultDTO
│   └── exception/
│       ├── BizException.java      - Business logic exception (code + message)
│       └── NotFoundException.java - 404 Not Found exception
└── src/main/proto/                - gRPC Protobuf definitions
    ├── common.proto               - Common types
    ├── bag_service.proto
    ├── combat_service.proto
    ├── crafting_service.proto
    ├── equip_service.proto
    ├── escort_service.proto
    ├── file_service.proto
    ├── gameworld_service.proto
    ├── leaderboard_service.proto
    ├── localization_service.proto
    ├── main_fb_service.proto
    ├── mount_service.proto
    ├── pet_service.proto
    ├── role_service.proto
    ├── rune_service.proto
    ├── shop_service.proto
    ├── starmap_service.proto
    ├── territory_service.proto
    ├── trial_service.proto
    ├── wallet_service.proto
    ├── analytics_service.proto
    ├── angel_service.proto
    ├── arena_service.proto
    ├── artifact_service.proto
    ├── notification_service.proto
    ├── cs/                        - Client→Server proto messages
    └── sc/                        - Server→Client proto messages
```

---

## 🔑 Classes Quan Trọng

### `GenericResult<T>`
Wrapper chuẩn hóa cho tất cả REST responses nội bộ:
```java
// Response thành công
GenericResult.ok(data);           // { code: 0, message: "OK", data: ... }

// Response lỗi
GenericResult.err(1001, "Insufficient funds");  // { code: 1001, message: "...", data: null }
```

### `ApiError`
Lỗi đơn giản cho public API:
```java
return ResponseEntity.badRequest().body(new ApiError("Invalid request"));
// { "message": "Invalid request" }
```

### `BizException`
Exception cho business logic:
```java
throw new BizException("Không đủ tiền");  // RuntimeException với HTTP 400
throw new NotFoundException("Item not found");  // RuntimeException với HTTP 404
```

---

## 🔌 Cách Sử Dụng

### Thêm dependency trong `pom.xml` của service:
```xml
<dependency>
    <groupId>org.SouthMillion</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Import DTO trong controller:
```java
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.api.GenericResult;
import org.SouthMillion.exception.BizException;
```

---

## 📡 gRPC Proto Files

Các file `.proto` định nghĩa interface cho gRPC communication giữa services. Được compile thành Java classes trong quá trình `mvn compile`.

| Proto File | Service sử dụng |
|---|---|
| `bag_service.proto` | bag-service ←→ webSocket-server |
| `equip_service.proto` | equip-service ←→ webSocket-server |
| `role_service.proto` | role-service ←→ webSocket-server |
| `shop_service.proto` | shop-service ←→ webSocket-server |
| `wallet_service.proto` | wallet-service ←→ các services |
| `trial_service.proto` | trial-service ←→ webSocket-server |
| `territory_service.proto` | territory-service ←→ webSocket-server |
| `starmap_service.proto` | starmap-service ←→ webSocket-server |
| `rune_service.proto` | rune-service ←→ webSocket-server |
| `mount_service.proto` | mount-service ←→ webSocket-server |
| `pet_service.proto` | pet-service ←→ webSocket-server |
| `combat_service.proto` | battleserver-service ←→ webSocket-server |
| `crafting_service.proto` | crafting-service ←→ webSocket-server |
| `gameworld_service.proto` | gameworld-service ←→ webSocket-server |
| `leaderboard_service.proto` | leaderboard-service ←→ webSocket-server |

---

## 🗂️ DTO Packages Chi Tiết

### `dto/wallet` — WalletDTOs
- `WalletDTOs.Change` — currency itemId + amount
- `WalletDTOs.BatchReq` — batch currency request (roleId, changes[], reason, idemKey)
- `WalletDTOs.MutateResp` — mutation response (balances after)
- `WalletDTOs.BalancesResp` — balances map (itemId → amount)
- `ResultDTO<T>` — result wrapper dùng trong internal API

### `dto/bag` — BagDTOs
- `BagDTOs` — bag operation DTOs
- `BagAddItemReq/Resp` — thêm item vào túi
- `BagConsumeReq` — tiêu thụ item từ túi
- `GrantReq` — cấp item (từ drop/reward)
- `ItemInfo` — thông tin item đơn giản
- `UseItemReq` — sử dụng item

### `dto/equip` — EquipDTOs
- `EquipDTOs.EquipReq/UnequipReq/OkResp/ListResp` — trang bị/tháo/kết quả
- `EquipFumoDTOs` — Fumo enchantment DTOs

### `dto/role` — RoleDTOs
- `RoleDTOs` — thông tin nhân vật
- `CreateRoleReq` — tạo nhân vật mới
- `mail/` — DTOs cho mail system
- `advertisement/` — DTOs cho quảng cáo
- `settings/` — cài đặt nhân vật

---

## 🏗️ Build

```bash
cd GameServer/common-lib
mvn clean install
```

> ⚠️ Phải build **trước tất cả services** khác. Tất cả services đều phụ thuộc vào common-lib.

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22



# 📋 GameServer — Tổng Quan Tất Cả Services

> **Version**: 1.1.0 | **Framework**: Spring Boot 3.5.3 + Spring Cloud 2025.0.0 | **Java**: 21  
> **Kiến trúc**: Microservices | **Service Discovery**: Eureka | **Message Bus**: Kafka | **Cache**: Redis  
> **Cập nhật**: 2026-03-16

---

## 📊 Thống Kê Tổng Quan

| Thông tin | Số lượng |
|-----------|----------|
| Tổng số services | **57** |
| Services có port | **57** (tất cả đã có port) |
| Services có gRPC | ~20 |
| Shared libraries | 2 (common, common-lib) |
| Phases | 6 (P0 → P5 + Special) |

> **Ghi chú port**: Services P5 có port cấu hình trong `application-local.yml` / `application-prod.yml` (đánh dấu `*`)

---

## 🗺️ Kiến Trúc Tổng Thể

```
Client (Mobile/Web)
       │
       ▼
[Gateway :8080] ──────────────────────────────────────────────────────┐
       │                                                               │
       ├──► [Session :8096] ──► [User :8110] ──► [Role :8410]        │
       │                                                               │
       ├──► [WebSocket-Server :8094]  ◄──── Protobuf Binary Protocol  │
       │         │ (Feign/gRPC to all business services)              │
       │         ▼                                                     │
       └──► [All Business Services via Eureka LB]                    │
                                                                       │
[Eureka :8761] ◄────── All services register here                    │
[Config :8888] ◄────── Centralized configuration                     │
```

---

## 📦 PHASE P0 — Core Infrastructure

### 🔵 common-lib
- **Port**: N/A (Library)
- **Mô tả**: Shared library dùng chung cho tất cả services
- **Chức năng**:
  - DTOs và Protobuf generated stubs
  - gRPC service interfaces (BagService, RoleService, ShopService, v.v.)
  - Common utilities và base classes
  - Event DTOs (BagChangedEvent, v.v.)

---

### 🔵 eureka-server
- **Port**: `8761`
- **Dashboard**: http://localhost:8761
- **Mô tả**: Service Discovery — tất cả services đăng ký và tìm nhau qua đây
- **Chức năng**: Service registry, health check, load balancing discovery

---

### 🔵 config-service
- **Port**: `8888`
- **Health**: http://localhost:8888/actuator/health
- **Mô tả**: Centralized Configuration Management
- **Chức năng**:
  - Serve config files (JSON/YAML) cho game world (monster, skill, drop, v.v.)
  - ETag caching với TTL 60s
  - Hỗ trợ classpath và filesystem mode
  - Clear cache API (với token)

---

### 🔵 gateway-service
- **Port**: `8080`
- **Health**: http://localhost:8080/actuator/health
- **Mô tả**: API Gateway — entry point duy nhất cho tất cả HTTP/REST requests
- **Chức năng**:
  - Route requests đến các microservices qua Eureka load balancer
  - JWT authentication filter
  - CORS configuration
  - WebSocket proxy (`/websocket-server/ws/**`)
- **Auth Whitelist**: `/actuator/**`, `/session-service/api/session/login`, `/config-service/api/config/**`

---

### 🔵 webSocket-server
- **Port**: `8094`
- **Protocol**: WebSocket + Binary Protobuf
- **Mô tả**: Real-time game communication server — cầu nối chính giữa client và tất cả game services
- **Chức năng**:
  - Nhận/gửi tin nhắn Protobuf binary qua WebSocket
  - Route message đến service tương ứng qua Feign/gRPC
  - Player session registry (Redis-backed)
  - Cross-server session management
  - Kafka consumer (BagChangedEvent → push client)
  - Handlers cho ~50 game modules
- **Dependencies**: Redis, Kafka, tất cả business services (Feign + gRPC)
- **WebSocket URL**: `ws://localhost:8080/websocket-server/ws/game?token={jwt-token}`

---

## 📦 PHASE P1 — Database & Core Gameplay

### 🟢 user-service
- **Port**: `8110`
- **Database**: `user_db`
- **Mô tả**: Quản lý tài khoản người dùng (đăng ký, đăng nhập)
- **Chức năng**:
  - Đăng ký / đăng nhập
  - Quản lý user account
  - Internal auth API cho các services khác
  - AuthController, UserController, InternalAuthController

---

### 🟢 role-service
- **Port**: `8410` | **gRPC**: `9410`
- **Database**: `db_role`
- **Mô tả**: Quản lý nhân vật (character/role) của người chơi
- **Chức năng**:
  - CRUD nhân vật, đặt tên, tăng exp/level
  - Cấu hình nhân vật (RoleConfigCache: level table, base attr, exp bundle)
  - Hệ thống mail in-game (basic)
  - Cài đặt hệ thống người chơi
  - Quảng cáo & reward (AdvertisementController)
  - Kafka producer (BagEventProducer)
  - gRPC server cho các service khác query role info
- **Flyway migrations**: V1 (init), V2 (roleId bigint)

---

### 🟢 serverInfo-service
- **Port**: `8095`
- **Database**: `serverinfo_db`
- **Mô tả**: Quản lý thông tin server game (danh sách server, trạng thái)
- **Chức năng**:
  - CRUD thông tin server
  - Redis cache server info
  - Cung cấp danh sách server cho client khi login

---

### 🟢 session-service
- **Port**: `8096`
- **Database**: — (Redis only)
- **Mô tả**: Xác thực phiên đăng nhập và JWT token management
- **Chức năng**:
  - Login flow: xác thực user → phát JWT
  - JWT token validation
  - Rate limiting (RateLimitService)
  - User online status tracking
  - Time sync endpoint (không cần auth)
- **Cache**: Redis
- **Dependencies**: user-service (Feign)

---

### 🟢 wallet-service
- **Port**: `8210`
- **Database**: `wallet_db`
- **Mô tả**: Quản lý ví tiền trong game (gold, diamond, coin, v.v.)
- **Chức năng**:
  - CRUD ví tiền cho mỗi role
  - Lịch sử giao dịch (WalletLedger)
  - Internal API cho các services khác gọi
  - WalletController + InternalWalletController

---

### 🟢 report-service
- **Port**: `8098`
- **Database**: `report_db`
- **Mô tả**: Ghi lại sự kiện quan trọng (boss kill, thông báo hệ thống)
- **Chức năng**:
  - Lưu sự kiện báo cáo (ReportEvent)
  - Quản lý thông báo (Notice/Announcement)
  - Boss kill tracking (BossService)
  - Kafka event consumer

---

### 🟢 iap-verify-service
- **Port**: `8580`
- **Database**: `iap_verify_db`
- **Mô tả**: Xác thực thanh toán In-App Purchase (Apple/Google)
- **Chức năng**:
  - Verify receipt từ App Store / Google Play
  - Ghi lịch sử giao dịch IAP
  - Chống gian lận thanh toán

---

### 🟢 bag-service
- **Port**: `8230` | **gRPC**: `9230`
- **Database**: `bag_db`
- **Mô tả**: Quản lý túi đồ (inventory) của nhân vật
- **Chức năng**:
  - Thêm/xóa/dùng items (BagDomainService)
  - Event deduplication (BagEventDedup)
  - Recycle progress tracking
  - Kafka producer (BagChangedEvent → webSocket-server push)
  - gRPC server
- **Dependencies**: wallet-service (Feign)

---

### 🟢 equip-service
- **Port**: `8240` | **gRPC**: `9240`
- **Database**: `equip_db`
- **Mô tả**: Quản lý trang bị (equipment) của nhân vật
- **Chức năng**:
  - Trang bị/tháo đồ
  - Nâng cấp trang bị
  - Fumo/enhancement system

---

### 🟢 drop-service
- **Port**: `8250`
- **Database**: — (Stateless)
- **Mô tả**: Hệ thống rơi đồ (loot drops) từ monster/boss
- **Chức năng**:
  - Tính toán drop tables từ config
  - Phát rewards theo xác suất
  - Tích hợp với config-service

---

### 🟢 shop-service
- **Port**: `8260` | **gRPC**: `9260`
- **Database**: `shop_db`
- **Mô tả**: Hệ thống cửa hàng trong game
- **Chức năng**:
  - Mua bán items qua shop
  - Shop config cache (ShopConfigCache)
  - Giới hạn mua hàng theo ngày/tuần (ShopLimit)
  - Nhiều loại shop khác nhau
- **Dependencies**: bag-service, wallet-service, item-service, role-service, config-service (Feign)

---

### 🟢 gift-service
- **Port**: `8270`
- **Database**: — (Stateless)
- **Mô tả**: Hệ thống quà tặng và phần thưởng
- **Chức năng**:
  - Gửi quà giữa người chơi
  - Reward package management
  - Tích hợp với scheduler cho daily gifts

---

### 🟢 box-service
- **Port**: `8290`
- **Database**: `box_db`
- **Mô tả**: Hệ thống hộp quà (mystery box / gacha)
- **Chức năng**:
  - Mở hộp quà bằng key/currency
  - Drop logic theo xác suất
  - Lịch sử mở hộp

---

### 🟢 crafting-service
- **Port**: `8280` | **gRPC**: `9280`
- **Database**: `crafting_db`
- **Mô tả**: Hệ thống chế tạo vật phẩm (crafting/synthesis)
- **Chức năng**:
  - Công thức chế tạo từ config
  - Tiêu thụ nguyên liệu và tạo item mới
  - gRPC server

---

## 📦 PHASE P2 — Combat, World & Social

### 🟡 arena-service
- **Port**: `8084`
- **Database**: `game_arena`
- **Mô tả**: Hệ thống đấu trường PvP
- **Chức năng**:
  - Xếp hạng đấu trường
  - Matching đối thủ
  - Cross-server arena
  - Lịch sử trận đấu
  - Phát reward theo hạng

---

### 🟡 trial-service
- **Port**: `8300` | **gRPC**: `9300`
- **Database**: `game_trial`
- **Mô tả**: Hệ thống thử thách/dungeon theo stages
- **Chức năng**:
  - Multi-stage progression
  - Giới hạn lượt thử mỗi ngày (3 lượt, reset midnight)
  - Score & star rating (0-3 sao)
  - Speed record tracking
  - Reward claims theo bit flag
  - Reset daily/all progress

---

### 🟡 task-service
- **Port**: `8097`
- **Database**: `game_task`
- **Mô tả**: Hệ thống nhiệm vụ (quest/daily tasks)
- **Chức năng**:
  - Task progress tracking
  - Achievement system
  - 7-day sign-in (SevenDaySign)
  - Kafka consumer (Arena, Combat, Trial events)
  - Statistics service
- **Dependencies**: bag-service, wallet-service, leaderboard-service (Feign)

---

### 🟡 battleserver-service
- **Port**: `8082` | **gRPC**: `9082`
- **Database**: `db_battle_service`
- **Mô tả**: Server chiến đấu — xử lý combat logic
- **Chức năng**:
  - Battle computation
  - Combat result processing
  - gRPC server (BattleServerGrpcClient từ webSocket-server)

---

### 🟡 globalserver-service
- **Port**: `8100`
- **Database**: `globalserver_service_db`
- **Mô tả**: Server quản lý dữ liệu global/cross-server
- **Chức năng**:
  - Cross-server data synchronization
  - Global state management
  - Kafka integration

---

### 🟡 gameworld-service
- **Port**: `8105` | **gRPC**: `9105`
- **Database**: `gameworld_db`
- **Mô tả**: Quản lý thế giới game (maps, scenes, monsters)
- **Chức năng**:
  - Scene management
  - Monster spawning
  - Player position tracking
  - gRPC server (GameWorldGrpcClient từ webSocket-server)

---

### 🟡 starmap-service
- **Port**: `8092` | **gRPC**: `9092`
- **Database**: `game_starmap`
- **Mô tả**: Hệ thống bản đồ sao / thiên văn (tăng sức mạnh)
- **Chức năng**:
  - Kích hoạt và nâng cấp sao (Star)
  - Hệ thống chòm sao (Constellation)
  - Tích lũy năng lượng sao
  - Tính toán celestial power
- **Dependencies**: bag-service, wallet-service, role-service (Feign)

---

### 🟡 territory-service
- **Port**: `8360` | **gRPC**: `9086`
- **Database**: `game_territory`
- **Mô tả**: Hệ thống lãnh địa/căn cứ của người chơi
- **Chức năng**:
  - Sở hữu và nâng cấp lãnh thổ
  - Xây dựng công trình (TerritoryBuilding)
  - Sản xuất tài nguyên tự động (gold)
  - Hàng đợi xây dựng (construction queue)
  - Tính điểm phồn thịnh (prosperity)
  - Đổi tên & thay đổi ngoại hình lãnh địa
- **Dependencies**: bag-service, wallet-service (Feign)

---

### 🟡 escort-service
- **Port**: `8340`
- **Database**: `game_escort`
- **Mô tả**: Hệ thống hộ tống vận chuyển hàng hóa
- **Chức năng**:
  - 5 cấp chất lượng (Trắng → Cam)
  - Sinh nhiệm vụ ngẫu nhiên theo trọng số chất lượng
  - Giới hạn 10 nhiệm vụ/ngày
  - Thời gian thực hiện 2 giờ
  - Sự kiện tấn công ngẫu nhiên (10%)
  - Bonus khi hoàn thành hoàn hảo
  - 3 lượt refresh miễn phí/ngày
  - Theo dõi thành tích

---

### 🟡 world-service
- **Port**: `8370`
- **Database**: `game_world`
- **Mô tả**: Quản lý sự kiện thế giới và boss thế giới
- **Chức năng**:
  - World Boss management (WorldBoss)
  - World event system (WorldEvent)
  - World state tracking
  - Scene management (SceneManagementService)

---

### 🟡 chat-service
- **Port**: `8460`
- **Database**: `chat_db`
- **Mô tả**: Hệ thống chat toàn diện
- **Chức năng**:
  - Chat thế giới (World chat)
  - Chat bang hội (Guild chat)
  - Chat nhóm (Team chat)
  - Chat riêng tư (Private 1-1)
  - Thông báo hệ thống
  - Lịch sử chat
  - Mute/unmute người chơi
  - Tự động xóa tin nhắn cũ

| Kênh | ID | Mô tả |
|------|----|-------|
| World | 1 | Chat toàn server |
| Guild | 2 | Chat nội bộ bang hội |
| Team | 3 | Chat nhóm |
| Private | 4 | Tin nhắn riêng |
| System | 5 | Thông báo hệ thống |

---

### 🟡 guild-service
- **Port**: `8440`
- **Database**: `guild_db`
- **Mô tả**: Hệ thống bang hội (Guild/Clan)
- **Chức năng**:
  - CRUD bang hội, tối đa 50 thành viên, 3 cấp bậc
  - Hệ thống đơn xin vào bang
  - Nâng cấp kỹ thuật bang (5 nhánh: ATK/DEF/HP/CRT/SPD)
  - Kho bang hội (100 slots)
  - Hệ thống cống hiến hàng ngày
  - Chuyển giao thủ lĩnh
  - Xếp hạng bang hội

---

## 📦 PHASE P3 — Enhancement & Support

### 🔴 friend-service
- **Port**: `8450`
- **Database**: `friend_db`
- **Mô tả**: Hệ thống bạn bè
- **Chức năng**:
  - Danh sách bạn bè (tối đa 100)
  - Gửi/chấp nhận/từ chối lời mời kết bạn
  - Block/unblock (tối đa 50)
  - Theo dõi trạng thái online
  - Friendship level 1-5
  - Tặng quà cho bạn bè
  - Tìm kiếm người chơi

---

### 🔴 mail-service
- **Port**: `8470`
- **Database**: `mail_db`
- **Mô tả**: Hệ thống thư trong game
- **Chức năng**:
  - System mail (thư hệ thống)
  - Player-to-player mail
  - Reward mail với đính kèm (items, gold, gems, exp)
  - Notice mail
  - Bulk mail gửi hàng loạt
  - Tự động hết hạn sau 7 ngày
  - Trạng thái đọc/chưa đọc
  - Nhận phần thưởng đính kèm

---

### 🔴 leaderboard-service
- **Port**: `8480` | **gRPC**: `9088`
- **Database**: `leaderboard_db`
- **Mô tả**: Hệ thống bảng xếp hạng
- **Chức năng**:
  - 8 loại xếp hạng (Power, Level, Arena, Wealth, Guild, Pet, Mount, PVP)
  - Top 100 người chơi mỗi loại
  - Cập nhật rank real-time
  - Theo dõi thay đổi hạng (tăng/giảm)
  - Redis caching (TTL 5 phút)
  - Auto-refresh mỗi 5 phút
  - Tra cứu rank cá nhân

| Loại | ID | Tiêu chí |
|------|----|----|
| Power | 1 | Tổng chiến lực |
| Level | 2 | Level + EXP |
| Arena | 3 | Điểm đấu trường |
| Wealth | 4 | Tổng tài sản |
| Guild | 5 | Điểm bang hội |
| Pet | 6 | Chiến lực thú cưng |
| Mount | 7 | Chiến lực ngựa |
| PVP | 8 | Điểm PVP |

---

### 🔴 pet-service
- **Port**: `8112`
- **Database**: `game_pet`
- **Mô tả**: Hệ thống thú cưng (collection, evolution, equipment)
- **Chức năng**:
  - Thu thập, tăng cấp, tiến hóa, thải bỏ thú cưng
  - Hệ thống thức tỉnh (awakening/grade up)
  - Skill system (học skill, mở khóa slot, khóa slot)
  - Normal gem (4 slots/pet)
  - Special gem (2 slots/pet, random attributes)
  - Trang phục thú cưng (PetCloth)
  - Tàn tích thú cưng (PetRemains)
  - Quản lý 2 slot thú cưng tham chiến (PetFightIndex)
  - 20 loại thao tác
- **Message IDs**: 2100-2139

---

### 🔴 mount-service
- **Port**: `8089`
- **Database**: `game_mount`
- **Mô tả**: Hệ thống ngựa/tọa kỵ
- **Chức năng**:
  - Mở khóa, tăng cấp, nâng phẩm ngựa
  - Cưỡi/tháo ngựa
  - Hệ thống ngoại hình và skin
  - Ngựa thám hiểm (exploration rewards)
  - Harness system (trang bị ngựa với random attributes)
  - Mặc/tháo/phân giải/refresh harness
  - Nâng sao

---

### 🔴 rune-service
- **Port**: `8093` | **gRPC**: `9093`
- **Database**: `game_rune`
- **Mô tả**: Hệ thống ngọc rune tăng sức mạnh trang bị
- **Chức năng**:
  - Tạo rune với random attributes theo phẩm chất
  - 5 cấp phẩm chất (Trắng → Cam)
  - Nhiều hướng nâng cấp: Level, Quality, Star, Refinement
  - 1 main attr + tối đa 3 sub attrs (theo phẩm)
  - Gắn rune vào slots trang bị
  - Reroll sub attributes

---

### 🔴 item-service
- **Port**: `8220`
- **Database**: — (Stateless)
- **Mô tả**: Quản lý metadata của tất cả items trong game
- **Chức năng**:
  - CRUD item definitions (không phải inventory)
  - Item type, attributes, stack size, v.v.
  - Phục vụ cho bag-service, shop-service, drop-service tra cứu item info

---

### 🔴 angel-service
- **Port**: `8090` | **gRPC**: `9090`
- **Database**: `game_angel`
- **Mô tả**: Hệ thống thiên thần / cánh (Angel/Wing companion)
- **Chức năng**:
  - Mở khóa, tăng cấp, nâng phẩm thiên thần
  - Trang bị/hiển thị thiên thần
  - Hệ thống 4 skill slots với nâng cấp
  - Nâng sao
  - Tiến hóa (breakthrough stages)
  - Tích điểm phước lành (Blessing)
  - Ngoại hình tùy chỉnh (skins)
  - Tính chiến lực (combat power)

---

### 🔴 artifact-service
- **Port**: `8091` | **gRPC**: `9087`
- **Database**: `game_artifact`
- **Mô tả**: Hệ thống thần khí (Divine Artifact/Legendary Weapon — 神器)
- **Chức năng**:
  - Mở khóa, tăng cấp, nâng phẩm thần khí
  - Tinh luyện (Refine — 精炼)
  - Thức tỉnh (Awaken — 觉醒)
  - Tích lũy hồn lực (Soul Power — 魂力)
  - Thần tính tinh华 (Divine Essence — 神性精华)
  - Hệ thống phước lành tier-based (祝福)
  - 4 attribute slots với refresh/lock

---

### 🔴 analytics-service
- **Port**: `8510`
- **Database**: `game_analytics`
- **Mô tả**: Thu thập và phân tích dữ liệu gameplay
- **Chức năng**:
  - Event tracking (player actions, purchases, v.v.)
  - Analytics reports
  - Data aggregation

---

### 🔴 notification-service
- **Port**: `8520`
- **Database**: `game_notification`
- **Mô tả**: Hệ thống thông báo push/in-app
- **Chức năng**:
  - Push notifications
  - In-app notification management
  - Trạng thái đọc/chưa đọc

---

### 🔴 moderation-service
- **Port**: `8570`
- **Database**: `game_moderation`
- **Mô tả**: Quản lý nội dung và kiểm duyệt
- **Chức năng**:
  - Lọc nội dung không phù hợp (tên, chat)
  - Player reports
  - Chặn/unblock players

---

### 🔴 file-service
- **Port**: `8540`
- **Database**: — (Stateless)
- **Mô tả**: Quản lý và phục vụ file tài nguyên
- **Chức năng**:
  - Upload/download game assets
  - Static resource serving
  - CDN integration support

---

### 🔴 scheduler-service
- **Port**: `8550`
- **Database**: — (Redis db:5)
- **Mô tả**: Dịch vụ lên lịch tác vụ định kỳ (cron jobs)
- **Chức năng**:
  - Daily reset jobs (DailyResetJob)
  - Weekly reset jobs (WeeklyResetJob)
  - Gọi các service reset: gift, guild, leaderboard, shop, task
- **Dependencies**: gift-service, guild-service, leaderboard-service, shop-service, task-service (Feign)

---

### 🔴 localization-service
- **Port**: `8560` | **gRPC**: `9560`
- **Database**: — (Redis db:6)
- **Mô tả**: Đa ngôn ngữ và bản địa hóa (i18n/l10n)
- **Chức năng**:
  - Cung cấp bản dịch cho client theo ngôn ngữ
  - Quản lý string bundles
  - Hỗ trợ nhiều ngôn ngữ

---

## 📦 PHASE P4 — Optional Features

### 🟣 main-fb-service
- **Port**: `8128` | **gRPC**: `9128`
- **Database**: `game_mainfb`
- **Mô tả**: Main Boss Fight — hệ thống đánh boss chính
- **Chức năng**:
  - Boss fight management
  - Stamina system (stamina-item-id: 50001)
  - gRPC server cho webSocket-server
  - MainFbController, MainFbHandler

---

### 🟣 anti-cheat-service
- **Port**: `8590`
- **Database**: `game_anticheat`
- **Mô tả**: Chống gian lận trong game
- **Chức năng**:
  - Phát hiện hành vi bất thường
  - Log và báo cáo suspicious actions
  - Integration với moderation-service

---

## 📦 PHASE P5 — New Gameplay Systems

> ⚠️ Tất cả services P5 cấu hình port trong `application-local.yml` / `application-prod.yml` (không phải `application.yml` chính)

### ⚫ lingzhu-service
- **Port**: `8380`* | **gRPC** (chưa config port)
- **Database**: `lingzhudb`
- **Mô tả**: Hệ thống Linh Châu (靈珠) — đá quý/ngọc tăng lực
- **Chức năng**: Thu thập và nâng cấp linh châu, buff stats

---

### ⚫ knights-service
- **Port**: `8310`*
- **Database**: `knightsdb`
- **Mô tả**: Hệ thống Hiệp Sĩ/Tướng (Knights system)
- **Chức năng**: Thu thập, nâng cấp, deploy hiệp sĩ chiến đấu

---

### ⚫ pagoda-service
- **Port**: `8320`* | **gRPC** (chưa config port)
- **Database**: `pagodadb`
- **Mô tả**: Hệ thống Tháp (Pagoda/Tower dungeons)
- **Chức năng**: Leo tháp nhiều tầng, nhận rewards

---

### ⚫ scroll-service
- **Port**: `8330`*
- **Database**: `scrolldb`
- **Mô tả**: Hệ thống Cuộn (Scroll/Special items)
- **Chức năng**: 
  - ScrollItem và ScrollMeta management
  - Đặc biệt/hiếm scroll items

---

### ⚫ gem-service
- **Port**: `8381`*
- **Database**: `gemdb`
- **Mô tả**: Hệ thống Đá Quý / Gem enhancement
- **Chức năng**: Thu thập, nâng cấp, gắn đá quý vào slot trang bị

---

### ⚫ activity-service
- **Port**: `8382`*
- **Database**: `activitydb`
- **Mô tả**: Hệ thống sự kiện game (Activities/Events)
- **Chức năng**:
  - Sự kiện khai server (OpenServerActivity)
  - Random activity events (RandActivity)
  - Handler từ webSocket-server

---

### ⚫ dataaccess-service
- **Port**: — (Deprecated)
- **Database**: —
- **Chú ý**: Service này đã bị deprecated/xóa (xem docs/DATAACCESS-SERVICE-REMOVED.md)

---

### ⚫ shizhuang-service
- **Port**: `8350`*
- **Database**: `game_shizhuang`
- **Mô tả**: Hệ thống Thời Trang / Trang Phục (时装 — Costume/Fashion)
- **Chức năng**:
  - Quản lý trang phục nhân vật (ShiZhuang)
  - Thiên thần (Angel skins) — PlayerAngelEntity
  - Trang phục mô hình (PlayerClothesEntity)
  - Hệ thống skin thiên thần
- **Dependencies**: bag-service, role-service, wallet-service, item-service (Feign)

---

## 📦 SPECIAL — Admin & Support

### 🟤 admin-service
- **Port**: `9091`
- **Database**: `game_admin`
- **Mô tả**: Cổng quản trị (Admin Panel)
- **Chức năng**:
  - Dashboard quản lý server
  - Quản lý người chơi
  - Xem thống kê tổng hợp
  - Cấu hình runtime

---

### 🟤 gm-service
- **Port**: `9093`
- **Database**: `game_gm`
- **Mô tả**: Game Master Tool — Công cụ GM quản lý người chơi
- **Chức năng**:
  - **Item Management**: Tặng/xóa items của người chơi, xem inventory
  - **Currency Management**: Thêm/trừ gold/diamond/coin, xem wallet
  - **VIP Management**: Cập nhật VIP level
  - **User Management**: Ban/unban user (tạm thời hoặc vĩnh viễn), xem thông tin
  - **Broadcast**: Gửi thông báo hệ thống
  - **Audit Logging**: Ghi log tất cả hành động GM, xem lịch sử

---

## 📌 Bảng Tổng Hợp Port

> `*` = Port cấu hình trong profile `application-local.yml` / `application-prod.yml`  
> `(impl)` = có gRPC implementation nhưng `grpc.server.port` chưa cấu hình trong yml

| Service | Port | gRPC Port | Phase | Database |
|---------|------|-----------|-------|----------|
| eureka-server | 8761 | — | P0 | — |
| config-service | 8888 | — | P0 | — |
| gateway-service | 8080 | — | P0 | — |
| webSocket-server | 8094 | — (client only) | P0 | — |
| user-service | 8110 | — | P1 | user_db |
| session-service | 8096 | — | P1 | — (Redis) |
| serverInfo-service | 8095 | — | P1 | serverinfo_db |
| role-service | 8410 | 9410 | P1 | db_role |
| wallet-service | 8210 | — | P1 | wallet_db |
| bag-service | 8230 | 9230 | P1 | bag_db |
| equip-service | 8240 | 9240 | P1 | equip_db |
| drop-service | 8250 | — | P1 | — (Stateless) |
| shop-service | 8260 | 9260 | P1 | shop_db |
| gift-service | 8270 | — | P1 | — (Stateless) |
| box-service | 8290 | — | P1 | box_db |
| crafting-service | 8280 | 9280 | P1 | crafting_db |
| report-service | 8098 | — | P1 | report_db |
| iap-verify-service | 8580 | — | P1 | iap_verify_db |
| arena-service | 8084 | — | P2 | game_arena |
| trial-service | 8300 | 9300 | P2 | game_trial |
| task-service | 8097 | — | P2 | game_task |
| battleserver-service | 8082 | 9082 | P2 | db_battle_service |
| globalserver-service | 8100 | — | P2 | globalserver_service_db |
| gameworld-service | 8105 | 9105 | P2 | gameworld_db |
| starmap-service | 8092 | 9092 | P2 | game_starmap |
| territory-service | 8360 | 9086 | P2 | game_territory |
| escort-service | 8340 | — | P2 | game_escort |
| world-service | 8370 | — | P2 | game_world |
| chat-service | 8460 | — | P2 | chat_db |
| guild-service | 8440 | — | P2 | guild_db |
| friend-service | 8450 | — | P3 | friend_db |
| mail-service | 8470 | — | P3 | mail_db |
| leaderboard-service | 8480 | 9088 | P3 | leaderboard_db |
| pet-service | 8112 | — | P3 | game_pet |
| mount-service | 8089 | — | P3 | game_mount |
| rune-service | 8093 | 9093 | P3 | game_rune |
| item-service | 8220 | — | P3 | — (Stateless) |
| angel-service | 8090 | 9090 | P3 | game_angel |
| artifact-service | 8091 | 9087 | P3 | game_artifact |
| analytics-service | 8510 | — | P3 | game_analytics |
| notification-service | 8520 | — | P3 | game_notification |
| moderation-service | 8570 | — | P3 | game_moderation |
| file-service | 8540 | — | P3 | — (Stateless) |
| scheduler-service | 8550 | — | P3 | — (Redis db:5) |
| localization-service | 8560 | 9560 | P3 | — (Redis db:6) |
| main-fb-service | 8128 | 9128 | P4 | game_mainfb |
| anti-cheat-service | 8590 | — | P4 | game_anticheat |
| lingzhu-service | 8380* | — | P5 | lingzhudb |
| knights-service | 8310* | — | P5 | knightsdb |
| pagoda-service | 8320* | — | P5 | pagodadb |
| scroll-service | 8330* | — | P5 | scrolldb |
| gem-service | 8381* | — | P5 | gemdb |
| activity-service | 8382* | — | P5 | activitydb |
| shizhuang-service | 8350* | — | P5 | game_shizhuang |
| dataaccess-service | — (deprecated) | — | P5 | — |
| admin-service | 9091 | — | Special | game_admin |
| gm-service | 9093 | — | Special | game_gm |

---

## 🔗 Service Dependencies (Feign / gRPC)

```
webSocket-server ──► (Feign) ──► role, bag, wallet, shop, equip, arena, trial,
                                  task, mail, friend, guild, chat, leaderboard,
                                  pet, mount, rune, angel, artifact, item,
                                  escort, starmap, territory, world, crafting,
                                  gem, lingzhu, knights, pagoda, scroll,
                                  shizhuang, activity, analytics, notification,
                                  localization, gm, main-fb
                 ──► (gRPC) ──► role(9410), bag(9230), equip(9240), crafting(9280),
                                  trial(9300), starmap(9092), territory(9086),
                                  angel(9090), artifact(9087), rune(9093),
                                  leaderboard(9088), gameworld(9105),
                                  main-fb(9128), localization(9560),
                                  battleserver(9082), shop(9260)

session-service  ──► (Feign) ──► user-service
shop-service     ──► (Feign) ──► bag, wallet, item, role, config
bag-service      ──► (Feign) ──► wallet-service
task-service     ──► (Feign) ──► bag, wallet, leaderboard
scheduler-service──► (Feign) ──► gift, guild, leaderboard, shop, task
shizhuang-service──► (Feign) ──► bag, role, wallet, item
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.5.3 |
| Cloud | Spring Cloud 2025.0.0 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Inter-service (REST) | OpenFeign |
| Inter-service (RPC) | gRPC (net.devh:grpc-spring-boot-starter 3.1.0) |
| Message Queue | Apache Kafka |
| Cache | Redis (Lettuce) |
| Database | MySQL 8.x |
| Migration | Flyway |
| Serialization | Protobuf 3.25.5 |
| Resilience | Resilience4j CircuitBreaker |
| Build | Maven 3.6+ |
| Container | Docker / Docker Compose |
| Orchestration | Kubernetes (k8s) |

---

## 🚀 Thứ Tự Khởi Động

```
1. Infrastructure:
   eureka-server (8761) → config-service (8888) → gateway-service (8080)
   
2. Core:
   user-service (8110) → session-service (8096) → role-service (8410)
   serverInfo-service (8095)
   
3. Economy:
   wallet-service (8210) → item-service (8220) → bag-service (8230)
   equip-service (8240) → drop-service (8250) → shop-service (8260)
   
4. Communication:
   webSocket-server (8094)  ← cần tất cả services đã sẵn sàng
   
5. Business Services (thứ tự tùy ý):
   Tất cả P2, P3, P4, P5 services
```

---

## 📝 Ghi Chú

- Services P5 có port cấu hình trong `application-local.yml` / `application-prod.yml`, đánh dấu `*` trong bảng
- **bag-service** (9230), **shop-service** (9260), **battleserver-service** (9082): gRPC đã được fix — thêm `@GrpcService`, `grpc-spring-boot-starter`, `grpc.server.port`, cập nhật webSocket-server grpc.client config
- **localization-service** (8560): sử dụng Redis db:6 (không có MySQL), có gRPC server port 9560
- **scheduler-service** (8550): sử dụng Redis db:5 (không có MySQL)
- **angel-service** (8090) và **webSocket-server** (8094) ở gần nhau — không nhầm
- **artifact-service** (8091) và **config-service** (8888) — config đã đổi từ 8091 sang 8888
- **dataaccess-service** đã bị deprecated (xem docs/DATAACCESS-SERVICE-REMOVED.md)
- **common** folder: tài liệu/archive cũ (không phải module Maven)
- **common-lib**: module Maven duy nhất được share, chứa Protobuf stubs và DTOs

---

*Tài liệu này được cập nhật thủ công từ codebase — 2026-03-16*

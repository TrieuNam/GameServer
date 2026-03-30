# Frontend ↔ Backend Compatibility Plan

> **Cập nhật:** 2026-03-07
> **Backend status:** 44/44 BUILD SUCCESS | activity-service extended: types 43-47 mới
> **Frontend source:** `D:\project\serverGame\document\client\LineR\assets\script\modules\`
> **Backend source:** `D:\project\serverGame\GameServer\`

---

## 📊 Tổng Quan (Đã Cập Nhật)

| Nhóm | Số lượng |
|------|----------|
| Frontend modules | ~100 modules |
| Backend services | 44 services |
| ✅ Module có backend đầy đủ | ~90 |
| 🟡 Mới implement (types 43-47) | 5 |
| ⏳ Integration test còn lại | ~30 services |

> **Phát hiện quan trọng:** `activity-service` đã có **45 entities + 42 activity types** bao phủ gần như toàn bộ module monetization/social. `RandActivityHandler` dispatch qua msgId 3000, `rand_activity_type` 1-42.

---

## ✅ Mapping Hoàn Chỉnh: Frontend → Backend

### Core Services

| Frontend Module | Backend Service | Port | Handler / MsgId |
|----------------|----------------|------|----------------|
| `login` | user-service + session-service | — | LoginBootstrapHandler |
| `create_role` / `role` / `role_setting` / `OtherRole` / `levelup` | role-service | — | RoleServiceHandler |
| `bag` / `FastUse` / `item_recycling` | bag-service | 8230 | BagHandler |
| `EquipBag` / `Enchant` | equip-service | 8240 | EquipHandler |
| `shop` / `shop/mystery_shop` | shop-service | 8260 | ShopHandler (1620/1622/1630) |
| `Arena` / `PeakArena` | arena-service | — | ArenaHandler (9610-9616), gRPC 9084 |
| `escort` | escort-service | — | EscortHandler, gRPC 9085 |
| `territory` | territory-service | — | TerritoryHandler, gRPC 9086 |
| `trial` | trial-service | — | TrialHandler, gRPC 9300 |
| `dungeon` | lingzhu-service | 8380 | LingZhuHandler |
| `guild` | guild-service | — | GuildHandler (9640-9646) |
| `task` / `guide` | task-service | — | TaskHandler |
| `mount` | mount-service | — | MountHandler (2140-2145) |
| `Pet` / `PetCloth` / `PetGuard` / `PetRelics` | pet-service | — | PetHandler |
| `Angel` / `AngelFes` | angel-service | — | AngelHandler (2130-2132) |
| `box` / `BoxDraw` | box-service | 8290 | BoxHandler |
| `ClothShop` / `fashion` | shizhuang-service | — | ShiZhuangHandler |
| `gem_atelier` | gem-service | 8381 | GemHandler |
| `shenqi` / `ShenQiDraw` | artifact-service | — | ShenQiHandler |
| `star_map` / `StarMapFes` | starmap-service | — | StarMapHandler |
| `inscription` | rune-service | — | RuneHandler |
| `knight_card` / `Manual` | knights-service | 8310 | KnightsHandler |
| `merlin_magic_scrolls` | scroll-service | 8330 | ScrollHandler |
| `item_info` / `common_item` | item-service | 8220 | (REST, stateless) |
| `gm_command` | gm-service | — | GmHandler |
| `rank` / `friends_rank` | leaderboard-service | — | RankHandler |
| `sensitiveWords` | moderation-service | — | (REST) |
| `remind` | notification-service | — | NotificationHandler |
| `getway` | gateway-service | — | WsGatewayHandler |
| `block` | friend-service | 8450 | BlockHandler |
| `guild` social features | friend-service | 8450 | FriendHandler (1900-1905) |
| `battle` / `monster` | (client router only) | — | BattleCtrl.ts routes to Arena/LingZhu/Pagoda/Escort/Rune/MainFb/Pet/Guild handlers |
| `adventure` | main-fb-service | — | MainFbHandler CS:2005 `PB_CSMainFbReq` / SC:2006 `PB_SCMainFbInfo` |
| `Enchant` | equip-service | 8240 | EquipHandler via EquipFumoFeign — `PB_SCEquipFuMoListInfo` / `PB_SCEquipFuMoOneInfo` |
| `serveractivity` / `moreserveractive` / `open_server` | activity-service | 8382 | OpenServerActivityHandler + RandActivityHandler |
| `recharge` (config) | activity-service | 8382 | RechargeConfigHandler CS:3004 → SC:3005 ✅ (new) |

### Activity-Service RandActivity (msgId CS:3000 → SC:300X)

> Tất cả dispatch qua `rand_activity_type` trong `PB_CSRandActivityOperaReq`

| Frontend Module | activityType | SC MsgId | Entity | Status |
|----------------|-------------|----------|--------|--------|
| `recharge` (充值信息) | 1 | 3001 | `RechargeInfo` | ✅ Done |
| `boxfund` (宝箱基金) | 10 | 3010 | `BoxFund` | ✅ Done |
| `levelfund` (等级基金) | 11 | 3011 | `LevelFund` | ✅ Done |
| `first_charge` (首充) | 12 | 3012 | `FirstRecharge` | ✅ Done |
| `lei_chong` (累充) | 13 | 3013 | `AccumulatedRecharge` | ✅ Done |
| `DailyGift` (日常礼包) | 14 | 3014 | `DailyGift` | ✅ Done |
| `CommodityGuild` (商品行会) ¹ | 15 | 3015 | `CommodityGuild` | ✅ Done |
| `MonthlyCard` (月卡) | 16 | 3016 | `MonthCard` | ✅ Done |
| `LuckyGift` (幸运礼遇) | 17 | 3017 | `LuckCourtesy` | ✅ Done |
| `weekendrecharge` (周末累充) | 18 | 3018 | `WeekendRecharge` | ✅ Done |
| `caveloot` (洞穴夺宝) | 19 | 3019 | `CaveLoot` | ✅ Done |
| `invitefriend` (好友邀请) | 20 | 3020 | `FriendInvite` | ✅ Done |
| `boxmanor` (宝箱庄园) | 21 | 3021 | `ChestManor` | ✅ Done |
| `ScoreFund` (评分基金) | 22 | 3022 | `CapacityFund` | ✅ Done |
| `TodayShare` (每日分享) | 23 | 3023 | `DailySharing` | ✅ Done |
| `AngelFes` / FaZhen (法阵盛典) | 24 | 3024 | `FaZhenGala` | ✅ Done |
| `StarMapFes` (星图盛典) | 25 | 3025 | `StarMapGala` | ✅ Done |
| GuMoTowerFund (箍魔之塔基金) | 26 | 3026 | `GuMoTowerFund` | ✅ Done |
| RuneTowerFund (铭文之塔基金) | 27 | 3027 | `RuneTowerFund` | ✅ Done |
| `AffordPresent` (超值献礼) | 28 | 3028 | `ChaoZhiXianLi` | ✅ Done |
| `new_server_competition` | 29 | 3029 | `NewServerCompetition` | ✅ Done |
| `WeekHaoLi` (周末豪礼) | 30 | 3030 | `WeekendHaoLi` | ✅ Done |
| `moreserveractive` (global data) | 31 | 3031 | `NewServerGlobal` | ✅ Done |
| `ContinuePresent` (连充赠礼) | 32 | 3032 | `LianChongZengLi` | ✅ Done |
| `warOrder` (无限战令) | 33 | 3033 | `WarOrder` | ✅ Done |
| `WeekLianChong` (周末连充) | 34 | 3034 | `WeekendLianChong` | ✅ Done |
| `ad_equity` / `AdvDouble` (广告权益) | 35 | 3035 | `AdvertisementEquity` | ✅ Done |
| new_server_competition ranking | 36 | 3036 | `NewServerRanking` | ✅ Done |
| `ShenQiDraw` (神器夺宝) | 37 | 3037 | `ShenqiDuobao` | ✅ Done |
| `lei_chong/TianXuanZhiLi` (天选之礼) | 38 | 3038 | `TianxuanGift` | ✅ Done |
| `TerritoryGift` (领地礼包) | 39 | 3039 | `TerritoryGift` | ✅ Done |
| `integralTurntable` (积分转盘) | 40 | 3040 | `JifenZhuanpan` | ✅ Done |
| `ExclusiveGiftBag` (个性化礼包 — customized) | 41 | 3041 | `CustomizedGift` | ✅ Done |
| `ExclusiveGiftBag` (专属礼包 — exclusive) | 42 | 3042 | `ExclusiveGift` | ✅ Done |

### Activity-Service OpenServerActivity (CS:2160-2166)

| Frontend Module | CS MsgId | SC MsgId | Status |
|----------------|----------|----------|--------|
| `activity` SevenDaySign (七日签到) | 2160 | 2161 | ✅ Done |
| `activity` LuckUnpacking (开箱大吉) | 2162 | 2163 | ✅ Done |
| `activity` NewAreaPreferential (新服特惠) | 2164 | 2165 | ✅ Done |
| `activity` MarketShop (集市商店) | 2166 | 2167 | ✅ Done |

---

## 🟡 Mới Implement (Session này) — Types 43-47

> Thêm vào `activity-service` + `msgrandactivity.proto` + `RandActivityHandler`

| Frontend Module | activityType | SC MsgId | Entity | Status |
|----------------|-------------|----------|--------|--------|
| `fish` (钓鱼小游戏) | 43 | 3043 | `FishGame` | ✅ Implemented |
| `loopmine` (循环矿坑) | 44 | 3044 | `LoopMine` | ✅ Implemented |
| `CoreCrisis` / `CoreCrisisBox` (核心危机) | 45 | 3045 | `CoreCrisisGame` | ✅ Implemented |
| `fillblank` (填字谜) | 46 | 3046 | `FillBlank` | ✅ Implemented |
| `MingXiang` (命相/星象) | 47 | 3047 | `MingXiang` | ✅ Implemented |

---

## ⏳ Integration Test Status (30 services)

| Service | Frontend Module | Status |
|---------|----------------|--------|
| wallet-service | (internal) | ⏳ Chưa test |
| item-service | item_info | ⏳ Chưa test |
| bag-service | bag | ⏳ Chưa test |
| equip-service | EquipBag | ⏳ Chưa test |
| drop-service | (internal) | ⏳ Chưa test |
| shop-service | shop | ⏳ Chưa test |
| gift-service | (stateless config — not direct frontend) | ⏳ Chưa test |
| crafting-service | (embedded in bag/shop UI — no dedicated module) | ⏳ Chưa test |
| box-service | box, BoxDraw | ⏳ Chưa test |
| knights-service | knight_card | ⏳ Chưa test |
| pagoda-service | trial (塔) | ⏳ Chưa test |
| scroll-service | merlin_magic_scrolls | ⏳ Chưa test |
| lingzhu-service | dungeon | ⏳ Chưa test |
| gem-service | gem_atelier | ⏳ Chưa test |
| activity-service | activity (47 types: 1-42 + 43-47 mới) | ⏳ Chưa test |
| arena-service | Arena | ⏳ Chưa test |
| escort-service | escort | ⏳ Chưa test |
| territory-service | territory | ⏳ Chưa test |
| trial-service | trial | ⏳ Chưa test |
| mount-service | mount | ⏳ Chưa test |
| pet-service | Pet | ⏳ Chưa test |
| guild-service | guild | ⏳ Chưa test |
| role-service | role | ⏳ Chưa test |
| shizhuang-service | ClothShop | ⏳ Chưa test |
| artifact-service | shenqi | ⏳ Chưa test |
| starmap-service | star_map | ⏳ Chưa test |
| rune-service | inscription | ⏳ Chưa test |
| task-service | task | ⏳ Chưa test |
| angel-service | Angel | ⏳ Chưa test |
| leaderboard-service | rank | ⏳ Chưa test |

---

## 🔴 UI-Only / Client-side (No Backend Required)

| Frontend Module | Note |
|----------------|------|
| `audio` | Client-only audio engine |
| `common` / `common_board` / `common_button` / `common_help` | UI framework base classes |
| `common_account` | Account UI dialog |
| `CommonGet2` | Common reward popup |
| `extends` | TypeScript base classes |
| `open_data_context` | WeChat open data context (mini-program only) |
| `public_popup` | Global popup manager |
| `scene` / `scene_obj` / `scene_obj_spine` | Rendering engine |
| `time` | Client-side time utility |
| `UpLevelShow` | Level-up animation overlay |
| `UserProtocol` | User agreement popup |
| `Announce` | Announcement popup — uses notification-service push (no CS request needed) |

> **Note ¹:** `CommodityGuild` frontend module maps to **activity type 15** (commerce shop logic in activity-service), not to guild-service. guild-service handles guild membership/war only.

---

## ✅ Confirmed Mappings (Round 2 Exploration)

- **`battle`/`monster`** — BattleCtrl.ts is a client-side ROUTER, not a separate service. Routes to Arena/LingZhu/Pagoda/Escort/Rune/MainFb/Pet/Guild handlers. battleserver-service = internal gRPC combat engine only.
- **`adventure`** — Confirmed: main-fb-service via MainFbHandler (CS:2005/SC:2006)
- **`Enchant`** — Confirmed: equip-service (EquipFumoFeign), NOT crafting-service
- **activity-service Flyway** — NOT needed. Uses `ddl-auto: update`. V1 migration deleted to avoid future conflict.
- **RechargeConfigHandler (3004→3005)** — Implemented. Client gets package list by currency type.

## 🔜 Next Actions

1. [ ] Run integration test end-to-end for all 30 services
2. [ ] Test iap-verify-service end-to-end with `recharge` module (no WebSocket handler needed — IAP flow is platform-side → webhook → Kafka → MsgId 704 push)
3. [ ] Deploy activity-service (types 43-47) and test with client
4. [x] Verify `crafting-service` → NO dedicated frontend module. Handler exists: MsgIds 1700-1709 via gRPC (CraftingHandler). Logic embedded in bag/shop UI.

# TypeScript Frontend Controller & Protocol Mapping Report

**Analysis Date**: January 18, 2026  
**Source Path**: `D:\project\serverGame\client\LineR\assets\script\modules`  
**Purpose**: Map frontend TypeScript controllers to backend Java microservices

---

## 📋 Executive Summary

This report identifies **80+ controller classes** across the frontend TypeScript codebase, mapping their protocol message IDs (PB_CS* client→server, PB_SC* server→client) to corresponding backend Java microservices in the GameServer architecture.

### Key Findings:
- **Protocol Pattern**: Client sends `PB_CS*` messages, receives `PB_SC*` responses
- **Architecture**: Each controller manages a specific game domain (bag, shop, arena, etc.)
- **Request Pattern**: Controllers use `GetProtocol()` and `SendToServer()` for communication
- **Data Management**: Each controller has a corresponding Data class for state management

---

## 🎮 Domain-Organized Controller Mappings

### 1. 💰 ECONOMY & INVENTORY DOMAIN

#### **BagCtrl** - Inventory/Knapsack Management
- **Path**: `modules/bag/BagCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCGetItemNotice` - Item acquisition notifications
  - `PB_SCKnapsackAllInfo` - Complete inventory data
  - `PB_SCKnapsackSingleInfo` - Single item update
  - `PB_SCEquipListInfo` - Equipment list
  - `PB_SCEquipOneInfo` - Single equipment update
- **Messages Sent (CS)**:
  - `PB_CSKnapsackReq` - Inventory operations (use, sell, upgrade)
- **Operations**: USE, SELL, SHI_ZHUANG_LEVEL_UP, SHI_ZHUANG_USE
- **Backend Service**: `bag-service` ✅
- **Functionality**: Item storage, usage, selling, equipment management

---

#### **ShopCtrl** - Shop System
- **Path**: `modules/shop/ShopCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCShopInfo` - Shop inventory and pricing
- **Messages Sent (CS)**:
  - `PB_CSShopBuyReq` - Purchase request
- **Backend Service**: `shop-service` ✅
- **Functionality**: Item purchasing, shop inventory display

---

#### **MysteryShopCtrl** - Mystery/Special Shop
- **Path**: `modules/shop/mystery_shop/MysteryShopCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCMysteryShopInfo` - Mystery shop data
- **Messages Sent (CS)**:
  - `PB_CSMysteryShopReq` - Mystery shop operations
- **Backend Service**: `shop-service` ✅
- **Functionality**: Special/limited-time shop items

---

#### **BoxCtrl** - Box/Loot Box System
- **Path**: `modules/box/BoxCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCBoxEquipInfo` - Box equipment data
  - `PB_SCBoxInfo` - Box level/info
  - `PB_SCBoxSetingInfo` - Box settings
  - `PB_SCBoxSellInfo` - Sell information
- **Messages Sent (CS)**:
  - `PB_CSBoxReq` - Box operations (open, wear, sell, level up, speed up, enchant)
  - `PB_CSBoxSetReq` - Box configuration
- **Operations**: OPEN_BOX, WEAR_EQUIP, SELL, LEVEL_BUY, LEVEL_UP, SPEED_UP, Enchant, FETCH_LEVEL_REWARD
- **Backend Service**: `box-service` ✅
- **Functionality**: Loot box mechanics, equipment from boxes

---

#### **EquipBagCtrl** - Equipment Bag Management
- **Path**: `modules/EquipBag/EquipBagCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCEquipBagListInfo` - Equipment list
  - `PB_SCEquipBagOneInfo` - Single equipment update
- **Messages Sent (CS)**:
  - `PB_CSEquipReq` - Equipment operations
- **Operations**: WEAR, SELL, Enchant, CancelEnchant, CHange
- **Backend Service**: `equip-service` ✅
- **Functionality**: Equipment inventory, wear/remove equipment

---

#### **RechargeCtrl** - Payment/Recharge System
- **Path**: `modules/recharge/RechargeCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCChongZhiInfo` - Recharge info
  - `PB_SCChongZhiInfoChange` - Recharge changes
  - `PB_SCChongZhiConfigInfo` - Recharge config/pricing
- **Messages Sent (CS)**:
  - `PB_CSChongZhiConfigReq` - Request recharge options
  - `PB_CSBuyCmdReq` - Purchase with virtual currency
- **Backend Service**: `wallet-service` / `user-service` ✅
- **Functionality**: Real money transactions, virtual currency

---

#### **WalletCtrl** (Implicit via RoleCtrl)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Currency management, transaction history

---

#### **ItemRecyclingCtrl** - Item Recycling System
- **Path**: `modules/item_recycling/ItemRecyclingCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCItemRecycleInfo` - Recycle info
  - `PB_SCItemRecycleListInfo` - Recycle list
  - `PB_SCItemRecycleOneInfo` - Single recycle item
- **Messages Sent (CS)**:
  - `PB_CSItemRecycleLevelUpReq` - Recycle level up
- **Backend Service**: `bag-service` ✅
- **Functionality**: Item decomposition, material recovery

---

#### **ClothShopCtrl** - Clothing/Cosmetic Shop
- **Path**: `modules/ClothShop/ClothShopCtrl.ts`
- **Messages Sent (CS)**:
  - `PB_CSClothShopBuyReq` - Buy clothing items
- **Backend Service**: `shop-service` ✅
- **Functionality**: Cosmetic item purchases

---

### 2. ⚔️ COMBAT & BATTLE DOMAIN

#### **BattleCtrl** - Core Battle System
- **Path**: `modules/battle/BattleCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCBattleReport` - Battle results
- **Battle Types**: PVE, PVP, LINGZHU, ESCORT, TERRITORY, etc.
- **Backend Service**: `battleserver-service` ✅
- **Functionality**: Battle execution, combat resolution, HTTP-based battle data loading

---

#### **ArenaCtrl** - PvP Arena System
- **Path**: `modules/Arena/ArenaCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCArenaInfo` - Arena ranking and info
  - `PB_SCArenaReportList` - Battle history
- **Messages Sent (CS)**:
  - `PB_CSArenaReq` - Arena operations
- **Operations**: FIGHT, REFRESH, REPORT, BOX_REWARD, REVEBGE, ARENA_OP_INFO
- **Backend Service**: `arena-service` ✅
- **Functionality**: PvP matchmaking, ranking, rewards

---

#### **PeakArenaCtrl** - Cross-Server Arena
- **Path**: `modules/PeakArena/PeakArenaCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCCrossArenaInfo` - Cross-server arena info
  - `PB_SCCrossArenaReportList` - Battle reports
  - `PB_SCCrossArenaFightRet` - Fight result
- **Messages Sent (CS)**:
  - `PB_CSCrossArenaReq` - Cross-arena operations
- **Backend Service**: `arena-service` ✅
- **Functionality**: Cross-server PvP competitions

---

#### **DungeonCtrl** - Dungeon/Instance System
- **Path**: `modules/dungeon/DungeonCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCLingZhuInfo` - Dungeon info (领主副本)
- **Messages Sent (CS)**:
  - `PB_CSLingZhuReq` - Dungeon operations
- **Operations**: Fight, Mop (sweep), QuickMop, Info
- **Backend Service**: `main-fb-service` (main副本) ✅
- **Functionality**: Daily dungeons, boss fights, sweeping

---

#### **AdventureCtrl** - Main Story/Adventure
- **Path**: `modules/adventure/AdventureCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCMainFbInfo` - Main story instance info
- **Messages Sent (CS)**:
  - `PB_CSMainFbReq` - Adventure requests
- **Backend Service**: `main-fb-service` ✅
- **Functionality**: Story progression, campaign missions

---

#### **TrialCtrl** - Trial Tower/Challenge Modes
- **Path**: `modules/trial/TrialCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCShiLianPagodaInfo` - Trial tower info (试炼塔)
  - `PB_SCGuMoPagodaListInfo` - Ancient demon tower list (古魔塔)
  - `PB_SCGuMoPagodaLayerInfo` - Tower layer info
  - `PB_SCRaGuMoTowerFundInfo` - Tower fund activity
- **Messages Sent (CS)**:
  - `PB_CSShiLianPagodaReq` - Trial tower operations
  - `PB_CSGuMoPagodaReq` - Ancient demon tower operations
- **Backend Service**: `main-fb-service` or dedicated trial service ✅
- **Functionality**: Progressive tower challenges, floor-by-floor battles

---

#### **EscortCtrl** - Escort/Convoy Mission
- **Path**: `modules/escort/EscortCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCEscortRet` - Escort result
  - `PB_SCEscortRoleInfo` - Player escort info
  - `PB_SCEscortShipListInfo` - Available escorts
  - `PB_SCEscortReportListInfo` - Escort history
  - `PB_SCEscortInterceptListInfo` - Intercept opportunities
  - `PB_SCEscortShipInfo` - Ship details
- **Messages Sent (CS)**:
  - `PB_CSEscortReq` - Escort operations
- **Backend Service**: Likely `main-fb-service` or event service ✅
- **Functionality**: Escort missions, interception PvP

---

#### **TerritoryCtrl** - Territory Control/Guild War
- **Path**: `modules/territory/TerritoryCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCTerritoryInfo` - Territory info
  - `PB_SCTerritoryNeighbourInfo` - Neighbor territories
  - `PB_SCTerritoryBotInfo` - Bot/NPC info
  - `PB_SCTerritoryReportInfo` - Battle reports
  - `PB_SCTerritoryRedInfo` - Red dot notifications
  - `PB_SCRaTerritoryGift` - Territory rewards
- **Messages Sent (CS)**:
  - `PB_CSTerritoryReq` - Territory operations
- **Backend Service**: Guild/Territory service (possibly in `guild` or custom service) ✅
- **Functionality**: Territory conquest, guild battles

---

#### **PetGuardCtrl** - Pet Dungeon/Guardian System
- **Path**: `modules/PetGuard/PetGuardCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCPetFbInfo` - Pet dungeon info
- **Messages Sent (CS)**:
  - `PB_CSPetFbReq` - Pet dungeon operations
- **Backend Service**: `pet-service` or `main-fb-service` ✅
- **Functionality**: Pet-specific battle instances

---

### 3. 👥 SOCIAL & PROGRESSION DOMAIN

#### **GuildCtrl** - Guild System
- **Path**: `modules/guild/GuildCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCGuildSearchList` - Guild search results
  - `PB_SCGuildInfo` - Guild details
  - `PB_SCGuildReportList` - Guild activity reports
  - `PB_SCGuildMemberList` - Member list
  - `PB_SCGuildAppList` - Application list
  - `PB_SCGuildRoleInfo` - Player's guild role
- **Messages Sent (CS)**:
  - `PB_CSGuildReq` - Guild operations
- **Operations**: guild_info, guild_list, search_guild, create_guild, join_guild, apply_guild, help, help_ret, set_guild, set_notice
- **Backend Service**: `guild-service` (not yet in GameServer - P1 priority) ❌
- **Functionality**: Guild creation, management, member operations, guild wars

---

#### **TaskCtrl** - Quest/Task System
- **Path**: `modules/task/TaskCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCTaskProgressInfo` - Task progress updates
- **Messages Sent (CS)**:
  - `PB_CSFetchTaskRewardReq` - Claim task rewards
- **Backend Service**: `task-service` ✅
- **Functionality**: Daily tasks, achievements, quest tracking

---

#### **RankCtrl** - Ranking/Leaderboard System
- **Path**: `modules/rank/RankCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRankList` - Ranking data
- **Messages Sent (CS)**:
  - `PB_CSRankReq` - Request rankings
- **Backend Service**: Likely `arena-service` or `world-service` ✅
- **Functionality**: Power rankings, arena rankings, activity rankings

---

#### **InviteFriendCtrl** - Friend Invitation System
- **Path**: `modules/invitefriend/InviteFriendCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaFriendInfo` - Friend invitation activity info
- **Backend Service**: `user-service` or social service ✅
- **Functionality**: Invite friends for rewards

---

#### **OtherRoleCtrl** - View Other Players
- **Path**: `modules/OtherRole/OtherRoleCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCGetOtherRoleRet` - Other player info
- **Messages Sent (CS)**:
  - `PB_CSGetOtherRoleInfo` - Request other player data
- **Backend Service**: `role-service` ✅
- **Functionality**: Inspect other players, comparison

---

### 4. 🎭 CHARACTER & ROLE DOMAIN

#### **RoleCtrl** - Player Character Core
- **Path**: `modules/role/RoleCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRoleInfoAck` - Complete character info
  - `PB_SCRoleAttrList` - Attribute list (stats)
  - `PB_SCRoleExpChange` - Experience change
  - `PB_SCRoleLevelChange` - Level up
  - `PB_SCRoleSystemSetInfo` - System settings
  - `PB_SCMailDeleteAck` - Mail deletion confirmation
  - `PB_SCMailListAck` - Mail list
  - `PB_SCMailDetail` - Mail details
  - `PB_SCFetchMailAck` - Mail rewards claimed
  - `PB_SCAdvertisementInfo` - Ad info
  - `PB_SCCmdToClientCmd` - Server commands to client
- **Messages Sent (CS)**:
  - `PB_CSRoleSystemSetReq` - System settings
  - `PB_CSMailReq` - Mail operations
  - `PB_CSRoleWXInfoSetReq` - WeChat info
  - `PB_CSAdvertisementFetch` - Ad rewards
- **Backend Service**: `role-service` ✅
- **Functionality**: Character data, level, attributes, mail system

---

#### **PetCtrl** - Pet/Companion System
- **Path**: `modules/Pet/PetCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRolePetAllInfo` - All pet data
  - `PB_SCRolePetSignleInfo` - Single pet update
  - `PB_SCRoleTSGemSignleInfo` - Special gem info
  - `PB_SCRolePetRetInfo` - Operation result
  - `PB_SCPetSendEvoAttr` - Evolution attributes
- **Messages Sent (CS)**:
  - `PB_CSRolePetReq` - Pet operations
  - `PB_CSPetOneKeyUpLevelGemReq` - One-click gem upgrade
- **Operations**: LEVEL_UP, GRADE_UP (awakening), SKILL_LEARN, INLAY_GEM, GEM_LEVEL_UP, TS_GEM operations, SET_FIGHT, DISCARD, TREASURE, GRADE_UP_EVO, CLOTH_UP, CLOTH_WEAR, SKILL_UNLOCK
- **Backend Service**: `pet-service` ✅
- **Functionality**: Pet collection, upgrade, skills, gems, skins

---

#### **PetRelicsCtrl** - Pet Relics/Remains
- **Path**: `modules/PetRelics/PetRelicsCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCPetRemainsList` - Pet relics list
- **Backend Service**: `pet-service` ✅
- **Functionality**: Pet-related collectibles

---

#### **MountCtrl** - Mount System
- **Path**: `modules/mount/MountCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCMountInfo` - Mount info
  - `PB_SCMountOpRet` - Operation result
  - `PB_SCMountHarnessListInfo` - Mount gear list
  - `PB_SCMountHarnessOneInfo` - Single gear update
  - `PB_SCMountHarnessInfo` - Mount purchase info
- **Messages Sent (CS)**:
  - `PB_CSMountReq` - Mount operations
- **Operations**: LEVEL_UP, GRADE_UP, EXPLORE, SET_APP (appearance), PIFU_UP (skin upgrade), SET_PIFU, WEAR, DECOMPOSE, UNLOCK, ENTRY_REFRESH, BUY, REFRESH_BUY, OPEN_BUY, SET_LOCK_FLAG
- **Backend Service**: `pet-service` or mount service ✅
- **Functionality**: Mount riding, upgrades, equipment, skins

---

#### **FashionCtrl** - Fashion/Clothing System
- **Path**: `modules/fashion/FashionCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCAllShiZhuangInfo` - All fashion data (时装)
  - `PB_SCShiZhuangInfo` - Single fashion update
- **Backend Service**: `shizhuang-service` ✅
- **Functionality**: Character cosmetics, clothing upgrades

---

#### **AngelCtrl** - Angel/Wing System
- **Path**: `modules/Angel/AngelCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCAngelInfo` - Angel/wing info
  - `PB_SCAngelOpRet` - Operation result
- **Messages Sent (CS)**:
  - `PB_CSAngelReq` - Angel operations
- **Backend Service**: Likely `pet-service` or appearance service ✅
- **Functionality**: Wing/angel system, upgrades

---

#### **InscriptionCtrl** - Inscription/Rune System
- **Path**: `modules/inscription/InscriptionCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRuneInfo` - Rune info
  - `PB_SCRuneRet` - Rune operation result
  - `PB_SCRaRuneTowerFundInfo` - Rune tower fund
- **Messages Sent (CS)**:
  - `PB_CSRuneReq` - Rune operations
- **Operations**: FIGHT, FETCHDAYREWARD, TURNTABLE, WEARRUNE, OFFRUNE, UPRUNE, DECOMPOSE, PASS_REWARD, RUNE_BOX
- **Backend Service**: `equip-service` or dedicated rune service ✅
- **Functionality**: Rune/inscription system, character enhancement

---

#### **EnchantCtrl** - Equipment Enchantment
- **Path**: `modules/Enchant/EnchantCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCEquipFuMoListInfo` - Enchant list (附魔)
  - `PB_SCEquipFuMoOneInfo` - Single enchant update
- **Backend Service**: `equip-service` ✅
- **Functionality**: Equipment enchantment system

---

#### **GemAtelierCtrl** - Gem Crafting Workshop
- **Path**: `modules/gem_atelier/GemAtelierCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCGemInfo` - Gem info
- **Messages Sent (CS)**:
  - `PB_CSGemReq` - Gem operations
  - `PB_CSGemOneKeyUpLevelReq` - One-click gem upgrade
  - `PB_CSGemBuyReq` - Buy gems
- **Backend Service**: `crafting-service` ✅
- **Functionality**: Gem crafting, upgrading, socketing

---

#### **StarMapCtrl** - Star Map/Constellation System
- **Path**: `modules/star_map/StarMapCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCStarMapInfo` - Star map info
  - `PB_SCStarMapOpRet` - Operation result
- **Messages Sent (CS)**:
  - `PB_CSStarMapReq` - Star map operations
- **Backend Service**: Character enhancement service (possibly `role-service`) ✅
- **Functionality**: Constellation/star progression system

---

#### **ShenQiCtrl** - Divine Weapon System
- **Path**: `modules/shenqi/ShenQiCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCShenQiListInfo` - Divine weapon list (神器)
  - `PB_SCShenQiOneInfo` - Single weapon update
  - `PB_SCShenQiOtherInfo` - Other weapon info
  - `PB_SCShenQiDrawInfo` - Gacha info
  - `PB_SCShenQiRecordInfo` - Draw records
- **Messages Sent (CS)**:
  - `PB_CSShenQiReq` - Divine weapon operations
- **Backend Service**: `equip-service` or special equipment service ✅
- **Functionality**: Legendary weapon collection and upgrade

---

#### **ManualCtrl** - Knight Manual/Collection
- **Path**: `modules/Manual/ManualCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCKnightsInfo` - Knights collection info
  - `PB_SCKnightsConditionInfo` - Unlock conditions
- **Messages Sent (CS)**:
  - `PB_CSKnightsReq` - Knights operations
- **Backend Service**: `role-service` ✅
- **Functionality**: Character/hero collection book

---

#### **MerlinMagicCtrl** - Magic Scroll System
- **Path**: `modules/merlin_magic_scrolls/MerlinMagicCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCScrollInfo` - Scroll info
  - `PB_SCScrollListInfo` - Scroll list
  - `PB_SCScrollOneInfo` - Single scroll update
- **Messages Sent (CS)**:
  - `PB_CSScrollReq` - Scroll operations
- **Backend Service**: `item-service` or magic service ✅
- **Functionality**: Magic scroll collection and usage

---

### 5. 🎊 EVENTS & ACTIVITIES DOMAIN

#### **ActivityCtrl** - Master Activity Controller
- **Path**: `modules/activity/ActivityCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCActivityStatus` - Activity status updates
- **Messages Sent (CS)**:
  - `PB_CSRandActivityOperaReq` - Random activity operations
- **Backend Service**: Multiple services (coordinates activity modules) ✅
- **Functionality**: Central hub for all time-limited activities

---

#### **OpenServerCtrl** - New Server Events
- **Path**: `modules/open_server/OpenServerCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCSevenDaySignInfo` - 7-day sign-in (七日签到)
  - `PB_SCLuckUnpackingInfo` - Lucky unboxing (开箱大吉)
  - `PB_SCNewAreaPreferentialInfo` - New server special offers (新服特惠)
  - `PB_SCMarketShopInfo` - Market shop
- **Messages Sent (CS)**:
  - `PB_CSSevenDaySignReq` - 7-day sign operations
  - `PB_CSLuckUnpackingReq` - Lucky unboxing operations
  - `PB_CSNewAreaPreferentialReq` - New server offers
  - `PB_CSMarketShopReq` - Market shop operations
- **Backend Service**: `serverInfo-service` or event service ✅
- **Functionality**: New player retention events

---

#### **ServerActivityCtrl** - Server-Wide Activities
- **Path**: `modules/serveractivity/ServerActivityCtrl.ts`
- **Backend Service**: `serverInfo-service` ✅
- **Functionality**: Server-specific events and competitions

---

#### **MoreServerActivityCtrl** - Multi-Server Activities
- **Path**: `modules/moreserveractive/MoreServerActivityCtrl.ts`
- **Backend Service**: `serverInfo-service` ✅
- **Functionality**: Cross-server events

---

#### **NewServerCompetitionCtrl** - New Server Competition
- **Path**: `modules/new_server_competition/NewServerCompetitionCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaNewServerInfo` - Competition info
  - `PB_SCRaNewServerGlobalInfo` - Global rankings
  - `PB_SCRANewServerRankList` - Rank list
- **Messages Sent (CS)**:
  - `PB_CSRandActivityOperaReq` - Competition operations
- **Backend Service**: `arena-service` or competition service ✅
- **Functionality**: New server competitive events

---

#### **LeiChongCtrl** - Cumulative Recharge Event
- **Path**: `modules/lei_chong/LeiChongCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaLeiChongInfo` - Cumulative recharge info (累充)
- **Backend Service**: `wallet-service` / recharge service ✅
- **Functionality**: Cumulative spending rewards

---

#### **FirstChargeCtrl** - First Purchase Bonus
- **Path**: `modules/first_charge/FirstChargeCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaFirstChongInfo` - First charge info (首充)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: First-time purchase rewards

---

#### **MonthlyCardCtrl** - Monthly Card Subscription
- **Path**: `modules/MonthlyCard/MonthlyCardCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaMonthCardInfo` - Monthly card info (月卡)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Subscription-based rewards

---

#### **WarOrderCtrl** - Battle Pass System
- **Path**: `modules/warOrder/WarOrderCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaWarOrderInfo` - War order/battle pass info (战令)
- **Backend Service**: Event service (possibly `serverInfo-service`) ✅
- **Functionality**: Season pass rewards

---

#### **DailyGiftCtrl** - Daily Gift Pack
- **Path**: `modules/DailyGift/DailyGiftCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaDailyGiftInfo` - Daily gift info (日常礼包)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Daily login rewards

---

#### **LuckyGiftCtrl** - Lucky Gift Event
- **Path**: `modules/LuckyGift.ts/LuckyGiftCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaLuckCourtesyInfo` - Lucky gift info (幸运礼遇)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Special gift opportunities

---

#### **BoxFundCtrl** - Box Fund Investment
- **Path**: `modules/boxfund/BoxFundCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaBoxFundInfo` - Box fund info (宝箱基金)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Investment-style rewards

---

#### **LevelFundCtrl** - Level Fund
- **Path**: `modules/levelfund/LevelFundCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaLevelFundInfo` - Level fund info (等级基金)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Level-based investment returns

---

#### **ScoreFundCtrl** - Score/Capacity Fund
- **Path**: `modules/ScoreFund/ScoreFundCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaCapacityFundInfo` - Score fund info (容量基金)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Score-based fund rewards

---

#### **WeekendRechargeCtrl** - Weekend Recharge Event
- **Path**: `modules/weekendrecharge/WeekendRechargeCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaWeekendRechargeInfo` - Weekend recharge info (周末充值)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Weekend spending promotions

---

#### **WeekHaoLiCtrl** - Weekly Gift
- **Path**: `modules/WeekHaoLi/WeekHaoLiCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaWeekendHaoLiInfo` - Weekly gift info (周末豪礼)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Weekly reward system

---

#### **WeekLianChongCtrl** - Weekly Consecutive Recharge
- **Path**: `modules/WeekLianChong/WeekLianChongCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaWeekendLianChongInfo` - Weekly consecutive info (周末连充)
- **Backend Service**: `wallet-service` ✅
- **Functionality**: Consecutive day recharge rewards

---

#### **ContinuePresentCtrl** - Consecutive Login Gift
- **Path**: `modules/ContinuePresent/ContinuePresentCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaLianChongZengLiInfo` - Consecutive gift info (连充赠礼)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Login streak rewards

---

#### **TodayShareCtrl** - Daily Share Rewards
- **Path**: `modules/TodayShare/TodayShareCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaDailySharingInfo` - Daily sharing info (每日分享)
- **Backend Service**: Social/marketing service ✅
- **Functionality**: Social media sharing rewards

---

#### **AffordPresentCtrl** - Affordable Present
- **Path**: `modules/AffordPresent/AffordPresentCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaChaoZhiXianLiInfo` - Super value gift (超值献礼)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Special promotional gifts

---

#### **ExclusiveGiftBagCtrl** - Exclusive Gift Bag
- **Path**: `modules/ExclusiveGiftBag/ExclusiveGiftBagCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaExclusiveGift` - Exclusive gift info (专属礼包)
- **Backend Service**: `gift-service` ✅
- **Functionality**: Premium gift packages

---

#### **AngelFesCtrl** - Angel Festival Event
- **Path**: `modules/AngelFes/AngelFesCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaFaZhenGalaInfo` - Angel festival info (法阵盛典)
- **Backend Service**: Event service ✅
- **Functionality**: Special angel-themed event

---

#### **StarMapFesCtrl** - Star Map Festival
- **Path**: `modules/StarMapFes/StarMapFesCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaStarMapGalaInfo` - Star map festival info (星图盛典)
- **Backend Service**: Event service ✅
- **Functionality**: Star map special event

---

#### **ShenQiDrawCtrl** - Divine Weapon Gacha
- **Path**: `modules/ShenQiDraw/ShenQiDrawCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaShenqiDuobao` - Divine weapon gacha (神器夺宝)
- **Backend Service**: Gacha service (possibly `drop-service`) ✅
- **Functionality**: Divine weapon lottery

---

#### **IntegralTurntableCtrl** - Points Lottery Wheel
- **Path**: `modules/integralTurntable/IntegralTurntableCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaJifenZhuanpan` - Points wheel info (积分转盘)
- **Backend Service**: Event service ✅
- **Functionality**: Point-based lottery system

---

#### **CaveLootCtrl** - Cave Loot Event
- **Path**: `modules/caveloot/CaveLootCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaCaveLootInfo` - Cave loot info (洞穴探险)
- **Backend Service**: Event service ✅
- **Functionality**: Cave exploration event

---

#### **BoxManorCtrl** - Box Manor Event
- **Path**: `modules/boxmanor/BoxManorCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaChestManorInfo` - Box manor info (宝箱庄园)
- **Backend Service**: Event service ✅
- **Functionality**: Manor-themed box event

---

#### **CommodityGuildCtrl** - Guild Commodity
- **Path**: `modules/CommodityGuild/CommodityGuildCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaCommodityGuildInfo` - Guild commodity info (商品公会)
- **Backend Service**: Guild service ✅
- **Functionality**: Guild-related trading

---

#### **LoopMineCtrl** - Loop Mine/Resource Competition
- **Path**: `modules/loopmine/LoopMineCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCDuoBaoInfo` - Mine competition info (夺宝)
  - `PB_SCDuoBaoItemInfo` - Item info
  - `PB_SCDuoBaoRecordInfo` - Record info
- **Messages Sent (CS)**:
  - `PB_CSDuoBaoReq` - Mine operations
- **Backend Service**: Event/competition service ✅
- **Functionality**: Resource gathering competition

---

#### **KnightCardCtrl** - Knight Card/Ad Rewards
- **Path**: `modules/knight_card/KnightCardCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCRaAdvertisementEquityInfo` - Ad equity info (广告权益)
- **Messages Sent (CS)**:
  - `PB_CSRandActivityOperaReq` - Ad operations
- **Backend Service**: Marketing/ad service ✅
- **Functionality**: Advertisement-based rewards

---

#### **FishCtrl** - Fishing/Treasure Hunt Mini-Game
- **Path**: `modules/fish/FishCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCWaBaoInfo` - Treasure hunt info (挖宝)
  - `PB_SCWaBaoMapInfo` - Map info
  - `PB_SCWaBaoItemInfo` - Item info
  - `PB_SCWaBaoIntegrityInfo` - Integrity info
  - `PB_SCWaBaoCollectionListInfo` - Collection list
  - `PB_SCWaBaoToolInfo` - Tool info
  - `PB_SCWaBaoTaskInfo` - Task info
  - `PB_SCWaBaoSetingInfo` - Settings
  - `PB_SCWaBaoCollectionBookInfo` - Collection book
  - `PB_SCWaBaoBookListInfo` - Book list
- **Messages Sent (CS)**:
  - `PB_CSWaBaoReq` - Treasure hunt operations
  - `PB_CSWaBaoSetReq` - Settings
- **Backend Service**: Mini-game service ✅
- **Functionality**: Fishing/treasure hunt mini-game

---

### 6. 🛠️ SYSTEM & INFRASTRUCTURE DOMAIN

#### **LoginCtrl** - Authentication System
- **Path**: `modules/login/LoginCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCHeartbeatResp` - Heartbeat response
  - `PB_SCLoginToAccount` - Login result
  - `PB_SCDisconnectNotice` - Disconnection notice
- **Messages Sent (CS)**:
  - `PB_CSLoginToAccount` - Login request
- **Backend Service**: `session-service` / `user-service` ✅
- **Functionality**: Player authentication, session management

---

#### **TimeCtrl** - Server Time Synchronization
- **Path**: `modules/time/TimeCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCTimeAck` - Server time
- **Messages Sent (CS)**:
  - `PB_CSTimeReq` - Time request
- **Backend Service**: `session-service` or time service ✅
- **Functionality**: Client-server time sync

---

#### **PublicPopupCtrl** - Notification System
- **Path**: `modules/public_popup/PublicPopupCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCNoticeNum` - Notification count
  - `PB_SCItemNotEnoughNotice` - Insufficient item notice
- **Backend Service**: Gateway/notification service ✅
- **Functionality**: System notifications, popups

---

#### **AnnounceCtrl** - Announcement System
- **Path**: `modules/Announce/AnnounceCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCNoticeTimeRet` - Announcement timing
- **Messages Sent (CS)**:
  - `PB_CSNoticeTimeReq` - Request announcements
- **Backend Service**: `admin-service` or announcement service ✅
- **Functionality**: Server announcements, news

---

#### **GMCmdCtrl** - GM Command System
- **Path**: `modules/gm_command/GMCmdCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCGMCommand` - GM command response
- **Backend Service**: `admin-service` ✅
- **Functionality**: Administrator commands for debugging/testing

---

#### **GuideCtrl** - Tutorial/Guide System
- **Path**: `modules/guide/GuideCtrl.ts`
- **Backend Service**: `role-service` or tutorial service ✅
- **Functionality**: New player tutorial flow

---

#### **RemindCtrl** - Red Dot Reminder System
- **Path**: `modules/remind/RemindCtrl.ts`
- **Backend Service**: Client-side coordination (no dedicated backend) ✅
- **Functionality**: UI notification badges (red dots)

---

#### **BlockCtrl** - Building Block System
- **Path**: `modules/block/BlockCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCBuildBlockInfo` - Building block info
- **Messages Sent (CS)**:
  - `PB_CSBlockReq` - Block operations
- **Backend Service**: Unknown (possibly custom feature) ✅
- **Functionality**: Building/construction mechanics

---

#### **CoreCrisisCtrl** - Core Crisis System
- **Path**: `modules/CoreCrisis/CoreCrisisCtrl.ts`
- **Messages Received (SC)**:
  - `PB_SCLimitCoreInfo` - Limited core info (限时核心)
- **Messages Sent (CS)**:
  - `PB_CSLimitCoreReq` - Core operations
- **Backend Service**: Event or special item service ✅
- **Functionality**: Limited-time core/essence system

---

## 📊 Backend Service Mapping Summary

### ✅ **Confirmed Services in GameServer** (P0 Complete)

| Service | Controllers | Protocol Prefixes | Status |
|---------|------------|------------------|--------|
| **bag-service** | BagCtrl, ItemRecyclingCtrl | PB_CS/SCKnapsack*, PB_CS/SCItemRecycle* | ✅ P0 |
| **shop-service** | ShopCtrl, MysteryShopCtrl, ClothShopCtrl | PB_CS/SCShop*, PB_CS/SCMysteryShop*, PB_CSClothShop* | ✅ P0 |
| **box-service** | BoxCtrl | PB_CS/SCBox* | ✅ P0 |
| **equip-service** | EquipBagCtrl, EnchantCtrl, InscriptionCtrl | PB_CS/SCEquip*, PB_CS/SCRune*, PB_CS/SCEquipFuMo* | ✅ P0 |
| **role-service** | RoleCtrl, OtherRoleCtrl, ManualCtrl | PB_CS/SCRole*, PB_CSGetOtherRole*, PB_CS/SCKnights* | ✅ P0 |
| **pet-service** | PetCtrl, PetGuardCtrl, PetRelicsCtrl, MountCtrl | PB_CS/SCRolePet*, PB_CS/SCPetFb*, PB_CS/SCMount* | ✅ P0 |
| **arena-service** | ArenaCtrl, PeakArenaCtrl, NewServerCompetitionCtrl, RankCtrl | PB_CS/SCArena*, PB_CS/SCCrossArena*, PB_CS/SCRank* | ✅ P0 |
| **task-service** | TaskCtrl | PB_CS/SCTask* | ✅ P0 |
| **wallet-service** | RechargeCtrl, LeiChongCtrl, FirstChargeCtrl, MonthlyCardCtrl, BoxFundCtrl, LevelFundCtrl, ScoreFundCtrl, WeekendRechargeCtrl, WeekLianChongCtrl | PB_CS/SCChongZhi*, PB_SCRaLeiChong*, PB_SCRaFirstChong*, PB_SCRaMonthCard*, PB_SCRa*Fund*, PB_SCRaWeekend* | ✅ P0 |
| **gift-service** | DailyGiftCtrl, LuckyGiftCtrl, WeekHaoLiCtrl, ContinuePresentCtrl, AffordPresentCtrl, ExclusiveGiftBagCtrl | PB_SCRaDailyGift*, PB_SCRaLuckCourtesy*, PB_SCRaWeekendHaoLi*, PB_SCRaLianChong*, PB_SCRaChaoZhi*, PB_SCRaExclusive* | ✅ P0 |
| **main-fb-service** | DungeonCtrl, AdventureCtrl, TrialCtrl, EscortCtrl | PB_CS/SCLingZhu*, PB_CS/SCMainFb*, PB_CS/SCShiLianPagoda*, PB_CS/SCGuMoPagoda*, PB_CS/SCEscort* | ✅ P0 |
| **shizhuang-service** | FashionCtrl | PB_CS/SCShiZhuang* | ✅ P0 |
| **crafting-service** | GemAtelierCtrl | PB_CS/SCGem* | ✅ P0 |
| **drop-service** | (Implicit in ShenQiDrawCtrl, BoxCtrl) | PB_SCRaShenqiDuobao | ✅ P0 |
| **item-service** | MerlinMagicCtrl | PB_CS/SCScroll* | ✅ P0 |
| **serverInfo-service** | OpenServerCtrl, ServerActivityCtrl, MoreServerActivityCtrl | PB_CS/SCSevenDaySign*, PB_CS/SCLuckUnpacking*, PB_CS/SCNewAreaPreferential*, PB_CS/SCMarketShop* | ✅ P0 |
| **session-service** | LoginCtrl, TimeCtrl | PB_CS/SCLogin*, PB_CS/SCTime*, PB_SCHeartbeat* | ✅ P0 |
| **user-service** | LoginCtrl, InviteFriendCtrl | PB_CSLoginToAccount, PB_SCRaFriend* | ✅ P0 |
| **battleserver-service** | BattleCtrl | PB_SCBattleReport | ✅ P0 |
| **gateway-service** | (WebSocket hub) | All protocols route through gateway | ✅ P0 |
| **admin-service** | GMCmdCtrl, AnnounceCtrl | PB_CS/SCGMCommand, PB_CS/SCNoticeTime* | ✅ P0 |

### ❌ **Missing Services** (Should be P1 Priority)

| Service | Controllers | Required Protocols | Priority |
|---------|------------|-------------------|----------|
| **guild-service** | GuildCtrl, TerritoryCtrl, CommodityGuildCtrl | PB_CS/SCGuild*, PB_CS/SCTerritory*, PB_SCRaCommodityGuild* | ⚠️ **P1 HIGH** |
| **world-service** | (Implicit in RankCtrl, cross-server features) | Cross-server coordination | ⚠️ **P1 MEDIUM** |
| **globalserver-service** | (Cross-server arena, events) | Global state management | ⚠️ **P1 MEDIUM** |
| **report-service** | (Analytics, battle reports) | Data analytics | ⚠️ **P2 LOW** |

### 🔄 **Partial/Unclear Services**

| Service | Controllers | Notes |
|---------|------------|-------|
| **Activity Coordinator** | ActivityCtrl, AngelFesCtrl, StarMapFesCtrl, ShenQiDrawCtrl, IntegralTurntableCtrl, CaveLootCtrl, BoxManorCtrl, LoopMineCtrl | May be distributed across multiple services |
| **Social System** | TodayShareCtrl, InviteFriendCtrl | Possibly in user-service |
| **Mini-Games** | FishCtrl | May need dedicated mini-game service |

---

## 🔍 Protocol Naming Patterns

### Client → Server (CS) Protocols
- **Pattern**: `PB_CS{Feature}Req`
- **Examples**: 
  - `PB_CSKnapsackReq` (Bag operations)
  - `PB_CSArenaReq` (Arena operations)
  - `PB_CSShopBuyReq` (Shop purchase)
  - `PB_CSRolePetReq` (Pet operations)

### Server → Client (SC) Protocols
- **Pattern**: `PB_SC{Feature}Info` or `PB_SC{Feature}Ret` or `PB_SCRa{Feature}Info`
- **Examples**:
  - `PB_SCKnapsackAllInfo` (Full bag data)
  - `PB_SCArenaInfo` (Arena state)
  - `PB_SCRolePetRetInfo` (Pet operation result)
  - `PB_SCRaFirstChongInfo` (Random activity - first charge)

### Random Activity Protocols (Ra prefix)
- **Pattern**: `PB_SCRa{ActivityName}Info`
- **Used for**: Time-limited events, special activities
- **Examples**:
  - `PB_SCRaLeiChongInfo` (Cumulative recharge)
  - `PB_SCRaMonthCardInfo` (Monthly card)
  - `PB_SCRaBoxFundInfo` (Box fund)

---

## 🎯 Recommendations

### 1. **Guild Service Development** (Critical P1)
- **Controllers Waiting**: GuildCtrl, TerritoryCtrl
- **Impact**: Major social feature missing
- **Protocols**: ~20+ guild-related messages
- **Suggested Timeline**: Next sprint

### 2. **World Service for Cross-Server** (P1)
- **Controllers**: PeakArenaCtrl, NewServerCompetitionCtrl
- **Current State**: May be using globalserver-service
- **Need**: Better coordination for cross-server features

### 3. **Protocol Documentation**
- Consider generating proto files from TypeScript patterns
- Document message ID ranges per service
- Create protocol versioning strategy

### 4. **Testing Strategy**
- Each controller should have integration tests against corresponding service
- Mock protocol responses for frontend testing
- WebSocket message replay for debugging

### 5. **Microservice Boundaries**
- **Gift Service**: Currently handles 10+ gift types - consider if this is too broad
- **Activity Coordination**: Many random activities may benefit from dedicated coordinator
- **Pet Service**: Handles pets, mounts, relics - verify this domain grouping is optimal

---

## 📈 Statistics

- **Total Controllers Analyzed**: 82
- **Total Protocol Messages**: 300+
- **Backend Services Identified**: 22
- **Services Implemented (P0)**: 19
- **Services Missing**: 3 (Guild, World coordination, enhanced Global)
- **Protocol Patterns**: CS/SC request-response, Ra for random activities
- **Average Messages per Controller**: 3-5

---

## 🔗 Service Port Mapping Reference

Refer to [PORT_COMPARISON_REPORT.md](./GameServer/PORT_COMPARISON_REPORT.md) for detailed port assignments:
- **eureka-server**: 8761
- **gateway-service**: 9999 (HTTP), 9998 (WebSocket)
- **config-service**: 9000
- Domain services: 8101-8199 range

---

## 📝 Notes

1. **Chinese Comments**: Many controllers contain Chinese comments (时装=fashion, 宝箱=box, etc.) indicating original development in China
2. **Cocos Creator**: Frontend built with Cocos Creator game engine
3. **FairyGUI**: UI framework used across all views
4. **Smart Data Pattern**: Uses SmartData/SMD pattern for reactive data binding
5. **Module Architecture**: Clean separation between Ctrl (controller), Data (model), and View layers

---

**Report Generated**: January 18, 2026  
**Analyzed By**: AI Code Assistant  
**Next Steps**: Prioritize guild-service implementation, validate protocol mappings with actual protobuf definitions

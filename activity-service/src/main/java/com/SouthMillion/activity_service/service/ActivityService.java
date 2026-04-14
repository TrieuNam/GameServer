package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.*;
import com.SouthMillion.activity_service.repository.*;
import com.SouthMillion.activity_service.repository.FishGameRepository;
import com.SouthMillion.activity_service.repository.LoopMineRepository;
import com.SouthMillion.activity_service.repository.CoreCrisisGameRepository;
import com.SouthMillion.activity_service.repository.FillBlankRepository;
import com.SouthMillion.activity_service.repository.MingXiangRepository;
import com.SouthMillion.activity_service.client.AngelFeign;
import com.SouthMillion.activity_service.client.BagFeign;
import com.SouthMillion.activity_service.client.BoxFeign;
import com.SouthMillion.activity_service.client.ConfigFeign;
import com.SouthMillion.activity_service.client.RoleFeign;
import com.SouthMillion.activity_service.client.WalletFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.role.RoleDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j @Service @RequiredArgsConstructor
public class ActivityService {

    private final SevenDaySignRepository sevenDayRepo;
    private final LuckUnpackingRepository luckRepo;
    private final NewAreaPreferentialRepository newAreaRepo;
    private final MarketShopRepository marketRepo;
    private final DuoBaoDataRepository duoBaoRepo;
    private final RechargeInfoRepository rechargeInfoRepo;
    private final FirstRechargeRepository firstRechargeRepo;
    private final AccumulatedRechargeRepository accumulatedRechargeRepo;
    private final MonthCardRepository monthCardRepo;
    private final BoxFundRepository boxFundRepo;
    private final LevelFundRepository levelFundRepo;
    private final CapacityFundRepository capacityFundRepo;
    private final GuMoTowerFundRepository gumoTowerFundRepo;
    private final DailyGiftRepository dailyGiftRepo;
    private final CaveLootRepository caveLootRepo;
    private final FriendInviteRepository friendInviteRepo;
    private final FriendInviteShareProgressRepository friendInviteShareProgressRepo;
    private final DailySharingRepository dailySharingRepo;
    private final CommodityGuildRepository commodityGuildRepo;
    private final LuckCourtesyRepository luckCourtesyRepo;
    private final WeekendRechargeRepository weekendRechargeRepo;
    private final ChestManorRepository chestManorRepo;
    private final FaZhenGalaRepository faZhenGalaRepo;
    private final StarMapGalaRepository starMapGalaRepo;
    private final RuneTowerFundRepository runeTowerFundRepo;
    private final ChaoZhiXianLiRepository chaoZhiXianLiRepo;
    private final NewServerCompetitionRepository newServerCompetitionRepo;
    private final WeekendHaoLiRepository weekendHaoLiRepo;
    private final LianChongZengLiRepository lianChongZengLiRepo;
    private final WeekendLianChongRepository weekendLianChongRepo;
    private final NewServerGlobalRepository newServerGlobalRepo;
    private final WarOrderRepository warOrderRepo;
    private final AdvertisementEquityRepository advertisementEquityRepo;
    private final NewServerRankingRepository newServerRankingRepo;
    private final ShenqiDuobaoRepository shenqiDuobaoRepo;
    private final TianxuanGiftRepository tianxuanGiftRepo;
    private final TerritoryGiftRepository territoryGiftRepo;
    private final JifenZhuanpanRepository jifenZhuanpanRepo;
    private final CustomizedGiftRepository customizedGiftRepo;
    private final ExclusiveGiftRepository exclusiveGiftRepo;
    private final FishGameRepository fishGameRepo;
    private final LoopMineRepository loopMineRepo;
    private final CoreCrisisGameRepository coreCrisisGameRepo;
    private final FillBlankRepository fillBlankRepo;
    private final MingXiangRepository mingXiangRepo;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private WalletFeign walletFeign;

    @Autowired(required = false)
    private AngelFeign angelFeign;

    @Autowired(required = false)
    private BagFeign bagFeign;

    @Autowired(required = false)
    private BoxFeign boxFeign;

    @Autowired(required = false)
    private ConfigFeign configFeign;

    @Autowired(required = false)
    private RoleFeign roleFeign;

    private static final long ACTIVITY_DURATION = 7L * 24 * 3600; // 7 days in seconds
    private static final String BOX_FUND_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/baoxiangjijin.json";
    private volatile BoxFundConfig boxFundConfigCache;
    private static final String LEVEL_FUND_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/dengjijijin.json";
    private volatile LevelFundConfig levelFundConfigCache;
    private static final String DAILY_GIFT_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/richanglibao.json";
    private volatile List<DailyGiftCfgEntry> dailyGiftConfigCache;
    private static final String CHEST_MANOR_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/baoxiangzhuangyuan.json";
    // Map<seq, JsonNode> loaded from baoxiangzhuangyuan.json "reward" array
    private volatile Map<Integer, JsonNode> chestManorConfigCache;
    private static final String INVITE_FRIEND_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/baozilaile.json";
    private volatile InviteFriendConfig inviteFriendConfigCache;
    private static final String JIFEN_ZHUANPAN_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/jifenzhuanpan_auto.json";
    private volatile JifenZhuanpanConfig jifenZhuanpanConfigCache;
    private static final String KNIGHT_CARD_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/knight_card.json";
    private volatile JsonNode knightCardConfigCache;

    // ===== SevenDaySign =====
    public SevenDaySign getSevenDay(Long roleId) {
        return sevenDayRepo.findByRoleId(roleId).orElseGet(() -> sevenDayRepo.save(
                SevenDaySign.builder().roleId(roleId).days(1).receiveFlag(0)
                        .endTimestamp(Instant.now().getEpochSecond() + ACTIVITY_DURATION).build()));
    }

    @Transactional
    public SevenDaySign claimSevenDay(Long roleId, int day) {
        SevenDaySign sign = getSevenDay(roleId);
        int bit = 1 << (day - 1);
        if ((sign.getReceiveFlag() & bit) == 0 && day <= sign.getDays()) {
            sign.setReceiveFlag(sign.getReceiveFlag() | bit);
            sevenDayRepo.save(sign);
        }
        return sign;
    }

    // ===== LuckUnpacking =====
    public LuckUnpacking getLuck(Long roleId) {
        return luckRepo.findByRoleId(roleId).orElseGet(() -> luckRepo.save(
                LuckUnpacking.builder().roleId(roleId).receiveFlag(0).openBoxNum(0).boxLevel(0)
                        .endTimestamp(Instant.now().getEpochSecond() + ACTIVITY_DURATION).build()));
    }

    @Transactional
    public LuckUnpacking claimLuck(Long roleId, int seq) {
        LuckUnpacking luck = getLuck(roleId);
        int bit = 1 << seq;
        if ((luck.getReceiveFlag() & bit) == 0) {
            luck.setReceiveFlag(luck.getReceiveFlag() | bit);
            luckRepo.save(luck);
        }
        return luck;
    }

    // ===== NewAreaPreferential =====
    public NewAreaPreferential getNewArea(Long roleId) {
        return newAreaRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                List<Integer> zeros = new ArrayList<>(Collections.nCopies(12, 0));
                String json = objectMapper.writeValueAsString(zeros);
                return newAreaRepo.save(NewAreaPreferential.builder().roleId(roleId).buyTimesJson(json)
                        .endTimestamp(Instant.now().getEpochSecond() + ACTIVITY_DURATION).build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public NewAreaPreferential buyNewArea(Long roleId, int itemIndex) {
        NewAreaPreferential na = getNewArea(roleId);
        try {
            List<Integer> times = objectMapper.readValue(na.getBuyTimesJson(), new TypeReference<>(){});
            if (itemIndex >= 0 && itemIndex < times.size()) {
                times.set(itemIndex, times.get(itemIndex) + 1);
                na.setBuyTimesJson(objectMapper.writeValueAsString(times));
                newAreaRepo.save(na);
            }
        } catch (Exception e) {
            log.error("buyNewArea parse error", e);
        }
        return na;
    }

    // ===== MarketShop =====
    public MarketShop getMarket(Long roleId) {
        return marketRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                long now = Instant.now().getEpochSecond();
                String emptyJson = objectMapper.writeValueAsString(Collections.emptyList());
                return marketRepo.save(MarketShop.builder().roleId(roleId)
                        .endTimestamp(now + ACTIVITY_DURATION)
                        .nextFreeRefresh(now + 3600).nextAutoRefresh(now + 86400)
                        .curShopGroup(1).shopGoodsSeqJson(emptyJson).shopBuyTimesJson(emptyJson)
                        .randomCnts(3).build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public MarketShop buyMarket(Long roleId, int goodsSeq) {
        MarketShop shop = getMarket(roleId);
        try {
            List<Integer> buyTimes = objectMapper.readValue(shop.getShopBuyTimesJson(), new TypeReference<>(){});
            buyTimes.add(1);
            shop.setShopBuyTimesJson(objectMapper.writeValueAsString(buyTimes));
            marketRepo.save(shop);
        } catch (Exception e) {
            log.error("buyMarket parse error", e);
        }
        return shop;
    }

    @Transactional
    public MarketShop refreshMarket(Long roleId) {
        MarketShop shop = getMarket(roleId);
        long now = Instant.now().getEpochSecond();
        shop.setNextFreeRefresh(now + 3600);
        shop.setCurShopGroup(shop.getCurShopGroup() + 1);
        marketRepo.save(shop);
        return shop;
    }

    // ===== DuoBao (夺宝) =====

    /**
     * Handle DuoBao operations.
     * opType: 1=GET_INFO, 2=DRAW(param1=type,param2=count), 3=REFRESH(param1=type), 4=CLAIM_REWARD(param1=type,param2=seq)
     * Returns map with "success" and "dataList" (list of 2 maps, one per type).
     */
    @Transactional
    public Map<String, Object> handleDuoBao(Long roleId, int opType, int param1, int param2) {
        log.info("[DuoBao] roleId={}, opType={}, param1={}, param2={}", roleId, opType, param1, param2);

        DuoBaoData type1 = getOrCreateDuoBao(roleId, 1);
        DuoBaoData type2 = getOrCreateDuoBao(roleId, 2);

        switch (opType) {
            case 2 -> { // DRAW: param1=type, param2=count (ignored for now, always 1 draw)
                DuoBaoData data = (param1 == 2) ? type2 : type1;
                data.setIntegral(data.getIntegral() + 10); // 10 integral per draw
                duoBaoRepo.save(data);
                if (param1 == 2) type2 = data; else type1 = data;
            }
            case 3 -> { // REFRESH: param1=type
                DuoBaoData data = (param1 == 2) ? type2 : type1;
                int now = (int) Instant.now().getEpochSecond();
                if (data.getFreeRefreshNum() > 0) {
                    data.setFreeRefreshNum(data.getFreeRefreshNum() - 1);
                    data.setFreeRefreshTime(now + 3600); // 1h cooldown
                } else {
                    data.setFreeRefreshTime(now + 3600);
                }
                duoBaoRepo.save(data);
                if (param1 == 2) type2 = data; else type1 = data;
            }
            case 4 -> { // CLAIM_REWARD: param1=type, param2=reward seq (0-indexed)
                DuoBaoData data = (param1 == 2) ? type2 : type1;
                int bit = 1 << param2;
                if ((data.getFetchFlag() & bit) == 0) {
                    data.setFetchFlag(data.getFetchFlag() | bit);
                    duoBaoRepo.save(data);
                }
                if (param1 == 2) type2 = data; else type1 = data;
            }
            default -> { /* case 1 GET_INFO: no state change */ }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(duoBaoToMap(type1));
        dataList.add(duoBaoToMap(type2));
        result.put("dataList", dataList);
        return result;
    }

    private DuoBaoData getOrCreateDuoBao(Long roleId, int type) {
        return duoBaoRepo.findByRoleIdAndDuoBaoType(roleId, type).orElseGet(() ->
                duoBaoRepo.save(DuoBaoData.builder()
                        .roleId(roleId).duoBaoType(type)
                        .integral(0).fetchFlag(0)
                        .freeRefreshNum(3)
                        .freeRefreshTime((int) Instant.now().getEpochSecond())
                        .build()));
    }

    private Map<String, Object> duoBaoToMap(DuoBaoData d) {
        Map<String, Object> m = new HashMap<>();
        m.put("integral", d.getIntegral());
        m.put("fetchFlag", d.getFetchFlag());
        m.put("freeRefreshNum", d.getFreeRefreshNum());
        m.put("freeRefreshTime", d.getFreeRefreshTime());
        return m;
    }

    // ===== RandActivity (3000) generic dispatch =====

    /**
     * Handles all rand-activity types (msg 3000).
     * activityType determines which SC msg to send back (scMsgId = 3000 + activityType).
     * Returns a Map whose keys match proto field names (camelCase) expected by RandActivityHandler.
     */
    public Map<String, Object> handleRandActivity(Long roleId, int activityType, int operaType,
                                                   int param1, int param2, int param3) {
        int dispatchActivityType = normalizeActivityType(activityType);
        log.info("[RandActivity] roleId={}, actType={}, dispatchActType={}, op={}, p1={}",
                roleId, activityType, dispatchActivityType, operaType, param1);

        return switch (dispatchActivityType) {
            case 1  -> handleRechargeInfo(roleId, operaType, param1, param2);
            case 10 -> handleBoxFund(roleId, operaType, param1, param2);
            case 11 -> handleLevelFund(roleId, operaType, param1, param2);
            case 12 -> handleFirstRecharge(roleId, operaType);
            case 13 -> handleAccumulatedRecharge(roleId, operaType, param1);
            case 14 -> handleDailyGift(roleId, operaType, param1);
            case 15 -> handleCommodityGuild(roleId, operaType, param1);
            case 16 -> handleMonthCard(roleId, operaType, param1, param2);
            case 17 -> handleLuckCourtesy(roleId, operaType, param1, param2);
            case 18 -> handleWeekendRecharge(roleId, operaType, param1);
            case 19 -> handleCaveLoot(roleId, operaType, param1, param2);
            case 20 -> handleFriendInvite(roleId, operaType, param1);
            case 21 -> handleChestManor(roleId, operaType, param1);
            case 22 -> handleCapacityFund(roleId, operaType, param1);
            case 23 -> handleDailySharing(roleId, operaType);
            case 24 -> handleFaZhenGala(roleId, operaType, param1);
            case 25 -> handleStarMapGala(roleId, operaType, param1);
            case 26 -> handleGuMoTowerFund(roleId, operaType, param1);
            case 27 -> handleRuneTowerFund(roleId, operaType, param1);
            case 28 -> handleChaoZhiXianLi(roleId, operaType, param1);
            case 29 -> handleNewServerCompetition(roleId, operaType, param1);
            case 30 -> handleWeekendHaoLi(roleId, operaType, param1);
            case 31 -> handleNewServerGlobal(param1); // param1 = serverId
            case 32 -> handleLianChongZengLi(roleId, operaType, param1);
            case 33 -> handleWarOrder(roleId, operaType, param1, param2);
            case 34 -> handleWeekendLianChong(roleId, operaType);
            case 35 -> handleAdvertisementEquity(roleId, operaType);
            case 36 -> handleNewServerRanking(roleId, operaType, param1);
            case 37 -> handleShenqiDuobao(roleId, operaType, param1);
            case 38 -> handleTianxuanGift(roleId, operaType, param1);
            case 39 -> handleTerritoryGift(roleId, operaType);
            case 40 -> handleJifenZhuanpan(roleId, operaType, param1);
            case 41 -> handleCustomizedGift(roleId, operaType, param1);
            case 42 -> handleExclusiveGift(roleId, operaType, param1);
            case 43 -> handleFishGame(roleId, operaType, param1);
            case 44 -> handleLoopMine(roleId, operaType, param1);
            case 45 -> handleCoreCrisis(roleId, operaType, param1);
            case 46 -> handleFillBlank(roleId, operaType, param1);
            case 47 -> handleMingXiang(roleId, operaType, param1);
            default -> {
                log.warn("[RandActivity] Unimplemented activityType={}", activityType);
                yield Map.of();
            }
        };
    }

    private int normalizeActivityType(int activityType) {
        return switch (activityType) {
            case 2049 -> 10; // BoxFund
            case 2050 -> 11; // LevelFund
            case 2051 -> 15; // CommodityGuild
            case 2052 -> 12; // FirstCharge
            case 2053 -> 13; // LeiChong
            case 2054 -> 14; // DailyGift
            case 2055 -> 16; // MonthlyCard
            case 2056 -> 17; // LuckyGift
            case 2057 -> 20; // InviteFriend
            case 2058 -> 18; // WeekendRecharge
            case 2059 -> 19; // CaveLoot
            case 2060 -> 21; // BoxManor
            case 2061 -> 22; // ScoreFund
            case 2062 -> 23; // TodayShare
            case 2063 -> 24; // FaZhenGala
            case 2064 -> 26; // GuMoChengJiu
            case 2065 -> 27; // InscripeChengJiu
            case 2066 -> 25; // StarMapGala
            case 2067 -> 28; // ChaoZhiXianLi
            case 2068 -> 29; // NewServerCompetition
            case 2069 -> 30; // WeekHaoLi
            case 2070 -> 32; // LianChongZengLi
            case 2071 -> 33; // WarOrder
            case 2072 -> 34; // WeekLianChong
            case 2073 -> 35; // AdEquity
            case 2074 -> 37; // ShenQiDuoBao
            case 2075 -> 38; // TianXuanZhiLi
            case 2076 -> 39; // TerritoryGift
            case 2077 -> 40; // JiFenChouJiang
            case 2078 -> 41; // ShouChongDingZhi
            case 2079 -> 42; // ZhuanShuLiBaoRuKou
            default -> activityType;
        };
    }

    // === Type 1: 充值信息 (Recharge Info) ===
    
    private Map<String, Object> handleRechargeInfo(Long roleId, int opType, int param1, int param2) {
        RechargeInfo info = rechargeInfoRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyTimesJson = objectMapper.writeValueAsString(List.of(0, 0, 0, 0, 0));
                return rechargeInfoRepo.save(RechargeInfo.builder()
                        .roleId(roleId)
                        .historyChongzhi(0L)
                        .historyChongzhiCount(0)
                        .todayChongzhi(0)
                        .chongzhiRewardTimes(emptyTimesJson)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init RechargeInfo", e);
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("historyChongzhi", info.getHistoryChongzhi());
        result.put("historyChongzhiCount", info.getHistoryChongzhiCount());
        result.put("todayChongzhi", info.getTodayChongzhi());
        try {
            List<Integer> times = objectMapper.readValue(info.getChongzhiRewardTimes(), new TypeReference<>() {});
            result.put("chongzhiRewardTimes", times);
        } catch (Exception e) {
            result.put("chongzhiRewardTimes", List.of());
        }
        return result;
    }

    // === Type 12: 首充 (First Recharge) ===
    
    @Transactional
    private Map<String, Object> handleFirstRecharge(Long roleId, int opType) {
        FirstRecharge first = firstRechargeRepo.findByRoleId(roleId).orElseGet(() ->
                firstRechargeRepo.save(FirstRecharge.builder()
                        .roleId(roleId)
                        .firstChongMark(0)
                        .fetchMark(0)
                        .build()));

        // opType: 1=GET_INFO, 2=CLAIM_REWARD
        if (opType == 2 && first.getFirstChongMark() == 1 && first.getFetchMark() == 0) {
            first.setFetchMark(1);
            firstRechargeRepo.save(first);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("firstChongMark", first.getFirstChongMark());
        result.put("fetchMark", first.getFetchMark());
        return result;
    }

    // === Type 13: 累充 (Accumulated Recharge) ===

    /** Max supported milestone index — bitmask stored in int64. */
    private static final int LEI_CHONG_MAX_INDEX = 63;

    @Transactional
    private Map<String, Object> handleAccumulatedRecharge(Long roleId, int opType, int param1) {
        AccumulatedRecharge acc = accumulatedRechargeRepo.findByRoleId(roleId).orElseGet(() ->
                accumulatedRechargeRepo.save(AccumulatedRecharge.builder()
                        .roleId(roleId)
                        .fetchFlag(0L)
                        .build()));

        // opType: 0=GET_INFO (client), 1=CLAIM_MILESTONE (client) | legacy: 1=GET_INFO, 2=CLAIM_MILESTONE
        if (opType == 1 || opType == 2) {
            // [L1] Guard: param1 must be a valid bitmask index
            if (param1 < 0 || param1 > LEI_CHONG_MAX_INDEX) {
                log.warn("[LeiChong] invalid param1={} for roleId={}, skip claim", param1, roleId);
            } else {
                // [H3] Guard: player must have recharged at least once before claiming any milestone
                long historyChongzhi = rechargeInfoRepo.findByRoleId(roleId)
                        .map(RechargeInfo::getHistoryChongzhi)
                        .orElse(0L);
                if (historyChongzhi <= 0) {
                    log.warn("[LeiChong] roleId={} has no recharge history, reject claim for index={}", roleId, param1);
                } else {
                    long bit = 1L << param1;
                    if ((acc.getFetchFlag() & bit) == 0) {
                        acc.setFetchFlag(acc.getFetchFlag() | bit);
                        accumulatedRechargeRepo.save(acc);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fetchFlag", acc.getFetchFlag());
        return result;
    }

    // === Type 16: 月卡 (Month Card) ===
    
    @Transactional
    private Map<String, Object> handleMonthCard(Long roleId, int opType, int cardType, int param2) {
        List<MonthCard> cards = monthCardRepo.findByRoleId(roleId);
        
        // opType: 1=GET_INFO, 2=BUY_CARD, 3=CLAIM_DAILY
        if (opType == 2) { // BUY_CARD
            MonthCard card = monthCardRepo.findByRoleIdAndCardType(roleId, cardType)
                .orElse(null);
            if (card == null) {
                int now = (int) Instant.now().getEpochSecond();
                card = MonthCard.builder()
                        .roleId(roleId)
                        .cardType(cardType)
                        .fetchMark(0)
                        .haveDays(30)
                        .endTimestamp(now + 30 * 86400)
                        .firstBuyMark(1)
                        .buyLevel(1)
                        .build();
                monthCardRepo.save(card);
                cards = monthCardRepo.findByRoleId(roleId); // refresh
            } else {
                // Extend existing card
                card.setHaveDays(card.getHaveDays() + 30);
                card.setEndTimestamp(card.getEndTimestamp() + 30 * 86400);
                monthCardRepo.save(card);
            }
        } else if (opType == 3 && cardType > 0) { // CLAIM_DAILY
            MonthCard card = monthCardRepo.findByRoleIdAndCardType(roleId, cardType).orElse(null);
            if (card != null && card.getHaveDays() > 0) {
                int today = (int) (Instant.now().getEpochSecond() / 86400);
                int bit = 1 << (today % 30);
                if ((card.getFetchMark() & bit) == 0) {
                    card.setFetchMark(card.getFetchMark() | bit);
                    monthCardRepo.save(card);
                }
            }
        }

        List<Map<String, Object>> cardList = new ArrayList<>();
        for (MonthCard c : cards) {
            Map<String, Object> m = new HashMap<>();
            m.put("cardType", c.getCardType());
            m.put("fetchMark", c.getFetchMark());
            m.put("haveDays", c.getHaveDays());
            m.put("endTimestamp", c.getEndTimestamp());
            m.put("firstBuyMark", c.getFirstBuyMark());
            m.put("buyLevel", c.getBuyLevel());
            cardList.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cardList", cardList);
        return result;
    }

    // === Type 10: 宝箱基金 (Box Fund) ===
    
    @Transactional
    private Map<String, Object> handleBoxFund(Long roleId, int opType, int param1, int param2) {
        BoxFund fund = getOrCreateBoxFund(roleId);
        BoxFundConfig config = getBoxFundConfig();

        // client flow: opType 0=GET_INFO, 1=CLAIM_REWARD (param1=0 common / 1 senior, param2=seq)
        // legacy flow: opType 2=BUY_PHASE, 3=CLAIM_COMMON (param1=seq), 4=CLAIM_SENIOR (param1=seq)
        switch (opType) {
            case 1 -> {
                claimBoxFundReward(roleId, fund, config, param1, param2);
            }
            case 2 -> {
                buyBoxFundPhase(roleId, fund, config, param1);
            }
            case 3 -> {
                claimBoxFundReward(roleId, fund, config, 0, param1);
            }
            case 4 -> {
                claimBoxFundReward(roleId, fund, config, 1, param1);
            }
        }

        return boxFundSnapshot(fund);
    }

    private BoxFund getOrCreateBoxFund(Long roleId) {
        return boxFundRepo.findByRoleId(roleId).orElseGet(() ->
                boxFundRepo.save(BoxFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));
    }

    private void buyBoxFundPhase(Long roleId, BoxFund fund, BoxFundConfig config, int phase) {
        if (phase <= 0 || !config.phaseShowLevels.containsKey(phase)) {
            log.warn("[BoxFund] roleId={} invalid phase buy request phase={}", roleId, phase);
            return;
        }
        int roleLevel = getRoleLevel(roleId);
        int showLevel = config.phaseShowLevels.getOrDefault(phase, Integer.MAX_VALUE);
        if (roleLevel < showLevel) {
            log.warn("[BoxFund] roleId={} phase={} locked by roleLevel={} showLevel={}",
                    roleId, phase, roleLevel, showLevel);
            return;
        }
        int bit = 1 << phase;
        if ((fund.getPhaseBuyFlag() & bit) != 0) {
            return;
        }
        fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
        boxFundRepo.save(fund);
    }

    private void claimBoxFundReward(Long roleId, BoxFund fund, BoxFundConfig config, int rewardType, int seq) {
        BoxFundGiftConfig gift = config.giftsBySeq.get(seq);
        if (gift == null) {
            log.warn("[BoxFund] roleId={} invalid claim seq={} rewardType={}", roleId, seq, rewardType);
            return;
        }
        if (getBoxLevel(roleId) < gift.level()) {
            log.warn("[BoxFund] roleId={} seq={} blocked by boxLevel<requiredLevel {}<{}",
                    roleId, seq, getBoxLevel(roleId), gift.level());
            return;
        }

        long bit = 1L << seq;
        if (rewardType == 0) {
            if ((fund.getCommonFetchFlag() & bit) != 0) {
                return;
            }
            if (!grantBoxFundItems(roleId, gift.ordinaryItems(), "box_fund_common")) {
                return;
            }
            fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
            boxFundRepo.save(fund);
            return;
        }

        int phaseBit = 1 << gift.phase();
        if ((fund.getPhaseBuyFlag() & phaseBit) == 0) {
            log.warn("[BoxFund] roleId={} seq={} senior claim without purchase for phase={}",
                    roleId, seq, gift.phase());
            return;
        }
        if ((fund.getSeniorFetchFlag() & bit) != 0) {
            return;
        }
        if (!grantBoxFundItems(roleId, gift.seniorItems(), "box_fund_senior")) {
            return;
        }
        fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
        boxFundRepo.save(fund);
    }

    private boolean grantBoxFundItems(Long roleId, List<BagDTOs.GrantItem> items, String reason) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        if (bagFeign == null) {
            log.error("[BoxFund] bagFeign unavailable for roleId={} reason={}", roleId, reason);
            return false;
        }
        try {
            BagDTOs.GrantReq request = new BagDTOs.GrantReq();
            request.setRoleId(String.valueOf(roleId));
            request.setItems(items);
            request.setReason(reason);
            bagFeign.grantItems(request);
            return true;
        } catch (Exception e) {
            log.error("[BoxFund] grant failed roleId={} reason={} items={}", roleId, reason, items, e);
            return false;
        }
    }

    private Map<String, Object> boxFundSnapshot(BoxFund fund) {
        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    private int getRoleLevel(Long roleId) {
        if (roleFeign == null) {
            log.error("[BoxFund] roleFeign unavailable for roleId={}", roleId);
            return 0;
        }
        try {
            return roleFeign.detail(roleId)
                    .map(RoleDTOs.RoleResp::getLevel)
                    .filter(Objects::nonNull)
                    .orElse(0);
        } catch (Exception e) {
            log.error("[BoxFund] failed to load role level for roleId={}", roleId, e);
            return 0;
        }
    }

    private int getBoxLevel(Long roleId) {
        if (boxFeign == null) {
            log.error("[BoxFund] boxFeign unavailable for roleId={}", roleId);
            return 0;
        }
        try {
            BoxDTOs.InfoResp info = boxFeign.info(roleId);
            return info != null ? Math.max(info.getBoxLevel(), 0) : 0;
        } catch (Exception e) {
            log.error("[BoxFund] failed to load box level for roleId={}", roleId, e);
            return 0;
        }
    }

    private BoxFundConfig getBoxFundConfig() {
        BoxFundConfig cached = boxFundConfigCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (boxFundConfigCache == null) {
                boxFundConfigCache = loadBoxFundConfig();
            }
            return boxFundConfigCache;
        }
    }

    private BoxFundConfig loadBoxFundConfig() {
        if (configFeign == null) {
            log.error("[BoxFund] configFeign unavailable, using empty config");
            return BoxFundConfig.empty();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(BOX_FUND_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[BoxFund] empty config body path={}", BOX_FUND_CONFIG_PATH);
                return BoxFundConfig.empty();
            }
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            Map<Integer, BoxFundGiftConfig> giftsBySeq = new HashMap<>();
            Map<Integer, Integer> phaseShowLevels = new HashMap<>();

            JsonNode giftConfigure = root.path("gift_configure");
            if (giftConfigure.isArray()) {
                for (JsonNode node : giftConfigure) {
                    BoxFundGiftConfig gift = new BoxFundGiftConfig(
                            readInt(node, "seq"),
                            readInt(node, "phase"),
                            readInt(node, "level"),
                            parseGrantItems(node.get("ordinary_item")),
                            parseGrantItems(node.get("senior_item"))
                    );
                    giftsBySeq.put(gift.seq(), gift);
                }
            }

            JsonNode phaseConfigure = root.path("phase_configure");
            if (phaseConfigure.isArray()) {
                for (JsonNode node : phaseConfigure) {
                    phaseShowLevels.put(readInt(node, "phase"), readInt(node, "show_level"));
                }
            }

            return new BoxFundConfig(giftsBySeq, phaseShowLevels);
        } catch (Exception e) {
            log.error("[BoxFund] failed to load config path={}", BOX_FUND_CONFIG_PATH, e);
            return BoxFundConfig.empty();
        }
    }

    private List<BagDTOs.GrantItem> parseGrantItems(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<BagDTOs.GrantItem> items = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode child : node) {
                addGrantItem(items, child);
            }
            return items;
        }
        addGrantItem(items, node);
        return items;
    }

    private void addGrantItem(List<BagDTOs.GrantItem> items, JsonNode node) {
        int itemId = readInt(node, "item_id");
        int num = readInt(node, "num");
        if (itemId <= 0 || num <= 0) {
            return;
        }
        items.add(BagDTOs.GrantItem.builder().itemId(itemId).num(num).build());
    }

    private int readInt(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        if (value == null || value.isNull() || value.isMissingNode()) {
            return 0;
        }
        if (value.isNumber()) {
            return value.intValue();
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record BoxFundGiftConfig(int seq, int phase, int level,
                                     List<BagDTOs.GrantItem> ordinaryItems,
                                     List<BagDTOs.GrantItem> seniorItems) {
    }

    private record BoxFundConfig(Map<Integer, BoxFundGiftConfig> giftsBySeq,
                                 Map<Integer, Integer> phaseShowLevels) {
        private static BoxFundConfig empty() {
            return new BoxFundConfig(Map.of(), Map.of());
        }
    }

    private record LevelFundGiftConfig(int seq, int phase, int level,
                                       List<BagDTOs.GrantItem> ordinaryItems,
                                       List<BagDTOs.GrantItem> seniorItems) {
    }

    private record LevelFundConfig(Map<Integer, LevelFundGiftConfig> giftsBySeq,
                                   Map<Integer, Integer> phaseShowLevels) {
        private static LevelFundConfig empty() {
            return new LevelFundConfig(Map.of(), Map.of());
        }
    }

    private record JzpLevelConfig(int startLevel, int endLevel, int rewardGroup, int type) {}

    private record JzpLuckDrawReward(int seq, int rewardGroup, int rate, int baoDiId,
                                     List<BagDTOs.GrantItem> rewardItems) {}

    private record JzpDrawConfig(int firstConsumeScore, int tenConsumeScore,
                                 int baoDiTimes, int canCumulativeBaoDi) {}

    private record JifenZhuanpanConfig(
            List<JzpLevelConfig> levelConfigs,
            List<JzpLuckDrawReward> rewards,
            JzpDrawConfig drawConfig) {
        static JifenZhuanpanConfig empty() {
            return new JifenZhuanpanConfig(List.of(), List.of(), new JzpDrawConfig(10, 100, 70, 1));
        }
    }

    private record InviteFriendRewardCfg(int type, int invitationFriendNum,
                                         List<BagDTOs.GrantItem> rewardItems) {
    }

    private record InviteFriendConfig(Map<Integer, InviteFriendRewardCfg> rewardsByType) {
        private static InviteFriendConfig empty() {
            return new InviteFriendConfig(Map.of());
        }
    }

    // === Type 11: 等级基金 (Level Fund) ===
    
    @Transactional
    private Map<String, Object> handleLevelFund(Long roleId, int opType, int param1, int param2) {
        LevelFund fund = getOrCreateLevelFund(roleId);
        LevelFundConfig config = getLevelFundConfig();

        // client flow: opType 0=GET_INFO, 1=CLAIM_REWARD (param1=0 common / 1 senior, param2=seq)
        // legacy flow: opType 2=BUY_PHASE, 3=CLAIM_COMMON, 4=CLAIM_SENIOR
        switch (opType) {
            case 1 -> claimLevelFundReward(roleId, fund, config, param1, param2);
            case 2 -> buyLevelFundPhase(roleId, fund, config, param1);
            case 3 -> claimLevelFundReward(roleId, fund, config, 0, param1);
            case 4 -> claimLevelFundReward(roleId, fund, config, 1, param1);
        }

        return levelFundSnapshot(fund);
    }

    private LevelFund getOrCreateLevelFund(Long roleId) {
        return levelFundRepo.findByRoleId(roleId).orElseGet(() ->
                levelFundRepo.save(LevelFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));
    }

    private void buyLevelFundPhase(Long roleId, LevelFund fund, LevelFundConfig config, int phase) {
        if (phase <= 0 || !config.phaseShowLevels.containsKey(phase)) {
            log.warn("[LevelFund] roleId={} invalid phase buy request phase={}", roleId, phase);
            return;
        }
        int roleLevel = getRoleLevel(roleId);
        int showLevel = config.phaseShowLevels.getOrDefault(phase, Integer.MAX_VALUE);
        if (roleLevel < showLevel) {
            log.warn("[LevelFund] roleId={} phase={} locked by roleLevel={} showLevel={}",
                    roleId, phase, roleLevel, showLevel);
            return;
        }
        int bit = 1 << phase;
        if ((fund.getPhaseBuyFlag() & bit) != 0) {
            return;
        }
        fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
        levelFundRepo.save(fund);
    }

    private void claimLevelFundReward(Long roleId, LevelFund fund, LevelFundConfig config, int rewardType, int seq) {
        LevelFundGiftConfig gift = config.giftsBySeq.get(seq);
        if (gift == null) {
            log.warn("[LevelFund] roleId={} invalid claim seq={} rewardType={}", roleId, seq, rewardType);
            return;
        }

        int roleLevel = getRoleLevel(roleId);
        if (roleLevel < gift.level()) {
            log.warn("[LevelFund] roleId={} seq={} blocked by roleLevel<requiredLevel {}<{}",
                    roleId, seq, roleLevel, gift.level());
            return;
        }

        long bit = 1L << seq;
        if (rewardType == 0) {
            if ((fund.getCommonFetchFlag() & bit) != 0) {
                return;
            }
            if (!grantLevelFundItems(roleId, gift.ordinaryItems(), "level_fund_common")) {
                return;
            }
            fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
            levelFundRepo.save(fund);
            return;
        }

        int phaseBit = 1 << gift.phase();
        if ((fund.getPhaseBuyFlag() & phaseBit) == 0) {
            log.warn("[LevelFund] roleId={} seq={} senior claim without purchase for phase={}",
                    roleId, seq, gift.phase());
            return;
        }
        if ((fund.getSeniorFetchFlag() & bit) != 0) {
            return;
        }
        if (!grantLevelFundItems(roleId, gift.seniorItems(), "level_fund_senior")) {
            return;
        }
        fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
        levelFundRepo.save(fund);
    }

    private boolean grantLevelFundItems(Long roleId, List<BagDTOs.GrantItem> items, String reason) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        if (bagFeign == null) {
            log.error("[LevelFund] bagFeign unavailable for roleId={} reason={}", roleId, reason);
            return false;
        }
        try {
            BagDTOs.GrantReq request = new BagDTOs.GrantReq();
            request.setRoleId(String.valueOf(roleId));
            request.setItems(items);
            request.setReason(reason);
            bagFeign.grantItems(request);
            return true;
        } catch (Exception e) {
            log.error("[LevelFund] grant failed roleId={} reason={} items={}", roleId, reason, items, e);
            return false;
        }
    }

    private Map<String, Object> levelFundSnapshot(LevelFund fund) {
        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    private LevelFundConfig getLevelFundConfig() {
        LevelFundConfig cached = levelFundConfigCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (levelFundConfigCache == null) {
                levelFundConfigCache = loadLevelFundConfig();
            }
            return levelFundConfigCache;
        }
    }

    private LevelFundConfig loadLevelFundConfig() {
        if (configFeign == null) {
            log.error("[LevelFund] configFeign unavailable, using empty config");
            return LevelFundConfig.empty();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(LEVEL_FUND_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[LevelFund] empty config body path={}", LEVEL_FUND_CONFIG_PATH);
                return LevelFundConfig.empty();
            }
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            Map<Integer, LevelFundGiftConfig> giftsBySeq = new HashMap<>();
            Map<Integer, Integer> phaseShowLevels = new HashMap<>();

            JsonNode giftConfigure = root.path("gift_configure");
            if (giftConfigure.isArray()) {
                for (JsonNode node : giftConfigure) {
                    LevelFundGiftConfig gift = new LevelFundGiftConfig(
                            readInt(node, "seq"),
                            readInt(node, "phase"),
                            readInt(node, "level"),
                            parseGrantItems(node.get("ordinary_item")),
                            parseGrantItems(node.get("senior_item"))
                    );
                    giftsBySeq.put(gift.seq(), gift);
                }
            }

            JsonNode phaseConfigure = root.path("phase_configure");
            if (phaseConfigure.isArray()) {
                for (JsonNode node : phaseConfigure) {
                    phaseShowLevels.put(readInt(node, "phase"), readInt(node, "show_level"));
                }
            }

            return new LevelFundConfig(giftsBySeq, phaseShowLevels);
        } catch (Exception e) {
            log.error("[LevelFund] failed to load config path={}", LEVEL_FUND_CONFIG_PATH, e);
            return LevelFundConfig.empty();
        }
    }

    // === Type 22: 评分基金 (Capacity Fund) ===
    
    @Transactional
    private Map<String, Object> handleCapacityFund(Long roleId, int opType, int param1) {
        CapacityFund fund = capacityFundRepo.findByRoleId(roleId).orElseGet(() ->
                capacityFundRepo.save(CapacityFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=BUY_PHASE, 3=CLAIM_COMMON, 4=CLAIM_SENIOR
        switch (opType) {
            case 2 -> {
                int bit = 1 << param1;
                if ((fund.getPhaseBuyFlag() & bit) == 0) {
                    fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
                    capacityFundRepo.save(fund);
                }
            }
            case 3 -> {
                long bit = 1L << param1;
                if ((fund.getCommonFetchFlag() & bit) == 0) {
                    fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
                    capacityFundRepo.save(fund);
                }
            }
            case 4 -> {
                long bit = 1L << param1;
                if ((fund.getSeniorFetchFlag() & bit) == 0) {
                    fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
                    capacityFundRepo.save(fund);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    // === Type 26: 箍魔之塔基金 (GuMo Tower Fund) ===
    
    @Transactional
    private Map<String, Object> handleGuMoTowerFund(Long roleId, int opType, int param1) {
        GuMoTowerFund fund = gumoTowerFundRepo.findByRoleId(roleId).orElseGet(() ->
                gumoTowerFundRepo.save(GuMoTowerFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=BUY_PHASE, 3=CLAIM_COMMON, 4=CLAIM_SENIOR
        switch (opType) {
            case 2 -> {
                int bit = 1 << param1;
                if ((fund.getPhaseBuyFlag() & bit) == 0) {
                    fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
                    gumoTowerFundRepo.save(fund);
                }
            }
            case 3 -> {
                long bit = 1L << param1;
                if ((fund.getCommonFetchFlag() & bit) == 0) {
                    fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
                    gumoTowerFundRepo.save(fund);
                }
            }
            case 4 -> {
                long bit = 1L << param1;
                if ((fund.getSeniorFetchFlag() & bit) == 0) {
                    fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
                    gumoTowerFundRepo.save(fund);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    // === Type 14: 每日特惠 (Daily Gift) ===
    
    @Transactional
    private Map<String, Object> handleDailyGift(Long roleId, int opType, int param1) {
        DailyGift gift = dailyGiftRepo.findByRoleId(roleId).orElseGet(() ->
                dailyGiftRepo.save(DailyGift.builder()
                        .roleId(roleId)
                        .buyFlag(0L)
                        .buyCountJson("[]")
                        .build()));

        List<Integer> buyCount = parseIntArraySafe(gift.getBuyCountJson());
        if (buyCount.isEmpty() && gift.getBuyFlag() != null && gift.getBuyFlag() != 0L) {
            buyCount = migrateBuyFlagToBuyCount(gift.getBuyFlag(), 64);
            gift.setBuyCountJson(writeIntArraySafe(buyCount));
            dailyGiftRepo.save(gift);
        }

        // DailyGift counters reset every day.
        LocalDate today = LocalDate.now();
        LocalDate updatedDate = gift.getUpdatedAt() != null ? gift.getUpdatedAt().toLocalDate() : today;
        if (gift.getUpdatedAt() != null && !updatedDate.equals(today)) {
            buyCount = new ArrayList<>();
            gift.setBuyFlag(0L);
            gift.setBuyCountJson("[]");
            dailyGiftRepo.save(gift);
        }

        int roleLevel = getRoleLevel(roleId);

        // Client contract: opType 0 = GET_INFO, 1 = BUY (param1 = package type).
        // Keep compatibility with legacy clients that may send opType 2 for BUY.
        if ((opType == 1 || opType == 2) && param1 >= 0) {
            DailyGiftCfgEntry cfg = findDailyGiftCfg(roleLevel, param1);
            if (cfg == null) {
                log.warn("[DailyGift] roleId={} no config for type={} level={}", roleId, param1, roleLevel);
            } else {
                ensureArraySize(buyCount, param1 + 1);
                int alreadyBought = buyCount.get(param1);
                if (alreadyBought >= cfg.limitConvertCount()) {
                    log.warn("[DailyGift] roleId={} type={} buy limit reached {}/{}",
                            roleId, param1, alreadyBought, cfg.limitConvertCount());
                } else {
                    boolean paid = consumeDailyGiftCost(roleId, cfg, param1, alreadyBought);
                    if (paid) {
                        boolean granted = grantBoxFundItems(roleId, cfg.rewardItems(), "daily_gift_buy_type" + param1);
                        if (granted) {
                            buyCount.set(param1, alreadyBought + 1);
                            gift.setBuyCountJson(writeIntArraySafe(buyCount));

                            // Keep legacy bitflag in sync for old readers.
                            if (param1 < Long.SIZE) {
                                long bit = 1L << param1;
                                gift.setBuyFlag((gift.getBuyFlag() == null ? 0L : gift.getBuyFlag()) | bit);
                            }
                            dailyGiftRepo.save(gift);
                        }
                    }
                }
            }
        }

        // Ensure client-side index reads are safe even when cfg type indexes are sparse.
        ensureArraySize(buyCount, 64);

        Map<String, Object> result = new HashMap<>();
        result.put("level", roleLevel);
        result.put("buyCount", buyCount);
        return result;
    }

    private DailyGiftCfgEntry findDailyGiftCfg(int roleLevel, int giftType) {
        List<DailyGiftCfgEntry> cfg = getDailyGiftConfig();
        for (DailyGiftCfgEntry e : cfg) {
            boolean levelMatched = roleLevel >= e.startLevel() && (roleLevel <= e.endLevel() || e.endLevel() == 0);
            if (levelMatched && e.type() == giftType) {
                return e;
            }
        }
        return null;
    }

    private boolean consumeDailyGiftCost(Long roleId, DailyGiftCfgEntry cfg, int giftType, int alreadyBought) {
        // price_type: 1=diamond, 2=gold, 3=real-money(order flow)
        if (cfg.price() <= 0) return true;
        if (cfg.priceType() != 1 && cfg.priceType() != 2) {
            return true;
        }
        if (walletFeign == null) {
            log.error("[DailyGift] walletFeign unavailable for roleId={} type={}", roleId, giftType);
            return false;
        }

        long currencyItemId = cfg.priceType() == 1 ? 2L : 1L; // 2=diamond, 1=gold
        try {
            WalletDTOs.BatchReq req = WalletDTOs.BatchReq.builder()
                    .roleId(String.valueOf(roleId))
                    .changes(List.of(WalletDTOs.Change.builder()
                            .itemId(currencyItemId)
                            .amount(-cfg.price())
                            .build()))
                    .reason(302)
                    .idemKey("daily-gift-buy-" + roleId + "-" + giftType + "-" + alreadyBought)
                    .build();
            walletFeign.batchAdd(req);
            return true;
        } catch (Exception e) {
            log.error("[DailyGift] roleId={} type={} failed to deduct price={} priceType={}: {}",
                    roleId, giftType, cfg.price(), cfg.priceType(), e.getMessage());
            return false;
        }
    }

    private List<DailyGiftCfgEntry> getDailyGiftConfig() {
        List<DailyGiftCfgEntry> cached = dailyGiftConfigCache;
        if (cached != null) return cached;
        synchronized (this) {
            if (dailyGiftConfigCache == null) {
                dailyGiftConfigCache = loadDailyGiftConfig();
            }
            return dailyGiftConfigCache;
        }
    }

    private List<DailyGiftCfgEntry> loadDailyGiftConfig() {
        if (configFeign == null) {
            log.error("[DailyGift] configFeign unavailable, using empty config");
            return List.of();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(DAILY_GIFT_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[DailyGift] empty config body path={}", DAILY_GIFT_CONFIG_PATH);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            JsonNode reward = root.path("reward");
            if (!reward.isArray()) {
                return List.of();
            }

            List<DailyGiftCfgEntry> cfg = new ArrayList<>();
            for (JsonNode node : reward) {
                int startLevel = readInt(node, "start_level");
                int endLevel = readInt(node, "end_level");
                int type = readInt(node, "type");
                int limit = Math.max(readInt(node, "limit_convert_count"), 0);
                int priceType = readInt(node, "price_type");
                int price = Math.max(readInt(node, "price"), 0);
                List<BagDTOs.GrantItem> rewardItems = parseGrantItems(node.get("reward_item"));
                cfg.add(new DailyGiftCfgEntry(startLevel, endLevel, type, limit, priceType, price, rewardItems));
            }
            log.info("[DailyGift] loaded {} reward entries from config", cfg.size());
            return cfg;
        } catch (Exception e) {
            log.error("[DailyGift] failed to load config path={}", DAILY_GIFT_CONFIG_PATH, e);
            return List.of();
        }
    }

    private List<Integer> parseIntArraySafe(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("[DailyGift] parse buyCountJson failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String writeIntArraySafe(List<Integer> values) {
        try {
            return objectMapper.writeValueAsString(values != null ? values : List.of());
        } catch (Exception e) {
            log.warn("[DailyGift] write buyCountJson failed: {}", e.getMessage());
            return "[]";
        }
    }

    private List<Integer> migrateBuyFlagToBuyCount(Long buyFlag, int minSize) {
        List<Integer> counts = new ArrayList<>(Collections.nCopies(Math.max(minSize, 0), 0));
        long flags = buyFlag != null ? buyFlag : 0L;
        for (int i = 0; i < Long.SIZE; i++) {
            if (((flags >> i) & 1L) == 1L) {
                ensureArraySize(counts, i + 1);
                counts.set(i, 1);
            }
        }
        return counts;
    }

    private void ensureArraySize(List<Integer> values, int size) {
        while (values.size() < size) {
            values.add(0);
        }
    }

    private record DailyGiftCfgEntry(int startLevel,
                                     int endLevel,
                                     int type,
                                     int limitConvertCount,
                                     int priceType,
                                     int price,
                                     List<BagDTOs.GrantItem> rewardItems) {}

    // === Type 19: 山洞夺宝 (Cave Loot) ===
    
    @Transactional
    private Map<String, Object> handleCaveLoot(Long roleId, int opType, int param1, int param2) {
        CaveLoot loot = caveLootRepo.findByRoleId(roleId).orElseGet(() ->
                caveLootRepo.save(CaveLoot.builder()
                        .roleId(roleId)
                        .lotteryCount(0)
                        .chongzhiReceiveFlag(0)
                        .taskFetchFlag(0L)
                        .openLevel(1)
                        .totalChongzhi(0)
                        .buyTimesJson("[]")
                        .taskParamJson("[]")
                        .rewardReceiveJson("[]")
                        .build()));

        // opType: 0=GET_INFO, 1=BUY_GIFT(param1=seq), 2=DRAW_ONE, 3=DRAW_TEN,
        //         4=CLAIM_TASK(param1=task_type), 5=CLAIM_RECHARGE(param1=seq)
        try {
            switch (opType) {
                case 1 -> {
                    // Buy shop gift — increment buyTimes[seq]
                    List<Integer> buyTimes = parseCaveLootArray(loot.getBuyTimesJson());
                    while (buyTimes.size() <= param1) buyTimes.add(0);
                    buyTimes.set(param1, buyTimes.get(param1) + 1);
                    loot.setBuyTimesJson(objectMapper.writeValueAsString(buyTimes));
                    caveLootRepo.save(loot);
                }
                case 2 -> {
                    // Draw once
                    loot.setLotteryCount((loot.getLotteryCount() != null ? loot.getLotteryCount() : 0) + 1);
                    caveLootRepo.save(loot);
                }
                case 3 -> {
                    // Draw ten
                    loot.setLotteryCount((loot.getLotteryCount() != null ? loot.getLotteryCount() : 0) + 10);
                    caveLootRepo.save(loot);
                }
                case 4 -> {
                    // Claim task reward — increment rewardReceive[task_type]
                    List<Integer> rewardReceive = parseCaveLootArray(loot.getRewardReceiveJson());
                    while (rewardReceive.size() <= param1) rewardReceive.add(0);
                    rewardReceive.set(param1, rewardReceive.get(param1) + 1);
                    loot.setRewardReceiveJson(objectMapper.writeValueAsString(rewardReceive));
                    caveLootRepo.save(loot);
                }
                case 5 -> {
                    // Claim recharge reward — set bitmask bit at seq position
                    int bit = 1 << param1;
                    if ((loot.getChongzhiReceiveFlag() & bit) == 0) {
                        loot.setChongzhiReceiveFlag(loot.getChongzhiReceiveFlag() | bit);
                        caveLootRepo.save(loot);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[CaveLoot] failed to update roleId={} opType={} param1={}", roleId, opType, param1, e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("openLevel",          loot.getOpenLevel()          != null ? loot.getOpenLevel()          : 1);
        result.put("lotteryCount",       loot.getLotteryCount()       != null ? loot.getLotteryCount()       : 0);
        result.put("totalChongzhi",      loot.getTotalChongzhi()      != null ? loot.getTotalChongzhi()      : 0);
        result.put("chongzhiReceiveFlag",loot.getChongzhiReceiveFlag()!= null ? loot.getChongzhiReceiveFlag(): 0);
        result.put("buyTimes",     parseCaveLootArraySafe(loot.getBuyTimesJson()));
        result.put("taskParam",    parseCaveLootArraySafe(loot.getTaskParamJson()));
        result.put("rewardReceive",parseCaveLootArraySafe(loot.getRewardReceiveJson()));
        return result;
    }

    private List<Integer> parseCaveLootArray(String json) throws Exception {
        if (json == null || json.isBlank()) return new ArrayList<>();
        return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
    }

    private List<Integer> parseCaveLootArraySafe(String json) {
        try { return parseCaveLootArray(json); } catch (Exception e) { return List.of(); }
    }

    // === Type 20: 好友邀请 (Friend Invite) ===
    
    @Transactional
    private Map<String, Object> handleFriendInvite(Long roleId, int opType, int param1) {
        InviteFriendConfig config = getInviteFriendConfig();
        FriendInvite invite = friendInviteRepo.findByRoleId(roleId).orElseGet(() ->
                friendInviteRepo.save(FriendInvite.builder()
                        .roleId(roleId)
                        .inviteCount(0)
                        .fetchFlag(0L)
                        .build()));

        // client flow: opType 0=GET_INFO, 1=CLAIM_REWARD(param1=milestone type)
        // legacy compatibility: opType 2=CLAIM_REWARD
        if (opType == 1 || opType == 2) {
            InviteFriendRewardCfg rewardCfg = config.rewardsByType().get(param1);
            if (rewardCfg == null) {
                log.warn("[FriendInvite] roleId={} invalid milestone type={}", roleId, param1);
                return friendInviteSnapshot(invite);
            }

            int inviteCount = invite.getInviteCount() != null ? invite.getInviteCount() : 0;
            if (inviteCount < rewardCfg.invitationFriendNum()) {
                log.warn("[FriendInvite] roleId={} claim blocked type={} inviteCount={} required={}",
                        roleId, param1, inviteCount, rewardCfg.invitationFriendNum());
                return friendInviteSnapshot(invite);
            }

            long bit = 1L << param1;
            long fetchFlag = invite.getFetchFlag() != null ? invite.getFetchFlag() : 0L;
            if ((fetchFlag & bit) == 0) {
                if (!grantInviteFriendReward(roleId, rewardCfg)) {
                    return friendInviteSnapshot(invite);
                }
                invite.setFetchFlag(fetchFlag | bit);
                friendInviteRepo.save(invite);
            }
        }

        return friendInviteSnapshot(invite);
    }

    private Map<String, Object> friendInviteSnapshot(FriendInvite invite) {
        Map<String, Object> result = new HashMap<>();
        int friendCount = invite.getInviteCount() != null ? invite.getInviteCount() : 0;
        long rewardFlag = invite.getFetchFlag() != null ? invite.getFetchFlag() : 0L;

        // Keep both canonical and legacy keys for handler compatibility.
        result.put("friendCount", friendCount);
        result.put("rewardFlag", rewardFlag);
        result.put("inviteCount", friendCount);
        result.put("fetchFlag", rewardFlag);
        return result;
    }

    private boolean grantInviteFriendReward(Long roleId, InviteFriendRewardCfg rewardCfg) {
        if (rewardCfg.rewardItems() == null || rewardCfg.rewardItems().isEmpty()) {
            return true;
        }
        if (bagFeign == null) {
            log.error("[FriendInvite] bagFeign unavailable for roleId={} type={}", roleId, rewardCfg.type());
            return false;
        }
        try {
            BagDTOs.GrantReq request = new BagDTOs.GrantReq();
            request.setRoleId(String.valueOf(roleId));
            request.setItems(rewardCfg.rewardItems());
            request.setReason("friend_invite_claim");
            bagFeign.grantItems(request);
            return true;
        } catch (Exception e) {
            log.error("[FriendInvite] grant failed roleId={} type={} rewards={}",
                    roleId, rewardCfg.type(), rewardCfg.rewardItems(), e);
            return false;
        }
    }

    private InviteFriendConfig getInviteFriendConfig() {
        InviteFriendConfig cached = inviteFriendConfigCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (inviteFriendConfigCache == null) {
                inviteFriendConfigCache = loadInviteFriendConfig();
            }
            return inviteFriendConfigCache;
        }
    }

    private InviteFriendConfig loadInviteFriendConfig() {
        if (configFeign == null) {
            log.error("[FriendInvite] configFeign unavailable, using empty config");
            return InviteFriendConfig.empty();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(INVITE_FRIEND_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[FriendInvite] empty config body path={}", INVITE_FRIEND_CONFIG_PATH);
                return InviteFriendConfig.empty();
            }

            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            Map<Integer, InviteFriendRewardCfg> rewardsByType = new HashMap<>();
            JsonNode rewardNode = root.path("reward");
            if (rewardNode.isArray()) {
                for (JsonNode node : rewardNode) {
                    int type = readInt(node, "type");
                    rewardsByType.put(type, new InviteFriendRewardCfg(
                            type,
                            readInt(node, "invitation_friend_num"),
                            parseGrantItems(node.get("reward_item"))
                    ));
                }
            }

            return new InviteFriendConfig(rewardsByType);
        } catch (Exception e) {
            log.error("[FriendInvite] failed to load config path={}", INVITE_FRIEND_CONFIG_PATH, e);
            return InviteFriendConfig.empty();
        }
    }

    @Transactional
    public Map<String, Object> recordFriendInviteShare(
            Long roleId,
            Long userId,
            Long shareRoleId,
            Long shareUserId,
            Integer shareServerId) {
        Map<String, Object> result = new HashMap<>();
        result.put("ret", 0);
        result.put("added", 0);

        if (roleId == null || shareRoleId == null || roleId <= 0 || shareRoleId <= 0 || roleId.equals(shareRoleId)) {
            result.put("reason", "invalid_role_params");
            return result;
        }

        boolean firstBind = false;
        try {
            if (!friendInviteShareProgressRepo.existsByInviterRoleIdAndInvitedRoleId(shareRoleId, roleId)) {
                friendInviteShareProgressRepo.save(FriendInviteShareProgress.builder()
                        .inviterRoleId(shareRoleId)
                        .invitedRoleId(roleId)
                        .inviterUserId(shareUserId)
                        .invitedUserId(userId)
                        .shareServerId(shareServerId)
                        .build());
                firstBind = true;
            }
        } catch (DataIntegrityViolationException dup) {
            firstBind = false;
        }

        FriendInvite invite = friendInviteRepo.findByRoleId(shareRoleId).orElseGet(() ->
                friendInviteRepo.save(FriendInvite.builder()
                        .roleId(shareRoleId)
                        .inviteCount(0)
                        .fetchFlag(0L)
                        .build()));

        if (firstBind) {
            int current = invite.getInviteCount() != null ? invite.getInviteCount() : 0;
            invite.setInviteCount(current + 1);
            friendInviteRepo.save(invite);
            result.put("added", 1);
        }

        result.putAll(friendInviteSnapshot(invite));
        return result;
    }

    // === Type 23: 每日分享 (Daily Sharing) ===
    
    @Transactional
    private Map<String, Object> handleDailySharing(Long roleId, int opType) {
        DailySharing sharing = dailySharingRepo.findByRoleId(roleId).orElseGet(() ->
                dailySharingRepo.save(DailySharing.builder()
                        .roleId(roleId)
                        .fetchCount(0)
                        .build()));

        // opType: 1=GET_INFO, 2=SHARE (increment fetchCount)
        if (opType == 2) {
            sharing.setFetchCount(sharing.getFetchCount() + 1);
            dailySharingRepo.save(sharing);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fetchCount", sharing.getFetchCount());
        return result;
    }

    // === Type 15: 商品行会 (Commodity Guild) ===
    
    @Transactional
    private Map<String, Object> handleCommodityGuild(Long roleId, int opType, int param1) {
        CommodityGuild guild = commodityGuildRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyTimes = objectMapper.writeValueAsString(new ArrayList<>());
                return commodityGuildRepo.save(CommodityGuild.builder()
                        .roleId(roleId)
                        .curDiscount(10)  // 100% of price by default
                        .openLevel(1)
                        .purchasedTimesJson(emptyTimes)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init CommodityGuild", e);
            }
        });

        // opType:
        // 0 = GET_INFO (default, return current state)
        // 1 = DRAW_DISCOUNT (draw random discount from config)
        // 2 = BUY (param1=itemSeq, increment purchase count)
        if (opType == 1) {
            // Draw discount: randomly select from discount configuration
            // Config discounts are typically: 7 (70%), 8 (80%), 9 (90%), 10 (100%)
            // For now, using a simple random draw from [7, 8, 9, 10]
            int[] discounts = {7, 8, 9, 10};
            int randomDiscount = discounts[ThreadLocalRandom.current().nextInt(discounts.length)];
            guild.setCurDiscount(randomDiscount);
            commodityGuildRepo.save(guild);
        } else if (opType == 2) {
            // Buy: increment purchasedTimes for the given item seq
            try {
                List<Integer> times = objectMapper.readValue(guild.getPurchasedTimesJson(), new TypeReference<>() {});
                // Ensure list is large enough to hold this seq index
                while (times.size() <= param1) times.add(0);
                times.set(param1, times.get(param1) + 1);
                guild.setPurchasedTimesJson(objectMapper.writeValueAsString(times));
                commodityGuildRepo.save(guild);
            } catch (Exception e) {
                log.error("Failed to update purchasedTimes for seq={}", param1, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("curDiscount", guild.getCurDiscount());
        result.put("openLevel", guild.getOpenLevel());
        try {
            List<Integer> times = objectMapper.readValue(guild.getPurchasedTimesJson(), new TypeReference<>() {});
            result.put("purchasedTimes", times);
        } catch (Exception e) {
            result.put("purchasedTimes", List.of());
        }
        return result;
    }

    // === Type 17: 幸运礼遇 (Luck Courtesy) ===
    
    @Transactional
    private Map<String, Object> handleLuckCourtesy(Long roleId, int opType, int param1, int param2) {
        LuckCourtesy courtesy = luckCourtesyRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyGifts = objectMapper.writeValueAsString(new ArrayList<>());
                return luckCourtesyRepo.save(LuckCourtesy.builder()
                        .roleId(roleId)
                        .openLevel(1)
                        .giftInfoJson(emptyGifts)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init LuckCourtesy", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=giftSeq), 3=REFRESH(param1=giftSeq)
        Map<String, Object> result = new HashMap<>();
        result.put("openLevel", courtesy.getOpenLevel());
        try {
            List<Map<String, Object>> giftInfo = objectMapper.readValue(
                    courtesy.getGiftInfoJson(), new TypeReference<>() {});
            result.put("giftInfo", giftInfo);
        } catch (Exception e) {
            result.put("giftInfo", List.of());
        }
        return result;
    }

    // === Type 18: 周末累充 (Weekend Recharge) ===
    
    @Transactional
    private Map<String, Object> handleWeekendRecharge(Long roleId, int opType, int param1) {
        WeekendRecharge recharge = weekendRechargeRepo.findByRoleId(roleId).orElseGet(() ->
                weekendRechargeRepo.save(WeekendRecharge.builder()
                        .roleId(roleId)
                        .openLevel(1)
                        .totalChongzhi(0)
                        .receiveFlag(0)
                        .build()));

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=milestoneIndex)
        if (opType == 2) {
            int bit = 1 << param1;
            if ((recharge.getReceiveFlag() & bit) == 0) {
                recharge.setReceiveFlag(recharge.getReceiveFlag() | bit);
                weekendRechargeRepo.save(recharge);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("openLevel", recharge.getOpenLevel());
        result.put("totalChongzhi", recharge.getTotalChongzhi());
        result.put("receiveFlag", recharge.getReceiveFlag());
        return result;
    }

    // === Type 21: 宝箱庄园 (Chest Manor) ===
    
    @Transactional
    private Map<String, Object> handleChestManor(Long roleId, int opType, int param1) {
        ChestManor manor = chestManorRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyTimes = objectMapper.writeValueAsString(new ArrayList<>());
                return chestManorRepo.save(ChestManor.builder()
                        .roleId(roleId)
                        .openLevel(1)
                        .buyTimesJson(emptyTimes)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init ChestManor", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=seq)
        if (opType == 2) {
            try {
                Map<Integer, JsonNode> cfg = getChestManorConfig();
                JsonNode entry = cfg.get(param1);
                if (entry == null) {
                    log.warn("[ChestManor] roleId={} invalid seq={}", roleId, param1);
                } else {
                    int configBuyTimes = entry.path("buy_times").asInt(1);
                    int priceType = entry.path("price_type").asInt(0);

                    List<Integer> times = objectMapper.readValue(manor.getBuyTimesJson(), new TypeReference<>() {});
                    while (times.size() <= param1) times.add(0);
                    int alreadyBought = times.get(param1);

                    if (alreadyBought >= configBuyTimes) {
                        log.warn("[ChestManor] roleId={} seq={} buy limit reached {}/{}", roleId, param1, alreadyBought, configBuyTimes);
                    } else {
                        boolean canGrant = true;
                        // price_type 1=Diamond, 2=Gold — deduct currency
                        if (priceType == 1 || priceType == 2) {
                            int buyMoney = entry.path("buy_money").asInt(0);
                            long currencyItemId = priceType == 1 ? 2L : 1L; // 2=paid_gold(diamond), 1=gold
                            if (walletFeign != null && buyMoney > 0) {
                                try {
                                    WalletDTOs.BatchReq req = WalletDTOs.BatchReq.builder()
                                            .roleId(String.valueOf(roleId))
                                            .changes(List.of(WalletDTOs.Change.builder()
                                                    .itemId(currencyItemId)
                                                    .amount(-buyMoney)
                                                    .build()))
                                            .reason(302) // 302 = activity purchase
                                            .idemKey("manor-buy-" + roleId + "-" + param1 + "-" + alreadyBought)
                                            .build();
                                    walletFeign.batchAdd(req);
                                } catch (Exception e) {
                                    log.error("[ChestManor] roleId={} seq={} failed to deduct currency: {}", roleId, param1, e.getMessage());
                                    canGrant = false;
                                }
                            }
                        }
                        // price_type 3 = real money (order flow handles payment, just record and grant)
                        if (canGrant) {
                            // Grant reward items
                            JsonNode rewardItems = entry.get("reward_item");
                            List<BagDTOs.GrantItem> grantList = parseGrantItems(rewardItems);
                            if (!grantList.isEmpty()) {
                                grantBoxFundItems(roleId, grantList, "chest_manor_buy_seq" + param1);
                            }
                            times.set(param1, alreadyBought + 1);
                            manor.setBuyTimesJson(objectMapper.writeValueAsString(times));
                            chestManorRepo.save(manor);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[ChestManor] Failed to process buy for roleId={} seq={}", roleId, param1, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("openLevel", manor.getOpenLevel());
        try {
            List<Integer> times = objectMapper.readValue(manor.getBuyTimesJson(), new TypeReference<>() {});
            result.put("buyTimes", times);
        } catch (Exception e) {
            result.put("buyTimes", List.of());
        }
        return result;
    }

    private Map<Integer, JsonNode> getChestManorConfig() {
        Map<Integer, JsonNode> cached = chestManorConfigCache;
        if (cached != null) return cached;
        synchronized (this) {
            if (chestManorConfigCache == null) {
                chestManorConfigCache = loadChestManorConfig();
            }
            return chestManorConfigCache;
        }
    }

    private Map<Integer, JsonNode> loadChestManorConfig() {
        if (configFeign == null) {
            log.error("[ChestManor] configFeign unavailable, using empty config");
            return Map.of();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(CHEST_MANOR_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[ChestManor] empty config body path={}", CHEST_MANOR_CONFIG_PATH);
                return Map.of();
            }
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            Map<Integer, JsonNode> map = new HashMap<>();
            JsonNode reward = root.path("reward");
            if (reward.isArray()) {
                for (JsonNode node : reward) {
                    int seq = node.path("seq").asInt(-1);
                    if (seq >= 0) map.put(seq, node);
                }
            }
            log.info("[ChestManor] loaded {} reward entries from config", map.size());
            return map;
        } catch (Exception e) {
            log.error("[ChestManor] failed to load config path={}", CHEST_MANOR_CONFIG_PATH, e);
            return Map.of();
        }
    }

    // === Type 24: 法阵盛典 (FaZhen Gala) ===
    
    @Transactional
    private Map<String, Object> handleFaZhenGala(Long roleId, int opType, int param1) {
        FaZhenGala gala = faZhenGalaRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return faZhenGalaRepo.save(FaZhenGala.builder()
                        .roleId(roleId)
                        .level(1)
                        .endTimestamp((int) (Instant.now().getEpochSecond() + ACTIVITY_DURATION))
                        .fetchFlag(0)
                        .taskNumJson(emptyArray)
                        .giftNumJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init FaZhenGala", e);
            }
        });

        List<Integer> taskNum = readIntegerList(gala.getTaskNumJson());
        List<Integer> giftNum = readIntegerList(gala.getGiftNumJson());
        boolean dirty = syncFaZhenTaskProgress(roleId, gala, taskNum);

        // client flow: 0=GET_INFO, 1=FETCH_TASK_REWARD(param1=taskId), 2=BUY_OR_FETCH_GIFT(param1=giftSeq)
        switch (opType) {
            case 1 -> {
                if (param1 >= 0) {
                    int bit = 1 << param1;
                    if ((gala.getFetchFlag() & bit) == 0) {
                        gala.setFetchFlag(gala.getFetchFlag() | bit);
                        dirty = true;
                    }
                }
            }
            case 2 -> {
                if (param1 >= 0) {
                    ensureListSize(giftNum, param1 + 1);
                    giftNum.set(param1, giftNum.get(param1) + 1);
                    dirty = true;
                }
            }
            default -> {
                // info only
            }
        }

        if (dirty) {
            try {
                gala.setTaskNumJson(objectMapper.writeValueAsString(taskNum));
                gala.setGiftNumJson(objectMapper.writeValueAsString(giftNum));
                faZhenGalaRepo.save(gala);
            } catch (Exception e) {
                log.error("Failed to persist FaZhenGala state for roleId={}", roleId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", gala.getLevel());
        result.put("endTimestamp", gala.getEndTimestamp());
        result.put("fetchFlag", gala.getFetchFlag());
        result.put("taskNum", taskNum);
        result.put("giftNum", giftNum);
        return result;
    }

    private boolean syncFaZhenTaskProgress(Long roleId, FaZhenGala gala, List<Integer> taskNum) {
        if (angelFeign == null) {
            return false;
        }

        try {
            Map<String, Object> angelData = angelFeign.getAngelData(roleId);
            if (!readBoolean(angelData.get("success"))) {
                return false;
            }

            List<Map<String, Object>> angels = objectMapper.convertValue(
                    angelData.getOrDefault("angels", List.of()), new TypeReference<>() {});
            if (angels.isEmpty()) {
                return false;
            }

            Map<String, Object> primaryAngel = pickPrimaryAngel(angels);
            int fazhenLevel = Math.max(readInt(primaryAngel.get("level")), 1);
            int soulStoneTotal = readInt(primaryAngel.get("skill1Level"))
                    + readInt(primaryAngel.get("skill2Level"))
                    + readInt(primaryAngel.get("skill3Level"))
                    + readInt(primaryAngel.get("skill4Level"));

            boolean dirty = false;
            dirty |= fillRange(taskNum, 0, 9, fazhenLevel);
            dirty |= fillRange(taskNum, 10, 19, soulStoneTotal);
            if (!Objects.equals(gala.getLevel(), fazhenLevel)) {
                gala.setLevel(fazhenLevel);
                dirty = true;
            }
            return dirty;
        } catch (Exception e) {
            log.debug("FaZhenGala progress sync skipped for roleId={}", roleId, e);
            return false;
        }
    }

    private Map<String, Object> pickPrimaryAngel(List<Map<String, Object>> angels) {
        for (Map<String, Object> angel : angels) {
            if (readBoolean(angel.get("isEquipped"))) {
                return angel;
            }
        }
        for (Map<String, Object> angel : angels) {
            if (readBoolean(angel.get("isActive"))) {
                return angel;
            }
        }
        return angels.get(0);
    }

    private boolean fillRange(List<Integer> values, int startIdx, int endIdx, int value) {
        ensureListSize(values, endIdx + 1);
        boolean dirty = false;
        for (int idx = startIdx; idx <= endIdx; idx++) {
            if (!Objects.equals(values.get(idx), value)) {
                values.set(idx, value);
                dirty = true;
            }
        }
        return dirty;
    }

    private List<Integer> readIntegerList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<Integer>>() {}));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void ensureListSize(List<Integer> values, int size) {
        while (values.size() < size) {
            values.add(0);
        }
    }

    private int readInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private boolean readBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    // === Type 25: 星图盛典 (StarMap Gala) ===
    
    @Transactional
    private Map<String, Object> handleStarMapGala(Long roleId, int opType, int param1) {
        StarMapGala gala = starMapGalaRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return starMapGalaRepo.save(StarMapGala.builder()
                        .roleId(roleId)
                        .level(1)
                        .endTimestamp((int) (Instant.now().getEpochSecond() + ACTIVITY_DURATION))
                        .fetchFlag(0)
                        .taskNumJson(emptyArray)
                        .giftNumJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init StarMapGala", e);
            }
        });

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=rewardIndex)
        if (opType == 2) {
            int bit = 1 << param1;
            if ((gala.getFetchFlag() & bit) == 0) {
                gala.setFetchFlag(gala.getFetchFlag() | bit);
                starMapGalaRepo.save(gala);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", gala.getLevel());
        result.put("endTimestamp", gala.getEndTimestamp());
        result.put("fetchFlag", gala.getFetchFlag());
        try {
            List<Integer> taskNum = objectMapper.readValue(gala.getTaskNumJson(), new TypeReference<>() {});
            List<Integer> giftNum = objectMapper.readValue(gala.getGiftNumJson(), new TypeReference<>() {});
            result.put("taskNum", taskNum);
            result.put("giftNum", giftNum);
        } catch (Exception e) {
            result.put("taskNum", List.of());
            result.put("giftNum", List.of());
        }
        return result;
    }

    // === Type 27: 铭文之塔基金 (Rune Tower Fund) ===
    
    @Transactional
    private Map<String, Object> handleRuneTowerFund(Long roleId, int opType, int param1) {
        RuneTowerFund fund = runeTowerFundRepo.findByRoleId(roleId).orElseGet(() ->
                runeTowerFundRepo.save(RuneTowerFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=BUY_PHASE, 3=CLAIM_COMMON, 4=CLAIM_SENIOR
        switch (opType) {
            case 2 -> {
                int bit = 1 << param1;
                if ((fund.getPhaseBuyFlag() & bit) == 0) {
                    fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
                    runeTowerFundRepo.save(fund);
                }
            }
            case 3 -> {
                long bit = 1L << param1;
                if ((fund.getCommonFetchFlag() & bit) == 0) {
                    fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
                    runeTowerFundRepo.save(fund);
                }
            }
            case 4 -> {
                long bit = 1L << param1;
                if ((fund.getSeniorFetchFlag() & bit) == 0) {
                    fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
                    runeTowerFundRepo.save(fund);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    // === Type 28: 超值献礼 (ChaoZhi XianLi - Premium Gift) ===
    
    @Transactional
    private Map<String, Object> handleChaoZhiXianLi(Long roleId, int opType, int param1) {
        ChaoZhiXianLi gift = chaoZhiXianLiRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String zeroArray = objectMapper.writeValueAsString(new ArrayList<>(Collections.nCopies(5, 0)));
                return chaoZhiXianLiRepo.save(ChaoZhiXianLi.builder()
                        .roleId(roleId)
                        .level(1)
                        .buyMark(0)
                        .itemNumJson(zeroArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init ChaoZhiXianLi", e);
            }
        });

        boolean dirty = false;
        if (gift.getBuyMark() == null) {
            gift.setBuyMark(0);
            dirty = true;
        } else if (gift.getBuyMark() > 0 && gift.getBuyMark() != 1) {
            gift.setBuyMark(1);
            dirty = true;
        }

        List<Integer> itemNum;
        try {
            itemNum = new ArrayList<>(objectMapper.readValue(gift.getItemNumJson(), new TypeReference<>() {}));
        } catch (Exception e) {
            itemNum = new ArrayList<>();
            dirty = true;
        }
        while (itemNum.size() < 5) {
            itemNum.add(0);
            dirty = true;
        }

        // client contract: 0=GET_INFO, 1=CLAIM_DAILY_REWARD(param1=seq), 2=MARK_PURCHASED (legacy compatibility)
        if (opType == 1) {
            if (gift.getBuyMark() > 0 && param1 >= 0 && param1 < itemNum.size() && itemNum.get(param1) <= 0) {
                itemNum.set(param1, 1);
                dirty = true;
            }
        } else if (opType == 2 && gift.getBuyMark() == 0) {
            gift.setBuyMark(1);
            dirty = true;
        }

        if (dirty) {
            try {
                gift.setItemNumJson(objectMapper.writeValueAsString(itemNum));
            } catch (Exception e) {
                log.error("Failed to persist ChaoZhiXianLi itemNumJson for roleId={}", roleId, e);
            }
            chaoZhiXianLiRepo.save(gift);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", gift.getLevel());
        result.put("buyMark", gift.getBuyMark() != null && gift.getBuyMark() > 0 ? 1 : 0);
        result.put("itemNum", itemNum);
        return result;
    }

    // === Type 29: 新服比拼 (New Server Competition) ===
    
    @Transactional
    private Map<String, Object> handleNewServerCompetition(Long roleId, int opType, int param1) {
        NewServerCompetition competition = newServerCompetitionRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return newServerCompetitionRepo.save(NewServerCompetition.builder()
                        .roleId(roleId)
                        .fetchFlagJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init NewServerCompetition", e);
            }
        });

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=categoryIndex)
        // Note: fetchFlag is a JSON array, not a simple bitmask

        Map<String, Object> result = new HashMap<>();
        try {
            List<Integer> fetchFlag = objectMapper.readValue(
                    competition.getFetchFlagJson(), new TypeReference<>() {});
            result.put("fetchFlag", fetchFlag);
        } catch (Exception e) {
            result.put("fetchFlag", List.of());
        }
        return result;
    }

    // === Type 30: 周末豪礼 (Weekend HaoLi - Weekend Premium Gift) ===
    
    @Transactional
    private Map<String, Object> handleWeekendHaoLi(Long roleId, int opType, int param1) {
        WeekendHaoLi haoLi = weekendHaoLiRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return weekendHaoLiRepo.save(WeekendHaoLi.builder()
                        .roleId(roleId)
                        .level(1)
                        .buyTimesJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init WeekendHaoLi", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=itemIndex)
        if (opType == 2) {
            try {
                List<Integer> times = objectMapper.readValue(haoLi.getBuyTimesJson(), new TypeReference<>() {});
                while (times.size() <= param1) times.add(0);
                times.set(param1, times.get(param1) + 1);
                haoLi.setBuyTimesJson(objectMapper.writeValueAsString(times));
                weekendHaoLiRepo.save(haoLi);
            } catch (Exception e) {
                log.error("Failed to update buyTimes", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", haoLi.getLevel());
        try {
            List<Integer> times = objectMapper.readValue(haoLi.getBuyTimesJson(), new TypeReference<>() {});
            result.put("buyTimes", times);
        } catch (Exception e) {
            result.put("buyTimes", List.of());
        }
        return result;
    }

    // === Type 32: 连充赠礼 (LianChong ZengLi - Consecutive Recharge Gift) ===
    
    @Transactional
    private Map<String, Object> handleLianChongZengLi(Long roleId, int opType, int param1) {
        LianChongZengLi zengli = lianChongZengLiRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return lianChongZengLiRepo.save(LianChongZengLi.builder()
                        .roleId(roleId)
                        .level(1)
                        .curTaskDay(0)
                        .todayTaskFinish(0)
                        .chongzhiTaskProceedJson(emptyArray)
                        .friendTaskProceedJson(emptyArray)
                        .receiveRewardsFlagJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init LianChongZengLi", e);
            }
        });

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=rewardIndex)

        Map<String, Object> result = new HashMap<>();
        result.put("level", zengli.getLevel());
        result.put("curTaskDay", zengli.getCurTaskDay());
        result.put("todayTaskFinish", zengli.getTodayTaskFinish());
        try {
            List<Integer> chongzhiTask = objectMapper.readValue(
                    zengli.getChongzhiTaskProceedJson(), new TypeReference<>() {});
            List<Integer> friendTask = objectMapper.readValue(
                    zengli.getFriendTaskProceedJson(), new TypeReference<>() {});
            List<Integer> receiveFlags = objectMapper.readValue(
                    zengli.getReceiveRewardsFlagJson(), new TypeReference<>() {});
            result.put("chongzhiTaskProceed", chongzhiTask);
            result.put("friendTaskProceed", friendTask);
            result.put("receiveRewardsFlag", receiveFlags);
        } catch (Exception e) {
            result.put("chongzhiTaskProceed", List.of());
            result.put("friendTaskProceed", List.of());
            result.put("receiveRewardsFlag", List.of());
        }
        return result;
    }

    // === Type 34: 周末连充 (Weekend LianChong - Weekend Consecutive Recharge) ===
    
    @Transactional
    private Map<String, Object> handleWeekendLianChong(Long roleId, int opType) {
        WeekendLianChong lianChong = weekendLianChongRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return weekendLianChongRepo.save(WeekendLianChong.builder()
                        .roleId(roleId)
                        .level(1)
                        .dayChongzhiNum(0L)
                        .chongzhiNumJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init WeekendLianChong", e);
            }
        });

        // opType: 1=GET_INFO, (other ops would update recharge amounts)

        Map<String, Object> result = new HashMap<>();
        result.put("level", lianChong.getLevel());
        result.put("dayChongzhiNum", lianChong.getDayChongzhiNum());
        try {
            List<Long> chongzhiNums = objectMapper.readValue(
                    lianChong.getChongzhiNumJson(), new TypeReference<>() {});
            result.put("chongzhiNum", chongzhiNums);
        } catch (Exception e) {
            result.put("chongzhiNum", List.of());
        }
        return result;
    }

    // === Type 31: 新服比拼全局 (New Server Competition Global) ===
    
    private Map<String, Object> handleNewServerGlobal(int serverId) {
        NewServerGlobal global = newServerGlobalRepo.findByServerId(serverId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return newServerGlobalRepo.save(NewServerGlobal.builder()
                        .serverId(serverId)
                        .endTimeJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init NewServerGlobal", e);
            }
        });

        // opType: 1=GET_INFO (returns global phase end times for all players on server)

        Map<String, Object> result = new HashMap<>();
        try {
            List<Integer> endTimes = objectMapper.readValue(
                    global.getEndTimeJson(), new TypeReference<>() {});
            result.put("endTime", endTimes);
        } catch (Exception e) {
            result.put("endTime", List.of());
        }
        return result;
    }

    // === Type 33: 无限战令 (War Order / Battle Pass) ===
    
    @Transactional
    private Map<String, Object> handleWarOrder(Long roleId, int opType, int param1, int param2) {
        WarOrder warOrder = warOrderRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return warOrderRepo.save(WarOrder.builder()
                        .roleId(roleId)
                        .openLevel(1)
                        .isBuy(0) // 0=free track only
                        .timeSeqTimestamp(0)
                        .level(1)
                        .exp(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .dayTaskFlag(0)
                        .weekTaskFlag(0)
                        .dayTaskNumJson(emptyArray)
                        .weekTaskNumJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init WarOrder", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY_PREMIUM, 3=CLAIM_FREE(param1=level), 4=CLAIM_PREMIUM(param1=level)
        // Note: proto has typo "falg" instead of "flag" — field names match proto

        if (opType == 2) {
            // Buy premium track
            warOrder.setIsBuy(1);
            warOrderRepo.save(warOrder);
        } else if (opType == 3 && param1 > 0) {
            // Claim free track reward
            long bit = 1L << (param1 - 1);
            if ((warOrder.getCommonFetchFlag() & bit) == 0) {
                warOrder.setCommonFetchFlag(warOrder.getCommonFetchFlag() | bit);
                warOrderRepo.save(warOrder);
            }
        } else if (opType == 4 && param1 > 0) {
            // Claim premium track reward
            long bit = 1L << (param1 - 1);
            if ((warOrder.getSeniorFetchFlag() & bit) == 0) {
                warOrder.setSeniorFetchFlag(warOrder.getSeniorFetchFlag() | bit);
                warOrderRepo.save(warOrder);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("openLevel", warOrder.getOpenLevel());
        result.put("isBuy", warOrder.getIsBuy());
        result.put("timeSeqTimestamp", warOrder.getTimeSeqTimestamp());
        result.put("level", warOrder.getLevel());
        result.put("exp", warOrder.getExp());
        result.put("commonFetchFalg", warOrder.getCommonFetchFlag()); // Note: typo "falg" in proto
        result.put("seniorFetchFalg", warOrder.getSeniorFetchFlag()); // Note: typo "falg" in proto
        result.put("dayTaskFalg", warOrder.getDayTaskFlag());
        result.put("weekTaskFalg", warOrder.getWeekTaskFlag());
        try {
            List<Integer> dayTasks = objectMapper.readValue(
                    warOrder.getDayTaskNumJson(), new TypeReference<>() {});
            List<Integer> weekTasks = objectMapper.readValue(
                    warOrder.getWeekTaskNumJson(), new TypeReference<>() {});
            result.put("dayTaskNum", dayTasks);
            result.put("weekTaskNum", weekTasks);
        } catch (Exception e) {
            result.put("dayTaskNum", List.of());
            result.put("weekTaskNum", List.of());
        }
        return result;
    }

    // === Type 35: 广告权益 (Advertisement Equity / Knights Welfare) ===
    
    @Transactional
    private Map<String, Object> handleAdvertisementEquity(Long roleId, int opType) {
        AdvertisementEquity equity = advertisementEquityRepo.findByRoleId(roleId).orElseGet(() ->
            advertisementEquityRepo.save(AdvertisementEquity.builder()
                    .roleId(roleId)
                    .isBuy(0)
                    .fetchFlag(0)
                    .refreshTime(0)
                    .build())
        );

        int now = (int) Instant.now().getEpochSecond();
        boolean dirty = false;

        // Auto-reset when the 12h window has expired
        if (equity.getRefreshTime() > 0 && now > equity.getRefreshTime()) {
            equity.setFetchFlag(0);
            equity.setRefreshTime(0);
            dirty = true;
        }

        // Load knight_card config (cached)
        JsonNode knightCfg = loadKnightCardConfig();
        int configHours = 12;
        if (knightCfg != null && knightCfg.has("knight_card")) {
            JsonNode arr = knightCfg.get("knight_card");
            if (arr.isArray() && arr.size() > 0) {
                configHours = arr.get(0).path("time").asInt(12);
            }
        }

        // client flow: 0=GET_INFO, 1=FETCH_NEXT_REWARD, 2=BUY_SUBSCRIPTION
        // legacy/external flow: 3=FETCH_NEXT_REWARD
        switch (opType) {
            case 1, 3 -> {
                // Start a new cycle if no active window
                if (equity.getRefreshTime() <= now) {
                    equity.setFetchFlag(0);
                    equity.setRefreshTime(now + configHours * 3600);
                    dirty = true;
                }
                int nextSeq = nextKnightCardSeq(equity.getFetchFlag());
                if (nextSeq > 0) {
                    int bit = 1 << nextSeq;
                    if ((equity.getFetchFlag() & bit) == 0) {
                        equity.setFetchFlag(equity.getFetchFlag() | bit);
                        dirty = true;
                        // Grant the reward item for this seq
                        grantKnightCardReward(roleId, nextSeq, knightCfg);
                    }
                }
            }
            case 2 -> {
                if (equity.getIsBuy() != 1) {
                    boolean paid = deductKnightCardPrice(roleId, knightCfg);
                    if (paid) {
                        equity.setIsBuy(1);
                        dirty = true;
                        grantKnightCardFirstBuyReward(roleId, knightCfg);
                    }
                }
            }
            default -> {
                // info only
            }
        }

        if (dirty) {
            advertisementEquityRepo.save(equity);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isBuy", equity.getIsBuy());
        result.put("fetchFlag", equity.getFetchFlag());
        result.put("refreshTime", equity.getRefreshTime());
        return result;
    }

    private int nextKnightCardSeq(int fetchFlag) {
        for (int seq = 1; seq <= 5; seq++) {
            int bit = 1 << seq;
            boolean alreadyFetched = (fetchFlag & bit) != 0;
            boolean previousFetched = seq == 1 || (fetchFlag & (1 << (seq - 1))) != 0;
            if (!alreadyFetched && previousFetched) {
                return seq;
            }
        }
        return 0;
    }

    private JsonNode loadKnightCardConfig() {
        if (knightCardConfigCache != null) {
            return knightCardConfigCache;
        }
        if (configFeign == null) {
            log.warn("[KnightCard] configFeign unavailable");
            return null;
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(KNIGHT_CARD_CONFIG_PATH, null);
            if (response.getBody() == null || response.getBody().length == 0) {
                log.error("[KnightCard] empty config body path={}", KNIGHT_CARD_CONFIG_PATH);
                return null;
            }
            knightCardConfigCache = objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
            return knightCardConfigCache;
        } catch (Exception e) {
            log.error("[KnightCard] failed to load config path={}", KNIGHT_CARD_CONFIG_PATH, e);
            return null;
        }
    }

    /**
     * Looks up the knight_zheng entry matching the player's level and the given seq,
     * then calls bagFeign.grantItems to award the guanggao_item.
     */
    private void grantKnightCardReward(Long roleId, int seq, JsonNode knightCfg) {
        if (bagFeign == null) {
            log.error("[KnightCard] bagFeign unavailable, cannot grant reward roleId={} seq={}", roleId, seq);
            return;
        }
        if (knightCfg == null || !knightCfg.has("knight_zheng")) {
            log.error("[KnightCard] config missing knight_zheng, roleId={} seq={}", roleId, seq);
            return;
        }

        // Fetch player level
        int roleLevel = 1;
        try {
            Optional<RoleDTOs.RoleResp> roleOpt = roleFeign.detail(roleId);
            roleLevel = roleOpt.map(RoleDTOs.RoleResp::getLevel).orElse(1);
        } catch (Exception e) {
            log.warn("[KnightCard] failed to fetch role level roleId={}, defaulting to 1: {}", roleId, e.getMessage());
        }

        // Find the matching knight_zheng entry
        JsonNode zhengArr = knightCfg.get("knight_zheng");
        JsonNode matched = null;
        for (JsonNode entry : zhengArr) {
            int entrySeq = entry.path("seq").asInt(-1);
            int levelMin = entry.path("level_min").asInt(0);
            int levelMax = entry.path("level_max").asInt(0);
            if (entrySeq == seq && roleLevel >= levelMin && roleLevel <= levelMax) {
                matched = entry;
                break;
            }
        }

        if (matched == null) {
            log.error("[KnightCard] no knight_zheng entry for seq={} level={} roleId={}", seq, roleLevel, roleId);
            return;
        }

        List<BagDTOs.GrantItem> items = parseGrantItems(matched.get("guanggao_item"));
        if (items.isEmpty()) {
            log.warn("[KnightCard] guanggao_item is empty for seq={} level={}", seq, roleLevel);
            return;
        }

        try {
            BagDTOs.GrantReq req = new BagDTOs.GrantReq();
            req.setRoleId(String.valueOf(roleId));
            req.setItems(items);
            req.setReason("knight_card_seq" + seq);
            bagFeign.grantItems(req);
            log.info("[KnightCard] granted seq={} level={} items={} roleId={}", seq, roleLevel, items, roleId);
        } catch (Exception e) {
            log.error("[KnightCard] grantItems failed roleId={} seq={}: {}", roleId, seq, e.getMessage());
        }
    }

    /**
     * Deducts the buy_money (diamond) from the player's wallet.
     * Returns true if deduction succeeded (or no price configured), false otherwise.
     */
    private boolean deductKnightCardPrice(Long roleId, JsonNode knightCfg) {
        if (knightCfg == null || !knightCfg.has("knight_card")) {
            return true; // no config = no charge
        }
        JsonNode arr = knightCfg.get("knight_card");
        if (!arr.isArray() || arr.size() == 0) {
            return true;
        }
        int buyMoney = arr.get(0).path("buy_money").asInt(0);
        if (buyMoney <= 0) {
            return true;
        }
        if (walletFeign == null) {
            log.error("[KnightCard] walletFeign unavailable for purchase roleId={}", roleId);
            return false;
        }
        try {
            WalletDTOs.BatchReq req = WalletDTOs.BatchReq.builder()
                    .roleId(String.valueOf(roleId))
                    .changes(List.of(WalletDTOs.Change.builder()
                            .itemId(2L) // 2 = paid_gold (diamond)
                            .amount(-buyMoney)
                            .build()))
                    .reason(302) // 302 = activity purchase
                    .idemKey("knight-card-buy-" + roleId)
                    .build();
            var resp = walletFeign.batchAdd(req);
            boolean ok = resp != null && resp.getData() != null && resp.getData().ok();
            if (!ok) {
                log.warn("[KnightCard] purchase deduct failed roleId={} buyMoney={} resp={}", roleId, buyMoney, resp);
            }
            return ok;
        } catch (Exception e) {
            log.error("[KnightCard] wallet deduct failed roleId={} buyMoney={}: {}", roleId, buyMoney, e.getMessage());
            return false;
        }
    }

    /**
     * Grants the first_buy_reward_item after a successful subscription purchase.
     */
    private void grantKnightCardFirstBuyReward(Long roleId, JsonNode knightCfg) {
        if (knightCfg == null || !knightCfg.has("knight_card")) {
            return;
        }
        JsonNode arr = knightCfg.get("knight_card");
        if (!arr.isArray() || arr.size() == 0) {
            return;
        }
        List<BagDTOs.GrantItem> items = parseGrantItems(arr.get(0).get("first_buy_reward_item"));
        if (items.isEmpty()) {
            return; // config currently has no first_buy rewards
        }
        if (bagFeign == null) {
            log.error("[KnightCard] bagFeign unavailable for first_buy_reward roleId={}", roleId);
            return;
        }
        try {
            BagDTOs.GrantReq req = new BagDTOs.GrantReq();
            req.setRoleId(String.valueOf(roleId));
            req.setItems(items);
            req.setReason("knight_card_first_buy");
            bagFeign.grantItems(req);
            log.info("[KnightCard] granted first_buy_reward items={} roleId={}", items, roleId);
        } catch (Exception e) {
            log.error("[KnightCard] first_buy grantItems failed roleId={}: {}", roleId, e.getMessage());
        }
    }

    // === Type 36: 新服比拼排行榜 (New Server Competition Ranking) ===
    
    @Transactional
    private Map<String, Object> handleNewServerRanking(Long roleId, int opType, int param1) {
        NewServerRanking ranking = newServerRankingRepo.findByRoleId(roleId).orElseGet(() ->
            newServerRankingRepo.save(NewServerRanking.builder()
                    .roleId(roleId)
                    .rankingType(param1) // Category type from param1
                    .myRank(0)
                    .myRankValue(0L)
                    .myBestRank(0)
                    .build())
        );

        // opType: 1=GET_INFO (param1=rankingType: combat power, level, etc.)
        // Note: Real ranking calculation would query all players and sort

        Map<String, Object> result = new HashMap<>();
        result.put("type", ranking.getRankingType());
        result.put("myRank", ranking.getMyRank());
        result.put("myRankValue", ranking.getMyRankValue());
        result.put("myBestRank", ranking.getMyBestRank());
        return result;
    }

    // === Type 37: 神器夺宝 (ShenQi Duobao / Artifact Treasure Hunt) ===
    
    @Transactional
    private Map<String, Object> handleShenqiDuobao(Long roleId, int opType, int param1) {
        ShenqiDuobao duobao = shenqiDuobaoRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return shenqiDuobaoRepo.save(ShenqiDuobao.builder()
                        .roleId(roleId)
                        .roleLevel(1)
                        .tasksJson(emptyArray)
                        .giftsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init ShenqiDuobao", e);
            }
        });

        // opType: 1=GET_INFO, 2=CLAIM_TASK(param1=taskSeq), 3=BUY_GIFT(param1=giftSeq)

        if (opType == 3 && param1 > 0) {
            // Buy gift pack (increment buyTimes for that gift)
            try {
                List<Map<String, Object>> gifts = objectMapper.readValue(
                        duobao.getGiftsJson(), new TypeReference<>() {});
                boolean found = false;
                for (Map<String, Object> g : gifts) {
                    if (g.get("giftSeq") instanceof Number n && n.intValue() == param1) {
                        int buyTimes = g.get("buyTimes") instanceof Number bt ? bt.intValue() : 0;
                        g.put("buyTimes", buyTimes + 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    gifts.add(Map.of("giftSeq", param1, "buyTimes", 1));
                }
                duobao.setGiftsJson(objectMapper.writeValueAsString(gifts));
                shenqiDuobaoRepo.save(duobao);
            } catch (Exception e) {
                log.error("Failed to update gift buyTimes", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", duobao.getRoleLevel());
        try {
            List<Map<String, Object>> tasks = objectMapper.readValue(
                    duobao.getTasksJson(), new TypeReference<>() {});
            List<Map<String, Object>> gifts = objectMapper.readValue(
                    duobao.getGiftsJson(), new TypeReference<>() {});
            result.put("tasks", tasks);
            result.put("gifts", gifts);
        } catch (Exception e) {
            result.put("tasks", List.of());
            result.put("gifts", List.of());
        }
        return result;
    }

    // === Type 38: 天选之礼 (TianXuan Gift / Chosen Gift) ===

    /** Safety cap: no single gift can be purchased more than this many times. */
    private static final int TIANXUAN_BUY_MAX = 99;

    @Transactional
    private Map<String, Object> handleTianxuanGift(Long roleId, int opType, int param1) {
        TianxuanGift gift = tianxuanGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            // [H1] Fetch real roleLevel from role-service on first init
            int realLevel = 1;
            try {
                if (roleFeign != null) {
                    realLevel = roleFeign.detail(roleId)
                            .map(r -> r.getLevel() != null ? r.getLevel() : 1)
                            .orElse(1);
                }
            } catch (Exception e) {
                log.warn("[TianXuan] roleFeign unavailable for roleId={}, defaulting level=1", roleId);
            }
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                int now = (int) (System.currentTimeMillis() / 1000);
                return tianxuanGiftRepo.save(TianxuanGift.builder()
                        .roleId(roleId)
                        .roleLevel(realLevel)
                        .giftOpenTimestamp(now)
                        .giftCloseTimestamp(now + 86400) // 24 hours
                        .giftCdEndTimestamp(0)
                        .hasFetchFreeGift(false)
                        .groupId(1)
                        .accumulatedChongzhiNum(0)
                        .giftsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init TianxuanGift", e);
            }
        });

        int now = (int) (System.currentTimeMillis() / 1000);

        // opType: 0=GET_INFO, 2=FETCH_FREE_GIFT (client), 3=BUY_GIFT (BagHandler bridge), 1=BUY_GIFT (legacy SendAngelReq path)
        if (opType == 2) {
            // [M3] Validate time window before allowing FETCH_FREE_GIFT
            if (gift.getGiftOpenTimestamp() > 0 && gift.getGiftCloseTimestamp() > 0
                    && (now < gift.getGiftOpenTimestamp() || now > gift.getGiftCloseTimestamp())) {
                log.warn("[TianXuan] FETCH_FREE_GIFT outside time window for roleId={}", roleId);
            } else {
                gift.setHasFetchFreeGift(true);
                tianxuanGiftRepo.save(gift);
            }
        } else if ((opType == 3 || opType == 1) && param1 > 0) {
            // [M3] Validate time window before allowing BUY
            if (gift.getGiftOpenTimestamp() > 0 && gift.getGiftCloseTimestamp() > 0
                    && (now < gift.getGiftOpenTimestamp() || now > gift.getGiftCloseTimestamp())) {
                log.warn("[TianXuan] BUY_GIFT outside time window for roleId={} seq={}", roleId, param1);
            } else {
                // [M1] Enforce purchase cap per gift
                try {
                    List<Map<String, Object>> gifts = objectMapper.readValue(
                            gift.getGiftsJson(), new TypeReference<>() {});
                    boolean found = false;
                    for (Map<String, Object> g : gifts) {
                        if (g.get("seq") instanceof Number n && n.intValue() == param1) {
                            int buyNum = g.get("buyNum") instanceof Number bn ? bn.intValue() : 0;
                            if (buyNum >= TIANXUAN_BUY_MAX) {
                                log.warn("[TianXuan] roleId={} already reached buy cap for seq={}", roleId, param1);
                            } else {
                                g.put("buyNum", buyNum + 1);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("seq", param1);
                        entry.put("buyNum", 1);
                        gifts.add(entry);
                    }
                    gift.setGiftsJson(objectMapper.writeValueAsString(gifts));
                    tianxuanGiftRepo.save(gift);
                } catch (Exception e) {
                    log.error("[TianXuan] Failed to update gift buyNum for roleId={} seq={}", roleId, param1, e);
                }
            }
        }

        // [H2] Always return up-to-date accumulatedChongzhiNum from recharge table (not stale DB value)
        long accumulatedChongzhi = rechargeInfoRepo.findByRoleId(roleId)
                .map(RechargeInfo::getHistoryChongzhi)
                .orElse(0L);

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", gift.getRoleLevel());
        result.put("giftOpenTimestamp", gift.getGiftOpenTimestamp());
        result.put("giftCloseTimestamp", gift.getGiftCloseTimestamp());
        result.put("giftCdEndTimestamp", gift.getGiftCdEndTimestamp());
        result.put("hasFetchFreeGift", gift.getHasFetchFreeGift());
        result.put("groupId", gift.getGroupId());
        result.put("accumulatedChongzhiNum", (int) Math.min(accumulatedChongzhi, Integer.MAX_VALUE));
        try {
            List<Map<String, Object>> gifts = objectMapper.readValue(
                    gift.getGiftsJson(), new TypeReference<>() {});
            result.put("gifts", gifts);
        } catch (Exception e) {
            result.put("gifts", List.of());
        }
        return result;
    }

    // === Type 39: 领地礼包 (Territory Gift) ===
    
    @Transactional
    private Map<String, Object> handleTerritoryGift(Long roleId, int opType) {
        TerritoryGift gift = territoryGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            int now = (int) (System.currentTimeMillis() / 1000);
            return territoryGiftRepo.save(TerritoryGift.builder()
                    .roleId(roleId)
                    .buyCount(0)
                    .nowType(1) // Default type
                    .nextTime(now + 3600) // 1 hour refresh
                    .build());
        });

        // opType: 1=GET_INFO, 2=BUY

        if (opType == 2) {
            gift.setBuyCount(gift.getBuyCount() + 1);
            territoryGiftRepo.save(gift);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("buyCount", gift.getBuyCount());
        result.put("nowType", gift.getNowType());
        result.put("nextTime", gift.getNextTime());
        return result;
    }

    // === Type 40: 积分转盘 (Jifen Zhuanpan / Points Wheel) ===
    
    @Transactional
    private Map<String, Object> handleJifenZhuanpan(Long roleId, int opType, int param1) {
        JifenZhuanpanConfig config = getJifenZhuanpanConfig();
        JzpDrawConfig drawConfig = config.drawConfig();

        JifenZhuanpan zhuanpan = jifenZhuanpanRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return jifenZhuanpanRepo.save(JifenZhuanpan.builder()
                        .roleId(roleId)
                        .roleLevel(0)
                        .rewardGroup(1)
                        .timesToBigPrize(Math.max(1, drawConfig.baoDiTimes()))
                        .jifen(0)
                        .rewardSeqsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init JifenZhuanpan", e);
            }
        });

        int roleLevel = getRoleLevel(roleId);
        if (roleLevel <= 0) {
            roleLevel = zhuanpan.getRoleLevel() != null && zhuanpan.getRoleLevel() > 0
                    ? zhuanpan.getRoleLevel() : 1;
        }
        zhuanpan.setRoleLevel(roleLevel);
        zhuanpan.setRewardGroup(resolveRewardGroup(config, roleLevel, zhuanpan.getRewardGroup()));
        if (zhuanpan.getTimesToBigPrize() == null || zhuanpan.getTimesToBigPrize() <= 0) {
            zhuanpan.setTimesToBigPrize(Math.max(1, drawConfig.baoDiTimes()));
        }

        // opType: 0=GET_INFO, 1=DRAW(param1=1|10), 2=CONFIRM/CLEAR_LAST_DRAW
        if (opType == 1 && (param1 == 1 || param1 == 10)) {
            int spins = param1;
            int consume = (spins == 10)
                    ? Math.max(0, drawConfig.tenConsumeScore())
                    : Math.max(0, drawConfig.firstConsumeScore());

            int currentJifen = zhuanpan.getJifen() != null ? zhuanpan.getJifen() : 0;
            if (currentJifen < consume) {
                log.warn("[JifenZhuanpan] insufficient jifen roleId={} jifen={} consume={} spins={}",
                        roleId, currentJifen, consume, spins);
                return buildJifenZhuanpanResult(zhuanpan);
            }

            List<JzpLuckDrawReward> groupRewards = config.rewards().stream()
                    .filter(r -> r.rewardGroup() == zhuanpan.getRewardGroup())
                    .toList();
            List<JzpLuckDrawReward> pityRewards = groupRewards.stream()
                    .filter(r -> r.baoDiId() == 1)
                    .toList();

            int pityCountdown = zhuanpan.getTimesToBigPrize() != null
                    ? zhuanpan.getTimesToBigPrize() : Math.max(1, drawConfig.baoDiTimes());
            int pityReset = Math.max(1, drawConfig.baoDiTimes());

            List<Integer> rewardSeqs = new ArrayList<>(spins);
            List<BagDTOs.GrantItem> grantItems = new ArrayList<>();
            for (int i = 0; i < spins; i++) {
                boolean shouldPity = pityCountdown <= 1;
                JzpLuckDrawReward drawn = shouldPity
                        ? weightedDraw(!pityRewards.isEmpty() ? pityRewards : groupRewards)
                        : weightedDraw(groupRewards);
                if (drawn == null) {
                    continue;
                }

                rewardSeqs.add(drawn.seq());
                if (drawn.rewardItems() != null && !drawn.rewardItems().isEmpty()) {
                    grantItems.addAll(drawn.rewardItems());
                }

                if (drawn.baoDiId() == 1) {
                    pityCountdown = pityReset;
                } else {
                    pityCountdown = Math.max(1, pityCountdown - 1);
                }
            }

            if (!grantItems.isEmpty() && bagFeign != null) {
                try {
                    BagDTOs.GrantReq request = new BagDTOs.GrantReq();
                    request.setRoleId(String.valueOf(roleId));
                    request.setItems(grantItems);
                    request.setReason("jifen_zhuanpan_draw");
                    bagFeign.grantItems(request);
                } catch (Exception e) {
                    log.error("[JifenZhuanpan] grant failed roleId={} rewards={}", roleId, rewardSeqs, e);
                    return buildJifenZhuanpanResult(zhuanpan);
                }
            }

            zhuanpan.setJifen(currentJifen - consume);
            zhuanpan.setTimesToBigPrize(pityCountdown);
            try {
                zhuanpan.setRewardSeqsJson(objectMapper.writeValueAsString(rewardSeqs));
            } catch (Exception e) {
                zhuanpan.setRewardSeqsJson("[]");
            }
            jifenZhuanpanRepo.save(zhuanpan);
        } else if (opType == 2) {
            // Client sends this after showing draw animation/reward popup.
            zhuanpan.setRewardSeqsJson("[]");
            jifenZhuanpanRepo.save(zhuanpan);
        }

        return buildJifenZhuanpanResult(zhuanpan);
    }

    private JifenZhuanpanConfig getJifenZhuanpanConfig() {
        JifenZhuanpanConfig cached = jifenZhuanpanConfigCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (jifenZhuanpanConfigCache == null) {
                jifenZhuanpanConfigCache = loadJifenZhuanpanConfig();
            }
            return jifenZhuanpanConfigCache;
        }
    }

    private JifenZhuanpanConfig loadJifenZhuanpanConfig() {
        if (configFeign == null) {
            log.error("[JifenZhuanpan] configFeign unavailable, using empty config");
            return JifenZhuanpanConfig.empty();
        }
        try {
            ResponseEntity<byte[]> response = configFeign.getFile(JIFEN_ZHUANPAN_CONFIG_PATH, null);
            byte[] body = response != null ? response.getBody() : null;
            if (body == null || body.length == 0) {
                log.error("[JifenZhuanpan] empty config body path={}", JIFEN_ZHUANPAN_CONFIG_PATH);
                return JifenZhuanpanConfig.empty();
            }

            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            List<JzpLevelConfig> levelConfigs = new ArrayList<>();
            List<JzpLuckDrawReward> rewards = new ArrayList<>();

            JsonNode levelConfigNode = root.path("level_configuration");
            if (levelConfigNode.isArray()) {
                for (JsonNode node : levelConfigNode) {
                    levelConfigs.add(new JzpLevelConfig(
                            readInt(node, "start_level"),
                            readInt(node, "end_level"),
                            readInt(node, "reward_group"),
                            readInt(node, "type")
                    ));
                }
            }

            JsonNode rewardNode = root.path("luck_draw_reward");
            if (rewardNode.isArray()) {
                for (JsonNode node : rewardNode) {
                    rewards.add(new JzpLuckDrawReward(
                            readInt(node, "seq"),
                            readInt(node, "reward_group"),
                            readInt(node, "rate"),
                            readInt(node, "bao_di_id"),
                            parseGrantItems(node.get("reward"))
                    ));
                }
            }

            JzpDrawConfig drawConfig = new JzpDrawConfig(10, 100, 70, 1);
            JsonNode drawConfigNode = root.path("luck_draw_configuration");
            if (drawConfigNode.isArray() && !drawConfigNode.isEmpty()) {
                JsonNode first = drawConfigNode.get(0);
                drawConfig = new JzpDrawConfig(
                        readInt(first, "first_consume_score"),
                        readInt(first, "ten_consume_score"),
                        Math.max(1, readInt(first, "bao_di_times")),
                        readInt(first, "can_cumulative_bao_di")
                );
            }

            return new JifenZhuanpanConfig(levelConfigs, rewards, drawConfig);
        } catch (Exception e) {
            log.error("[JifenZhuanpan] failed to load config path={}", JIFEN_ZHUANPAN_CONFIG_PATH, e);
            return JifenZhuanpanConfig.empty();
        }
    }

    private int resolveRewardGroup(JifenZhuanpanConfig config, int roleLevel, Integer fallbackRewardGroup) {
        for (JzpLevelConfig levelConfig : config.levelConfigs()) {
            if (roleLevel >= levelConfig.startLevel() && roleLevel <= levelConfig.endLevel()) {
                return Math.max(1, levelConfig.rewardGroup());
            }
        }
        if (fallbackRewardGroup != null && fallbackRewardGroup > 0) {
            return fallbackRewardGroup;
        }
        return 1;
    }

    private JzpLuckDrawReward weightedDraw(List<JzpLuckDrawReward> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (JzpLuckDrawReward candidate : candidates) {
            totalWeight += Math.max(0, candidate.rate());
        }

        if (totalWeight <= 0) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int cumulative = 0;
        for (JzpLuckDrawReward candidate : candidates) {
            cumulative += Math.max(0, candidate.rate());
            if (roll <= cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private Map<String, Object> buildJifenZhuanpanResult(JifenZhuanpan zhuanpan) {
        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", zhuanpan.getRoleLevel() != null ? zhuanpan.getRoleLevel() : 1);
        result.put("rewardGroup", zhuanpan.getRewardGroup() != null ? zhuanpan.getRewardGroup() : 1);
        result.put("timesToBigPrize", zhuanpan.getTimesToBigPrize() != null ? zhuanpan.getTimesToBigPrize() : 1);
        result.put("jifen", zhuanpan.getJifen() != null ? zhuanpan.getJifen() : 0);
        try {
            List<Integer> rewardSeqs = objectMapper.readValue(
                    zhuanpan.getRewardSeqsJson() != null ? zhuanpan.getRewardSeqsJson() : "[]",
                    new TypeReference<>() {});
            result.put("rewardSeqs", rewardSeqs);
        } catch (Exception e) {
            result.put("rewardSeqs", List.of());
        }
        return result;
    }

    // === Type 41: 个性化礼包 (Customized Gift) ===
    
    /** Duration within which ShouChong activity is open after character creation (seconds). */
    private static final long SHOUCHONG_OPEN_SECONDS = 3L * 86400; // 3 days

    @Transactional
    private Map<String, Object> handleCustomizedGift(Long roleId, int opType, int param1) {
        CustomizedGift gift = customizedGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            // [H1] Fetch real roleLevel from role-service on first init
            int realLevel = 1;
            try {
                if (roleFeign != null) {
                    realLevel = roleFeign.detail(roleId)
                            .map(r -> r.getLevel() != null ? r.getLevel() : 1)
                            .orElse(1);
                }
            } catch (Exception e) {
                log.warn("[ShouChong] roleFeign unavailable for roleId={}, defaulting level=1", roleId);
            }
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return customizedGiftRepo.save(CustomizedGift.builder()
                        .roleId(roleId)
                        .roleLevel(realLevel)
                        .isOpen(true)
                        .hasBuyGift(false)
                        .fetchFlagsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init CustomizedGift", e);
            }
        });

        // opType: 0=GET_INFO (client), 2=BUY (internal bridge), 1=CLAIM (client) | legacy: 3=CLAIM

        if (opType == 2) {
            // BUY: called by internal bridge; mark gift bought and set all rewards to OK_TO_FETCH
            gift.setHasBuyGift(true);
            try {
                List<Integer> flags = objectMapper.readValue(gift.getFetchFlagsJson(), new TypeReference<>() {});
                // Upgrade CAN_NOT_FETCH (0) to OK_TO_FETCH (1) for all existing entries
                for (int i = 0; i < flags.size(); i++) {
                    if (flags.get(i) == 0) flags.set(i, 1);
                }
                // Ensure at least 5 entries (matches shouchongzhuanshu config gift_configure count)
                while (flags.size() < 5) flags.add(1);
                gift.setFetchFlagsJson(objectMapper.writeValueAsString(flags));
            } catch (Exception e) {
                log.error("[ShouChong] Failed to init fetchFlags on buy for roleId={}", roleId, e);
            }
            customizedGiftRepo.save(gift);
        } else if ((opType == 3 || opType == 1) && param1 >= 0) {
            // [M2] Validate 3-day creation window before allowing CLAIM
            boolean withinWindow = true;
            try {
                if (roleFeign != null) {
                    long createTime = roleFeign.detail(roleId)
                            .map(r -> r.getCreateTimeEpochSec() != null ? r.getCreateTimeEpochSec() : 0L)
                            .orElse(0L);
                    if (createTime > 0) {
                        long now = System.currentTimeMillis() / 1000;
                        withinWindow = (now - createTime) <= SHOUCHONG_OPEN_SECONDS;
                    }
                }
            } catch (Exception e) {
                log.warn("[ShouChong] roleFeign unavailable for 3-day check, roleId={}, allowing claim", roleId);
            }
            if (!withinWindow) {
                log.warn("[ShouChong] roleId={} is outside 3-day window, reject CLAIM for index={}", roleId, param1);
            } else {
                // CLAIM: set per-reward state at param1 index to FETCHED (2)
                try {
                    List<Integer> flags = objectMapper.readValue(gift.getFetchFlagsJson(), new TypeReference<>() {});
                    // Extend array if needed
                    while (flags.size() <= param1) flags.add(gift.getHasBuyGift() ? 1 : 0);
                    if (flags.get(param1) == 1) flags.set(param1, 2);
                    gift.setFetchFlagsJson(objectMapper.writeValueAsString(flags));
                    customizedGiftRepo.save(gift);
                } catch (Exception e) {
                    log.error("[ShouChong] Failed to update fetchFlags for roleId={} index={}", roleId, param1, e);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", gift.getRoleLevel());
        result.put("isOpen", gift.getIsOpen());
        result.put("hasBuyGift", gift.getHasBuyGift());
        try {
            List<Integer> flags = objectMapper.readValue(gift.getFetchFlagsJson(), new TypeReference<>() {});
            // Ensure hasBuyGift state is reflected: any 0-state becomes 1 if bought
            if (gift.getHasBuyGift()) {
                for (int i = 0; i < flags.size(); i++) {
                    if (flags.get(i) == 0) flags.set(i, 1);
                }
            }
            result.put("fetchFlags", flags);
        } catch (Exception e) {
            result.put("fetchFlags", List.of());
        }
        return result;
    }

    // === Type 42: 专属礼包 (Exclusive Gift) ===
    
    @Transactional
    private Map<String, Object> handleExclusiveGift(Long roleId, int opType, int param1) {
        ExclusiveGift gift = exclusiveGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return exclusiveGiftRepo.save(ExclusiveGift.builder()
                        .roleId(roleId)
                        .giftsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init ExclusiveGift", e);
            }
        });

        // opType: 0/1=GET_INFO, 2=BUY(param1=seq)

        int now = (int) (System.currentTimeMillis() / 1000);
        int defaultEndTimestamp = now + 86400; // 24 hours
        List<Map<String, Object>> gifts = new ArrayList<>();
        try {
            gifts = objectMapper.readValue(gift.getGiftsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[ExclusiveGift] roleId={} parse giftsJson failed, reset to defaults: {}", roleId, e.getMessage());
        }

        if (gifts.isEmpty()) {
            gifts.add(new HashMap<>(Map.of(
                    "seq", 0,
                    "alreadyBuyTimes", 0,
                    "endTimestamp", defaultEndTimestamp
            )));
            gifts.add(new HashMap<>(Map.of(
                    "seq", 1,
                    "alreadyBuyTimes", 0,
                    "endTimestamp", defaultEndTimestamp
            )));
        }

        if (opType == 2 && param1 >= 0) {
            // Buy exclusive gift
            try {
                boolean found = false;
                for (Map<String, Object> g : gifts) {
                    if (g.get("seq") instanceof Number n && n.intValue() == param1) {
                        int buyTimes = g.get("alreadyBuyTimes") instanceof Number bt ? bt.intValue() : 0;
                        g.put("alreadyBuyTimes", buyTimes + 1);
                        if (!(g.get("endTimestamp") instanceof Number)) {
                            g.put("endTimestamp", defaultEndTimestamp);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    gifts.add(Map.of(
                            "seq", param1,
                            "alreadyBuyTimes", 1,
                            "endTimestamp", defaultEndTimestamp
                    ));
                }
                gift.setGiftsJson(objectMapper.writeValueAsString(gifts));
                exclusiveGiftRepo.save(gift);
            } catch (Exception e) {
                log.error("Failed to update exclusive gift", e);
            }
        } else {
            try {
                gift.setGiftsJson(objectMapper.writeValueAsString(gifts));
                exclusiveGiftRepo.save(gift);
            } catch (Exception e) {
                log.warn("[ExclusiveGift] roleId={} failed to persist default gifts: {}", roleId, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> savedGifts = objectMapper.readValue(
                    gift.getGiftsJson(), new TypeReference<>() {});
            result.put("gifts", savedGifts);
        } catch (Exception e) {
            result.put("gifts", List.of());
        }
        return result;
    }

    // === Type 43: 钓鱼小游戏 (Fish Game) ===

    @Transactional
    private Map<String, Object> handleFishGame(Long roleId, int opType, int param1) {
        FishGame fish = fishGameRepo.findByRoleId(roleId).orElseGet(() -> {
            FishGame n = new FishGame();
            n.setRoleId(roleId);
            return fishGameRepo.save(n);
        });

        // Reset daily attempts if date changed
        LocalDate today = LocalDate.now();
        if (!today.equals(fish.getLastAttemptDate())) {
            fish.setDailyAttempts(0);
            fish.setLastAttemptDate(today);
        }

        int maxDaily = 10;
        // opType: 1=GET_INFO, 2=CAST_ROD, 3=CLAIM_REWARD(param1=rewardTier)
        if (opType == 2 && fish.getDailyAttempts() < maxDaily) {
            int caught = ThreadLocalRandom.current().nextInt(1, 5);
            fish.setFishCount(fish.getFishCount() + caught);
            fish.setTotalFishCaught(fish.getTotalFishCaught() + caught);
            fish.setDailyAttempts(fish.getDailyAttempts() + 1);
            fishGameRepo.save(fish);
        } else if (opType == 3 && param1 > 0) {
            long bit = 1L << (param1 - 1);
            if ((fish.getFetchFlag() & bit) == 0) {
                fish.setFetchFlag(fish.getFetchFlag() | bit);
                fishGameRepo.save(fish);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fishCount", fish.getFishCount());
        result.put("dailyAttempts", fish.getDailyAttempts());
        result.put("maxDaily", maxDaily);
        result.put("totalFishCaught", fish.getTotalFishCaught());
        result.put("fetchFlag", fish.getFetchFlag());
        return result;
    }

    // === Type 44: 循环矿坑 (Loop Mine) ===

    @Transactional
    private Map<String, Object> handleLoopMine(Long roleId, int opType, int param1) {
        LoopMine mine = loopMineRepo.findByRoleId(roleId).orElseGet(() -> {
            LoopMine n = new LoopMine();
            n.setRoleId(roleId);
            return loopMineRepo.save(n);
        });

        LocalDate today = LocalDate.now();
        if (!today.equals(mine.getLastAttemptDate())) {
            mine.setDailyAttempts(0);
            mine.setLastAttemptDate(today);
        }

        int maxDaily = 5;
        int orePerMine = mine.getMineLevel() * 2;
        int oreToComplete = 100;

        // opType: 1=GET_INFO, 2=MINE, 3=CLAIM_CYCLE_REWARD
        if (opType == 2 && mine.getDailyAttempts() < maxDaily) {
            mine.setOreCount(mine.getOreCount() + orePerMine);
            mine.setDailyAttempts(mine.getDailyAttempts() + 1);
            if (mine.getOreCount() >= oreToComplete) {
                mine.setOreCount(mine.getOreCount() - oreToComplete);
                mine.setCycleCount(mine.getCycleCount() + 1);
            }
            loopMineRepo.save(mine);
        } else if (opType == 3 && param1 > 0) {
            long bit = 1L << (param1 - 1);
            if ((mine.getFetchFlag() & bit) == 0) {
                mine.setFetchFlag(mine.getFetchFlag() | bit);
                loopMineRepo.save(mine);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mineLevel", mine.getMineLevel());
        result.put("oreCount", mine.getOreCount());
        result.put("cycleCount", mine.getCycleCount());
        result.put("dailyAttempts", mine.getDailyAttempts());
        result.put("maxDaily", maxDaily);
        result.put("oreToComplete", oreToComplete);
        result.put("fetchFlag", mine.getFetchFlag());
        return result;
    }

    // === Type 45: 核心危机 (Core Crisis) ===

    @Transactional
    private Map<String, Object> handleCoreCrisis(Long roleId, int opType, int param1) {
        CoreCrisisGame crisis = coreCrisisGameRepo.findByRoleId(roleId).orElseGet(() -> {
            CoreCrisisGame n = new CoreCrisisGame();
            n.setRoleId(roleId);
            n.setEndTimestamp((int) (System.currentTimeMillis() / 1000) + 7 * 86400);
            return coreCrisisGameRepo.save(n);
        });

        int maxDaily = 3;
        // opType: 1=GET_INFO, 2=ATTACK(param1=dmg), 3=CLAIM_REWARD(param1=tier), 4=BUY
        if (opType == 2 && crisis.getDailyAttempts() < maxDaily) {
            int dmg = Math.max(1, param1);
            crisis.setTotalDamage(crisis.getTotalDamage() + dmg);
            crisis.setDailyAttempts(crisis.getDailyAttempts() + 1);
            // Advance stage every 1000 damage
            crisis.setStage((int) (crisis.getTotalDamage() / 1000) + 1);
            coreCrisisGameRepo.save(crisis);
        } else if (opType == 3 && param1 > 0) {
            long bit = 1L << (param1 - 1);
            if ((crisis.getFetchFlag() & bit) == 0) {
                crisis.setFetchFlag(crisis.getFetchFlag() | bit);
                coreCrisisGameRepo.save(crisis);
            }
        } else if (opType == 4) {
            crisis.setIsBuy(1);
            coreCrisisGameRepo.save(crisis);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stage", crisis.getStage());
        result.put("totalDamage", crisis.getTotalDamage());
        result.put("dailyAttempts", crisis.getDailyAttempts());
        result.put("maxDaily", maxDaily);
        result.put("fetchFlag", crisis.getFetchFlag());
        result.put("endTimestamp", crisis.getEndTimestamp());
        result.put("isBuy", crisis.getIsBuy());
        return result;
    }

    // === Type 46: 填字谜 (Fill Blank) ===

    @Transactional
    private Map<String, Object> handleFillBlank(Long roleId, int opType, int param1) {
        final int blanksPerPuzzle = 8;
        FillBlank fb = fillBlankRepo.findByRoleId(roleId).orElseGet(() -> {
            FillBlank n = new FillBlank();
            n.setRoleId(roleId);
            return fillBlankRepo.save(n);
        });

        // opType: 1=GET_INFO, 2=FILL(param1=blankIndex), 3=CLAIM_REWARD(param1=tier), 4=USE_HINT
        if (opType == 2 && param1 >= 0 && param1 < blanksPerPuzzle) {
            long bit = 1L << param1;
            fb.setFilledMask(fb.getFilledMask() | bit);
            // Check if puzzle complete (assume 8 blanks)
            if (Long.bitCount(fb.getFilledMask()) >= blanksPerPuzzle) {
                fb.setCompletedCount(fb.getCompletedCount() + 1);
                fb.setPuzzleIndex(fb.getPuzzleIndex() + 1);
                fb.setFilledMask(0L);
            }
            fillBlankRepo.save(fb);
        } else if (opType == 3 && param1 > 0 && param1 <= Long.SIZE) {
            long bit = 1L << (param1 - 1);
            if ((fb.getFetchFlag() & bit) == 0) {
                fb.setFetchFlag(fb.getFetchFlag() | bit);
                fillBlankRepo.save(fb);
            }
        } else if (opType == 4 && fb.getHintCount() > 0) {
            fb.setHintCount(fb.getHintCount() - 1);
            fillBlankRepo.save(fb);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("puzzleIndex", fb.getPuzzleIndex());
        result.put("filledMask", fb.getFilledMask());
        result.put("completedCount", fb.getCompletedCount());
        result.put("fetchFlag", fb.getFetchFlag());
        result.put("hintCount", fb.getHintCount());
        return result;
    }

    // === Type 47: 命相/星象 (Ming Xiang) ===

    @Transactional
    private Map<String, Object> handleMingXiang(Long roleId, int opType, int param1) {
        MingXiang mx = mingXiangRepo.findByRoleId(roleId).orElseGet(() -> {
            MingXiang n = new MingXiang();
            n.setRoleId(roleId);
            // Assign fate sign based on roleId
            n.setSignIndex((int) (roleId % 12) + 1);
            return mingXiangRepo.save(n);
        });

        long now = System.currentTimeMillis() / 1000;
        long cooldown = 8 * 3600; // 8h cooldown between divinations
        // opType: 1=GET_INFO, 2=DIVINE, 3=UPGRADE(param1=targetLevel), 4=CLAIM_REWARD(param1=tier)
        if (opType == 2 && (now - mx.getLastDivinationTime()) >= cooldown) {
            mx.setDivinationCount(mx.getDivinationCount() + 1);
            mx.setLastDivinationTime(now);
            mingXiangRepo.save(mx);
        } else if (opType == 3 && param1 > mx.getFortuneLevel()) {
            mx.setFortuneLevel(param1);
            mingXiangRepo.save(mx);
        } else if (opType == 4 && param1 > 0) {
            long bit = 1L << (param1 - 1);
            if ((mx.getFetchFlag() & bit) == 0) {
                mx.setFetchFlag(mx.getFetchFlag() | bit);
                mingXiangRepo.save(mx);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signIndex", mx.getSignIndex());
        result.put("fortuneLevel", mx.getFortuneLevel());
        result.put("divinationCount", mx.getDivinationCount());
        result.put("fetchFlag", mx.getFetchFlag());
        result.put("lastDivinationTime", mx.getLastDivinationTime());
        result.put("nextDivinationTime", mx.getLastDivinationTime() + cooldown);
        return result;
    }

    // ===== Recharge Config (CS:3004 → SC:3005) =====
    public Map<String, Object> getRechargeConfig(int currency, String spid) {
        // currency: 0=RMB, 1=TWD, 2=USD
        record Pkg(int seq, int moneyShow, int addGold, int extraRewardType, int extraReward) {}
        List<Pkg> pkgs = switch (currency) {
            case 1 -> List.of( // TWD
                new Pkg(0,  180,  60, 0,    0),
                new Pkg(1,  900, 300, 1,   30),
                new Pkg(2, 2980, 980, 1,   98),
                new Pkg(3, 9980,3280, 1,  328),
                new Pkg(4,19800,6480, 1,  648),
                new Pkg(5,59800,19980,1, 1998)
            );
            case 2 -> List.of( // USD
                new Pkg(0,   99,  60, 0,    0),
                new Pkg(1,  499, 300, 1,   30),
                new Pkg(2, 1499, 980, 1,   98),
                new Pkg(3, 4999,3280, 1,  328),
                new Pkg(4, 9999,6480, 1,  648),
                new Pkg(5,29999,19980,1, 1998)
            );
            default -> List.of( // RMB (0)
                new Pkg(0,   600,  60, 0,    0),
                new Pkg(1,  3000, 300, 1,   30),
                new Pkg(2,  9800, 980, 1,   98),
                new Pkg(3, 32800,3280, 1,  328),
                new Pkg(4, 64800,6480, 1,  648),
                new Pkg(5,199800,19980,1, 1998)
            );
        };
        List<Map<String, Object>> infoList = new ArrayList<>();
        for (Pkg p : pkgs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("seq", p.seq());
            m.put("moneyShow", p.moneyShow());
            m.put("addGold", p.addGold());
            m.put("extraRewardType", p.extraRewardType());
            m.put("extraReward", p.extraReward());
            m.put("descriptionFirstChongzhi", 0);
            infoList.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currencyType", currency);
        result.put("infoCount", pkgs.size());
        result.put("infoList", infoList);
        return result;
    }

    // ===== Advertisement Reward =====
    public Map<String, Object> claimAdReward(Long roleId, int adSeq, boolean isDiamond) {
        log.info("[Advertisement] roleId={}, adSeq={}, isDiamond={}", roleId, adSeq, isDiamond);
        long rewardAmount = isDiamond ? 10 : 1000;
        // Virtual currency itemId: 2=paid_gold(diamond), 1=gold
        long currencyItemId = isDiamond ? 2L : 1L;
        if (walletFeign != null) {
            try {
                WalletDTOs.BatchReq req = WalletDTOs.BatchReq.builder()
                        .roleId(String.valueOf(roleId))
                        .changes(List.of(WalletDTOs.Change.builder()
                                .itemId(currencyItemId)
                                .amount(rewardAmount)
                                .build()))
                        .reason(301) // 301 = ad reward
                        .idemKey("ad-" + roleId + "-" + adSeq)
                        .build();
                walletFeign.batchAdd(req);
                log.info("[Advertisement] Credited {} {} to roleId={}", rewardAmount, isDiamond ? "diamond" : "gold", roleId);
            } catch (Exception e) {
                log.warn("[Advertisement] Failed to credit wallet for roleId={}: {}", roleId, e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("adSeq", adSeq);
        result.put("rewardType", isDiamond ? "diamond" : "gold");
        result.put("rewardAmount", rewardAmount);
        return result;
    }

    // ===== List Active Activities =====
    public Map<String, Object> listActiveActivities() {
        long now = Instant.now().getEpochSecond();
        List<String> activeList = new ArrayList<>();
        activeList.add("sevenday");
        activeList.add("luck");
        activeList.add("newarea");
        activeList.add("market");
        activeList.add("duobao");
        activeList.add("rand");
        activeList.add("ad-reward");
        Map<String, Object> result = new HashMap<>();
        result.put("activities", activeList);
        result.put("count", activeList.size());
        result.put("timestamp", now);
        return result;
    }
}

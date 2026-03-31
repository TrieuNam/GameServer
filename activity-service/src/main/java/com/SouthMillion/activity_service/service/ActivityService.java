package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.*;
import com.SouthMillion.activity_service.repository.*;
import com.SouthMillion.activity_service.repository.FishGameRepository;
import com.SouthMillion.activity_service.repository.LoopMineRepository;
import com.SouthMillion.activity_service.repository.CoreCrisisGameRepository;
import com.SouthMillion.activity_service.repository.FillBlankRepository;
import com.SouthMillion.activity_service.repository.MingXiangRepository;
import com.SouthMillion.activity_service.client.WalletFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final long ACTIVITY_DURATION = 7L * 24 * 3600; // 7 days in seconds

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
        log.info("[RandActivity] roleId={}, actType={}, op={}, p1={}", roleId, activityType, operaType, param1);
        
        return switch (activityType) {
            case 1  -> handleRechargeInfo(roleId, operaType, param1, param2);
            case 10 -> handleBoxFund(roleId, operaType, param1);
            case 11 -> handleLevelFund(roleId, operaType, param1);
            case 12 -> handleFirstRecharge(roleId, operaType);
            case 13 -> handleAccumulatedRecharge(roleId, operaType, param1);
            case 14 -> handleDailyGift(roleId, operaType, param1);
            case 15 -> handleCommodityGuild(roleId, operaType, param1);
            case 16 -> handleMonthCard(roleId, operaType, param1, param2);
            case 17 -> handleLuckCourtesy(roleId, operaType, param1, param2);
            case 18 -> handleWeekendRecharge(roleId, operaType, param1);
            case 19 -> handleCaveLoot(roleId, operaType, param1);
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
    
    @Transactional
    private Map<String, Object> handleAccumulatedRecharge(Long roleId, int opType, int param1) {
        AccumulatedRecharge acc = accumulatedRechargeRepo.findByRoleId(roleId).orElseGet(() ->
                accumulatedRechargeRepo.save(AccumulatedRecharge.builder()
                        .roleId(roleId)
                        .fetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=CLAIM_MILESTONE (param1=milestone seq)
        if (opType == 2) {
            long bit = 1L << param1;
            if ((acc.getFetchFlag() & bit) == 0) {
                acc.setFetchFlag(acc.getFetchFlag() | bit);
                accumulatedRechargeRepo.save(acc);
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
    private Map<String, Object> handleBoxFund(Long roleId, int opType, int param1) {
        BoxFund fund = boxFundRepo.findByRoleId(roleId).orElseGet(() ->
                boxFundRepo.save(BoxFund.builder()
                        .roleId(roleId)
                        .phaseBuyFlag(0)
                        .commonFetchFlag(0L)
                        .seniorFetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=BUY_PHASE (param1=phase), 3=CLAIM_COMMON (param1=seq), 4=CLAIM_SENIOR (param1=seq)
        switch (opType) {
            case 2 -> { // BUY_PHASE
                int bit = 1 << param1;
                if ((fund.getPhaseBuyFlag() & bit) == 0) {
                    fund.setPhaseBuyFlag(fund.getPhaseBuyFlag() | bit);
                    boxFundRepo.save(fund);
                }
            }
            case 3 -> { // CLAIM_COMMON
                long bit = 1L << param1;
                if ((fund.getCommonFetchFlag() & bit) == 0) {
                    fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
                    boxFundRepo.save(fund);
                }
            }
            case 4 -> { // CLAIM_SENIOR
                long bit = 1L << param1;
                if ((fund.getSeniorFetchFlag() & bit) == 0) {
                    fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
                    boxFundRepo.save(fund);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
    }

    // === Type 11: 等级基金 (Level Fund) ===
    
    @Transactional
    private Map<String, Object> handleLevelFund(Long roleId, int opType, int param1) {
        LevelFund fund = levelFundRepo.findByRoleId(roleId).orElseGet(() ->
                levelFundRepo.save(LevelFund.builder()
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
                    levelFundRepo.save(fund);
                }
            }
            case 3 -> {
                long bit = 1L << param1;
                if ((fund.getCommonFetchFlag() & bit) == 0) {
                    fund.setCommonFetchFlag(fund.getCommonFetchFlag() | bit);
                    levelFundRepo.save(fund);
                }
            }
            case 4 -> {
                long bit = 1L << param1;
                if ((fund.getSeniorFetchFlag() & bit) == 0) {
                    fund.setSeniorFetchFlag(fund.getSeniorFetchFlag() | bit);
                    levelFundRepo.save(fund);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("phaseBuyFlag", fund.getPhaseBuyFlag());
        result.put("commonFetchFlag", fund.getCommonFetchFlag());
        result.put("seniorFetchFlag", fund.getSeniorFetchFlag());
        return result;
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
                        .build()));

        // opType: 1=GET_INFO, 2=BUY(param1=itemIndex)
        if (opType == 2) {
            long bit = 1L << param1;
            if ((gift.getBuyFlag() & bit) == 0) {
                gift.setBuyFlag(gift.getBuyFlag() | bit);
                dailyGiftRepo.save(gift);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("buyFlag", gift.getBuyFlag());
        return result;
    }

    // === Type 19: 山洞夺宝 (Cave Loot) ===
    
    @Transactional
    private Map<String, Object> handleCaveLoot(Long roleId, int opType, int param1) {
        CaveLoot loot = caveLootRepo.findByRoleId(roleId).orElseGet(() ->
                caveLootRepo.save(CaveLoot.builder()
                        .roleId(roleId)
                        .freeNum(0)
                        .chongzhiFetchFlag(0)
                        .taskFetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=LOTTERY, 3=CLAIM_RECHARGE_REWARD, 4=CLAIM_TASK_REWARD
        switch (opType) {
            case 2 -> {
                loot.setFreeNum(loot.getFreeNum() + 1);
                caveLootRepo.save(loot);
            }
            case 3 -> {
                int bit = 1 << param1;
                if ((loot.getChongzhiFetchFlag() & bit) == 0) {
                    loot.setChongzhiFetchFlag(loot.getChongzhiFetchFlag() | bit);
                    caveLootRepo.save(loot);
                }
            }
            case 4 -> {
                long bit = 1L << param1;
                if ((loot.getTaskFetchFlag() & bit) == 0) {
                    loot.setTaskFetchFlag(loot.getTaskFetchFlag() | bit);
                    caveLootRepo.save(loot);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("freeNum", loot.getFreeNum());
        result.put("chongzhiFetchFlag", loot.getChongzhiFetchFlag());
        result.put("taskFetchFlag", loot.getTaskFetchFlag());
        return result;
    }

    // === Type 20: 好友邀请 (Friend Invite) ===
    
    @Transactional
    private Map<String, Object> handleFriendInvite(Long roleId, int opType, int param1) {
        FriendInvite invite = friendInviteRepo.findByRoleId(roleId).orElseGet(() ->
                friendInviteRepo.save(FriendInvite.builder()
                        .roleId(roleId)
                        .inviteCount(0)
                        .fetchFlag(0L)
                        .build()));

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=milestoneIndex)
        if (opType == 2) {
            long bit = 1L << param1;
            if ((invite.getFetchFlag() & bit) == 0) {
                invite.setFetchFlag(invite.getFetchFlag() | bit);
                friendInviteRepo.save(invite);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inviteCount", invite.getInviteCount());
        result.put("fetchFlag", invite.getFetchFlag());
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
                        .curDiscount(100)
                        .openLevel(1)
                        .purchasedTimesJson(emptyTimes)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init CommodityGuild", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=itemIndex)
        if (opType == 2) {
            try {
                List<Integer> times = objectMapper.readValue(guild.getPurchasedTimesJson(), new TypeReference<>() {});
                while (times.size() <= param1) times.add(0);
                times.set(param1, times.get(param1) + 1);
                guild.setPurchasedTimesJson(objectMapper.writeValueAsString(times));
                commodityGuildRepo.save(guild);
            } catch (Exception e) {
                log.error("Failed to update purchasedTimes", e);
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

        // opType: 1=GET_INFO, 2=BUY(param1=chestIndex)
        if (opType == 2) {
            try {
                List<Integer> times = objectMapper.readValue(manor.getBuyTimesJson(), new TypeReference<>() {});
                while (times.size() <= param1) times.add(0);
                times.set(param1, times.get(param1) + 1);
                manor.setBuyTimesJson(objectMapper.writeValueAsString(times));
                chestManorRepo.save(manor);
            } catch (Exception e) {
                log.error("Failed to update buyTimes", e);
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

        // opType: 1=GET_INFO, 2=CLAIM_REWARD(param1=rewardIndex)
        if (opType == 2) {
            int bit = 1 << param1;
            if ((gala.getFetchFlag() & bit) == 0) {
                gala.setFetchFlag(gala.getFetchFlag() | bit);
                faZhenGalaRepo.save(gala);
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
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return chaoZhiXianLiRepo.save(ChaoZhiXianLi.builder()
                        .roleId(roleId)
                        .level(1)
                        .buyMark(0)
                        .itemNumJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init ChaoZhiXianLi", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=itemIndex)
        if (opType == 2) {
            int bit = 1 << param1;
            if ((gift.getBuyMark() & bit) == 0) {
                gift.setBuyMark(gift.getBuyMark() | bit);
                chaoZhiXianLiRepo.save(gift);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("level", gift.getLevel());
        result.put("buyMark", gift.getBuyMark());
        try {
            List<Integer> itemNum = objectMapper.readValue(gift.getItemNumJson(), new TypeReference<>() {});
            result.put("itemNum", itemNum);
        } catch (Exception e) {
            result.put("itemNum", List.of());
        }
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
                    .isBuy(0) // 0=not purchased
                    .fetchFlag(0)
                    .refreshTime(0)
                    .build())
        );

        // opType: 1=GET_INFO, 2=BUY_SUBSCRIPTION, 3=CLAIM_DAILY_REWARD(param1=rewardIndex)

        if (opType == 2) {
            // Purchase subscription
            equity.setIsBuy(1);
            advertisementEquityRepo.save(equity);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isBuy", equity.getIsBuy());
        result.put("fetchFlag", equity.getFetchFlag());
        result.put("refreshTime", equity.getRefreshTime());
        return result;
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
    
    @Transactional
    private Map<String, Object> handleTianxuanGift(Long roleId, int opType, int param1) {
        TianxuanGift gift = tianxuanGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                int now = (int) (System.currentTimeMillis() / 1000);
                return tianxuanGiftRepo.save(TianxuanGift.builder()
                        .roleId(roleId)
                        .roleLevel(1)
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

        // opType: 1=GET_INFO, 2=CLAIM_FREE, 3=BUY(param1=seq)

        if (opType == 2) {
            gift.setHasFetchFreeGift(true);
            tianxuanGiftRepo.save(gift);
        } else if (opType == 3 && param1 > 0) {
            // Buy gift
            try {
                List<Map<String, Object>> gifts = objectMapper.readValue(
                        gift.getGiftsJson(), new TypeReference<>() {});
                boolean found = false;
                for (Map<String, Object> g : gifts) {
                    if (g.get("seq") instanceof Number n && n.intValue() == param1) {
                        int buyNum = g.get("buyNum") instanceof Number bn ? bn.intValue() : 0;
                        g.put("buyNum", buyNum + 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    gifts.add(Map.of("seq", param1, "buyNum", 1));
                }
                gift.setGiftsJson(objectMapper.writeValueAsString(gifts));
                tianxuanGiftRepo.save(gift);
            } catch (Exception e) {
                log.error("Failed to update gift buyNum", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", gift.getRoleLevel());
        result.put("giftOpenTimestamp", gift.getGiftOpenTimestamp());
        result.put("giftCloseTimestamp", gift.getGiftCloseTimestamp());
        result.put("giftCdEndTimestamp", gift.getGiftCdEndTimestamp());
        result.put("hasFetchFreeGift", gift.getHasFetchFreeGift());
        result.put("groupId", gift.getGroupId());
        result.put("accumulatedChongzhiNum", gift.getAccumulatedChongzhiNum());
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
        JifenZhuanpan zhuanpan = jifenZhuanpanRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return jifenZhuanpanRepo.save(JifenZhuanpan.builder()
                        .roleId(roleId)
                        .roleLevel(1)
                        .rewardGroup(1)
                        .timesToBigPrize(10) // 10 spins until guaranteed big prize
                        .jifen(0)
                        .rewardSeqsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init JifenZhuanpan", e);
            }
        });

        // opType: 1=GET_INFO, 2=SPIN(param1=spinType: 1=single, 10=multi)

        if (opType == 2) {
            // Spin wheel
            int spins = (param1 == 10) ? 10 : 1;
            int newJifen = zhuanpan.getJifen() + (spins * 10); // 10 points per spin
            int newTimes = Math.max(0, zhuanpan.getTimesToBigPrize() - spins);
            zhuanpan.setJifen(newJifen);
            zhuanpan.setTimesToBigPrize(newTimes);
            jifenZhuanpanRepo.save(zhuanpan);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", zhuanpan.getRoleLevel());
        result.put("rewardGroup", zhuanpan.getRewardGroup());
        result.put("timesToBigPrize", zhuanpan.getTimesToBigPrize());
        result.put("jifen", zhuanpan.getJifen());
        try {
            List<Integer> rewardSeqs = objectMapper.readValue(
                    zhuanpan.getRewardSeqsJson(), new TypeReference<>() {});
            result.put("rewardSeqs", rewardSeqs);
        } catch (Exception e) {
            result.put("rewardSeqs", List.of());
        }
        return result;
    }

    // === Type 41: 个性化礼包 (Customized Gift) ===
    
    @Transactional
    private Map<String, Object> handleCustomizedGift(Long roleId, int opType, int param1) {
        CustomizedGift gift = customizedGiftRepo.findByRoleId(roleId).orElseGet(() -> {
            try {
                String emptyArray = objectMapper.writeValueAsString(new ArrayList<>());
                return customizedGiftRepo.save(CustomizedGift.builder()
                        .roleId(roleId)
                        .roleLevel(1)
                        .isOpen(true)
                        .hasBuyGift(false)
                        .fetchFlagsJson(emptyArray)
                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to init CustomizedGift", e);
            }
        });

        // opType: 1=GET_INFO, 2=BUY(param1=giftIndex), 3=CLAIM(param1=flagIndex)

        if (opType == 2) {
            gift.setHasBuyGift(true);
            customizedGiftRepo.save(gift);
        } else if (opType == 3 && param1 >= 0) {
            // Add fetch flag
            try {
                List<Integer> flags = objectMapper.readValue(
                        gift.getFetchFlagsJson(), new TypeReference<>() {});
                if (!flags.contains(param1)) {
                    flags.add(param1);
                }
                gift.setFetchFlagsJson(objectMapper.writeValueAsString(flags));
                customizedGiftRepo.save(gift);
            } catch (Exception e) {
                log.error("Failed to update fetchFlags", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roleLevel", gift.getRoleLevel());
        result.put("isOpen", gift.getIsOpen());
        result.put("hasBuyGift", gift.getHasBuyGift());
        try {
            List<Integer> flags = objectMapper.readValue(
                    gift.getFetchFlagsJson(), new TypeReference<>() {});
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

        // opType: 1=GET_INFO, 2=BUY(param1=seq)

        if (opType == 2 && param1 > 0) {
            // Buy exclusive gift
            try {
                List<Map<String, Object>> gifts = objectMapper.readValue(
                        gift.getGiftsJson(), new TypeReference<>() {});
                boolean found = false;
                for (Map<String, Object> g : gifts) {
                    if (g.get("seq") instanceof Number n && n.intValue() == param1) {
                        int buyTimes = g.get("alreadyBuyTimes") instanceof Number bt ? bt.intValue() : 0;
                        g.put("alreadyBuyTimes", buyTimes + 1);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    int now = (int) (System.currentTimeMillis() / 1000);
                    gifts.add(Map.of(
                            "seq", param1,
                            "alreadyBuyTimes", 1,
                            "endTimestamp", now + 86400 // 24 hours expiration
                    ));
                }
                gift.setGiftsJson(objectMapper.writeValueAsString(gifts));
                exclusiveGiftRepo.save(gift);
            } catch (Exception e) {
                log.error("Failed to update exclusive gift", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> gifts = objectMapper.readValue(
                    gift.getGiftsJson(), new TypeReference<>() {});
            result.put("gifts", gifts);
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
        FillBlank fb = fillBlankRepo.findByRoleId(roleId).orElseGet(() -> {
            FillBlank n = new FillBlank();
            n.setRoleId(roleId);
            return fillBlankRepo.save(n);
        });

        // opType: 1=GET_INFO, 2=FILL(param1=blankIndex), 3=CLAIM_REWARD(param1=tier), 4=USE_HINT
        if (opType == 2 && param1 >= 0) {
            long bit = 1L << param1;
            fb.setFilledMask(fb.getFilledMask() | bit);
            // Check if puzzle complete (assume 8 blanks)
            if (Long.bitCount(fb.getFilledMask()) >= 8) {
                fb.setCompletedCount(fb.getCompletedCount() + 1);
                fb.setPuzzleIndex(fb.getPuzzleIndex() + 1);
                fb.setFilledMask(0L);
            }
            fillBlankRepo.save(fb);
        } else if (opType == 3 && param1 > 0) {
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

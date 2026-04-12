package com.SouthMillion.webSocket_server.handler.activity;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgrandactivity.Msgrandactivity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles random/event activity operations (随机活动).
 *
 * CS msg 3000 PB_CSRandActivityOperaReq:
 *   rand_activity_type  — identifies which of the 42 activity types
 *   opera_type          — operation within that activity
 *   param1/param2/param3
 *
 * Response scMsgId = 3000 + activityType:
 *   1  → 3001 PB_SCChongZhiInfo          (充值信息)
 *   10 → 3010 PB_SCRaBoxFundInfo          (宝箱基金)
 *   11 → 3011 PB_SCRaLevelFundInfo        (等级基金)
 *   12 → 3012 PB_SCRaFirstChongInfo       (首充)
 *   13 → 3013 PB_SCRaLeiChongInfo         (累充)
 *   14 → 3014 PB_SCRaDailyGiftInfo        (日常礼包)
 *   15 → 3015 PB_SCRaCommodityGuildInfo   (商品行会)
 *   16 → 3016 PB_SCRaMonthCardInfo        (月卡)
 *   17 → 3017 PB_SCRaLuckCourtesyInfo     (幸运礼遇)
 *   18 → 3018 PB_SCRaWeekendRechargeInfo  (周末累充)
 *   19 → 3019 PB_SCRaCaveLootInfo         (洞穴夺宝)
 *   20 → 3020 PB_SCRaFriendInfo           (好友邀请)
 *   21 → 3021 PB_SCRaChestManorInfo       (宝箱庄园)
 *   22 → 3022 PB_SCRaCapacityFundInfo     (评分基金)
 *   23 → 3023 PB_SCRaDailySharingInfo     (每日分享)
 *   24 → 3024 PB_SCRaFaZhenGalaInfo       (法阵盛典)
 *   25 → 3025 PB_SCRaStarMapGalaInfo      (星图盛典)
 *   26 → 3026 PB_SCRaGuMoTowerFundInfo    (箍魔之塔基金)
 *   27 → 3027 PB_SCRaRuneTowerFundInfo    (铭文之塔基金)
 *   28 → 3028 PB_SCRaChaoZhiXianLiInfo   (超值献礼)
 *   29 → 3029 PB_SCRaNewServerInfo        (新服比拼)
 *   30 → 3030 PB_SCRaWeekendHaoLiInfo     (周末豪礼)
 *   31 → 3031 PB_SCRaNewServerGlobalInfo  (新服比拼全局)
 *   32 → 3032 PB_SCRaLianChongZengLiInfo  (连充赠礼)
 *   33 → 3033 PB_SCRaWarOrderInfo         (无限战令)
 *   34 → 3034 PB_SCRaWeekendLianChongInfo (周末连充)
 *   35 → 3035 PB_SCRaAdvertisementEquityInfo (广告权益)
 *   36 → 3036 PB_SCRANewServerRankList    (新服比拼排行榜)
 *   37 → 3037 PB_SCRaShenqiDuobao        (神器夺宝活动)
 *   38 → 3038 PB_SCRaTianXuanGift        (天选之礼)
 *   39 → 3039 PB_SCRaTerritoryGift       (领地礼包)
 *   40 → 3040 PB_SCRaJifenZhuanpan       (积分转盘)
 *   41 → 3041 PB_SCRaCustomizedGift      (个性化礼包)
 *   42 → 3042 PB_SCRaExclusiveGift       (专属礼包)
 *   default → 3003 PB_SCActivityStatus (generic ack)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RandActivityHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RandActivityHandler.class);

    private final ActivityFeign activityFeign;

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_RAND_ACTIVITY_OPERA_REQ};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgrandactivity.PB_CSRandActivityOperaReq req =
                        Msgrandactivity.PB_CSRandActivityOperaReq.parseFrom(payload);
                int clientActivityType = req.hasRandActivityType() ? req.getRandActivityType() : 0;
                int dispatchActivityType = normalizeActivityType(clientActivityType);
                int operaType    = req.hasOperaType()        ? req.getOperaType()        : 0;
                int param1       = req.hasParam1()           ? req.getParam1()           : 0;
                int param2       = req.hasParam2()           ? req.getParam2()           : 0;
                int param3       = req.hasParam3()           ? req.getParam3()           : 0;
                String roleId = String.valueOf(session.getRoleId());

                log.debug("[RandActivity] clientActType={}, dispatchActType={}, op={}, p1={}, roleId={}",
                        clientActivityType, dispatchActivityType, operaType, param1, roleId);

                Map<String, Object> body = new HashMap<>();
                body.put("activityType", dispatchActivityType);
                body.put("operaType",    operaType);
                body.put("param1",       param1);
                body.put("param2",       param2);
                body.put("param3",       param3);

                Map<String, Object> data;
                try {
                    data = activityFeign.randActivity(String.valueOf(roleId), body);
                    if (data == null) data = Map.of();
                } catch (Exception e) {
                    log.error("[RandActivity] backend error clientActType={} dispatchActType={} roleId={}",
                            clientActivityType, dispatchActivityType, roleId, e);
                    data = Map.of();
                }

                dispatchResponse(session, dispatchActivityType, clientActivityType, data);

            } catch (Exception e) {
                log.error("[RandActivity] Error for roleId={}", session.getRoleId(), e);
                try {
                    Emitters.emit(session, 3003, Msgrandactivity.PB_SCActivityStatus.newBuilder().build().toByteArray());
                } catch (Exception ignored) {}
            }
        });
    }

    // ===== Dispatch to specific proto builder =====

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

    private void dispatchResponse(PlayerSession session, int dispatchActivityType,
                                  int clientActivityType, Map<String, Object> data) throws Exception {
        switch (dispatchActivityType) {
            case 1  -> sendChongZhi(session, data);
            case 10 -> sendBoxFund(session, data);
            case 11 -> sendLevelFund(session, data);
            case 12 -> sendFirstChong(session, data);
            case 13 -> sendLeiChong(session, data);
            case 14 -> sendDailyGift(session, data);
            case 15 -> sendCommodityGuild(session, data);
            case 16 -> sendMonthCard(session, data);
            case 17 -> sendLuckCourtesy(session, data);
            case 18 -> sendWeekendRecharge(session, data);
            case 19 -> sendCaveLoot(session, data);
            case 20 -> sendFriendInfo(session, data);
            case 21 -> sendChestManor(session, data);
            case 22 -> sendCapacityFund(session, data);
            case 23 -> sendDailySharing(session, data);
            case 24 -> sendFaZhenGala(session, data);
            case 25 -> sendStarMapGala(session, data);
            case 26 -> sendGuMoTowerFund(session, data);
            case 27 -> sendRuneTowerFund(session, data);
            case 28 -> sendChaoZhiXianLi(session, data);
            case 29 -> sendNewServer(session, data);
            case 30 -> sendWeekendHaoLi(session, data);
            case 31 -> sendNewServerGlobal(session, data);
            case 32 -> sendLianChongZengLi(session, data);
            case 33 -> sendWarOrder(session, data);
            case 34 -> sendWeekendLianChong(session, data);
            case 35 -> sendAdvertisementEquity(session, data);
            case 36 -> sendNewServerRankList(session, data);
            case 37 -> sendShenqiDuobao(session, data);
            case 38 -> sendTianXuanGift(session, data);
            case 39 -> sendTerritoryGift(session, data);
            case 40 -> sendJifenZhuanpan(session, data);
            case 41 -> sendCustomizedGift(session, data);
            case 42 -> sendExclusiveGift(session, data);
            case 43 -> sendFishGame(session, data);
            case 44 -> sendLoopMine(session, data);
            case 45 -> sendCoreCrisis(session, data);
            case 46 -> sendFillBlank(session, data);
            case 47 -> sendMingXiang(session, data);
            default -> {
                log.warn("[RandActivity] Unknown activityType client={} dispatch={}", clientActivityType, dispatchActivityType);
                Emitters.emit(session, 3003, Msgrandactivity.PB_SCActivityStatus.newBuilder()
                                .setActivityType(clientActivityType).setStatus(0).build().toByteArray());
            }
        }
    }

    // ===== Individual activity response senders =====

    // type 1 → 3001 PB_SCChongZhiInfo (充值信息)
    private void sendChongZhi(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCChongZhiInfo.Builder b = Msgrandactivity.PB_SCChongZhiInfo.newBuilder();
        if (d.get("historyChongzhi") instanceof Number n)      b.setHistoryChongzhi(n.longValue());
        if (d.get("historyChongzhiCount") instanceof Number n) b.setHistoryChongzhiCount(n.intValue());
        if (d.get("todayChongzhi") instanceof Number n)        b.setTodayChongzhi(n.intValue());
        if (d.get("chongzhiRewardTimes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addChongzhiRewardTimes(n.intValue());
        }
        Emitters.emit(session, 3001, b.build().toByteArray());
    }

    // type 10 → 3010 PB_SCRaBoxFundInfo (宝箱基金)
    private void sendBoxFund(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaBoxFundInfo.Builder b = Msgrandactivity.PB_SCRaBoxFundInfo.newBuilder();
        if (d.get("phaseBuyFlag") instanceof Number n)    b.setPhaseBuyFlag(n.intValue());
        if (d.get("commonFetchFlag") instanceof Number n) b.setCommonFetchFlag(n.longValue());
        if (d.get("seniorFetchFlag") instanceof Number n) b.setSeniorFetchFlag(n.longValue());
        Emitters.emit(session, 3010, b.build().toByteArray());
    }

    // type 11 → 3011 PB_SCRaLevelFundInfo (等级基金)
    private void sendLevelFund(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaLevelFundInfo.Builder b = Msgrandactivity.PB_SCRaLevelFundInfo.newBuilder();
        if (d.get("phaseBuyFlag") instanceof Number n)    b.setPhaseBuyFlag(n.intValue());
        if (d.get("commonFetchFlag") instanceof Number n) b.setCommonFetchFlag(n.longValue());
        if (d.get("seniorFetchFlag") instanceof Number n) b.setSeniorFetchFlag(n.longValue());
        Emitters.emit(session, 3011, b.build().toByteArray());
    }

    // type 12 → 3012 PB_SCRaFirstChongInfo (首充)
    private void sendFirstChong(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaFirstChongInfo.Builder b = Msgrandactivity.PB_SCRaFirstChongInfo.newBuilder();
        if (d.get("firstChongMark") instanceof Number n) b.setFirstChongMark(n.intValue());
        if (d.get("fetchMark") instanceof Number n)      b.setFetchMark(n.intValue());
        Emitters.emit(session, 3012, b.build().toByteArray());
    }

    // type 13 → 3013 PB_SCRaLeiChongInfo (累充)
    private void sendLeiChong(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaLeiChongInfo.Builder b = Msgrandactivity.PB_SCRaLeiChongInfo.newBuilder();
        if (d.get("fetchFlag") instanceof Number n) b.setFetchFlag(n.longValue());
        Emitters.emit(session, 3013, b.build().toByteArray());
    }

    // type 14 → 3014 PB_SCRaDailyGiftInfo (日常礼包)
    private void sendDailyGift(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaDailyGiftInfo.Builder b = Msgrandactivity.PB_SCRaDailyGiftInfo.newBuilder();
        if (d.get("level") instanceof Number n) b.setLevel(n.intValue());
        if (d.get("buyCount") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addBuyCount(n.intValue());
        }
        Emitters.emit(session, 3014, b.build().toByteArray());
    }

    // type 15 → 3015 PB_SCRaCommodityGuildInfo (商品行会)
    private void sendCommodityGuild(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaCommodityGuildInfo.Builder b = Msgrandactivity.PB_SCRaCommodityGuildInfo.newBuilder();
        if (d.get("curDiscount") instanceof Number n)  b.setCurDiscount(n.intValue());
        if (d.get("openLevel") instanceof Number n)    b.setOpenLevel(n.intValue());
        if (d.get("purchasedTimes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addPurchasedTimes(n.intValue());
        }
        Emitters.emit(session, 3015, b.build().toByteArray());
    }

    // type 16 → 3016 PB_SCRaMonthCardInfo (月卡)
    private void sendMonthCard(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaMonthCardInfo.Builder b = Msgrandactivity.PB_SCRaMonthCardInfo.newBuilder();
        if (d.get("cardList") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    Msgrandactivity.PB_RaMonthCardData.Builder card = Msgrandactivity.PB_RaMonthCardData.newBuilder();
                    if (m.get("cardType") instanceof Number n)     card.setCardType(n.intValue());
                    if (m.get("fetchMark") instanceof Number n)    card.setFetchMark(n.intValue());
                    if (m.get("haveDays") instanceof Number n)     card.setHaveDays(n.intValue());
                    if (m.get("endTimestamp") instanceof Number n) card.setEndTimestamp(n.intValue());
                    if (m.get("firstBuyMark") instanceof Number n) card.setFirstBuyMark(n.intValue());
                    if (m.get("buyLevel") instanceof Number n)     card.setBuyLevel(n.intValue());
                    b.addCardList(card);
                }
            }
        }
        Emitters.emit(session, 3016, b.build().toByteArray());
    }

    // type 17 → 3017 PB_SCRaLuckCourtesyInfo (幸运礼遇)
    private void sendLuckCourtesy(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaLuckCourtesyInfo.Builder b = Msgrandactivity.PB_SCRaLuckCourtesyInfo.newBuilder();
        if (d.get("openLevel") instanceof Number n) b.setOpenLevel(n.intValue());
        if (d.get("giftInfo") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    // Note: proto has typo "Couttesy" — class name preserves it
                    Msgrandactivity.PB_RaLuckCouttesyGiftDef.Builder g =
                            Msgrandactivity.PB_RaLuckCouttesyGiftDef.newBuilder();
                    if (m.get("isValid") instanceof Number n)      g.setIsValid(n.intValue());
                    if (m.get("giftSeq") instanceof Number n)      g.setGiftSeq(n.intValue());
                    if (m.get("endTimestamp") instanceof Number n) g.setEndTimestamp(n.intValue());
                    b.addGiftInfo(g);
                }
            }
        }
        Emitters.emit(session, 3017, b.build().toByteArray());
    }

    // type 18 → 3018 PB_SCRaWeekendRechargeInfo (周末累充)
    private void sendWeekendRecharge(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaWeekendRechargeInfo.Builder b = Msgrandactivity.PB_SCRaWeekendRechargeInfo.newBuilder();
        if (d.get("openLevel") instanceof Number n)     b.setOpenLevel(n.intValue());
        if (d.get("totalChongzhi") instanceof Number n) b.setTotalChongzhi(n.intValue());
        if (d.get("receiveFlag") instanceof Number n)   b.setReceiveFlag(n.intValue());
        Emitters.emit(session, 3018, b.build().toByteArray());
    }

    // type 19 → 3019 PB_SCRaCaveLootInfo (洞穴夺宝)
    private void sendCaveLoot(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaCaveLootInfo.Builder b = Msgrandactivity.PB_SCRaCaveLootInfo.newBuilder();
        if (d.get("openLevel") instanceof Number n)           b.setOpenLevel(n.intValue());
        if (d.get("lotteryCount") instanceof Number n)        b.setLotteryCount(n.intValue());
        if (d.get("totalChongzhi") instanceof Number n)       b.setTotalChongzhi(n.intValue());
        if (d.get("chongzhiReceiveFlag") instanceof Number n) b.setChongzhiReceiveFlag(n.intValue());
        if (d.get("buyTimes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addBuyTimes(n.intValue());
        }
        if (d.get("taskParam") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addTaskParam(n.intValue());
        }
        if (d.get("rewardReceive") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addRewardReceive(n.intValue());
        }
        Emitters.emit(session, 3019, b.build().toByteArray());
    }

    // type 20 → 3020 PB_SCRaFriendInfo (好友邀请)
    private void sendFriendInfo(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaFriendInfo.Builder b = Msgrandactivity.PB_SCRaFriendInfo.newBuilder();
        if (d.get("friendCount") instanceof Number n) b.setFriendCount(n.intValue());
        if (d.get("rewardFlag") instanceof Number n)  b.setRewardFlag(n.intValue());
        Emitters.emit(session, 3020, b.build().toByteArray());
    }

    // type 21 → 3021 PB_SCRaChestManorInfo (宝箱庄园)
    private void sendChestManor(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaChestManorInfo.Builder b = Msgrandactivity.PB_SCRaChestManorInfo.newBuilder();
        if (d.get("openLevel") instanceof Number n) b.setOpenLevel(n.intValue());
        if (d.get("buyTimes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addBuyTimes(n.intValue());
        }
        Emitters.emit(session, 3021, b.build().toByteArray());
    }

    // type 22 → 3022 PB_SCRaCapacityFundInfo (评分基金)
    private void sendCapacityFund(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaCapacityFundInfo.Builder b = Msgrandactivity.PB_SCRaCapacityFundInfo.newBuilder();
        if (d.get("phaseBuyFlag") instanceof Number n)    b.setPhaseBuyFlag(n.intValue());
        if (d.get("commonFetchFlag") instanceof Number n) b.setCommonFetchFlag(n.longValue());
        if (d.get("seniorFetchFlag") instanceof Number n) b.setSeniorFetchFlag(n.longValue());
        Emitters.emit(session, 3022, b.build().toByteArray());
    }

    // type 23 → 3023 PB_SCRaDailySharingInfo (每日分享)
    private void sendDailySharing(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaDailySharingInfo.Builder b = Msgrandactivity.PB_SCRaDailySharingInfo.newBuilder();
        if (d.get("fetchCount") instanceof Number n) b.setFetchCount(n.intValue());
        Emitters.emit(session, 3023, b.build().toByteArray());
    }

    // type 24 → 3024 PB_SCRaFaZhenGalaInfo (法阵盛典)
    private void sendFaZhenGala(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaFaZhenGalaInfo.Builder b = Msgrandactivity.PB_SCRaFaZhenGalaInfo.newBuilder();
        if (d.get("level") instanceof Number n)        b.setLevel(n.intValue());
        if (d.get("endTimestamp") instanceof Number n) b.setEndTimestamp(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)    b.setFetchFlag(n.intValue());
        if (d.get("taskNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addTaskNum(n.intValue());
        }
        if (d.get("giftNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addGiftNum(n.intValue());
        }
        Emitters.emit(session, 3024, b.build().toByteArray());
    }

    // type 25 → 3025 PB_SCRaStarMapGalaInfo (星图盛典)
    private void sendStarMapGala(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaStarMapGalaInfo.Builder b = Msgrandactivity.PB_SCRaStarMapGalaInfo.newBuilder();
        if (d.get("level") instanceof Number n)        b.setLevel(n.intValue());
        if (d.get("endTimestamp") instanceof Number n) b.setEndTimestamp(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)    b.setFetchFlag(n.intValue());
        if (d.get("taskNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addTaskNum(n.intValue());
        }
        if (d.get("giftNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addGiftNum(n.intValue());
        }
        Emitters.emit(session, 3025, b.build().toByteArray());
    }

    // type 26 → 3026 PB_SCRaGuMoTowerFundInfo (箍魔之塔基金)
    private void sendGuMoTowerFund(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaGuMoTowerFundInfo.Builder b = Msgrandactivity.PB_SCRaGuMoTowerFundInfo.newBuilder();
        if (d.get("phaseBuyFlag") instanceof Number n)    b.setPhaseBuyFlag(n.intValue());
        if (d.get("commonFetchFlag") instanceof Number n) b.setCommonFetchFlag(n.longValue());
        if (d.get("seniorFetchFlag") instanceof Number n) b.setSeniorFetchFlag(n.longValue());
        Emitters.emit(session, 3026, b.build().toByteArray());
    }

    // type 27 → 3027 PB_SCRaRuneTowerFundInfo (铭文之塔基金)
    private void sendRuneTowerFund(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaRuneTowerFundInfo.Builder b = Msgrandactivity.PB_SCRaRuneTowerFundInfo.newBuilder();
        if (d.get("phaseBuyFlag") instanceof Number n)    b.setPhaseBuyFlag(n.intValue());
        if (d.get("commonFetchFlag") instanceof Number n) b.setCommonFetchFlag(n.longValue());
        if (d.get("seniorFetchFlag") instanceof Number n) b.setSeniorFetchFlag(n.longValue());
        Emitters.emit(session, 3027, b.build().toByteArray());
    }

    // type 28 → 3028 PB_SCRaChaoZhiXianLiInfo (超值献礼)
    private void sendChaoZhiXianLi(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaChaoZhiXianLiInfo.Builder b = Msgrandactivity.PB_SCRaChaoZhiXianLiInfo.newBuilder();
        if (d.get("level") instanceof Number n)   b.setLevel(n.intValue());
        if (d.get("buyMark") instanceof Number n) b.setBuyMark(n.intValue());
        if (d.get("itemNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addItemNum(n.intValue());
        }
        Emitters.emit(session, 3028, b.build().toByteArray());
    }

    // type 29 → 3029 PB_SCRaNewServerInfo (新服比拼)
    private void sendNewServer(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaNewServerInfo.Builder b = Msgrandactivity.PB_SCRaNewServerInfo.newBuilder();
        if (d.get("fetchFlag") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addFetchFlag(n.intValue());
        }
        Emitters.emit(session, 3029, b.build().toByteArray());
    }

    // type 30 → 3030 PB_SCRaWeekendHaoLiInfo (周末豪礼)
    private void sendWeekendHaoLi(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaWeekendHaoLiInfo.Builder b = Msgrandactivity.PB_SCRaWeekendHaoLiInfo.newBuilder();
        if (d.get("level") instanceof Number n) b.setLevel(n.intValue());
        if (d.get("buyTimes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addBuyTimes(n.intValue());
        }
        Emitters.emit(session, 3030, b.build().toByteArray());
    }

    // type 31 → 3031 PB_SCRaNewServerGlobalInfo (新服比拼全局)
    private void sendNewServerGlobal(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaNewServerGlobalInfo.Builder b = Msgrandactivity.PB_SCRaNewServerGlobalInfo.newBuilder();
        if (d.get("endTime") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addEndTime(n.intValue());
        }
        Emitters.emit(session, 3031, b.build().toByteArray());
    }

    // type 32 → 3032 PB_SCRaLianChongZengLiInfo (连充赠礼)
    private void sendLianChongZengLi(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaLianChongZengLiInfo.Builder b = Msgrandactivity.PB_SCRaLianChongZengLiInfo.newBuilder();
        if (d.get("level") instanceof Number n)           b.setLevel(n.intValue());
        if (d.get("curTaskDay") instanceof Number n)      b.setCurTaskDay(n.intValue());
        if (d.get("todayTaskFinish") instanceof Number n) b.setTodayTaskFinish(n.intValue());
        if (d.get("chongzhiTaskProceed") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addChongzhiTaskProceed(n.intValue());
        }
        if (d.get("friendTaskProceed") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addFriendTaskProceed(n.intValue());
        }
        if (d.get("receiveRewardsFlag") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addReceiveRewardsFlag(n.intValue());
        }
        Emitters.emit(session, 3032, b.build().toByteArray());
    }

    // type 33 → 3033 PB_SCRaWarOrderInfo (无限战令)
    // Note: proto field names have typo "falg" instead of "flag" — preserved in Java setters
    private void sendWarOrder(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaWarOrderInfo.Builder b = Msgrandactivity.PB_SCRaWarOrderInfo.newBuilder();
        if (d.get("openLevel") instanceof Number n)        b.setOpenLevel(n.intValue());
        if (d.get("isBuy") instanceof Number n)            b.setIsBuy(n.intValue());
        if (d.get("timeSeqTimestamp") instanceof Number n) b.setTimeSeqTimestamp(n.intValue());
        if (d.get("level") instanceof Number n)            b.setLevel(n.intValue());
        if (d.get("exp") instanceof Number n)              b.setExp(n.intValue());
        if (d.get("commonFetchFalg") instanceof Number n)  b.setCommonFetchFalg(n.longValue());
        if (d.get("seniorFetchFalg") instanceof Number n)  b.setSeniorFetchFalg(n.longValue());
        if (d.get("dayTaskFalg") instanceof Number n)      b.setDayTaskFalg(n.intValue());
        if (d.get("weekTaskFalg") instanceof Number n)     b.setWeekTaskFalg(n.intValue());
        if (d.get("dayTaskNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addDayTaskNum(n.intValue());
        }
        if (d.get("weekTaskNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addWeekTaskNum(n.intValue());
        }
        Emitters.emit(session, 3033, b.build().toByteArray());
    }

    // type 34 → 3034 PB_SCRaWeekendLianChongInfo (周末连充)
    // PB_BitMapDescribe nested fields omitted (empty lists) in stub implementation
    private void sendWeekendLianChong(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaWeekendLianChongInfo.Builder b = Msgrandactivity.PB_SCRaWeekendLianChongInfo.newBuilder();
        if (d.get("level") instanceof Number n)          b.setLevel(n.intValue());
        if (d.get("dayChongZhiNum") instanceof Number n) b.setDayChongZhiNum(n.longValue());
        if (d.get("chongZhiNum") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addChongZhiNum(n.longValue());
        }
        Emitters.emit(session, 3034, b.build().toByteArray());
    }

    // type 35 → 3035 PB_SCRaAdvertisementEquityInfo (广告权益/骑士福利)
    private void sendAdvertisementEquity(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaAdvertisementEquityInfo.Builder b =
                Msgrandactivity.PB_SCRaAdvertisementEquityInfo.newBuilder();
        if (d.get("isBuy") instanceof Number n)       b.setIsBuy(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)   b.setFetchFlag(n.intValue());
        if (d.get("refreshTime") instanceof Number n) b.setRefreshTime(n.intValue());
        Emitters.emit(session, 3035, b.build().toByteArray());
    }

    // type 36 → 3036 PB_SCRANewServerRankList (新服比拼排行榜)
    // Rank node list omitted (requires complex PB_RoleInfo population) — sends summary fields only
    private void sendNewServerRankList(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRANewServerRankList.Builder b = Msgrandactivity.PB_SCRANewServerRankList.newBuilder();
        if (d.get("type") instanceof Number n)        b.setType(n.intValue());
        if (d.get("myRank") instanceof Number n)      b.setMyRank(n.intValue());
        if (d.get("myRankValue") instanceof Number n) b.setMyRankValue(n.longValue());
        if (d.get("myBestRank") instanceof Number n)  b.setMyBestRank(n.intValue());
        Emitters.emit(session, 3036, b.build().toByteArray());
    }

    // type 37 → 3037 PB_SCRaShenqiDuobao (神器夺宝活动)
    private void sendShenqiDuobao(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaShenqiDuobao.Builder b = Msgrandactivity.PB_SCRaShenqiDuobao.newBuilder();
        if (d.get("roleLevel") instanceof Number n) b.setRoleLevel(n.intValue());
        if (d.get("tasks") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    Msgrandactivity.PB_SCRaShenqiDuobaoTask.Builder t =
                            Msgrandactivity.PB_SCRaShenqiDuobaoTask.newBuilder();
                    if (m.get("taskSeq") instanceof Number n)     t.setTaskSeq(n.intValue());
                    if (m.get("progress") instanceof Number n)    t.setProgress(n.intValue());
                    if (m.get("hasFetched") instanceof Boolean v) t.setHasFetched(v);
                    b.addTasks(t);
                }
            }
        }
        if (d.get("gifts") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    Msgrandactivity.PB_SCRaShenqiDuobaoGift.Builder g =
                            Msgrandactivity.PB_SCRaShenqiDuobaoGift.newBuilder();
                    if (m.get("giftSeq") instanceof Number n)  g.setGiftSeq(n.intValue());
                    if (m.get("buyTimes") instanceof Number n) g.setBuyTimes(n.intValue());
                    b.addGifts(g);
                }
            }
        }
        Emitters.emit(session, 3037, b.build().toByteArray());
    }

    // type 38 → 3038 PB_SCRaTianXuanGift (天选之礼)
    private void sendTianXuanGift(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaTianXuanGift.Builder b = Msgrandactivity.PB_SCRaTianXuanGift.newBuilder();
        if (d.get("roleLevel") instanceof Number n)              b.setRoleLevel(n.intValue());
        if (d.get("giftOpenTimestamp") instanceof Number n)      b.setGiftOpenTimestamp(n.intValue());
        if (d.get("giftCloseTimestamp") instanceof Number n)     b.setGiftCloseTimestamp(n.intValue());
        if (d.get("giftCdEndTimestamp") instanceof Number n)     b.setGiftCdEndTimestamp(n.intValue());
        if (d.get("hasFetchFreeGift") instanceof Boolean v)      b.setHasFetchFreeGift(v);
        if (d.get("groupId") instanceof Number n)                b.setGroupId(n.intValue());
        if (d.get("accumulatedChongzhiNum") instanceof Number n) b.setAccumulatedChongzhiNum(n.intValue());
        if (d.get("gifts") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    Msgrandactivity.PB_TianXuanGiftData.Builder g = Msgrandactivity.PB_TianXuanGiftData.newBuilder();
                    if (m.get("seq") instanceof Number n)    g.setSeq(n.intValue());
                    if (m.get("buyNum") instanceof Number n) g.setBuyNum(n.intValue());
                    b.addGifts(g);
                }
            }
        }
        Emitters.emit(session, 3038, b.build().toByteArray());
    }

    // type 39 → 3039 PB_SCRaTerritoryGift (领地礼包)
    private void sendTerritoryGift(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaTerritoryGift.Builder b = Msgrandactivity.PB_SCRaTerritoryGift.newBuilder();
        if (d.get("buyCount") instanceof Number n) b.setBuyCount(n.intValue());
        if (d.get("nowType") instanceof Number n)  b.setNowType(n.intValue());
        if (d.get("nextTime") instanceof Number n) b.setNextTime(n.intValue());
        Emitters.emit(session, 3039, b.build().toByteArray());
    }

    // type 40 → 3040 PB_SCRaJifenZhuanpan (积分转盘)
    private void sendJifenZhuanpan(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaJifenZhuanpan.Builder b = Msgrandactivity.PB_SCRaJifenZhuanpan.newBuilder();
        if (d.get("roleLevel") instanceof Number n)       b.setRoleLevel(n.intValue());
        if (d.get("rewardGroup") instanceof Number n)     b.setRewardGroup(n.intValue());
        if (d.get("timesToBigPrize") instanceof Number n) b.setTimesToBigPrize(n.intValue());
        if (d.get("jifen") instanceof Number n)           b.setJifen(n.intValue());
        if (d.get("rewardSeqs") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Number n) b.addRewardSeqs(n.intValue());
        }
        Emitters.emit(session, 3040, b.build().toByteArray());
    }

    // type 41 → 3041 PB_SCRaCustomizedGift (个性化礼包)
    private void sendCustomizedGift(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaCustomizedGift.Builder b = Msgrandactivity.PB_SCRaCustomizedGift.newBuilder();
        if (d.get("roleLevel") instanceof Number n)   b.setRoleLevel(n.intValue());
        if (d.get("isOpen") instanceof Boolean v)     b.setIsOpen(v);
        if (d.get("hasBuyGift") instanceof Boolean v) b.setHasBuyGift(v);
        if (d.get("fetchFlags") instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Number n) {
                    Msgrandactivity.PB_SCRaCustomizedGift.FetchFlag flag =
                            Msgrandactivity.PB_SCRaCustomizedGift.FetchFlag.forNumber(n.intValue());
                    if (flag != null) b.addFetchFlags(flag);
                }
            }
        }
        Emitters.emit(session, 3041, b.build().toByteArray());
    }

    // type 42 → 3042 PB_SCRaExclusiveGift (专属礼包)
    private void sendExclusiveGift(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaExclusiveGift.Builder b = Msgrandactivity.PB_SCRaExclusiveGift.newBuilder();
        if (d.get("gifts") instanceof List<?> l) {
            for (Object item : l) {
                if (item instanceof Map<?, ?> m) {
                    Msgrandactivity.PB_ExclusiveGiftData.Builder g = Msgrandactivity.PB_ExclusiveGiftData.newBuilder();
                    if (m.get("endTimestamp") instanceof Number n)    g.setEndTimestamp(n.intValue());
                    if (m.get("alreadyBuyTimes") instanceof Number n) g.setAlreadyBuyTimes(n.intValue());
                    if (m.get("seq") instanceof Number n)             g.setSeq(n.intValue());
                    b.addGifts(g);
                }
            }
        }
        Emitters.emit(session, 3042, b.build().toByteArray());
    }

    // type 43 → 3043 PB_SCRaFishGameInfo (钓鱼小游戏)
    private void sendFishGame(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaFishGameInfo.Builder b = Msgrandactivity.PB_SCRaFishGameInfo.newBuilder();
        if (d.get("fishCount") instanceof Number n)       b.setFishCount(n.intValue());
        if (d.get("dailyAttempts") instanceof Number n)   b.setDailyAttempts(n.intValue());
        if (d.get("maxDaily") instanceof Number n)        b.setMaxDaily(n.intValue());
        if (d.get("totalFishCaught") instanceof Number n) b.setTotalFishCaught(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)       b.setFetchFlag(n.longValue());
        Emitters.emit(session, 3043, b.build().toByteArray());
    }

    // type 44 → 3044 PB_SCRaLoopMineInfo (循环矿坑)
    private void sendLoopMine(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaLoopMineInfo.Builder b = Msgrandactivity.PB_SCRaLoopMineInfo.newBuilder();
        if (d.get("mineLevel") instanceof Number n)      b.setMineLevel(n.intValue());
        if (d.get("oreCount") instanceof Number n)       b.setOreCount(n.intValue());
        if (d.get("cycleCount") instanceof Number n)     b.setCycleCount(n.intValue());
        if (d.get("dailyAttempts") instanceof Number n)  b.setDailyAttempts(n.intValue());
        if (d.get("maxDaily") instanceof Number n)       b.setMaxDaily(n.intValue());
        if (d.get("oreToComplete") instanceof Number n)  b.setOreToComplete(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)      b.setFetchFlag(n.longValue());
        Emitters.emit(session, 3044, b.build().toByteArray());
    }

    // type 45 → 3045 PB_SCRaCoreCrisisInfo (核心危机)
    private void sendCoreCrisis(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaCoreCrisisInfo.Builder b = Msgrandactivity.PB_SCRaCoreCrisisInfo.newBuilder();
        if (d.get("stage") instanceof Number n)          b.setStage(n.intValue());
        if (d.get("totalDamage") instanceof Number n)    b.setTotalDamage(n.longValue());
        if (d.get("dailyAttempts") instanceof Number n)  b.setDailyAttempts(n.intValue());
        if (d.get("maxDaily") instanceof Number n)       b.setMaxDaily(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)      b.setFetchFlag(n.longValue());
        if (d.get("endTimestamp") instanceof Number n)   b.setEndTimestamp(n.intValue());
        if (d.get("isBuy") instanceof Number n)          b.setIsBuy(n.intValue());
        Emitters.emit(session, 3045, b.build().toByteArray());
    }

    // type 46 → 3046 PB_SCRaFillBlankInfo (填字谜)
    private void sendFillBlank(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaFillBlankInfo.Builder b = Msgrandactivity.PB_SCRaFillBlankInfo.newBuilder();
        if (d.get("puzzleIndex") instanceof Number n)    b.setPuzzleIndex(n.intValue());
        if (d.get("filledMask") instanceof Number n)     b.setFilledMask(n.longValue());
        if (d.get("completedCount") instanceof Number n) b.setCompletedCount(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)      b.setFetchFlag(n.longValue());
        if (d.get("hintCount") instanceof Number n)      b.setHintCount(n.intValue());
        Emitters.emit(session, 3046, b.build().toByteArray());
    }

    // type 47 → 3047 PB_SCRaMingXiangInfo (命相/星象)
    private void sendMingXiang(PlayerSession session, Map<String, Object> d) throws Exception {
        Msgrandactivity.PB_SCRaMingXiangInfo.Builder b = Msgrandactivity.PB_SCRaMingXiangInfo.newBuilder();
        if (d.get("signIndex") instanceof Number n)          b.setSignIndex(n.intValue());
        if (d.get("fortuneLevel") instanceof Number n)       b.setFortuneLevel(n.intValue());
        if (d.get("divinationCount") instanceof Number n)    b.setDivinationCount(n.intValue());
        if (d.get("fetchFlag") instanceof Number n)          b.setFetchFlag(n.longValue());
        if (d.get("lastDivinationTime") instanceof Number n) b.setLastDivinationTime(n.intValue());
        if (d.get("nextDivinationTime") instanceof Number n) b.setNextDivinationTime(n.intValue());
        Emitters.emit(session, 3047, b.build().toByteArray());
    }
}

package com.SouthMillion.webSocket_server.handler.box;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.LazyLoadHandler;
import com.SouthMillion.webSocket_server.handler.bag.BagHandler;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.handler.task.TaskHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.service.BagUpdateGate;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BoxFeign;
import com.SouthMillion.webSocket_server.service.client.EquipHttpClient;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgbox.Msgbox;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Handles treasure chest / box (宝箱) operations.
 *
 * Proto:
 *   1610 PB_CSBoxReq    — req_type + param  (main operations)
 *   1611 PB_CSBoxSetReq — auto-open settings
 *
 * Responses:
 *   1615 PB_SCBoxEquipInfo   — equip rolled from open
 *   1619 PB_SCBoxEquipCompareInfo — compare payload (candidate/equippedBefore/version)
 *   1616 PB_SCBoxInfo        — box state after most ops
 *   1617 PB_SCBoxSetingInfo  — auto-open settings ack
 *   1618 PB_SCBoxSellInfo    — sell result
 *
 * This handler implements LazyLoadHandler for on-demand loading when the player opens the box UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoxHandler implements MessageHandler, LazyLoadHandler {

    private final BoxFeign boxFeign;
    private final EquipHttpClient equipHttpClient;
    private final BagHandler bagHandler;
    private final RoleServiceHandler roleServiceHandler;
    private final RoleFeign roleFeign;
    private final TaskHandler taskHandler;
    private final TaskProgressPublisher taskProgressPublisher;
    private final WalletHttpClient walletHttpClient;
    private final BagUpdateGate bagUpdateGate;
    private final reactor.core.scheduler.Scheduler feignVtScheduler;

    private static final int REQ_OPEN     = 1;
    private static final int REQ_EQUIP    = 2;
    private static final int REQ_SELL     = 3;
    private static final int REQ_BUY      = 4;
    private static final int REQ_UPGRADE  = 5;
    private static final int REQ_QUICKEN  = 6;
    private static final int REQ_DECOMPOSE = 7;
    private static final int REQ_LEVEL_REWARD = 8;

    /** Box UI refresh calls are best-effort and must not stall open/wear/sell flows. */
    @Value("${box.ws.fetch-timeout-ms:200}")
    private long boxFetchTimeoutMs = 200L;

    @Override
    public int[] interests() {
        return new int[]{1610, 1611};
    }

    @Override
    public String getModuleName() {
        return "box";
    }

    @Override
    public Mono<Void> loadOnDemand(PlayerSession ps) {
        log.info("[Box] Lazy loading for roleId={}", ps.getRoleId());
        return pushAll(ps);
    }

    /** Gọi sau login: đẩy trạng thái hộp kho báu (1616) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();

        // Parallel execution: fetch all box data concurrently instead of sequentially
        // Old: sendCurrentInfo() → sendCurrentSetting() → sendPendingEquipInfo() (sequential, ~1500ms)
        // New: Mono.zip all calls (parallel, ~600ms - 60% faster)
        return Mono.zip(
                bestEffortFetchMono(() -> boxFeign.info(roleId), roleId, "info"),
                bestEffortFetchMono(() -> boxFeign.getSetting(roleId), roleId, "getSetting"),
                bestEffortFetchMono(() -> boxFeign.getCompareState(roleId), roleId, "getCompareState")
                        .defaultIfEmpty(null),
                bestEffortFetchMono(() -> boxFeign.equipInfo(roleId), roleId, "equipInfo")
                        .defaultIfEmpty(null)
        ).flatMap(tuple -> {
            BoxDTOs.InfoResp info = tuple.getT1();
            BoxDTOs.BoxSettingResp setting = tuple.getT2();
            BoxDTOs.BoxCompareStateResp compareState = tuple.getT3();
            BoxDTOs.EquipInfo equipInfo = tuple.getT4();

            // Send current info (1616)
            sendCurrentInfoFromResp(session, info);

            // Send settings (1617)
            sendSettingInfo(session, buildSettingInfo(setting));

            // Send pending equip (1615) if available
            sendPendingEquipInfoFromResp(session, compareState, equipInfo, 0);

            return Mono.<Void>empty();
        }).onErrorResume(e -> {
            log.warn("[Box] pushAll parallel fetch error roleId={}: {}", roleId, e.getMessage());
            // Fallback: send empty responses
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
            sendSettingInfo(session, Msgbox.PB_SCBoxSetingInfo.newBuilder().build());
            return Mono.<Void>empty();
        });
    }

    private <T> Mono<T> bestEffortFetchMono(Supplier<T> supplier, Long roleId, String operation) {
        long timeoutMs = Math.max(50L, boxFetchTimeoutMs);
        String token = FeignTokenHolder.get();

        return Mono.fromCallable(() -> {
                    if (token != null && !token.isBlank()) {
                        FeignTokenHolder.set(token);
                    }
                    try {
                        return supplier.get();
                    } finally {
                        FeignTokenHolder.clear();
                    }
                })
                .subscribeOn(feignVtScheduler)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(ex -> {
                    log.debug("[Box] {} skipped roleId={} timeoutMs={} ex={}", operation, roleId, timeoutMs, ex.toString());
                    return Mono.empty();
                });
    }

    private void sendCurrentInfoFromResp(PlayerSession session, BoxDTOs.InfoResp info) {
        try {
            Msgbox.PB_SCBoxInfo.Builder b = Msgbox.PB_SCBoxInfo.newBuilder();
            if (info != null) {
                b.setBoxLevel(info.getBoxLevel());
                b.setBuyTimes(info.getBoxBuyTimes());
                if (info.getLevelUpEndEpoch() > 0)
                    b.setTimestamp((int) (info.getLevelUpEndEpoch() / 1000));
                b.setArenaItemNum(info.getArenaItemNum());
                b.setShiZhuangNum(info.getShiZhuangNum());
                b.setLevelFetchFlag(info.getLevelFetchFlag());
            }
            sendBoxInfo(session, b.build());
        } catch (Exception e) {
            log.error("[Box] sendCurrentInfoFromResp error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    private void sendPendingEquipInfoFromResp(PlayerSession session,
                                              BoxDTOs.BoxCompareStateResp compareState,
                                              BoxDTOs.EquipInfo equipInfo,
                                              int defaultIsNew) {
        try {
            if (compareState != null) {
                BoxDTOs.EquipInfo compareEquipInfo = BoxDTOs.equipInfoFromCompareState(compareState, defaultIsNew);
                if (hasUsablePendingEquip(compareEquipInfo)) {
                    int isNew = compareEquipInfo.getIsNew() != null ? compareEquipInfo.getIsNew() : defaultIsNew;
                    sendEquipInfo(session, buildEquipInfo(compareEquipInfo.getEquipInfo(), isNew), compareState);
                    return;
                }
                if (isInvalidPendingEquip(compareEquipInfo)) {
                    Integer itemId = compareEquipInfo.getEquipInfo().getItemId();
                    log.warn("[Box] ignore stale compare equip itemId={}", itemId);
                    clearCompareStateSafely(session.getRoleId(), "invalid compare itemId=" + itemId);
                    compareState = null;
                }
            }

            if (hasUsablePendingEquip(equipInfo)) {
                int isNew = equipInfo.getIsNew() != null ? equipInfo.getIsNew() : defaultIsNew;
                sendEquipInfo(session, buildEquipInfo(equipInfo.getEquipInfo(), isNew), compareState);
                return;
            }
            if (isInvalidPendingEquip(equipInfo)) {
                Integer itemId = equipInfo.getEquipInfo().getItemId();
                log.warn("[Box] ignore stale pending equip itemId={}", itemId);
                clearCompareStateSafely(session.getRoleId(), "invalid pending itemId=" + itemId);
                sendEquipInfo(session, buildEmptyEquipInfo());
            }
        } catch (Exception e) {
            log.debug("[Box] pending equip send skipped ex={}", e.toString());
        }
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                if (msgId == 1611) {
                    handleSetReq(session, payload);
                } else {
                    Msgbox.PB_CSBoxReq req = Msgbox.PB_CSBoxReq.parseFrom(payload);
                    int reqType = req.hasReqType() ? req.getReqType() : 0;
                    int param   = req.hasParam()   ? req.getParam()   : 0;
                    Long roleId = session.getRoleId();

                    log.debug("[Box] reqType={}, param={}, roleId={}", reqType, param, roleId);

                    switch (reqType) {
                        case REQ_OPEN    -> handleOpen(session, roleId, param);
                        case REQ_EQUIP   -> handleEquip(session, roleId);
                        case REQ_SELL    -> handleSell(session, roleId);
                        case REQ_BUY     -> handleBuy(session, roleId);
                        case REQ_UPGRADE -> handleUpgrade(session, roleId);
                        case REQ_QUICKEN -> handleQuicken(session, roleId, param);
                        case REQ_DECOMPOSE -> handleDecompose(session, roleId);
                        case REQ_LEVEL_REWARD -> handleLevelReward(session, roleId, param);
                        default -> {
                            log.warn("[Box] Unknown reqType={}", reqType);
                            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[Box] Error for roleId={}", session.getRoleId(), e);
                sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
            }
        });
    }

    // 1611: read back current setting as ack
    private void handleSetReq(PlayerSession session, byte[] payload) {
        try {
            Msgbox.PB_CSBoxSetReq req = Msgbox.PB_CSBoxSetReq.parseFrom(payload);
            Long roleId = session.getRoleId();

            if (req.hasBoxSet() && roleId != null) {
                Msgbox.PB_BoxSet boxSet = req.getBoxSet();
                BoxDTOs.BoxSettingReq saveReq = new BoxDTOs.BoxSettingReq();
                saveReq.setRoleId(String.valueOf(roleId));
                saveReq.setBoxSet(BoxDTOs.BoxSettingResp.builder()
                        .equipEqality(boxSet.hasEquipEqality() ? boxSet.getEquipEqality() : 0)
                        .conditionFirstMark(boxSet.hasConditionFirstMark() ? boxSet.getConditionFirstMark() : 0)
                        .conditionFirst1(boxSet.hasConditionFirst1() ? boxSet.getConditionFirst1() : 0)
                        .conditionFirst2(boxSet.hasConditionFirst2() ? boxSet.getConditionFirst2() : 0)
                        .conditionSecondMark(boxSet.hasConditionSecondMark() ? boxSet.getConditionSecondMark() : 0)
                        .conditionSecond1(boxSet.hasConditionSecond1() ? boxSet.getConditionSecond1() : 0)
                        .conditionSecond2(boxSet.hasConditionSecond2() ? boxSet.getConditionSecond2() : 0)
                        .retainMark(boxSet.hasRetainMark() ? boxSet.getRetainMark() : 0)
                        .challengeMark(boxSet.hasChallengeMark() ? boxSet.getChallengeMark() : 0)
                        .equipCapMark(boxSet.hasEquipCapMark() ? boxSet.getEquipCapMark() : 0)
                        .equipSellMark(boxSet.hasEquipSellMark() ? boxSet.getEquipSellMark() : 0)
                        .openFiveMark(boxSet.hasOpenFiveMark() ? boxSet.getOpenFiveMark() : 0)
                        .build());
                boxFeign.saveSetting(saveReq);
            }

            BoxDTOs.BoxSettingResp setting = boxFeign.getSetting(roleId);
            sendSettingInfo(session, buildSettingInfo(setting));
        } catch (Exception e) {
            log.error("[Box] handleSetReq error", e);
            sendSettingInfo(session, Msgbox.PB_SCBoxSetingInfo.newBuilder().build());
        }
    }

    // req_type=1: open box → maybe 1615 (equip) + 1616 (info)
    private void handleOpen(PlayerSession session, Long roleId, int param) {
        long startedAt = System.currentTimeMillis();
        try {
            log.info("[Box.open.ws] roleId={} param={} begin", roleId, param);

            // Nếu còn pending từ lần open trước thì không open mới để tránh trừ số lần mở thêm.
            // Re-emit it as `isNew=1` so the legacy client reopens the compare popup
            // instead of staying stuck in UNCLICKABLE state after the user taps open again.
            long pendingStartedAt = System.currentTimeMillis();
            boolean hadPending = sendPendingEquipInfo(session, roleId, 1);
            log.info("[Box.open.ws] roleId={} step=pendingCheck elapsedMs={} hadPending={}",
                    roleId,
                    System.currentTimeMillis() - pendingStartedAt,
                    hadPending);
            if (hadPending) {
                long refreshStartedAt = System.currentTimeMillis();
                sendCurrentInfo(session, roleId);
                log.info("[Box.open.ws] roleId={} branch=blocked-pending refreshInfoMs={} totalMs={}",
                        roleId,
                        System.currentTimeMillis() - refreshStartedAt,
                        System.currentTimeMillis() - startedAt);
                return;
            }

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(roleId));
            int count = normalizeOpenCount(param);
            req.setCount(count);
            long roleLevelStartedAt = System.currentTimeMillis();
            int roleLevel = fetchRoleLevel(session, roleId);
            req.setRoleLevel(roleLevel);
            log.info("[Box.open.ws] roleId={} step=fetchRoleLevel elapsedMs={} roleLevel={}",
                    roleId,
                    System.currentTimeMillis() - roleLevelStartedAt,
                    roleLevel);

            long openCallStartedAt = System.currentTimeMillis();
            BoxDTOs.OpenResp resp = boxFeign.open(req);
            long openCallElapsedMs = System.currentTimeMillis() - openCallStartedAt;
            String compareStatus = resp != null && resp.getCompareState() != null ? resp.getCompareState().getStatus() : "NONE";
            log.info("[Box.open.ws] roleId={} step=openCall elapsedMs={} hasOpenEquip={} hasPending={} bonusCount={} compareStatus={} openBoxTotal={} lastOpenIsFive={}",
                    roleId,
                    openCallElapsedMs,
                    resp != null && resp.getOpenEquip() != null,
                    resp != null && resp.getPending() != null,
                    resp != null && resp.getBonusItems() != null ? resp.getBonusItems().size() : 0,
                    compareStatus,
                    resp != null ? resp.getOpenBoxTotal() : -1,
                    resp != null && resp.isLastOpenIsFive());
            if (!reportOpenBoxTaskProgress(roleId, count)) {
                taskHandler.pushCurrentTaskProgress(session);
            }
            if (!reportGetEquipTaskProgress(roleId, count)) {
                taskHandler.pushCurrentTaskProgress(session);
            }

            if (resp != null && resp.getOpenEquip() != null) {
                int isNew = resp.getIsNew() != null ? resp.getIsNew() : 1;
                sendEquipInfo(session, buildEquipInfo(resp.getOpenEquip(), isNew), resp.getCompareState());
            } else if (resp != null && resp.getPending() != null) {
                BoxDTOs.EquipInfo equipInfo = BoxDTOs.equipInfoFromPending(resp.getPending(), 1);
                if (equipInfo != null && equipInfo.getEquipInfo() != null) {
                    int isNew = equipInfo.getIsNew() != null ? equipInfo.getIsNew() : 1;
                    sendEquipInfo(session, buildEquipInfo(equipInfo.getEquipInfo(), isNew));
                }
            } else {
                // Some opens grant direct rewards/items instead of a compare popup.
                // Emit a generic 1507 reward notice so the client shows a visible reward popup
                // instead of looking like the box count was deducted with no result.
                sendEquipInfo(session, buildEmptyEquipInfo());
                sendBonusItemNotice(session, resp != null ? resp.getBonusItems() : null);
            }

            refreshBagAfterOpen(session, roleId);
            long refreshInfoStartedAt = System.currentTimeMillis();
            sendCurrentInfo(session, roleId);
            log.info("[Box.open.ws] roleId={} step=refreshInfo elapsedMs={} totalMs={}",
                    roleId,
                    System.currentTimeMillis() - refreshInfoStartedAt,
                    System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("[Box] handleOpen error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    private boolean reportOpenBoxTaskProgress(Long roleId, int count) {
        return taskProgressPublisher.publish(roleId, TaskCondition.OPEN_BOX.taskKey(), count, "websocket-box-open");
    }

    private boolean reportGetEquipTaskProgress(Long roleId, int count) {
        return taskProgressPublisher.publish(roleId, TaskCondition.GET_EQUIP.taskKey(), count, "websocket-box-open");
    }

    private void reportBoxWearQualityTaskProgress(PlayerSession session, Long roleId, Integer quality) {
        if (roleId == null || quality == null || quality <= 0) {
            return;
        }

        boolean anyFailed = false;
        for (TaskCondition condition : TaskCondition.equipQualityConditions(quality)) {
            boolean published = taskProgressPublisher.publish(roleId, condition.taskKey(), 1, "websocket-box-wear");
            anyFailed = anyFailed || !published;
        }
        if (anyFailed) {
            taskHandler.pushCurrentTaskProgress(session);
        }
    }

    private Integer resolvePendingEquipQuality(Long roleId) {
        if (roleId == null) {
            return null;
        }

        try {
            BoxDTOs.BoxCompareStateResp compareState = boxFeign.getCompareState(roleId);
            if (compareState != null && compareState.getCandidateEquip() != null) {
                Integer quality = compareState.getCandidateEquip().getQuality();
                if (quality != null && quality > 0) {
                    return quality;
                }
            }
        } catch (Exception e) {
            log.debug("[Box] resolve compare-state quality skipped roleId={} ex={}", roleId, e.toString());
        }

        try {
            BoxDTOs.InfoResp info = boxFeign.info(roleId);
            return extractPendingEquipQuality(info != null ? info.getPending() : null);
        } catch (Exception e) {
            log.debug("[Box] resolve pending quality skipped roleId={} ex={}", roleId, e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Integer extractPendingEquipQuality(Map<String, Object> pending) {
        if (pending == null || pending.isEmpty()) {
            return null;
        }

        Integer directQuality = parsePositiveInt(pending.get("quality"), pending.get("color"), pending.get("q"));
        if (directQuality != null) {
            return directQuality;
        }

        Object nested = pending.get("equip");
        if (nested instanceof Map<?, ?> nestedMap) {
            return parsePositiveInt(
                    nestedMap.get("quality"),
                    nestedMap.get("color"),
                    nestedMap.get("q")
            );
        }
        return null;
    }

    private Integer parsePositiveInt(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                int parsed = number.intValue();
                if (parsed > 0) {
                    return parsed;
                }
                continue;
            }
            try {
                int parsed = Integer.parseInt(String.valueOf(value).trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // ignore invalid quality field and continue trying fallbacks
            }
        }
        return null;
    }

    private int normalizeOpenCount(int param) {
        // Compatibility:
        // - legacy clients often send 0/1 as open-mode flags (single/five)
        // - some clients send 1/2 as mode indexes (single/five)
        // - explicit numeric count supports only 5 for multi-open
        if (param <= 0) return 1;
        if (param == 2 || param == 5) return 5;
        return 1;
    }

    // req_type=2: equip (wear) item → 1616
    private void handleEquip(PlayerSession session, Long roleId) {
        try {
            Integer pendingQuality = resolvePendingEquipQuality(roleId);

            BoxDTOs.WearReq req = new BoxDTOs.WearReq();
            req.setRoleId(String.valueOf(roleId));
            BoxDTOs.OkResp wearResp = boxFeign.wear(req);

            if (wearResp != null && wearResp.isOk()) {
                reportBoxWearQualityTaskProgress(session, roleId, pendingQuality);
                // Wear thành công phải đóng compare popup ngay để tránh render lại stale compare-state.
                clearCompareStateSafely(roleId, "wear success");
                sendEquipInfo(session, buildEmptyEquipInfo());
                sendEquipList(session, roleId);
                // Equip từ Box đã cộng/trừ stat ở backend -> push lại role attrs để client refresh chỉ số ngay.
                pushRoleAttr(session);
            } else {
                String msg = (wearResp == null) ? "NULL_RESP" : String.valueOf(wearResp.getMessage());
                log.warn("[Box] wear not ok roleId={} msg={}", roleId, msg);
            }
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleEquip error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=3: sell item → 1618 + 1616
    private void handleSell(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SellReq req = new BoxDTOs.SellReq();
            req.setRoleId(String.valueOf(roleId));
            BoxDTOs.SellResp sellResp = boxFeign.sell(req);

            if (sellResp != null && sellResp.isOk()) {
                log.info("[Box] sell success roleId={} sellCoin={} sellExp={}", roleId, sellResp.getSellCoin(), sellResp.getSellExp());
                // Sell xong thì pending rỗng: đẩy packet rỗng để client đóng BoxEquipView ngay.
                sendEquipInfo(session, buildEmptyEquipInfo());
                taskProgressPublisher.publish(roleId, TaskCondition.SELL_EQUIP_NUM.taskKey(), 1, "websocket-box-sell");
                int sellCoinDelta = clampPositiveInt(sellResp.getSellCoin());
                if (sellCoinDelta > 0) {
                    taskProgressPublisher.publish(roleId, TaskCondition.SELL_EQUIP_GOLD.taskKey(), sellCoinDelta, "websocket-box-sell");
                }
                // Đồng bộ role/wallet theo 2 nhịp để tránh race khi service cập nhật chậm hơn 1 tick.
                refreshRoleAndWalletAfterSell(session, roleId);
            } else {
                String msg = (sellResp == null) ? "NULL_RESP" : String.valueOf(sellResp.getMessage());
                log.warn("[Box] sell not ok roleId={} msg={}", roleId, msg);
            }

            Msgbox.PB_SCBoxSellInfo.Builder sellInfo = Msgbox.PB_SCBoxSellInfo.newBuilder();
            if (sellResp != null) {
                if (sellResp.getSellCoin() > 0) {
                    sellInfo.setSellCoin((int) Math.min(Integer.MAX_VALUE, sellResp.getSellCoin()));
                }
                if (sellResp.getSellExp() > 0) {
                    sellInfo.setSellExp((int) Math.min(Integer.MAX_VALUE, sellResp.getSellExp()));
                }
            }
            sendSellInfo(session, sellInfo.build());
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleSell error", e);
            sendSellInfo(session, Msgbox.PB_SCBoxSellInfo.newBuilder().build());
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=4: buy → 1616
    private void handleBuy(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SimpleReq req = new BoxDTOs.SimpleReq();
            req.setRoleId(String.valueOf(roleId));
            boxFeign.buy(req);
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleBuy error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=5: upgrade (level up) → 1616
    private void handleUpgrade(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SimpleReq req = new BoxDTOs.SimpleReq();
            req.setRoleId(String.valueOf(roleId));
            boxFeign.levelUp(req);
            taskProgressPublisher.publish(roleId, TaskCondition.BOX_UPGRADE_ACCUMULATE.taskKey(), 1, "websocket-box-upgrade");
            taskProgressPublisher.publish(roleId, TaskCondition.BOX_LEVEL.taskKey(), 1, "websocket-box-level");
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleUpgrade error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=6: quicken/accelerate (param=num) → 1616
    private void handleQuicken(PlayerSession session, Long roleId, int num) {
        try {
            BoxDTOs.QuickenReq req = new BoxDTOs.QuickenReq();
            req.setRoleId(String.valueOf(roleId));
            req.setNum(Math.max(1, num));
            boxFeign.quicken(req);
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleQuicken error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=7: decompose item → 1618 + 1616
    private void handleDecompose(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.DecomposeResp resp = boxFeign.decompose(roleId);

            Msgbox.PB_SCBoxSellInfo.Builder sellInfo = Msgbox.PB_SCBoxSellInfo.newBuilder();
            if (resp != null) {
                // 1618 supports coin/exp only; map decompose exp so client can refresh gain effects.
                if (resp.getGotExp() > 0) {
                    sellInfo.setSellExp((int) Math.min(Integer.MAX_VALUE, resp.getGotExp()));
                }
            }
            sendSellInfo(session, sellInfo.build());
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleDecompose error", e);
            sendSellInfo(session, Msgbox.PB_SCBoxSellInfo.newBuilder().build());
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // req_type=8: fetch level-buy reward (param=idx, starts from 1) → 1616
    private void handleLevelReward(PlayerSession session, Long roleId, int idx) {
        try {
            BoxDTOs.LevelRewardReq req = new BoxDTOs.LevelRewardReq();
            req.setRoleId(String.valueOf(roleId));
            req.setIdx(Math.max(1, idx));
            boxFeign.levelReward(req);
            sendCurrentInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Box] handleLevelReward error", e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    // Fetch current state and send 1616
    private void sendCurrentInfo(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.InfoResp info = bestEffortFetch(() -> boxFeign.info(roleId), roleId, "info");
            Msgbox.PB_SCBoxInfo.Builder b = Msgbox.PB_SCBoxInfo.newBuilder();
            if (info != null) {
                b.setBoxLevel(info.getBoxLevel());
                b.setBuyTimes(info.getBoxBuyTimes());
                if (info.getLevelUpEndEpoch() > 0)
                    b.setTimestamp((int) (info.getLevelUpEndEpoch() / 1000));
                b.setArenaItemNum(info.getArenaItemNum());
                b.setShiZhuangNum(info.getShiZhuangNum());
                b.setLevelFetchFlag(info.getLevelFetchFlag());
            }
            sendBoxInfo(session, b.build());
        } catch (Exception e) {
            log.error("[Box] sendCurrentInfo error for roleId={}", roleId, e);
            sendBoxInfo(session, Msgbox.PB_SCBoxInfo.newBuilder().build());
        }
    }

    private void sendCurrentSetting(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.BoxSettingResp setting = boxFeign.getSetting(roleId);
            sendSettingInfo(session, buildSettingInfo(setting));
        } catch (Exception e) {
            log.debug("[Box] sendCurrentSetting skipped roleId={} ex={}", roleId, e.toString());
        }
    }

    private int fetchRoleLevel(PlayerSession session, Long roleId) {
        int fallbackLevel = 1;
        if (session.getUserId() == null || roleId == null) {
            return fallbackLevel;
        }

        Integer resolvedLevel = bestEffortFetch(() -> {
            OtherRoleDTOs.OtherRoleInfo role = roleFeign.getOtherRole(session.getUserId(), String.valueOf(roleId));
            if (role != null && role.attributes() != null && role.attributes().level() > 0) {
                return role.attributes().level();
            }
            return fallbackLevel;
        }, roleId, "fetchRoleLevel");
        return resolvedLevel != null && resolvedLevel > 0 ? resolvedLevel : fallbackLevel;
    }

    private boolean sendPendingEquipInfo(PlayerSession session, Long roleId, int defaultIsNew) {
        try {
            BoxDTOs.BoxCompareStateResp compareState = bestEffortFetch(() -> boxFeign.getCompareState(roleId), roleId, "getCompareState");
            if (compareState != null) {
                BoxDTOs.EquipInfo compareEquipInfo = BoxDTOs.equipInfoFromCompareState(compareState, defaultIsNew);
                if (hasUsablePendingEquip(compareEquipInfo)) {
                    int isNew = compareEquipInfo.getIsNew() != null ? compareEquipInfo.getIsNew() : defaultIsNew;
                    sendEquipInfo(session, buildEquipInfo(compareEquipInfo.getEquipInfo(), isNew), compareState);
                    return true;
                }
                if (isInvalidPendingEquip(compareEquipInfo)) {
                    Integer itemId = compareEquipInfo.getEquipInfo().getItemId();
                    log.warn("[Box] ignore stale compare equip roleId={} itemId={}", roleId, itemId);
                    clearCompareStateSafely(roleId, "invalid compare itemId=" + itemId);
                    compareState = null;
                }
            }

            BoxDTOs.EquipInfo equipInfo = bestEffortFetch(() -> boxFeign.equipInfo(roleId), roleId, "equipInfo");
            if (hasUsablePendingEquip(equipInfo)) {
                int isNew = equipInfo.getIsNew() != null ? equipInfo.getIsNew() : defaultIsNew;
                sendEquipInfo(session, buildEquipInfo(equipInfo.getEquipInfo(), isNew), compareState);
                return true;
            }
            if (isInvalidPendingEquip(equipInfo)) {
                Integer itemId = equipInfo.getEquipInfo().getItemId();
                log.warn("[Box] ignore stale pending equip roleId={} itemId={}", roleId, itemId);
                clearCompareStateSafely(roleId, "invalid pending itemId=" + itemId);
                sendEquipInfo(session, buildEmptyEquipInfo());
            }
        } catch (Exception e) {
            log.debug("[Box] pending equip fetch skipped roleId={} ex={}", roleId, e.toString());
        }
        return false;
    }

    private boolean hasUsablePendingEquip(BoxDTOs.EquipInfo equipInfo) {
        return equipInfo != null
                && equipInfo.getEquipInfo() != null
                && equipInfo.getEquipInfo().getItemId() != null
                && equipInfo.getEquipInfo().getItemId() > 1;
    }

    private boolean isInvalidPendingEquip(BoxDTOs.EquipInfo equipInfo) {
        return equipInfo != null
                && equipInfo.getEquipInfo() != null
                && equipInfo.getEquipInfo().getItemId() != null
                && equipInfo.getEquipInfo().getItemId() > 0
                && equipInfo.getEquipInfo().getItemId() <= 1;
    }

    private void clearCompareStateSafely(Long roleId, String reason) {
        if (roleId == null) {
            return;
        }
        try {
            boxFeign.clearCompareState(roleId);
        } catch (Exception e) {
            log.debug("[Box] clearCompareState skipped roleId={} reason={} ex={}", roleId, reason, e.toString());
        }
    }

    private Msgbox.PB_SCBoxEquipInfo sanitizeClientEquipInfo(Long roleId,
                                                             Msgbox.PB_SCBoxEquipInfo info,
                                                             String source) {
        if (info == null || !info.hasEquipInfo()) {
            return info;
        }
        int itemId = info.getEquipInfo().getItemId();
        if (itemId > 0 && itemId <= 1) {
            log.warn("[Box] suppress invalid equip payload roleId={} source={} itemId={}", roleId, source, itemId);
            clearCompareStateSafely(roleId, source + " invalid equip itemId=" + itemId);
            return buildEmptyEquipInfo();
        }
        return info;
    }

    private BoxDTOs.BoxCompareStateResp sanitizeClientCompareState(Long roleId,
                                                                   BoxDTOs.BoxCompareStateResp compareState,
                                                                   String source) {
        if (compareState == null || compareState.getCandidateEquip() == null) {
            return compareState;
        }
        Integer itemId = compareState.getCandidateEquip().getItemId();
        if (itemId != null && itemId > 0 && itemId <= 1) {
            log.warn("[Box] suppress invalid compare payload roleId={} source={} itemId={}", roleId, source, itemId);
            clearCompareStateSafely(roleId, source + " invalid compare itemId=" + itemId);
            return null;
        }
        return compareState;
    }

    private Msgbox.PB_SCBoxEquipInfo buildEquipInfo(BoxDTOs.EquipRolled rolled, int isNew) {
        Msgbox.PB_SCBoxEquipInfo.Builder b = Msgbox.PB_SCBoxEquipInfo.newBuilder();
        b.setIsNew(isNew);
        if (rolled != null) {
            Msgequip.PB_EquipData.Builder e = Msgequip.PB_EquipData.newBuilder();
            if (rolled.getEquipType() != null) e.setEquipType(rolled.getEquipType());
            if (rolled.getItemId()    != null) e.setItemId(rolled.getItemId());
            if (rolled.getHp()        != null) e.setHp(rolled.getHp());
            if (rolled.getAttack()    != null) e.setAttack(rolled.getAttack());
            if (rolled.getDefend()    != null) e.setDefend(rolled.getDefend());
            if (rolled.getSpeed()     != null) e.setSpeed(rolled.getSpeed());
            if (rolled.getAttrType1() != null) e.setAttrType1(rolled.getAttrType1());
            if (rolled.getAttrValue1()!= null) e.setAttrValue1(rolled.getAttrValue1());
            if (rolled.getAttrType2() != null) e.setAttrType2(rolled.getAttrType2());
            if (rolled.getAttrValue2()!= null) e.setAttrValue2(rolled.getAttrValue2());
            b.setEquipInfo(e.build());
        }
        return b.build();
    }

    // Client box module đóng popup khi equipType = -1 (pending rỗng).
    private Msgbox.PB_SCBoxEquipInfo buildEmptyEquipInfo() {
        Msgequip.PB_EquipData empty = Msgequip.PB_EquipData.newBuilder()
                .setEquipType(-1)
                .setItemId(0)
                .build();
        return Msgbox.PB_SCBoxEquipInfo.newBuilder()
                .setIsNew(0)
                .setEquipInfo(empty)
                .build();
    }

    private Msgbox.PB_SCBoxSetingInfo buildSettingInfo(BoxDTOs.BoxSettingResp s) {
        Msgbox.PB_SCBoxSetingInfo.Builder b = Msgbox.PB_SCBoxSetingInfo.newBuilder();
        if (s != null) {
            b.setBoxSet(Msgbox.PB_BoxSet.newBuilder()
                    .setEquipEqality(s.getEquipEqality())
                    .setConditionFirstMark(s.getConditionFirstMark())
                    .setConditionFirst1(s.getConditionFirst1())
                    .setConditionFirst2(s.getConditionFirst2())
                    .setConditionSecondMark(s.getConditionSecondMark())
                    .setConditionSecond1(s.getConditionSecond1())
                    .setConditionSecond2(s.getConditionSecond2())
                    .setRetainMark(s.getRetainMark())
                    .setChallengeMark(s.getChallengeMark())
                    .setEquipCapMark(s.getEquipCapMark())
                    .setEquipSellMark(s.getEquipSellMark())
                    .setOpenFiveMark(s.getOpenFiveMark())
                    .build());
        }
        return b.build();
    }

    private void sendEquipInfo(PlayerSession session, Msgbox.PB_SCBoxEquipInfo info) {
        sendEquipInfo(session, info, null);
    }

    private void sendEquipInfo(PlayerSession session,
                               Msgbox.PB_SCBoxEquipInfo info,
                               BoxDTOs.BoxCompareStateResp compareState) {
        try {
            Long roleId = session.getRoleId();
            Msgbox.PB_SCBoxEquipInfo payload1615 = sanitizeClientEquipInfo(roleId, info, "sendEquipInfo");
            compareState = sanitizeClientCompareState(roleId, compareState, "sendEquipInfo");
            if (compareState != null && compareState.getCandidateEquip() != null) {
                BoxDTOs.EquipRolled candidateRolled = compareState.getCandidateEquip().toEquipRolled();
                int isNew = compareState.getIsNew() != null ? compareState.getIsNew() : (payload1615 != null ? payload1615.getIsNew() : 0);
                payload1615 = sanitizeClientEquipInfo(roleId, buildEquipInfo(candidateRolled, isNew), "compare candidate");
            }
            if (payload1615 == null) {
                payload1615 = buildEmptyEquipInfo();
            }
            // Legacy clients still rely on 1615 to open/close equip popup.
            Emitters.emit(session, 1615, payload1615.toByteArray());
            if (compareState != null) {
                if (compareState.getCandidateEquip() != null || compareState.getEquippedBefore() != null) {
                    String candidateStats = compareState.getCandidateEquip() == null
                            ? "null"
                            : String.format("itemId=%s hp=%s atk=%s def=%s spd=%s",
                            compareState.getCandidateEquip().getItemId(),
                            compareState.getCandidateEquip().getHp(),
                            compareState.getCandidateEquip().getAttack(),
                            compareState.getCandidateEquip().getDefend(),
                            compareState.getCandidateEquip().getSpeed());
                    String equippedBeforeStats = compareState.getEquippedBefore() == null
                            ? "null"
                            : String.format("itemId=%s hp=%s atk=%s def=%s spd=%s",
                            compareState.getEquippedBefore().getItemId(),
                            compareState.getEquippedBefore().getHp(),
                            compareState.getEquippedBefore().getAttack(),
                            compareState.getEquippedBefore().getDefend(),
                            compareState.getEquippedBefore().getSpeed());
                    log.info("[Box] emit compare roleId={} stateVersion={} candidate={} equippedBefore={}",
                            session.getRoleId(),
                            compareState.getStateVersion(),
                            candidateStats,
                            equippedBeforeStats);
                }
                Emitters.emit(session, MsgIds.SC_BOX_EQUIP_COMPARE_INFO, buildCompareInfo(payload1615, compareState).toByteArray());
            }
        } catch (Exception e) {
            log.error("[Box] sendEquipInfo failed", e);
        }
    }

    private Msgbox.PB_SCBoxEquipCompareInfo buildCompareInfo(Msgbox.PB_SCBoxEquipInfo legacyInfo,
                                                             BoxDTOs.BoxCompareStateResp compareState) {
        Msgbox.PB_SCBoxEquipCompareInfo.Builder b = Msgbox.PB_SCBoxEquipCompareInfo.newBuilder();

        if (compareState != null) {
            b.setCompareState(toCompareStateCode(compareState));
            if (compareState.getCandidateEquip() != null) {
                b.setCandidateEquip(toPbEquip(compareState.getCandidateEquip()));
            } else if (legacyInfo != null && legacyInfo.hasEquipInfo()) {
                b.setCandidateEquip(legacyInfo.getEquipInfo());
            }
            if (compareState.getEquippedBefore() != null) {
                b.setEquippedBefore(toPbEquip(compareState.getEquippedBefore()));
            } else {
                // Keep old-slot explicit to prevent client fallback that may mirror candidate attrs into old panel.
                b.setEquippedBefore(buildEmptyCompareEquip());
            }
            if (compareState.getStateVersion() != null && !compareState.getStateVersion().isBlank()) {
                b.setStateVersion(compareState.getStateVersion());
            } else {
                b.setStateVersion("missing_state_version");
            }
            return b.build();
        }

        if (legacyInfo != null && legacyInfo.hasEquipInfo()) {
            b.setCandidateEquip(legacyInfo.getEquipInfo());
            int compareStateCode = legacyInfo.getEquipInfo().getEquipType() == -1 ? 0 : 1;
            b.setCompareState(compareStateCode);
        }
        return b.build();
    }

    private Msgequip.PB_EquipData buildEmptyCompareEquip() {
        return Msgequip.PB_EquipData.newBuilder()
                .setEquipType(-1)
                .setItemId(0)
                .build();
    }

    private Msgequip.PB_EquipData toPbEquip(BoxDTOs.BoxCompareSnapshotDTO snapshot) {
        Msgequip.PB_EquipData.Builder b = Msgequip.PB_EquipData.newBuilder();
        if (snapshot.getEquipType() != null) b.setEquipType(snapshot.getEquipType());
        if (snapshot.getItemId() != null) b.setItemId(snapshot.getItemId());
        if (snapshot.getHp() != null) b.setHp(snapshot.getHp());
        if (snapshot.getAttack() != null) b.setAttack(snapshot.getAttack());
        if (snapshot.getDefend() != null) b.setDefend(snapshot.getDefend());
        if (snapshot.getSpeed() != null) b.setSpeed(snapshot.getSpeed());
        if (snapshot.getAttrType1() != null) b.setAttrType1(snapshot.getAttrType1());
        if (snapshot.getAttrValue1() != null) b.setAttrValue1(snapshot.getAttrValue1());
        if (snapshot.getAttrType2() != null) b.setAttrType2(snapshot.getAttrType2());
        if (snapshot.getAttrValue2() != null) b.setAttrValue2(snapshot.getAttrValue2());
        return b.build();
    }

    private int toCompareStateCode(BoxDTOs.BoxCompareStateResp compareState) {
        if (compareState == null) {
            return 0;
        }
        String status = compareState.getStatus();
        if (status == null || status.isBlank()) {
            return compareState.getCandidateEquip() != null ? 1 : 0;
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING", "OPENED", "OPENED_PENDING" -> 1;
            // Incomplete means old-equip snapshot is unknown; avoid showing full compare mode.
            case "PENDING_COMPARE_INCOMPLETE" -> 0;
            case "WORE", "EQUIPPED" -> 2;
            case "SOLD" -> 3;
            case "DECOMPOSED" -> 4;
            default -> compareState.getCandidateEquip() != null ? 1 : 0;
        };
    }

    private void sendBoxInfo(PlayerSession session, Msgbox.PB_SCBoxInfo info) {
        try {
            Emitters.emit(session, 1616, info.toByteArray());
        } catch (Exception e) {
            log.error("[Box] sendBoxInfo failed", e);
        }
    }

    private void sendSettingInfo(PlayerSession session, Msgbox.PB_SCBoxSetingInfo info) {
        try {
            Emitters.emit(session, 1617, info.toByteArray());
        } catch (Exception e) {
            log.error("[Box] sendSettingInfo failed", e);
        }
    }

    private void sendSellInfo(PlayerSession session, Msgbox.PB_SCBoxSellInfo info) {
        try {
            Emitters.emit(session, 1618, info.toByteArray());
        } catch (Exception e) {
            log.error("[Box] sendSellInfo failed", e);
        }
    }

    private void sendBonusItemNotice(PlayerSession session, List<Map<String, Object>> bonusItems) {
        if (session == null || bonusItems == null || bonusItems.isEmpty()) {
            return;
        }

        Msgknapsack.PB_SCGetItemNotice.Builder notice = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                .setGetType(0);
        int addedCount = 0;
        for (Map<String, Object> bonusItem : bonusItems) {
            if (bonusItem == null || bonusItem.isEmpty()) {
                continue;
            }
            int itemId = safeInt(bonusItem.get("itemId"), safeInt(bonusItem.get("item_id"), 0));
            long num = safeLong(bonusItem.get("num"), safeLong(bonusItem.get("amount"), 0L));
            if (itemId <= 0 || num <= 0L) {
                continue;
            }
            notice.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                    .setItemId(itemId)
                    .setNum(num)
                    .build());
            addedCount++;
        }

        if (addedCount <= 0) {
            return;
        }

        try {
            Emitters.emit(session, MsgIds.SC_GET_ITEM_NOTICE, notice.build().toByteArray());
            log.info("[Box] emitted bonus notice roleId={} itemCount={}", session.getRoleId(), addedCount);
        } catch (Exception e) {
            log.error("[Box] sendBonusItemNotice failed roleId={}", session.getRoleId(), e);
        }
    }

    // Đồng bộ danh sách trang bị sau khi wear từ box để client refresh trạng thái đang mặc.
    private void sendEquipList(PlayerSession session, Long roleId) {
        try {
            EquipDTOs.ListResp resp = equipHttpClient.list(String.valueOf(roleId));
            Msgequip.PB_SCEquipListInfo.Builder b = Msgequip.PB_SCEquipListInfo.newBuilder();
            if (resp != null && resp.getItems() != null) {
                for (EquipDTOs.EquipItem item : resp.getItems()) {
                    b.addEquipList(Msgequip.PB_EquipData.newBuilder()
                            .setEquipType(item.getEquipType())
                            .setItemId(item.getItemId())
                            .setHp(item.getHp())
                            .setAttack(item.getAttack())
                            .setDefend(item.getDefend())
                            .setSpeed(item.getSpeed())
                            .setAttrType1(item.getAttrType1())
                            .setAttrValue1(item.getAttrValue1())
                            .setAttrType2(item.getAttrType2())
                            .setAttrValue2(item.getAttrValue2())
                            .build());
                }
            }
            Emitters.emit(session, 1605, b.build().toByteArray());
        } catch (Exception e) {
            log.debug("[Box] sendEquipList skipped roleId={} ex={}", roleId, e.toString());
        }
    }

    private void pushRoleAttr(PlayerSession session) {
        Thread.ofVirtual().start(() -> {
            try {
                roleServiceHandler.pushRoleState(session)
                        .onErrorResume(ex -> {
                            log.debug("[Box] pushRoleAttr skipped roleId={} ex={}", session.getRoleId(), ex.toString());
                            return Mono.empty();
                        })
                        .block(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.debug("[Box] pushRoleAttr error roleId={} ex={}", session.getRoleId(), e.toString());
            }
        });
    }

    private void pushWalletBalance(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
                if (walletResp != null && walletResp.balances() != null) {
                    Emitters.sendWalletBalances(session, walletResp.balances());
                }
            } catch (Exception e) {
                log.debug("[Box] pushWalletBalance skipped roleId={} ex={}", roleId, e.toString());
            }
        });
    }

    private void refreshRoleAndWalletAfterSell(PlayerSession session, Long roleId) {
        pushRoleAttr(session);
        pushWalletBalance(session, roleId);

        // Best-effort retry shortly after first push to mitigate stale reads/cache races.
        Mono.delay(Duration.ofMillis(250))
                .onErrorResume(ex -> Mono.empty())
                .subscribe(ignored -> {
                    pushRoleAttr(session);
                    pushWalletBalance(session, roleId);
                });
    }

    private void refreshBagAfterOpen(PlayerSession session, Long roleId) {
        if (session == null || roleId == null) {
            return;
        }

        // Arm a one-shot gate: allow exactly one UI bag update for this open flow.
        // If the first push succeeds, the later duplicate bag-changed update is skipped.
        // If the first push fails, the fallback update is still allowed through.
        if (bagUpdateGate != null) {
            bagUpdateGate.arm(roleId);
        }

        // Keep the rescue retry, but only when the first immediate push actually failed.
        // If the first push succeeds, a blind delayed second 1505 can arrive late and overwrite
        // the fresher 1506 from BagChangedConsumer, which is what caused the box count rollback.
        boolean immediatePushSucceeded = pushBagSnapshot(session, roleId, "initial");
        if (!immediatePushSucceeded) {
            Mono.delay(Duration.ofMillis(300))
                    .onErrorResume(ex -> Mono.empty())
                    .subscribe(ignored -> pushBagSnapshotAsync(session, roleId));
        }
    }

    private boolean pushBagSnapshot(PlayerSession session, Long roleId, String phase) {
        AtomicBoolean success = new AtomicBoolean(true);
        try {
            bagHandler.pushAll(session)
                    .doOnError(ex -> success.set(false))
                    .onErrorResume(ex -> {
                        log.debug("[Box] refreshBagAfterOpen {} skipped roleId={} ex={}", phase, roleId, ex.toString());
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(2));
            boolean delivered = success.get();
            if (delivered && bagUpdateGate != null) {
                bagUpdateGate.markDelivered(roleId);
            }
            return delivered;
        } catch (Exception e) {
            log.debug("[Box] refreshBagAfterOpen {} failed roleId={} ex={}", phase, roleId, e.toString());
            return false;
        }
    }

    private void pushBagSnapshotAsync(PlayerSession session, Long roleId) {
        Thread.ofVirtual().start(() -> pushBagSnapshot(session, roleId, "retry"));
    }

    private int clampPositiveInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private int safeInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long safeLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private <T> T bestEffortFetch(Supplier<T> supplier, Long roleId, String operation) {
        long timeoutMs = Math.max(50L, boxFetchTimeoutMs);
        long startedAt = System.currentTimeMillis();
        String token = FeignTokenHolder.get();
        try {
            T result = Mono.fromCallable(() -> {
                        if (token != null && !token.isBlank()) {
                            FeignTokenHolder.set(token);
                        }
                        try {
                            return supplier.get();
                        } finally {
                            FeignTokenHolder.clear();
                        }
                    })
                    .subscribeOn(feignVtScheduler)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(ex -> {
                        log.debug("[Box] {} skipped roleId={} timeoutMs={} ex={}", operation, roleId, timeoutMs, ex.toString());
                        return Mono.empty();
                    })
                    .block();
            long elapsedMs = System.currentTimeMillis() - startedAt;
            if (elapsedMs > 80L) {
                log.info("[Box.fetch] roleId={} operation={} elapsedMs={} timeoutMs={} hasResult={}",
                        roleId, operation, elapsedMs, timeoutMs, result != null);
            }
            return result;
        } catch (Exception e) {
            log.debug("[Box] {} failed roleId={} timeoutMs={} ex={}", operation, roleId, timeoutMs, e.toString());
            return null;
        }
    }
}

package com.SouthMillion.webSocket_server.handler.activity;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.LazyLoadHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgopenserveractivity.Msgopenserveractivity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handles open-server activity operations (新服活动).
 *
 * Four sub-systems:
 *   2160 PB_CSSevenDaySignReq        → 2161 PB_SCSevenDaySignInfo        (七日签到)
 *   2162 PB_CSLuckUnpackingReq       → 2163 PB_SCLuckUnpackingInfo       (开箱大吉)
 *   2164 PB_CSNewAreaPreferentialReq → 2165 PB_SCNewAreaPreferentialInfo (新服特惠)
 *   2166 PB_CSMarketShopReq          → 2167 PB_SCMarketShopInfo          (集市商店)
 *
 * opera_type: 1=GET_INFO, 2=CLAIM/BUY, 3=REFRESH (MarketShop only)
 *
 * This handler implements LazyLoadHandler for on-demand loading when the player opens the activity UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenServerActivityHandler implements MessageHandler, LazyLoadHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OpenServerActivityHandler.class);

    private final ActivityFeign activityFeign;
    private final reactor.core.scheduler.Scheduler feignVtScheduler;

    private static final int OP_GET_INFO = 1;
    private static final int OP_CLAIM    = 2;
    private static final int OP_REFRESH  = 3;

    @Override
    public int[] interests() {
        return new int[]{2160, 2162, 2164, 2166};
    }

    @Override
    public String getModuleName() {
        return "activity";
    }

    @Override
    public Mono<Void> loadOnDemand(PlayerSession ps) {
        log.info("[OpenActivity] Lazy loading for roleId={}", ps.getRoleId());
        return pushAll(ps);
    }

    /** Push initial open-server activity snapshots right after login bootstrap. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) {
            return Mono.empty();
        }

        final String roleIdStr = String.valueOf(roleId);

        // Parallel execution: fetch all 4 activities concurrently instead of sequentially
        // Old: sevenDay → luck → newArea → market (sequential, ~2000ms)
        // New: Mono.zip all 4 calls (parallel, ~500ms - 75% faster)
        // Phase 5: Use virtual thread scheduler (feignVtScheduler) for better concurrency
        return Mono.zip(
                Mono.fromCallable(() -> activityFeign.getSevenDay(roleIdStr))
                        .subscribeOn(feignVtScheduler)
                        .onErrorResume(e -> {
                            log.debug("[OpenActivity/SevenDay] bootstrap fetch skipped roleId={} ex={}", roleId, e.toString());
                            return Mono.empty();
                        }),
                Mono.fromCallable(() -> activityFeign.getLuck(roleIdStr))
                        .subscribeOn(feignVtScheduler)
                        .onErrorResume(e -> {
                            log.debug("[OpenActivity/LuckUnpacking] bootstrap fetch skipped roleId={} ex={}", roleId, e.toString());
                            return Mono.empty();
                        }),
                Mono.fromCallable(() -> activityFeign.getNewArea(roleIdStr))
                        .subscribeOn(feignVtScheduler)
                        .onErrorResume(e -> {
                            log.debug("[OpenActivity/NewArea] bootstrap fetch skipped roleId={} ex={}", roleId, e.toString());
                            return Mono.empty();
                        }),
                Mono.fromCallable(() -> activityFeign.getMarket(roleIdStr))
                        .subscribeOn(feignVtScheduler)
                        .onErrorResume(e -> {
                            log.debug("[OpenActivity/MarketShop] bootstrap fetch skipped roleId={} ex={}", roleId, e.toString());
                            return Mono.empty();
                        })
        ).flatMap(tuple -> {
            // Emit all activities
            pushSafely("SevenDay", roleId, () -> emitSevenDayInfo(session, tuple.getT1()));
            pushSafely("LuckUnpacking", roleId, () -> emitLuckUnpackingInfo(session, tuple.getT2()));
            pushSafely("NewArea", roleId, () -> emitNewAreaPreferentialInfo(session, tuple.getT3()));
            pushSafely("MarketShop", roleId, () -> emitMarketShopInfo(session, tuple.getT4()));
            return Mono.<Void>empty();
        }).onErrorResume(e -> {
            log.warn("[OpenActivity] pushAll parallel fetch error roleId={}: {}", roleId, e.getMessage());
            return Mono.<Void>empty();
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                switch (msgId) {
                    case 2160 -> handleSevenDaySign(session, roleId, payload);
                    case 2162 -> handleLuckUnpacking(session, roleId, payload);
                    case 2164 -> handleNewAreaPreferential(session, roleId, payload);
                    case 2166 -> handleMarketShop(session, roleId, payload);
                    default   -> log.warn("[OpenActivity] Unknown msgId={}", msgId);
                }
            } catch (Exception e) {
                log.error("[OpenActivity] Error for msgId={}, roleId={}", msgId, session.getRoleId(), e);
            }
        });
    }

    private void pushSafely(String activityName, Long roleId, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[OpenActivity/{}] bootstrap skipped roleId={} ex={}", activityName, roleId, e.toString());
        }
    }

    private void emitSevenDayInfo(PlayerSession session, Map<String, Object> data) {
        Msgopenserveractivity.PB_SCSevenDaySignInfo.Builder builder =
                Msgopenserveractivity.PB_SCSevenDaySignInfo.newBuilder();
        if (data != null) {
            if (data.get("endTimestamp") instanceof Number n) builder.setEndTimestamp(n.intValue());
            if (data.get("days") instanceof Number n)         builder.setDays(n.intValue());
            if (data.get("receiveFlag") instanceof Number n)  builder.setReceiveFlag(n.intValue());
        }
        Emitters.emit(session, 2161, builder.build().toByteArray());
    }

    private void emitLuckUnpackingInfo(PlayerSession session, Map<String, Object> data) {
        Msgopenserveractivity.PB_SCLuckUnpackingInfo.Builder builder =
                Msgopenserveractivity.PB_SCLuckUnpackingInfo.newBuilder();
        if (data != null) {
            if (data.get("endTimestamp") instanceof Number n) builder.setEndTimestamp(n.intValue());
            if (data.get("receiveFlag") instanceof Number n)  builder.setReceiveFlag(n.intValue());
            if (data.get("openBoxNum") instanceof Number n)   builder.setOpenBoxNum(n.intValue());
            if (data.get("boxLevel") instanceof Number n)     builder.setBoxLevel(n.intValue());
        }
        Emitters.emit(session, 2163, builder.build().toByteArray());
    }

    private void emitNewAreaPreferentialInfo(PlayerSession session, Map<String, Object> data) {
        Msgopenserveractivity.PB_SCNewAreaPreferentialInfo.Builder builder =
                Msgopenserveractivity.PB_SCNewAreaPreferentialInfo.newBuilder();
        if (data != null && data.get("endTimestamp") instanceof Number n) {
            builder.setEndTimestamp(n.intValue());
        }
        Emitters.emit(session, 2165, builder.build().toByteArray());
    }

    private void emitMarketShopInfo(PlayerSession session, Map<String, Object> data) {
        Msgopenserveractivity.PB_SCMarketShopInfo.Builder builder =
                Msgopenserveractivity.PB_SCMarketShopInfo.newBuilder();
        if (data != null) {
            if (data.get("endTimestamp") instanceof Number n)      builder.setEndTimestamp(n.intValue());
            if (data.get("nextFreeRefresh") instanceof Number n)   builder.setNextFreeRefreshTimestamp(n.intValue());
            if (data.get("nextAutoRefresh") instanceof Number n)   builder.setNextAutoRefreshTimestamp(n.intValue());
            if (data.get("curShopGroup") instanceof Number n)      builder.setCurShopGroup(n.intValue());
            if (data.get("randomCnts") instanceof Number n)        builder.setRandomCnts(n.intValue());
        }
        Emitters.emit(session, 2167, builder.build().toByteArray());
    }

    // 2160 → 2161 PB_SCSevenDaySignInfo
    private void handleSevenDaySign(PlayerSession session, Long roleId, byte[] payload) throws Exception {
        Msgopenserveractivity.PB_CSSevenDaySignReq req =
                Msgopenserveractivity.PB_CSSevenDaySignReq.parseFrom(payload);
        int op     = req.hasOperaType() ? req.getOperaType() : OP_GET_INFO;
        int param1 = req.hasParam1()    ? req.getParam1()    : 1;

        log.debug("[OpenActivity/SevenDay] op={}, roleId={}", op, roleId);

        Map<String, Object> data = (op == OP_CLAIM)
                ? activityFeign.claimSevenDay(String.valueOf(roleId), param1)
                : activityFeign.getSevenDay(String.valueOf(roleId));
        emitSevenDayInfo(session, data);
    }

    // 2162 → 2163 PB_SCLuckUnpackingInfo
    private void handleLuckUnpacking(PlayerSession session, Long roleId, byte[] payload) throws Exception {
        Msgopenserveractivity.PB_CSLuckUnpackingReq req =
                Msgopenserveractivity.PB_CSLuckUnpackingReq.parseFrom(payload);
        int op     = req.hasOperaType() ? req.getOperaType() : OP_GET_INFO;
        int param1 = req.hasParam1()    ? req.getParam1()    : 0;

        log.debug("[OpenActivity/LuckUnpacking] op={}, roleId={}", op, roleId);

        Map<String, Object> data = (op == OP_CLAIM)
                ? activityFeign.claimLuck(String.valueOf(roleId), param1)
                : activityFeign.getLuck(String.valueOf(roleId));
        emitLuckUnpackingInfo(session, data);
    }

    // 2164 → 2165 PB_SCNewAreaPreferentialInfo
    private void handleNewAreaPreferential(PlayerSession session, Long roleId, byte[] payload) throws Exception {
        Msgopenserveractivity.PB_CSNewAreaPreferentialReq req =
                Msgopenserveractivity.PB_CSNewAreaPreferentialReq.parseFrom(payload);
        int op     = req.hasOperaType() ? req.getOperaType() : OP_GET_INFO;
        int param1 = req.hasParam1()    ? req.getParam1()    : 0;

        log.debug("[OpenActivity/NewArea] op={}, roleId={}", op, roleId);

        Map<String, Object> data = (op == OP_CLAIM)
                ? activityFeign.buyNewArea(String.valueOf(roleId), param1)
                : activityFeign.getNewArea(String.valueOf(roleId));
        emitNewAreaPreferentialInfo(session, data);
    }

    // 2166 → 2167 PB_SCMarketShopInfo
    private void handleMarketShop(PlayerSession session, Long roleId, byte[] payload) throws Exception {
        Msgopenserveractivity.PB_CSMarketShopReq req =
                Msgopenserveractivity.PB_CSMarketShopReq.parseFrom(payload);
        int op     = req.hasOperaType() ? req.getOperaType() : OP_GET_INFO;
        int param1 = req.hasParam1()    ? req.getParam1()    : 0;

        log.debug("[OpenActivity/MarketShop] op={}, p1={}, roleId={}", op, param1, roleId);

        Map<String, Object> data;
        if (op == OP_CLAIM) {
            data = activityFeign.buyMarket(String.valueOf(roleId), param1);
        } else if (op == OP_REFRESH) {
            data = activityFeign.refreshMarket(String.valueOf(roleId));
        } else {
            data = activityFeign.getMarket(String.valueOf(roleId));
        }
        emitMarketShopInfo(session, data);
    }
}

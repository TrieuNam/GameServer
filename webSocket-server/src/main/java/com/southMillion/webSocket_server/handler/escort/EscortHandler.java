package com.SouthMillion.webSocket_server.handler.escort;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.EscortGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.grpc.escort.*;
import org.SouthMillion.proto.Msgescort.Msgescort;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Escort Handler — CS:9620 PB_CSEscortReq
 * Uses protoc-generated classes from common-lib escort_service.proto
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscortHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EscortHandler.class);

    private final EscortGrpcClient escortGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;

    // Client contract from ESCORT_OPER_TYPE (0-based)
    private static final int OP_SHIP_UP = 0;
    private static final int OP_SET_SAIL = 1;
    private static final int OP_INTERCEPT = 2;
    private static final int OP_HELP = 3;
    private static final int OP_GET_SHIP_LIST = 4;
    private static final int OP_GET_HISTORY = 5;
    private static final int OP_GET_INTERCEPT_LIST = 6;
    private static final int OP_GET_SHIP_INFO = 7;
    private static final int OP_CLAIM_REWARD = 8;
    private static final int OP_BOSS_FIGHT = 9;
    private static final int OP_HARM_REWARD = 10;

    // Client contract from ESCORT_RET_TYPE (0-based)
    private static final int RET_SHIP_UP = 0;
    private static final int RET_SET_SAIL = 1;
    private static final int RET_TARGET_INTERCEPT = 2;
    private static final int RET_HELP = 3;

    @Override
    public int[] interests() { return new int[]{9620}; }

    /** Push on login: 9622 role info */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetInfo(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgescort.PB_CSEscortReq req = Msgescort.PB_CSEscortReq.parseFrom(payload);
                int type = req.hasType() ? req.getType() : 0;
                int p1   = req.hasP1()   ? req.getP1()   : 0;
                Long roleId = session.getRoleId();
                log.debug("[Escort] op={} p1={} roleId={}", type, p1, roleId);
                switch (type) {
                    case OP_SHIP_UP            -> handleShipUp(session, roleId);
                    case OP_SET_SAIL           -> handleStart(session, roleId, p1);
                    case OP_INTERCEPT          -> handleRob(session, roleId, p1);
                    case OP_HELP               -> handleSpeedup(session, roleId, p1);
                    case OP_GET_SHIP_LIST      -> handleGetShipList(session, roleId);
                    case OP_GET_HISTORY        -> handleGetHistory(session, roleId);
                    case OP_GET_INTERCEPT_LIST -> handleGetInterceptList(session, roleId);
                    case OP_GET_SHIP_INFO      -> handleGetShipInfo(session, roleId, p1);
                    case OP_CLAIM_REWARD       -> handleComplete(session, roleId);
                    case OP_BOSS_FIGHT, OP_HARM_REWARD -> {
                        log.info("[Escort] Unimplemented op={} roleId={}", type, roleId);
                        sendRet(session, type, -1, 0);
                    }
                    default -> { log.warn("[Escort] Unknown op={}", type); sendRet(session, type, -1, 0); }
                }
            } catch (Exception e) {
                log.error("[Escort] Error roleId={}", session.getRoleId(), e);
                sendRet(session, 0, -1, 0);
            }
        });
    }

    // ─── op=1: 9622 PB_SCEscortRoleInfo ─────────────────────────────────
    private void handleGetInfo(PlayerSession session, Long roleId) {
        try {
            EscortInfoResponse resp = escortGrpcClient.getEscortInfo(roleId);
            EscortRoleStats s = resp.getStats();
            Msgescort.PB_SCEscortRoleInfo.Builder b = Msgescort.PB_SCEscortRoleInfo.newBuilder()
                    .setShip(s.getShip())
                    .setEscortCount(s.getEscortCount())
                    .setInterceptCount(s.getInterceptCount())
                    .setHelpCount(s.getHelpCount())
                    .setMaxDamage(s.getMaxDamage())
                    .setRewardIndex(s.getRewardIndex());
            Emitters.emit(session, 9622, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Escort] handleGetInfo error", e);
            sendRet(session, 0, -1, 0);
        }
    }

    // ─── op=0: Ship up → 9621 PB_SCEscortRet(type=SHIP_UP) ─────────────
    private void handleShipUp(PlayerSession session, Long roleId) {
        try {
            EscortActionResponse resp = escortGrpcClient.upgradeShip(roleId);
            sendRet(session, RET_SHIP_UP, resp.getP1(), resp.getP2());
            if (resp.getSuccess()) {
                handleGetInfo(session, roleId);
            }
        } catch (Exception e) {
            log.error("[Escort] handleShipUp error", e);
            sendRet(session, RET_SHIP_UP, 0, 0);
        }
    }

    // ─── op=2: 9623 PB_SCEscortShipListInfo ──────────────────────────────
    private void handleGetShipList(PlayerSession session, Long roleId) {
        try {
            EscortShipListResponse resp = escortGrpcClient.getShipList(roleId);
            Msgescort.PB_SCEscortShipListInfo.Builder b = Msgescort.PB_SCEscortShipListInfo.newBuilder();
            if (resp.hasMyShip() && resp.getMyShip().getShipKey() > 0) {
                b.addMyShip(buildShipData(resp.getMyShip()));
            }
            for (EscortShipData ship : resp.getShipListList()) {
                b.addShipList(buildShipData(ship));
            }
            Emitters.emit(session, 9623, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Escort] handleGetShipList error", e);
            sendRet(session, OP_GET_SHIP_LIST, -1, 0);
        }
    }

    // ─── op=3: Start → 9621 PB_SCEscortRet ───────────────────────────────
    private void handleStart(PlayerSession session, Long roleId, int shipLevel) {
        try {
            EscortActionResponse resp = escortGrpcClient.startEscort(roleId, shipLevel);
            sendRet(session, RET_SET_SAIL, resp.getSuccess() ? 0 : -1, resp.getP1());
            if (resp.getSuccess()) {
                publishTaskProgress(roleId, taskActionConditionMapping.escortStartTaskKey(), "websocket-escort-start");
                handleGetShipList(session, roleId);
            }
        } catch (Exception e) {
            log.error("[Escort] handleStart error", e);
            sendRet(session, RET_SET_SAIL, -1, 0);
        }
    }

    // ─── op=4: Complete/Claim reward → 9621 ──────────────────────────────
    private void handleComplete(PlayerSession session, Long roleId) {
        try {
            EscortActionResponse resp = escortGrpcClient.claimReward(roleId);
            sendRet(session, OP_CLAIM_REWARD, resp.getSuccess() ? 0 : -1, resp.getP1());
            if (resp.getSuccess()) {
                publishTaskProgress(roleId, taskActionConditionMapping.escortCompleteTaskKey(), "websocket-escort-complete");
                syncPostClaimState(session, roleId);
                handleGetInfo(session, roleId);
            }
        } catch (Exception e) {
            log.error("[Escort] handleComplete error", e);
            sendRet(session, OP_CLAIM_REWARD, -1, 0);
        }
    }

    // ─── op=5: Rob/Intercept → 9621 ──────────────────────────────────────
    private void handleRob(PlayerSession session, Long roleId, int targetUid) {
        try {
            EscortActionResponse resp = escortGrpcClient.interceptEscort(roleId, 0, targetUid);
            sendRet(session, RET_TARGET_INTERCEPT, resp.getSuccess() ? 0 : -1, targetUid);
            if (resp.getSuccess()) {
                publishTaskProgress(roleId, taskActionConditionMapping.escortRobTaskKey(), "websocket-escort-rob");
            }
        } catch (Exception e) {
            log.error("[Escort] handleRob error", e);
            sendRet(session, RET_TARGET_INTERCEPT, -1, 0);
        }
    }

    // ─── op=6: Speedup/Help → 9621 ───────────────────────────────────────
    private void handleSpeedup(PlayerSession session, Long roleId, int targetUid) {
        try {
            EscortActionResponse resp = escortGrpcClient.helpEscort(roleId, 0, targetUid);
            sendRet(session, RET_HELP, resp.getSuccess() ? 0 : -1, targetUid);
        } catch (Exception e) {
            log.error("[Escort] handleSpeedup error", e);
            sendRet(session, RET_HELP, -1, 0);
        }
    }

    // ─── op=7: History → 9624 PB_SCEscortReportListInfo ──────────────────
    private void handleGetHistory(PlayerSession session, Long roleId) {
        try {
            EscortReportListResponse resp = escortGrpcClient.getReportList(roleId);
            Msgescort.PB_SCEscortReportListInfo.Builder b =
                    Msgescort.PB_SCEscortReportListInfo.newBuilder();
            int size = Math.min(resp.getReportListCount(), resp.getReportTimeCount());
            for (int i = 0; i < size; i++) {
                b.addReportList(resp.getReportList(i));
                b.addReportTime((int) resp.getReportTime(i));
            }
            Emitters.emit(session, 9624, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Escort] handleGetHistory error", e);
            sendRet(session, OP_GET_HISTORY, -1, 0);
        }
    }

    // ─── op=8: Intercept list → 9625 PB_SCEscortInterceptListInfo ────────
    private void handleGetInterceptList(PlayerSession session, Long roleId) {
        try {
            EscortInterceptListResponse resp = escortGrpcClient.getInterceptList(roleId);
            Msgescort.PB_SCEscortInterceptListInfo.Builder b =
                    Msgescort.PB_SCEscortInterceptListInfo.newBuilder();
            for (EscortInterceptData item : resp.getListList()) {
                b.addList(buildInterceptData(item));
            }
            Emitters.emit(session, 9625, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Escort] handleGetInterceptList error", e);
            sendRet(session, OP_GET_INTERCEPT_LIST, -1, 0);
        }
    }

    // ─── op=7: Ship info by ship_key → 9626 PB_SCEscortShipInfo ─────────
    private void handleGetShipInfo(PlayerSession session, Long roleId, int shipKey) {
        try {
            EscortInterceptListResponse resp = escortGrpcClient.getInterceptList(roleId);
            for (EscortInterceptData item : resp.getListList()) {
                if (item.getShip().getShipKey() == shipKey) {
                    Msgescort.PB_SCEscortShipInfo info = Msgescort.PB_SCEscortShipInfo.newBuilder()
                            .setInfo(buildInterceptData(item))
                            .build();
                    Emitters.emit(session, 9626, info.toByteArray());
                    return;
                }
            }

            Msgescort.PB_SCEscortShipData shipData = Msgescort.PB_SCEscortShipData.newBuilder()
                    .setShipKey(shipKey)
                    .build();
            Msgescort.PB_SCEscortInterceptData emptyInfo = Msgescort.PB_SCEscortInterceptData.newBuilder()
                    .setShip(shipData)
                    .build();
            Msgescort.PB_SCEscortShipInfo info = Msgescort.PB_SCEscortShipInfo.newBuilder()
                    .setInfo(emptyInfo)
                    .build();
            Emitters.emit(session, 9626, info.toByteArray());
        } catch (Exception e) {
            log.error("[Escort] handleGetShipInfo error roleId={} shipKey={}", roleId, shipKey, e);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────
    private Msgescort.PB_SCEscortShipData buildShipData(EscortShipData s) {
        return Msgescort.PB_SCEscortShipData.newBuilder()
                .setShip(s.getShip())
                .setUid(s.getUid())
                .setBeIntercept(s.getBeIntercept())
                .setOverTime((int) s.getOverTime())
                .setShipKey(s.getShipKey())
                .setIsHelp(s.getIsHelp())
                .build();
    }

    private Msgescort.PB_SCEscortInterceptData buildInterceptData(EscortInterceptData item) {
        Msgrole.PB_RoleInfo.Builder role = Msgrole.PB_RoleInfo.newBuilder();
        if (item.getRoleId() > 0) {
            role.setRoleId((int) item.getRoleId());
        }
        if (!item.getRoleName().isBlank()) {
            role.setName(com.google.protobuf.ByteString.copyFromUtf8(item.getRoleName()));
        }
        if (item.getLevel() > 0) {
            role.setLevel(item.getLevel());
        }

        return Msgescort.PB_SCEscortInterceptData.newBuilder()
                .setRoleInfo(role.build())
                .setShip(buildShipData(item.getShip()))
                .build();
    }

    private void sendRet(PlayerSession session, int type, int p1, int p2) {
        try {
            Msgescort.PB_SCEscortRet ret = Msgescort.PB_SCEscortRet.newBuilder()
                    .setType(type).setP1(p1).setP2(p2).build();
            Emitters.emit(session, 9621, ret.toByteArray());
        } catch (Exception e) {
            log.error("[Escort] sendRet failed", e);
        }
    }

    private void syncPostClaimState(PlayerSession session, Long roleId) {
        pushBagState(session, roleId);
        pushWalletBalance(session, roleId);

        Mono.delay(Duration.ofMillis(250))
                .onErrorResume(ex -> Mono.empty())
                .subscribe(ignored -> {
                    pushBagState(session, roleId);
                    pushWalletBalance(session, roleId);
                });
    }

    private void pushBagState(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            List<BagDTOs.ItemView> list = bagFeign.list(String.valueOf(roleId));
            Emitters.sendKnapsackAllInfo(session, list);
        } catch (Exception e) {
            log.warn("[Escort] Failed to push bag state for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private void pushWalletBalance(PlayerSession session, Long roleId) {
        if (roleId == null) {
            return;
        }
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(session, walletResp.balances());
            }
        } catch (Exception e) {
            log.warn("[Escort] Failed to push wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

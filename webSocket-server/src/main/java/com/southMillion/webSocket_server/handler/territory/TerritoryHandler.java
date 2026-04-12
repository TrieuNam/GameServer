package com.SouthMillion.webSocket_server.handler.territory;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.TerritoryGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.google.protobuf.ByteString;
import org.SouthMillion.grpc.territory.*;
import org.SouthMillion.proto.Msgterritory.Msgterritory;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Territory Handler — CS:9630 PB_CSTerritoryReq
 *
 * Wire: TerritoryHandler → TerritoryGrpcClient → territory-service (gRPC port 9086)
 *
 * SC messages (theo proto msgterritory.proto):
 *   9631 PB_SCTerritoryInfo        ← territory info
 *   9632 PB_SCTerritoryNeighbourInfo ← neighbour info
 *   9633 PB_SCTerritoryBotInfo     ← bot info
 *   9634 PB_SCTerritoryReportInfo  ← report/log
 *   9635 PB_SCTerritoryRedInfo     ← red dot
 *
 * Op types — khớp với client TERRITORY_REQ enum (TerritoryData.ts):
 *   0=INFO  1=NEIGHBOUR  2=FETCH_ITEM  3=FETCH_REWARD  4=BUY
 *   5=LEVEL_UP  6=BOT_STATUS  7=REFRESH_NEIGHBOUR  8=Log  9=REFRESH_CONTAINER
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerritoryHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TerritoryHandler.class);

    private final TerritoryGrpcClient territoryGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // FIX: căn chỉnh với client TERRITORY_REQ enum — trước đây sai hết dẫn đến
    // BOT_STATUS(6) không có case → sendError → gửi lại 9631 → vòng lặp vô hạn
    private static final int OP_GET_INFO             = 0;  // INFO
    private static final int OP_GET_NEIGHBOUR        = 1;  // NEIGHBOUR
    private static final int OP_FETCH_ITEM           = 2;  // FETCH_ITEM
    private static final int OP_FETCH_REWARD         = 3;  // FETCH_REWARD (collect)
    private static final int OP_BUY                  = 4;  // BUY bot
    private static final int OP_LEVEL_UP             = 5;  // LEVEL_UP
    private static final int OP_GET_BOT              = 6;  // BOT_STATUS ← key fix
    private static final int OP_REFRESH_NEIGHBOUR    = 7;  // REFRESH_NEIGHBOUR
    private static final int OP_GET_REPORT           = 8;  // Log
    private static final int OP_REFRESH_CONTAINER    = 9;  // REFRESH_CONTAINER

    @Override
    public int[] interests() {
        return new int[]{9630};
    }

    /** Push on login: 9631 territory info + 9635 red dot */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            handleGetInfo(session, roleId);
            handleGetRed(session, roleId);
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgterritory.PB_CSTerritoryReq req = Msgterritory.PB_CSTerritoryReq.parseFrom(payload);
                Long roleId = session.getRoleId();
                int type = req.hasType() ? req.getType() : 0;
                log.debug("[Territory] op={} roleId={}", type, roleId);
                switch (type) {
                    case OP_GET_INFO          -> handleGetInfo(session, roleId);
                    case OP_GET_NEIGHBOUR     -> handleGetNeighbour(session, roleId);
                    case OP_FETCH_ITEM        -> handleGetInfo(session, roleId);       // stub: trả info
                    case OP_FETCH_REWARD      -> handleDispatch(session, roleId, 1);   // gRPC action=1 collect
                    case OP_BUY               -> handleBuyBot(session, roleId);        // mua bot — stub riêng
                    case OP_LEVEL_UP          -> handleDispatch(session, roleId, 2);   // gRPC action=2 level_up
                    case OP_GET_BOT           -> handleGetBot(session, roleId);
                    case OP_REFRESH_NEIGHBOUR -> { handleGetNeighbour(session, roleId); taskProgressPublisher.publish(roleId, TaskCondition.TERRITORY_REFRESH_NEIGHBOUR.taskKey(), 1, "websocket-territory-refresh-neighbour"); }
                    case OP_GET_REPORT        -> handleGetReport(session, roleId);
                    case OP_REFRESH_CONTAINER -> handleGetRed(session, roleId);
                    default -> log.warn("[Territory] Unknown op={} roleId={}", type, roleId);
                }
            } catch (Exception e) {
                log.error("[Territory] Error roleId={}", session.getRoleId(), e);
                sendError(session);
            }
        });
    }

    // ─── op=0: 9631 PB_SCTerritoryInfo ───────────────────────────────────
    private void handleGetInfo(PlayerSession session, Long roleId) {
        try {
            TerritoryInfoResponse resp =
                    territoryGrpcClient.getTerritoryInfo(roleId);

            // Build role_info (field 1) — client crashes if this is null
            Msgrole.PB_RoleInfo.Builder roleInfo = Msgrole.PB_RoleInfo.newBuilder();
            if (roleId != null) {
                roleInfo.setRoleId(roleId.intValue());
            }
            if (session.getRoleName() != null && !session.getRoleName().isBlank()) {
                roleInfo.setName(ByteString.copyFromUtf8(session.getRoleName()));
            }

            Msgterritory.PB_SCTerritoryInfo.Builder b = Msgterritory.PB_SCTerritoryInfo.newBuilder()
                    .setRoleInfo(roleInfo.build())
                    .setTerritoryLevel(resp.getTerritoryLevel())
                    .setBotNum(resp.getBotNum())
                    .setBotRunNum(resp.getBotRunNum())
                    .setBotBuyCount(resp.getBotBuyCount())
                    .setRewardCount(resp.getRewardCount())
                    .setReason(resp.getReason());
            Emitters.emit(session, 9631, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Territory] handleGetInfo error", e);
            sendError(session);
        }
    }

    // ─── op=3/5: Collect(3) / LevelUp(5) → gRPC dispatchAction → re-push 9631
    private void handleDispatch(PlayerSession session, Long roleId, int type) {
        try {
            TerritoryActionResponse resp =
                    territoryGrpcClient.dispatchAction(roleId, type);
            if (resp.getSuccess()) {
                publishTaskProgress(roleId, type);
                if (type == 1) {
                    taskProgressPublisher.publish(roleId, TaskCondition.TERRITORY_MINE.taskKey(), 1, "websocket-territory-mine");
                }
                handleGetInfo(session, roleId);  // refresh info
            } else {
                sendError(session);
            }
        } catch (Exception e) {
            log.error("[Territory] handleDispatch op={} error", type, e);
            sendError(session);
        }
    }

    // ─── op=4: mua bot ──────────────────────────────────────────────────────
    private void handleBuyBot(PlayerSession session, Long roleId) {
        try {
            TerritoryActionResponse resp = territoryGrpcClient.dispatchAction(roleId, 3);
            if (resp.getSuccess()) {
                taskProgressPublisher.publish(roleId, TaskCondition.TERRITORY_BUY_BOT.taskKey(), 1, "websocket-territory-buy-bot");
            }
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Territory] handleBuyBot error roleId={}", roleId, e);
            handleGetInfo(session, roleId);
        }
    }

    // ─── op=1/7: 9632 PB_SCTerritoryNeighbourInfo ────────────────────────
    private void handleGetNeighbour(PlayerSession session, Long roleId) {
        try {
            TerritoryNeighbourResponse resp =
                    territoryGrpcClient.getNeighbourInfo(roleId);
            Msgterritory.PB_SCTerritoryNeighbourInfo.Builder b =
                    Msgterritory.PB_SCTerritoryNeighbourInfo.newBuilder()
                            .setNeighbourTime(resp.getNeighbourTime());
            Emitters.emit(session, 9632, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Territory] handleGetNeighbour error", e);
            sendError(session);
        }
    }

    // ─── op=6: 9633 PB_SCTerritoryBotInfo ────────────────────────────────
    private void handleGetBot(PlayerSession session, Long roleId) {
        try {
            territoryGrpcClient.getBotInfo(roleId);
            Msgterritory.PB_SCTerritoryBotInfo.Builder b =
                    Msgterritory.PB_SCTerritoryBotInfo.newBuilder();
            Emitters.emit(session, 9633, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Territory] handleGetBot error", e);
            sendError(session);
        }
    }

    // ─── op=8: 9634 PB_SCTerritoryReportInfo ─────────────────────────────
    private void handleGetReport(PlayerSession session, Long roleId) {
        try {
            territoryGrpcClient.getReportList(roleId);
            Msgterritory.PB_SCTerritoryReportInfo.Builder b =
                    Msgterritory.PB_SCTerritoryReportInfo.newBuilder();
            Emitters.emit(session, 9634, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Territory] handleGetReport error", e);
            sendError(session);
        }
    }

    // ─── op=9: 9635 PB_SCTerritoryRedInfo ────────────────────────────────
    private void handleGetRed(PlayerSession session, Long roleId) {
        try {
            TerritoryRedResponse resp =
                    territoryGrpcClient.getRedInfo(roleId);
            Msgterritory.PB_SCTerritoryRedInfo.Builder b =
                    Msgterritory.PB_SCTerritoryRedInfo.newBuilder()
                            .setRewardFlag(resp.getRewardFlag());
            Emitters.emit(session, 9635, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Territory] handleGetRed error", e);
            sendError(session);
        }
    }

    private void sendError(PlayerSession session) {
        // FIX: KHÔNG gửi 9631 PB_SCTerritoryInfo trong error case!
        // Trước đây sendError gửi 9631 với empty data → client nhận recvTerritoryInfo
        // → gọi SendTerritoryReq(BOT_STATUS) → server không handle → sendError → vòng lặp vô hạn
        log.warn("[Territory] sendError for roleId={} — no packet sent to avoid loop", session.getRoleId());
    }

    private void publishTaskProgress(Long roleId, int actionType) {
        String taskKey;
        String source;
        if (actionType == 1) {
            taskKey = taskActionConditionMapping.territoryFetchRewardTaskKey();
            source = "websocket-territory-fetch-reward";
        } else if (actionType == 2) {
            taskKey = taskActionConditionMapping.territoryLevelUpTaskKey();
            source = "websocket-territory-level-up";
        } else {
            return;
        }
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}


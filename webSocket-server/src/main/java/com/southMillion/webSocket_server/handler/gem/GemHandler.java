package com.SouthMillion.webSocket_server.handler.gem;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.GemFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Gem Handler — handles gem drawing system (宝石图纸).
 *
 * Proto: msgother.proto
 *   1660 PB_CSGemReq               — main gem operation (op_type + params)
 *   1661 PB_SCGemInfo              — gem drawing info response
 *   1666 PB_CSGemOneKeyUpLevelReq  — one-key upgrade request
 *   1667 PB_CSGemBuyReq            — gem buy request
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GemHandler implements MessageHandler {

    private final GemFeign gemFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_GET_INFO = 1;
    private static final int OP_INLAY    = 2;
    private static final int OP_REMOVE   = 3;
    private static final int OP_COMPOSE  = 4;

    @Override
    public int[] interests() {
        return new int[]{1660, 1666, 1667};
    }

    /** Gọi sau login: đẩy thông tin gem (1661) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleIdStr = session.getRoleId();
        if (roleIdStr == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                sendGemInfo(session, roleIdStr);
            } catch (NumberFormatException e) {
                log.warn("[Gem] pushAll: roleId không hợp lệ={}", roleIdStr);
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                switch (msgId) {
                    case 1660 -> handleGemReq(session, roleId, payload);
                    case 1666 -> handleOneKeyUpLevel(session, roleId, payload);
                    case 1667 -> handleGemBuy(session, roleId, payload);
                    default   -> log.warn("[Gem] Unexpected msgId={}", msgId);
                }
            } catch (Exception e) {
                log.error("[Gem] Error for roleId={}", session.getRoleId(), e);
                sendEmptyGemInfo(session);
            }
        });
    }

    // msgId=1660: Gem operation (get info, inlay, remove, compose)
    private void handleGemReq(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSGemReq req = Msgother.PB_CSGemReq.parseFrom(payload);
            int opType = req.hasOpType() ? req.getOpType() : OP_GET_INFO;
            int param1 = req.hasParam1() ? req.getParam1() : 0;
            int param2 = req.hasParam2() ? req.getParam2() : 0;

            log.debug("[Gem] op={}, param1={}, roleId={}", opType, param1, roleId);

            Map<String, Object> opResult = null;
            switch (opType) {
                case OP_INLAY   -> {
                    opResult = gemFeign.inlay(String.valueOf(roleId), param1, param2);
                    publishTaskProgress(roleId, opResult, taskActionConditionMapping.gemInlayTaskKey(), "websocket-gem-inlay");
                }
                case OP_REMOVE  -> gemFeign.remove(String.valueOf(roleId), param1);
                case OP_COMPOSE -> {
                    opResult = gemFeign.compose(String.valueOf(roleId), List.of(param1));
                    publishTaskProgress(roleId, opResult, taskActionConditionMapping.gemComposeTaskKey(), "websocket-gem-compose");
                }
                default         -> {} // OP_GET_INFO
            }

            sendGemInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Gem] handleGemReq error", e);
            sendEmptyGemInfo(session);
        }
    }

    // msgId=1666: One-key upgrade gems
    private void handleOneKeyUpLevel(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSGemOneKeyUpLevelReq req = Msgother.PB_CSGemOneKeyUpLevelReq.parseFrom(payload);
            List<Integer> itemIds = req.getItemIdsList();
            log.debug("[Gem] OneKeyUpLevel itemIds={}, roleId={}", itemIds, roleId);
            Map<String, Object> result = gemFeign.upgradeAll(String.valueOf(roleId), itemIds);
            publishTaskProgress(roleId, result, taskActionConditionMapping.gemUpgradeTaskKey(), "websocket-gem-upgrade");
            sendGemInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Gem] handleOneKeyUpLevel error", e);
            sendEmptyGemInfo(session);
        }
    }

    // msgId=1667: Buy gems
    private void handleGemBuy(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSGemBuyReq req = Msgother.PB_CSGemBuyReq.parseFrom(payload);
            List<Integer> itemIds = req.getItemIdsList();
            log.debug("[Gem] GemBuy itemIds={}, roleId={}", itemIds, roleId);
            Map<String, Object> result = gemFeign.buy(String.valueOf(roleId), itemIds);
            publishTaskProgress(roleId, result, taskActionConditionMapping.gemBuyTaskKey(), "websocket-gem-buy");
            sendGemInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Gem] handleGemBuy error", e);
            sendEmptyGemInfo(session);
        }
    }

    private void sendGemInfo(PlayerSession session, Long roleId) {
        try {
            Map<String, Object> data = gemFeign.getInfo(String.valueOf(roleId));
            Msgother.PB_SCGemInfo.Builder builder = Msgother.PB_SCGemInfo.newBuilder();
            if (data != null && data.get("drawingId") instanceof Number n) {
                builder.setDrawingId(n.intValue());
            } else {
                builder.setDrawingId(-1);
            }
            Emitters.emit(session, 1661, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Gem] sendGemInfo failed", e);
            sendEmptyGemInfo(session);
        }
    }

    private void sendEmptyGemInfo(PlayerSession session) {
        try {
            Emitters.emit(session, 1661, Msgother.PB_SCGemInfo.newBuilder().setDrawingId(-1).build().toByteArray());
        } catch (Exception ignored) {}
    }

    private void publishTaskProgress(Long roleId, Map<String, Object> result, String taskKey, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank()) {
            return;
        }
        if (isOperationSuccess(result)) {
            taskProgressPublisher.publish(roleId, taskKey, 1, source);
        }
    }

    private boolean isOperationSuccess(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        Object success = result.get("success");
        if (success == null) {
            return true;
        }
        return Boolean.TRUE.equals(success);
    }
}

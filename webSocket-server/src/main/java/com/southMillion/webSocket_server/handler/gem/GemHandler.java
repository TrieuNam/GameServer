package com.SouthMillion.webSocket_server.handler.gem;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.GemGrpcClient;
import org.SouthMillion.proto.gem.GemInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GemHandler.class);

    private final GemGrpcClient gemGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // Op types phải khớp với enum GEM_ATELIER_REQ_TYPE phía client
    private static final int OP_INLAY     = 1;
    private static final int OP_REMOVE    = 2;
    private static final int OP_COMPOSE   = 3;
    private static final int OP_MOVE      = 4;
    private static final int OP_TRANSFORM = 5;
    private static final int OP_LEVEL_UP  = 6;

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

    // msgId=1660: Gem operation
    private void handleGemReq(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgother.PB_CSGemReq req = Msgother.PB_CSGemReq.parseFrom(payload);
            int opType = req.hasOpType() ? req.getOpType() : 0;
            int param1 = req.hasParam1() ? req.getParam1() : 0;
            int param2 = req.hasParam2() ? req.getParam2() : 0;
            int param3 = req.hasParam3() ? req.getParam3() : 0;
            int param4 = req.hasParam4() ? req.getParam4() : 0;

            log.debug("[Gem] op={} p1={} p2={} p3={} p4={} roleId={}", opType, param1, param2, param3, param4, roleId);

            switch (opType) {
                case OP_INLAY -> {
                    // param1=drawing_id(unused), param2=item_id, param3=y, param4=x → slotType = x*100+y
                    boolean ok = gemGrpcClient.inlay(roleId, param2, param4 * 100 + param3).getSuccess();
                    publishTaskProgress(roleId, ok, taskActionConditionMapping.gemInlayTaskKey(), "websocket-gem-inlay");
                }
                case OP_REMOVE -> {
                    // param1=drawing_id(unused), param2=gem_list_index
                    gemGrpcClient.remove(roleId, param2);
                }
                case OP_COMPOSE -> {
                    // param1=main_gem_id, param2=material1_id, param3=material2_id
                    boolean ok = gemGrpcClient.compose(roleId, List.of(param1, param2, param3)).getSuccess();
                    publishTaskProgress(roleId, ok, taskActionConditionMapping.gemComposeTaskKey(), "websocket-gem-compose");
                }
                case OP_MOVE -> {
                    // param1=drawing_id, param2=gem_list_index, param3=y, param4=x
                    gemGrpcClient.move(roleId, param1, param2, param4, param3);
                }
                case OP_TRANSFORM -> {
                    // param1=source_gem_id, param2=target_gem_id
                    gemGrpcClient.transform(roleId, param1, param2);
                }
                case OP_LEVEL_UP -> {
                    // param1=gem_id
                    boolean ok = gemGrpcClient.levelUp(roleId, param1).getSuccess();
                    publishTaskProgress(roleId, ok, taskActionConditionMapping.gemUpgradeTaskKey(), "websocket-gem-levelup");
                }
                default -> {} // GET_INFO: chỉ trả về thông tin gem
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
            boolean ok = gemGrpcClient.upgradeAll(roleId, itemIds).getSuccess();
            publishTaskProgress(roleId, ok, taskActionConditionMapping.gemUpgradeTaskKey(), "websocket-gem-upgrade");
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
            boolean ok = gemGrpcClient.buy(roleId, itemIds).getSuccess();
            publishTaskProgress(roleId, ok, taskActionConditionMapping.gemBuyTaskKey(), "websocket-gem-buy");
            sendGemInfo(session, roleId);
        } catch (Exception e) {
            log.error("[Gem] handleGemBuy error", e);
            sendEmptyGemInfo(session);
        }
    }

    private void sendGemInfo(PlayerSession session, Long roleId) {
        try {
            GemInfoResponse info = gemGrpcClient.getGemInfo(roleId);
            Msgother.PB_SCGemInfo.Builder builder = Msgother.PB_SCGemInfo.newBuilder();
            builder.setDrawingId(info.getSuccess() ? info.getDrawingId() : -1);
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

    private void publishTaskProgress(Long roleId, boolean success, String taskKey, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank() || !success) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

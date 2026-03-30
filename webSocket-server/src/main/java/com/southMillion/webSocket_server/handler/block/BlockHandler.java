package com.SouthMillion.webSocket_server.handler.block;

import com.google.protobuf.InvalidProtocolBufferException;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BlockFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgblock.Msgblock;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Block Handler - Building Block (積木) system
 *
 * Messages:
 *   C→S  2180  PB_CSBlockReq     – client operation request
 *   S→C  2181  PB_SCBuildBlockInfo – server response with updated block info
 *
 * op_type values (mirror C++ buildblock):
 *   0 = Inlay   (嵌入)  params: map_id, block_index, pos_x, pos_y
 *   1 = Remove  (拆除)  params: map_id, block_index
 *   2 = Compose (合成)  params: map_id, block_index, block_index, block_index
 *   3 = Activate(激活)  params: seq
 *   4 = MapWear (装备模型) params: map_id
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockHandler implements MessageHandler {

    private final BlockFeign blockFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_INLAY    = 0;
    private static final int OP_REMOVE   = 1;
    private static final int OP_COMPOSE  = 2;
    private static final int OP_ACTIVATE = 3;
    private static final int OP_MAP_WEAR = 4;

    // Legacy BUILD_BLOCK_RET enum from C++ msgbuildblock.hpp
    private static final int RET_INIT = 0;
    private static final int RET_ADD = 1;
    private static final int RET_DELETE = 2;
    private static final int RET_INLAY = 3;
    private static final int RET_REMOVE = 4;
    private static final int RET_ACTIVATE = 5;
    private static final int RET_MAP_WEAR = 6;
    private static final int RET_COMPOSE = 7;
    private static final int RET_COMPOSE_FAIL = 8;

    private static final int MSG_CS_BLOCK_REQ = 2180;
    private static final int MSG_SC_BLOCK_INFO = 2181;

    @Override
    public int[] interests() {
        return new int[]{MSG_CS_BLOCK_REQ}; // CS_BLOCK_REQ
    }

    /** Gọi sau login: đẩy toàn bộ thông tin block (2181) về client. */
    public Mono<Void> pushAll(PlayerSession ps) {
        return Mono.fromRunnable(() -> {
            if (ps.getRoleId() == null) return;
            sendBlockList(ps);
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        if (ps.getRoleId() == null) {
            log.warn("[block] User not logged in");
            return;
        }
        log.info("[block] msgId={}, roleId={}", msgId, ps.getRoleId());
        try {
            if (msgId == MSG_CS_BLOCK_REQ) {
                handleBlockRequest(ps, payload);
            } else {
                log.warn("[block] Received server-to-client msgId: {}", msgId);
            }
        } catch (Exception e) {
            log.error("[block] Error handling message: msgId={}, roleId={}", msgId, ps.getRoleId(), e);
        }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // Request dispatch
    // ──────────────────────────────────────────────────────────────

    private void handleBlockRequest(PlayerSession ps, byte[] payload) {
        try {
            Msgblock.PB_CSBlockReq req = Msgblock.PB_CSBlockReq.parseFrom(payload);
            int opType = req.getOpType();
            List<Integer> params = req.getParamList();
            Long roleId = ps.getRoleId();

            log.debug("[block] op={}, params={}, roleId={}", opType, params, roleId);

            switch (opType) {
                case OP_INLAY:
                    handleInlay(ps, roleId, params);
                    break;
                case OP_REMOVE:
                    handleRemove(ps, roleId, params);
                    break;
                case OP_COMPOSE:
                    handleCompose(ps, roleId, params);
                    break;
                case OP_ACTIVATE:
                    handleActivate(ps, roleId, params);
                    break;
                case OP_MAP_WEAR:
                    handleMapWear(ps, roleId, params);
                    break;
                default:
                    log.warn("[block] Unknown op_type={}, roleId={}", opType, roleId);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("[block] Failed to parse PB_CSBlockReq: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("[block] Failed to handle block request: {}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Operation handlers
    // ──────────────────────────────────────────────────────────────

    /** 嵌入: params[0]=map_id, params[1]=block_index, params[2]=pos_x, params[3]=pos_y */
    private void handleInlay(PlayerSession ps, Long roleId, List<Integer> params) {
        if (params.size() != 4) {
            log.warn("[block] Inlay: insufficient params size={}, roleId={}", params.size(), roleId);
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("roleId", roleId);
        req.put("mapId", params.get(0));
        req.put("blockIndex", params.get(1));
        req.put("posX", params.get(2));
        req.put("posY", params.get(3));

        Map<String, Object> result = blockFeign.inlay(req);
        sendBlockInfo(ps, result, RET_INLAY);
        if (isOperationSuccess(result, RET_INLAY)) {
            publishTaskProgress(roleId, taskActionConditionMapping.blockInlayTaskKey(), "websocket-block-inlay");
            publishTaskProgress(roleId, "condition_70", "websocket-block-receive");
        }
        log.info("[block] Inlay done, roleId={}, mapId={}, blockIndex={}", roleId, params.get(0), params.get(1));
    }

    /** 拆除: params[0]=map_id, params[1]=block_index */
    private void handleRemove(PlayerSession ps, Long roleId, List<Integer> params) {
        if (params.size() != 2) {
            log.warn("[block] Remove: insufficient params size={}, roleId={}", params.size(), roleId);
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("roleId", roleId);
        req.put("mapId", params.get(0));
        req.put("blockIndex", params.get(1));

        Map<String, Object> result = blockFeign.remove(req);
        sendBlockInfo(ps, result, RET_REMOVE);
        if (isOperationSuccess(result, RET_REMOVE)) {
            publishTaskProgress(roleId, taskActionConditionMapping.blockRemoveTaskKey(), "websocket-block-remove");
        }
        log.info("[block] Remove done, roleId={}, mapId={}, blockIndex={}", roleId, params.get(0), params.get(1));
    }

    /** 合成: params[0]=map_id, params[1..N]=block_index list */
    private void handleCompose(PlayerSession ps, Long roleId, List<Integer> params) {
        if (params.size() != 4) {
            log.warn("[block] Compose: insufficient params size={}, roleId={}", params.size(), roleId);
            return;
        }

        Set<Integer> uniqueBlockIndex = new HashSet<>(params.subList(1, params.size()));
        if (uniqueBlockIndex.size() != 3) {
            log.warn("[block] Compose: block_index must be unique, params={}, roleId={}", params, roleId);
            return;
        }

        Map<String, Object> req = new HashMap<>();
        req.put("roleId", roleId);
        req.put("mapId", params.get(0));
        req.put("blockIndexList", params.subList(1, params.size()));

        Map<String, Object> result = blockFeign.compose(req);
        sendBlockInfo(ps, result, RET_COMPOSE);
        if (isOperationSuccess(result, RET_COMPOSE)) {
            publishTaskProgress(roleId, taskActionConditionMapping.blockComposeTaskKey(), "websocket-block-compose");
        }
        log.info("[block] Compose done, roleId={}, mapId={}", roleId, params.get(0));
    }

    /** 激活: params[0]=seq */
    private void handleActivate(PlayerSession ps, Long roleId, List<Integer> params) {
        if (params.size() != 1) {
            log.warn("[block] Activate: insufficient params, roleId={}", roleId);
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("roleId", roleId);
        req.put("seq", params.get(0));

        Map<String, Object> result = blockFeign.activate(req);
        sendBlockInfo(ps, result, RET_ACTIVATE);
        log.info("[block] Activate done, roleId={}, seq={}", roleId, params.get(0));
    }

    /** 装备模型: params[0]=map_id */
    private void handleMapWear(PlayerSession ps, Long roleId, List<Integer> params) {
        if (params.size() != 1) {
            log.warn("[block] MapWear: insufficient params, roleId={}", roleId);
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("roleId", roleId);
        req.put("mapId", params.get(0));

        Map<String, Object> result = blockFeign.wear(req);
        sendBlockInfo(ps, result, RET_MAP_WEAR);
        if (isOperationSuccess(result, RET_MAP_WEAR)) {
            publishTaskProgress(roleId, "condition_72", "websocket-block-design-change");
        }
        log.info("[block] MapWear done, roleId={}, mapId={}", roleId, params.get(0));
    }

    // ──────────────────────────────────────────────────────────────
    // Response builder
    // ──────────────────────────────────────────────────────────────

    private void sendBlockList(PlayerSession ps) {
        try {
            Map<String, Object> result = blockFeign.getInfo(String.valueOf(ps.getRoleId()));
            sendBlockInfo(ps, result, RET_INIT);
        } catch (Exception e) {
            log.error("[block] Failed to send block list: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendBlockInfo(PlayerSession ps, Map<String, Object> result, int defaultSendType) {
        try {
            Msgblock.PB_SCBuildBlockInfo.Builder builder = Msgblock.PB_SCBuildBlockInfo.newBuilder()
                    .setSendType(defaultSendType);

            if (result != null) {
                Integer sendType = getInt(result, "sendType", "send_type");
                if (sendType != null) builder.setSendType(sendType);

                Integer activateSeq = getInt(result, "activateSeq", "activate_seq");
                if (activateSeq != null) builder.setActivateSeq(activateSeq);

                Integer mapId = getInt(result, "mapId", "map_id");
                if (mapId != null) builder.setMapId(mapId);

                Object rawBlockList = firstNonNull(result.get("blockList"), result.get("block_list"));
                if (rawBlockList instanceof List<?> list) {
                    List<Map<String, Object>> blockList = (List<Map<String, Object>>) list;
                    for (Map<String, Object> block : blockList) {
                        Msgblock.PB_SCBlockNode.Builder node = Msgblock.PB_SCBlockNode.newBuilder();

                        Integer blockId = getInt(block, "blockId", "block_id");
                        if (blockId != null) node.setBlockId(blockId);

                        Integer blockMapId = getInt(block, "mapId", "map_id");
                        if (blockMapId != null) node.setMapId(blockMapId);

                        Integer posX = getInt(block, "posX", "pos_x");
                        if (posX != null) node.setPosX(posX);

                        Integer posY = getInt(block, "posY", "pos_y");
                        if (posY != null) node.setPosY(posY);

                        Integer color = getInt(block, "color");
                        if (color != null) node.setColor(color);

                        Integer blockIndex = getInt(block, "blockIndex", "block_index", "index");
                        if (blockIndex != null) node.setBlockIndex(blockIndex);

                        builder.addBlockList(node.build());
                    }
                }
            }

            Emitters.emit(ps, MSG_SC_BLOCK_INFO, builder.build().toByteArray());
            log.debug("[block] Sent PB_SCBuildBlockInfo to roleId={}", ps.getRoleId());
        } catch (Exception e) {
            log.error("[block] Failed to send block info: {}", e.getMessage(), e);
        }
    }

    private Integer getInt(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return null;
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number number) return number.intValue();
            if (value != null) {
                try {
                    return Integer.parseInt(String.valueOf(value));
                } catch (Exception ignore) {
                    // continue
                }
            }
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private boolean isOperationSuccess(Map<String, Object> result, int expectedSendType) {
        Integer sendType = getInt(result, "sendType", "send_type");
        return sendType != null && sendType == expectedSendType;
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

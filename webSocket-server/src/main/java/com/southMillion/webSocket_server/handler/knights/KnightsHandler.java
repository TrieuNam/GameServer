package com.SouthMillion.webSocket_server.handler.knights;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.grpc.KnightsGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.proto.knights.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Knights Handbook Handler (骑士手册).
 *
 * Proto: msgother.proto
 *   1625 PB_CSKnightsReq           — client request (op_type + param1)
 *   1626 PB_SCKnightsInfo          — handbook info (level, flag, level_flag)
 *   1627 PB_SCKnightsConditionInfo — condition list
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnightsHandler implements MessageHandler {

    private final KnightsGrpcClient knightsGrpcClient;
    private final ObjectMapper objectMapper;

    private static final int OP_GET_INFO       = 1;
    private static final int OP_GET_CONDITIONS = 2;
    private static final int OP_CLAIM_REWARD   = 3;
    private static final int OP_CLAIM_LEVEL    = 4;

    @Override
    public int[] interests() {
        return new int[]{1625}; // PB_CSKnightsReq
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgother.PB_CSKnightsReq req = Msgother.PB_CSKnightsReq.parseFrom(payload);
                int opType = req.hasOpType() ? req.getOpType() : OP_GET_INFO;
                int param1 = req.hasParam1() ? req.getParam1() : 0;
                Long roleId = session.getRoleId();

                log.debug("[Knights] op={}, param1={}, roleId={}", opType, param1, roleId);

                switch (opType) {
                    case OP_GET_CONDITIONS -> {
                        GenericResponse cond = knightsGrpcClient.getConditions(roleId);
                        sendConditionInfo(session, cond);
                    }
                    case OP_CLAIM_REWARD -> {
                        KnightsHandbookResponse result = knightsGrpcClient.claimSeqReward(roleId, param1);
                        sendKnightsInfo(session, result);
                    }
                    case OP_CLAIM_LEVEL -> {
                        KnightsHandbookResponse result = knightsGrpcClient.claimLevelReward(roleId, param1);
                        sendKnightsInfo(session, result);
                    }
                    default -> {
                        // OP_GET_INFO or unknown
                        KnightsHandbookResponse info = knightsGrpcClient.getOrCreate(roleId);
                        sendKnightsInfo(session, info);
                    }
                }
            } catch (Exception e) {
                log.error("[Knights] Error for roleId={}", session.getRoleId(), e);
                sendKnightsInfo(session, null);
            }
        });
    }

    private void sendKnightsInfo(PlayerSession session, KnightsHandbookResponse resp) {
        try {
            Msgother.PB_SCKnightsInfo.Builder builder = Msgother.PB_SCKnightsInfo.newBuilder();
            if (resp != null && resp.hasHandbook()) {
                KnightsHandbookData hb = resp.getHandbook();
                builder.setLevel(hb.getLevel());
                builder.setFlag((int) hb.getFlag());
                builder.setLevelFlag((int) hb.getLevelFlag());
            }
            Emitters.emit(session, 1626, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Knights] sendKnightsInfo failed", e);
        }
    }

    private void sendConditionInfo(PlayerSession session, GenericResponse resp) {
        try {
            Msgother.PB_SCKnightsConditionInfo.Builder builder =
                    Msgother.PB_SCKnightsConditionInfo.newBuilder();
            if (resp != null && resp.getSuccess() && !resp.getDataJson().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> parsed =
                            objectMapper.readValue(resp.getDataJson(), java.util.Map.class);
                    Object condList = parsed.get("conditions");
                    if (condList instanceof java.util.List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Number n) {
                                builder.addContitionList(n.intValue());
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    log.warn("[Knights] Failed to parse conditions JSON: {}", parseEx.getMessage());
                }
            }
            Emitters.emit(session, 1627, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Knights] sendConditionInfo failed", e);
        }
    }
}

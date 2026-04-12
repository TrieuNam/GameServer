package com.SouthMillion.webSocket_server.handler.pagoda;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.PagodaGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.proto.pagoda.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgpagoda.Msgpagoda;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Handles tower/pagoda dungeon operations.
 *
 * Two separate tower systems:
 *   2120 PB_CSShiLianPagodaReq  → 试炼之塔 (Trial Tower)      → 2121 PB_SCShiLianPagodaInfo
 *   2122 PB_CSGuMoPagodaReq     → 锢魔之塔 (Demon-Lock Tower) → 2123 PB_SCGuMoPagodaListInfo
 *
 * type field in request: 1=GET_INFO, 2=CHALLENGE, 3=CLAIM_REWARD
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagodaHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PagodaHandler.class);

    private final PagodaGrpcClient pagodaGrpcClient;
    private final ObjectMapper objectMapper;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_GET_INFO    = 1;
    private static final int OP_CHALLENGE   = 2;
    private static final int OP_CLAIM       = 3;

    @Override
    public int[] interests() {
        return new int[]{2120, 2122};
    }

    /** Goi sau login: day thong tin toa thap (2121, 2123) ve client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleIdStr = session.getRoleId();
        if (roleIdStr == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = roleIdStr;
                handleShiLian(session, roleId, new byte[0]);
                handleGuMo(session, roleId, new byte[0]);
            } catch (NumberFormatException e) {
                log.warn("[Pagoda] pushAll: roleId khong hop le={}", roleIdStr);
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                if (msgId == 2120) {
                    handleShiLian(session, roleId, payload);
                } else {
                    handleGuMo(session, roleId, payload);
                }
            } catch (Exception e) {
                log.error("[Pagoda] Error for msgId={}, roleId={}", msgId, session.getRoleId(), e);
            }
        });
    }

    // 2120: Trial Tower (试炼之塔) → 2121 PB_SCShiLianPagodaInfo
    private void handleShiLian(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgpagoda.PB_CSShiLianPagodaReq req = Msgpagoda.PB_CSShiLianPagodaReq.parseFrom(payload);
            int type = req.hasType() ? req.getType() : OP_GET_INFO;
            int p1   = req.hasP1()   ? req.getP1()   : 0;

            log.debug("[Pagoda/ShiLian] type={}, p1={}, roleId={}", type, p1, roleId);

            switch (type) {
                case OP_CHALLENGE -> {
                    ShiLianResponse challenge = pagodaGrpcClient.challengeShiLian(roleId, p1);
                    if (challenge.getSuccess() && challenge.hasShilian() && challenge.getShilian().getPassLevel() >= p1) {
                        publishTaskProgress(roleId, taskActionConditionMapping.pagodaShilianChallengeTaskKey(), "websocket-pagoda-shilian-challenge");
                    }
                }
                case OP_CLAIM -> {
                    BoolResponse claimed = pagodaGrpcClient.claimShiLian(roleId, p1);
                    if (claimed.getResult()) {
                        publishTaskProgress(roleId, taskActionConditionMapping.pagodaShilianClaimTaskKey(), "websocket-pagoda-shilian-claim");
                    }
                }
                default           -> {} // GET_INFO — just fetch below
            }

            ShiLianResponse data = pagodaGrpcClient.getShiLian(roleId);
            Msgpagoda.PB_SCShiLianPagodaInfo.Builder builder = Msgpagoda.PB_SCShiLianPagodaInfo.newBuilder();
            if (data.getSuccess() && data.hasShilian()) {
                ShiLianData sl = data.getShilian();
                builder.setPassLevel(sl.getPassLevel());
                builder.setBestLeve(sl.getBestLevel());
                builder.addUseItem(sl.getUseItem());
                builder.addRandomId(sl.getRandomId());
                builder.setSeasonEndTime((int) sl.getSeasonEndTime());
            }
            Emitters.emit(session, 2121, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Pagoda/ShiLian] Error for roleId={}", roleId, e);
            try {
                Emitters.emit(session, 2121, Msgpagoda.PB_SCShiLianPagodaInfo.newBuilder().build().toByteArray());
            } catch (Exception ignored) {}
        }
    }

    // 2122: Demon-Lock Tower (锢魔之塔) → 2123 PB_SCGuMoPagodaListInfo
    private void handleGuMo(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgpagoda.PB_CSGuMoPagodaReq req = Msgpagoda.PB_CSGuMoPagodaReq.parseFrom(payload);
            int type = req.hasType() ? req.getType() : OP_GET_INFO;
            int p1   = req.hasP1()   ? req.getP1()   : 0;

            log.debug("[Pagoda/GuMo] type={}, p1={}, roleId={}", type, p1, roleId);

            switch (type) {
                case OP_CHALLENGE -> {
                    GenericResponse challenge = pagodaGrpcClient.challengeGuMo(roleId, p1);
                    if (challenge.getSuccess()) {
                        publishTaskProgress(roleId, taskActionConditionMapping.pagodaGumoChallengeTaskKey(), "websocket-pagoda-gumo-challenge");
                    }
                }
                case OP_CLAIM -> {
                    BoolResponse claimed = pagodaGrpcClient.claimGuMo(roleId, p1);
                    if (claimed.getResult()) {
                        publishTaskProgress(roleId, taskActionConditionMapping.pagodaGumoClaimTaskKey(), "websocket-pagoda-gumo-claim");
                    }
                }
                default           -> {}
            }

            GenericResponse data = pagodaGrpcClient.getGuMo(roleId);
            Msgpagoda.PB_SCGuMoPagodaListInfo.Builder builder = Msgpagoda.PB_SCGuMoPagodaListInfo.newBuilder();
            if (data.getSuccess() && !data.getDataJson().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> parsed =
                            objectMapper.readValue(data.getDataJson(), java.util.Map.class);
                    if (parsed.get("dayReward") instanceof Number n)    builder.setDayReward(n.intValue());
                    if (parsed.get("lastdayLevel") instanceof Number n) builder.setLastdayLevel(n.intValue());
                } catch (Exception parseEx) {
                    log.warn("[Pagoda/GuMo] Failed to parse GuMo data JSON: {}", parseEx.getMessage());
                }
            }
            Emitters.emit(session, 2123, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Pagoda/GuMo] Error for roleId={}", roleId, e);
            try {
                Emitters.emit(session, 2123, Msgpagoda.PB_SCGuMoPagodaListInfo.newBuilder().build().toByteArray());
            } catch (Exception ignored) {}
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

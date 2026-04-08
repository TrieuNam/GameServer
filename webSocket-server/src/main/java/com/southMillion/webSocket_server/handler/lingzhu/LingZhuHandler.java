package com.SouthMillion.webSocket_server.handler.lingzhu;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.LingZhuGrpcClient;
import org.SouthMillion.proto.lingzhu.GetAllResponse;
import org.SouthMillion.proto.lingzhu.LingZhuProgressData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msglingzhu.Msglingzhu;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Handles lord dungeon (领主副本) operations.
 *
 * Proto: PB_CSLingZhuReq (2008) — type + p1 + p2
 * Response: PB_SCLingZhuInfo (2009) — lingzhu_list (stage, pass_level, sweep_count)
 *
 * type=1 GET_INFO, type=2 CHALLENGE, type=3 SWEEP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LingZhuHandler implements MessageHandler {

    private final LingZhuGrpcClient lingZhuGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_GET_INFO  = 1;
    private static final int OP_CHALLENGE = 2;
    private static final int OP_SWEEP     = 3;

    @Override
    public int[] interests() {
        return new int[]{2008}; // PB_CSLingZhuReq
    }

    /** Goi sau login: day danh sach linh tru (2009) ve client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleIdStr = session.getRoleId();
        if (roleIdStr == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                sendLingZhuInfo(session, roleIdStr);
            } catch (NumberFormatException e) {
                log.warn("[LingZhu] pushAll: roleId khong hop le={}", roleIdStr);
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msglingzhu.PB_CSLingZhuReq req = Msglingzhu.PB_CSLingZhuReq.parseFrom(payload);
                int type = req.hasType() ? req.getType() : OP_GET_INFO;
                int p1   = req.hasP1()   ? req.getP1()   : 0;
                int p2   = req.hasP2()   ? req.getP2()   : 0;
                Long roleId = session.getRoleId();

                log.debug("[LingZhu] type={}, p1={}, p2={}, roleId={}", type, p1, p2, roleId);

                switch (type) {
                    case OP_CHALLENGE -> {
                        boolean challengeOk = lingZhuGrpcClient.challenge(roleId, p1, p2).getSuccess();
                        if (challengeOk) {
                            publishTaskProgress(roleId, taskActionConditionMapping.lingzhuChallengeTaskKey(), "websocket-lingzhu-challenge");
                        }
                    }
                    case OP_SWEEP -> {
                        boolean sweepOk = lingZhuGrpcClient.sweep(roleId, p1, p2 > 0 ? p2 : 1).getSuccess();
                        if (sweepOk) {
                            publishTaskProgress(roleId, taskActionConditionMapping.lingzhuSweepTaskKey(), "websocket-lingzhu-sweep");
                        }
                    }
                    default           -> {} // GET_INFO — just send current state below
                }

                sendLingZhuInfo(session, roleId);
            } catch (Exception e) {
                log.error("[LingZhu] Error for roleId={}", session.getRoleId(), e);
                sendEmpty(session);
            }
        });
    }

    private void sendLingZhuInfo(PlayerSession session, Long roleId) {
        try {
            GetAllResponse resp = lingZhuGrpcClient.getAll(roleId);
            Msglingzhu.PB_SCLingZhuInfo.Builder builder = Msglingzhu.PB_SCLingZhuInfo.newBuilder();
            for (LingZhuProgressData d : resp.getItemsList()) {
                Msglingzhu.PB_CSLingZhuData.Builder item = Msglingzhu.PB_CSLingZhuData.newBuilder();
                item.setStage(d.getStage());
                item.setPassLevel(d.getPassLevel());
                item.setSweepCount(d.getSweepCount());
                builder.addLingzhuList(item.build());
            }
            Emitters.emit(session, 2009, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[LingZhu] sendLingZhuInfo failed", e);
            sendEmpty(session);
        }
    }

    private void sendEmpty(PlayerSession session) {
        try {
            Emitters.emit(session, 2009, Msglingzhu.PB_SCLingZhuInfo.newBuilder().build().toByteArray());
        } catch (Exception ignored) {}
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

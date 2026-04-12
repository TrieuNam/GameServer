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
 * Client contract (DungeonCtrl):
 * type=0 FIGHT (p1:stage, p2:level – validate only), type=1 MOP, type=2 QUICK_MOP, type=3 INFO, type=4 FINISH (p1:stage, p2:level – updates passLevel)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LingZhuHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LingZhuHandler.class);

    private final LingZhuGrpcClient lingZhuGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_FIGHT     = 0;
    private static final int OP_MOP       = 1;
    private static final int OP_QUICK_MOP = 2;
    private static final int OP_INFO      = 3;
    private static final int OP_FINISH    = 4;

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
                int type = req.hasType() ? req.getType() : OP_INFO;
                int p1   = req.hasP1()   ? req.getP1()   : 0;
                int p2   = req.hasP2()   ? req.getP2()   : 0;
                Long roleId = session.getRoleId();

                log.debug("[LingZhu] type={}, p1={}, p2={}, roleId={}", type, p1, p2, roleId);

                switch (type) {
                    case OP_FIGHT -> {
                        // Validate that the level can be challenged; passLevel update is deferred to OP_FINISH.
                        lingZhuGrpcClient.challenge(roleId, p1, p2);
                    }
                    case OP_MOP, OP_QUICK_MOP -> {
                        boolean sweepOk = lingZhuGrpcClient.sweep(roleId, p1, p2 > 0 ? p2 : 1).getSuccess();
                        if (sweepOk) {
                            publishTaskProgress(roleId, taskActionConditionMapping.lingzhuSweepTaskKey(), "websocket-lingzhu-sweep");
                        }
                    }
                    case OP_INFO -> {
                        // explicit info request
                    }
                    case OP_FINISH -> {
                        // Client sends this after winning the battle to confirm level completion.
                        boolean finishOk = lingZhuGrpcClient.finishChallenge(roleId, p1, p2).getSuccess();
                        if (finishOk) {
                            publishTaskProgress(roleId, taskActionConditionMapping.lingzhuChallengeTaskKey(), "websocket-lingzhu-finish");
                        }
                    }
                    default -> {
                        log.warn("[LingZhu] unknown op type={} roleId={}", type, roleId);
                    }
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

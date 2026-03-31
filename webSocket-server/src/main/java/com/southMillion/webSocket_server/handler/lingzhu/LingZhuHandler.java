package com.SouthMillion.webSocket_server.handler.lingzhu;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.LingZhuFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msglingzhu.Msglingzhu;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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

    private final LingZhuFeign lingZhuFeign;
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
                        Map<String, Object> challenge = lingZhuFeign.challenge(String.valueOf(roleId), p1, p2);
                        if (isSuccess(challenge)) {
                            publishTaskProgress(roleId, taskActionConditionMapping.lingzhuChallengeTaskKey(), "websocket-lingzhu-challenge");
                        }
                    }
                    case OP_SWEEP -> {
                        Map<String, Object> sweep = lingZhuFeign.sweep(String.valueOf(roleId), p1, p2 > 0 ? p2 : 1);
                        if (isSuccess(sweep)) {
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
            List<Map<String, Object>> dataList = lingZhuFeign.getAll(String.valueOf(roleId));
            Msglingzhu.PB_SCLingZhuInfo.Builder builder = Msglingzhu.PB_SCLingZhuInfo.newBuilder();
            if (dataList != null) {
                for (Map<String, Object> d : dataList) {
                    Msglingzhu.PB_CSLingZhuData.Builder item = Msglingzhu.PB_CSLingZhuData.newBuilder();
                    if (d.get("stage") instanceof Number n)      item.setStage(n.intValue());
                    if (d.get("passLevel") instanceof Number n)  item.setPassLevel(n.intValue());
                    if (d.get("sweepCount") instanceof Number n) item.setSweepCount(n.intValue());
                    builder.addLingzhuList(item.build());
                }
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

    private boolean isSuccess(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        Object success = result.get("success");
        if (success instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(success));
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

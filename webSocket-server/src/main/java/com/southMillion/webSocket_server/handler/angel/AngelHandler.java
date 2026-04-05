package com.SouthMillion.webSocket_server.handler.angel;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.AngelGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgangel.Msgangel;
import org.SouthMillion.proto.angel.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Angel系统处理器 - P3优先级
 * 负责处理天使相关的所有操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AngelHandler implements MessageHandler {

    private final AngelGrpcClient angelGrpcClient;
    private final RoleServiceHandler roleServiceHandler;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // Legacy req_type from servercommon/angeldef.hpp
    private static final int REQ_LEVEL_UP = 0;
    private static final int REQ_GRADE_UP = 1;
    private static final int REQ_EQUIP_LEVEL_UP = 2;
    private static final int REQ_APPEARANCE_LEVEL_UP = 3;
    private static final int REQ_USE_APPEARANCE = 4;

    // Legacy ret_type from servercommon/angeldef.hpp
    private static final int RET_LEVEL = 0;
    private static final int RET_GRADE = 1;
    private static final int RET_EQUIP_LEVEL = 2;
    private static final int RET_APPEARANCE_LEVEL = 3;
    private static final int RET_USER_APPEARANCE = 4;

    private static final int SC_ANGEL_INFO = 2131;
    private static final int SC_ANGEL_RET  = 2132;

    @Override
    public int[] interests() {
        return new int[]{2130}; // PB_CSAngelReq
    }

    /** Gọi sau login: chỉ đẩy angel info khi DB thực sự có angel rows. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            GetUserAngelsResponse resp = angelGrpcClient.getUserAngels(String.valueOf(roleId));
            if (resp == null || (resp.hasStatus() && !resp.getStatus().getSuccess()) || resp.getAngelsCount() == 0) {
                log.debug("[Angel] skip pushAll because no angel rows for roleId={}", roleId);
                return;
            }
            sendAngelInfo(session, resp);
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgangel.PB_CSAngelReq req = Msgangel.PB_CSAngelReq.parseFrom(payload);
                String userId = String.valueOf(session.getRoleId());
                int op = req.hasReqType() ? req.getReqType() : 0;
                int param = req.hasParam() ? req.getParam() : 0;
                int param2 = req.hasParam2() ? req.getParam2() : 0;
                log.debug("[Angel] op={} userId={}", op, userId);
                switch (op) {
                    case REQ_LEVEL_UP -> {
                        LevelUpAngelResponse r = angelGrpcClient.levelUpAngel(String.valueOf(userId), param);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.angelLevelUpTaskKey(), "websocket-angel-level-up");
                        }
                        int level = r.hasAngel() ? r.getAngel().getLevel() : 0;
                        sendRet(session, RET_LEVEL, level, 0);
                    }
                    case REQ_GRADE_UP -> {
                        GradeUpAngelResponse r = angelGrpcClient.gradeUpAngel(String.valueOf(userId), param);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.angelGradeUpTaskKey(), "websocket-angel-grade-up");
                        }
                        int grade = r.hasAngel() ? r.getAngel().getGrade() : 0;
                        sendRet(session, RET_GRADE, grade, 0);
                    }
                    case REQ_EQUIP_LEVEL_UP -> {
                        EquipAngelResponse r = angelGrpcClient.equipAngel(String.valueOf(userId), param);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.angelEquipTaskKey(), "websocket-angel-equip");
                            roleServiceHandler.pushRoleState(session).subscribe();
                        }
                        int equipValue = r.hasStatus() && r.getStatus().getSuccess() ? param : 0;
                        sendRet(session, RET_EQUIP_LEVEL, equipValue, param);
                    }
                    case REQ_APPEARANCE_LEVEL_UP -> {
                        AppearanceLevelUpResponse r = angelGrpcClient.appearanceLevelUp(userId, param, param2);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.angelAppearanceLevelUpTaskKey(), "websocket-angel-appearance-level-up");
                        }
                        int newLevel = r.getNewAppearanceLevel() > 0 ? r.getNewAppearanceLevel() : param;
                        sendRet(session, RET_APPEARANCE_LEVEL, newLevel, 0);
                    }
                    case REQ_USE_APPEARANCE -> {
                        UseAppearanceResponse r = angelGrpcClient.useAppearance(userId, 0, param);
                        if (r.getStatus().getSuccess()) {
                            roleServiceHandler.pushRoleState(session).subscribe();
                        }
                        sendRet(session, RET_USER_APPEARANCE, param, 0);
                    }
                    default -> {
                        log.warn("[Angel] unknown req_type={} roleId={}", op, session.getRoleId());
                    }
                }
                sendAngelInfo(session, userId);
            } catch (Exception e) {
                log.error("[Angel] Error roleId={}", session.getRoleId(), e);
            }
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void sendAngelInfo(PlayerSession session, String userId) {
        sendAngelInfo(session, angelGrpcClient.getUserAngels(String.valueOf(userId)));
    }

    private void sendAngelInfo(PlayerSession session, GetUserAngelsResponse resp) {
        if (resp == null || (resp.hasStatus() && !resp.getStatus().getSuccess()) || resp.getAngelsCount() == 0) {
            return;
        }

        Msgangel.PB_SCAngelInfo.Builder sc = Msgangel.PB_SCAngelInfo.newBuilder()
                .setAngelLevel(0).setAngelGrade(0).setUseAppearance(0);

        for (AngelData a : resp.getAngelsList()) {
            sc.setAngelLevel(Math.max(sc.getAngelLevel(), a.getLevel()));
            sc.setAngelGrade(Math.max(sc.getAngelGrade(), a.getGrade()));
            sc.addAngelEquipId(a.getAngelId());
            if (a.getIsEquipped()) {
                sc.setUseAppearance(a.getAppearanceId());
            }
            sc.addAppearanceData(Msgangel.PB_AngelAppearanceData.newBuilder()
                    .setId(a.getAngelId()).setLevel(a.getLevel()).build());
        }
        Emitters.emit(session, SC_ANGEL_INFO, sc.build().toByteArray());
    }

    private void sendRet(PlayerSession session, int retType, int code, int param1) {
        byte[] bytes = Msgangel.PB_SCAngelOpRet.newBuilder()
                .setRetType(retType).setParam1(code).setParam2(param1).build().toByteArray();
        Emitters.emit(session, SC_ANGEL_RET, bytes);
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

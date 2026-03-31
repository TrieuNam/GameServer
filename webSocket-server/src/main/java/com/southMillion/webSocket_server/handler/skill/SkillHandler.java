package com.SouthMillion.webSocket_server.handler.skill;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.SkillFeign;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.SouthMillion.proto.Msgskill.Msgskill;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Xử lý kỹ năng (skill) và thiên phú (talent) của nhân vật.
 *
 * <p>Mapping proto:
 * <ul>
 *   <li>CS 1470 PB_CSRoleSkillOperaReq  → onSkillOpera</li>
 *   <li>SC 1471 PB_SCRoleSkillAllInfo   ← emitSkillAllInfo</li>
 *   <li>CS 1480 PB_CSRoleTalentOperaReq → onTalentOpera</li>
 *   <li>SC 1481 PB_SCRoleTalentAllInfo  ← emitTalentAllInfo</li>
 * </ul>
 *
 * <p>ReqType cho skill (1470):
 * <ul>
 *   <li>0 = ROLE_SKILL_OPERA_REQ_TYPE_INFO           — lấy thông tin</li>
 *   <li>1 = ROLE_SKILL_OPERA_REQ_TYPE_LEARN_SKILL    — học kỹ năng (param1=skillId)</li>
 *   <li>2 = ROLE_SKILL_OPERA_REQ_TYPE_ONE_KEY_LEVEL_UP — nâng cấp tất cả</li>
 * </ul>
 *
 * <p>ReqType cho talent (1480):
 * <ul>
 *   <li>0 = ROLE_TALENT_OPERA_REQ_TYPE_INFO              — lấy thông tin</li>
 *   <li>1 = ROLE_TALENT_OPERA_REQ_TYPE_LEARN_TALENT_SKILL — học thiên phú (param1=skillId)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillHandler implements MessageHandler {

    private final SkillFeign skillFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // ─── ReqType constants ───────────────────────────────────────────────
    private static final int SKILL_REQ_INFO          = 0;
    private static final int SKILL_REQ_LEARN         = 1;
    private static final int SKILL_REQ_ONE_KEY_UP    = 2;

    private static final int TALENT_REQ_INFO         = 0;
    private static final int TALENT_REQ_LEARN        = 1;

    @Override
    public int[] interests() {
        return new int[]{
                MsgIds.CS_ROLE_SKILL_OPERA_REQ,   // 1470
                MsgIds.CS_ROLE_TALENT_OPERA_REQ   // 1480
        };
    }

    /**
     * Đẩy skill info + talent info về client ngay sau khi login thành công.
     */
    public Mono<Void> pushAll(PlayerSession ps) {
        Long roleId = ps.getRoleId();
        if (roleId == null) return Mono.empty();
        return FeignCall.withToken(ps.getSessionId(), "skill.pushAll",
                        () -> skillFeign.getSkillAll(roleId))
                .doOnNext(resp -> emitSkillAllInfo(ps, resp))
                .then(FeignCall.withToken(ps.getSessionId(), "talent.pushAll",
                        () -> skillFeign.getTalentAll(roleId)))
                .doOnNext(resp -> emitTalentAllInfo(ps, resp))
                .onErrorResume(e -> {
                    log.warn("[skill] pushAll error roleId={}: {}", roleId, e.toString());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return switch (msgId) {
            case MsgIds.CS_ROLE_SKILL_OPERA_REQ  -> onSkillOpera(ps, payload);
            case MsgIds.CS_ROLE_TALENT_OPERA_REQ -> onTalentOpera(ps, payload);
            default -> Mono.empty();
        };
    }

    // ═══════════════════════════════ SKILL ═══════════════════════════════

    private Mono<Void> onSkillOpera(PlayerSession ps, byte[] payload) {
        Long roleId = ps.getRoleId();
        if (roleId == null) return Mono.empty();

        return Mono.fromCallable(() -> Msgskill.PB_CSRoleSkillOperaReq.parseFrom(payload))
                .flatMap(req -> {
                    int reqType = req.hasReqType() ? req.getReqType() : SKILL_REQ_INFO;
                    int param1  = req.hasParam1()  ? req.getParam1()  : 0;
                    log.debug("[skill] reqType={} param1={} roleId={}", reqType, param1, roleId);

                    return switch (reqType) {
                        case SKILL_REQ_LEARN -> FeignCall.withToken(ps.getSessionId(), "skill.learn",
                                () -> skillFeign.learnSkill(roleId,
                                        SkillDTOs.LearnSkillReq.builder()
                                                .roleId(roleId)
                                                .skillId(param1)
                                                .reqType(reqType)
                                                .build()))
                            .doOnNext(resp -> {
                                emitSkillAllInfo(ps, resp);
                                publishTaskProgress(roleId,
                                    taskActionConditionMapping.skillLearnTaskKey(),
                                    "websocket-skill-learn");
                            })
                                .then();

                        case SKILL_REQ_ONE_KEY_UP -> FeignCall.withToken(ps.getSessionId(), "skill.oneKey",
                                () -> skillFeign.oneKeyLevelUp(roleId))
                            .doOnNext(resp -> {
                                emitSkillAllInfo(ps, resp);
                                publishTaskProgress(roleId,
                                    taskActionConditionMapping.skillOneKeyLevelUpTaskKey(),
                                    "websocket-skill-one-key-level-up");
                            })
                                .then();

                        default -> FeignCall.withToken(ps.getSessionId(), "skill.info",
                                () -> skillFeign.getSkillAll(roleId))
                                .doOnNext(resp -> emitSkillAllInfo(ps, resp))
                                .then();
                    };
                })
                .onErrorResume(e -> {
                    log.warn("[skill] onSkillOpera error roleId={}: {}", roleId, e.toString());
                    return Mono.empty();
                });
    }

    // ═══════════════════════════════ TALENT ══════════════════════════════

    private Mono<Void> onTalentOpera(PlayerSession ps, byte[] payload) {
        Long roleId = ps.getRoleId();
        if (roleId == null) return Mono.empty();

        return Mono.fromCallable(() -> Msgskill.PB_CSRoleTalentOperaReq.parseFrom(payload))
                .flatMap(req -> {
                    int reqType = req.hasReqType() ? req.getReqType() : TALENT_REQ_INFO;
                    int param1  = req.hasParam1()  ? req.getParam1()  : 0;
                    log.debug("[talent] reqType={} param1={} roleId={}", reqType, param1, roleId);

                    if (reqType == TALENT_REQ_LEARN) {
                        return FeignCall.withToken(ps.getSessionId(), "talent.learn",
                                () -> skillFeign.learnTalent(roleId,
                                        SkillDTOs.LearnTalentReq.builder()
                                                .roleId(roleId)
                                                .skillId(param1)
                                                .reqType(reqType)
                                                .build()))
                                .doOnNext(resp -> emitTalentAllInfo(ps, resp))
                                .then();
                    } else {
                        // TALENT_REQ_INFO (default)
                        return FeignCall.withToken(ps.getSessionId(), "talent.info",
                                () -> skillFeign.getTalentAll(roleId))
                                .doOnNext(resp -> emitTalentAllInfo(ps, resp))
                                .then();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("[talent] onTalentOpera error roleId={}: {}", roleId, e.toString());
                    return Mono.empty();
                });
    }

    // ═══════════════════════════════ EMITTERS ════════════════════════════

    /**
     * Gửi PB_SCRoleSkillAllInfo (MsgId 1471) về client.
     */
    private void emitSkillAllInfo(PlayerSession ps, SkillDTOs.SkillAllInfoResp resp) {
        if (resp == null) return;
        try {
            Msgskill.PB_SCRoleSkillAllInfo.Builder sc = Msgskill.PB_SCRoleSkillAllInfo.newBuilder();

            List<SkillDTOs.RoleSkillInfo> list = resp.getSkillList();
            if (list != null) {
                for (SkillDTOs.RoleSkillInfo s : list) {
                    if (s.getSkillId() == null) continue;
                    Msgskill.PB_RoleSkillInfoPro proto = Msgskill.PB_RoleSkillInfoPro.newBuilder()
                            .setSkillId(s.getSkillId())
                            .setSkillLevel(s.getSkillLevel() != null ? s.getSkillLevel() : 1)
                            .setSkillIndex(s.getSkillIndex() != null ? s.getSkillIndex() : 0)
                            .build();
                    sc.addSkillList(proto);
                }
            }
            sc.setSkillCount(sc.getSkillListCount());
            Emitters.emit(ps, MsgIds.SC_ROLE_SKILL_ALL_INFO, sc.build().toByteArray());
        } catch (Exception e) {
            log.warn("[skill] emitSkillAllInfo error: {}", e.toString());
        }
    }

    /**
     * Gửi PB_SCRoleTalentAllInfo (MsgId 1481) về client.
     */
    private void emitTalentAllInfo(PlayerSession ps, SkillDTOs.TalentAllInfoResp resp) {
        if (resp == null) return;
        try {
            Msgskill.PB_SCRoleTalentAllInfo.Builder sc = Msgskill.PB_SCRoleTalentAllInfo.newBuilder();

            List<SkillDTOs.RoleTalentInfo> list = resp.getTalentSkillList();
            if (list != null) {
                for (SkillDTOs.RoleTalentInfo t : list) {
                    if (t.getSkillId() == null) continue;
                    Msgskill.PB_RoleTalentInfoPro proto = Msgskill.PB_RoleTalentInfoPro.newBuilder()
                            .setSkillId(t.getSkillId())
                            .setSkillLevel(t.getSkillLevel() != null ? t.getSkillLevel() : 1)
                            .build();
                    sc.addTalentSkillList(proto);
                }
            }
            sc.setTalentSkillCount(sc.getTalentSkillListCount());
            Emitters.emit(ps, MsgIds.SC_ROLE_TALENT_ALL_INFO, sc.build().toByteArray());
        } catch (Exception e) {
            log.warn("[talent] emitTalentAllInfo error: {}", e.toString());
        }
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}


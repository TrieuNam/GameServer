package com.SouthMillion.webSocket_server.handler.rune;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.RuneGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgrune.Msgrune;
import org.SouthMillion.proto.rune.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuneHandler implements MessageHandler {

    private final RuneGrpcClient runeGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_GET_ALL        = 1;
    private static final int OP_LEVEL_UP       = 2;
    private static final int OP_UPGRADE_QUALITY = 3;
    private static final int OP_EQUIP          = 5;
    private static final int OP_UNEQUIP        = 6;

    private static final int SC_RUNE_INFO = 1670;
    private static final int SC_RUNE_RET  = 1672;

    @Override
    public int[] interests() { return new int[]{1671}; }

    /** Push rune info after login. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> sendRuneInfo(session, String.valueOf(roleId)));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.parseFrom(payload);
                String userId = String.valueOf(session.getRoleId());
                int op = req.hasOperType() ? req.getOperType() : 0;
                log.debug("[Rune] op={} roleId={}", op, userId);
                switch (op) {
                    case OP_GET_ALL        -> sendRuneInfo(session, userId);
                    case OP_LEVEL_UP       -> {
                        int idx = req.hasP1() ? req.getP1() : 0;
                        LevelUpRuneResponse r = runeGrpcClient.levelUpRune(String.valueOf(userId), idx);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.runeLevelUpTaskKey(), "websocket-rune-level-up");
                        }
                        sendRet(session, OP_LEVEL_UP, r.getStatus().getCode(), idx);
                    }
                    case OP_UPGRADE_QUALITY -> {
                        int idx = req.hasP1() ? req.getP1() : 0;
                        UpgradeRuneQualityResponse r = runeGrpcClient.upgradeQuality(String.valueOf(userId), idx);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.runeUpgradeQualityTaskKey(), "websocket-rune-upgrade-quality");
                        }
                        sendRet(session, OP_UPGRADE_QUALITY, r.getStatus().getCode(), idx);
                    }
                    case OP_EQUIP -> {
                        int runeIdx = req.hasP1() ? req.getP1() : 0;
                        int slot    = req.hasP2() ? req.getP2() : 0;
                        EquipRuneResponse r = runeGrpcClient.equipRune(String.valueOf(userId), runeIdx, slot);
                        if (r.getStatus().getSuccess()) {
                            publishTaskProgress(session.getRoleId(), taskActionConditionMapping.runeEquipTaskKey(), "websocket-rune-equip");
                        }
                        sendRet(session, OP_EQUIP, r.getStatus().getCode(), runeIdx);
                    }
                    case OP_UNEQUIP -> {
                        int slot = req.hasP1() ? req.getP1() : 0;
                        UnequipRuneResponse r = runeGrpcClient.unequipRune(String.valueOf(userId), slot);
                        sendRet(session, OP_UNEQUIP, r.getStatus().getCode(), slot);
                    }
                    default -> sendRet(session, op, -1, 0);
                }
            } catch (Exception e) {
                log.error("[Rune] Error roleId={}", session.getRoleId(), e);
            }
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void sendRuneInfo(PlayerSession session, String userId) {
        GetAllRunesResponse resp = runeGrpcClient.getAllRunes(String.valueOf(userId));
        GetEquippedRunesResponse equipped = runeGrpcClient.getEquippedRunes(String.valueOf(userId));

        Msgrune.PB_SCRuneInfo.Builder sc = Msgrune.PB_SCRuneInfo.newBuilder()
                .setTowerLevel(0).setTurntableNum(0)
                .setTurntableFlag(0).setTurntableRound(0).setDailyReward(0)
                .setPassRewardIndex(0);

        // equip slots
        for (RuneData r : equipped.getRunesList()) {
            sc.addRuneWearList(r.getEquipSlot());
        }
        // knapsack
        for (RuneData r : resp.getRunesList()) {
            sc.addRuneKnapsackList(Msgrune.PB_RuneData.newBuilder()
                    .setIndex(r.getRuneIndex())
                    .setId(r.getRuneId())
                    .setLevel(r.getLevel())
                    .build());
        }
        Emitters.emit(session, SC_RUNE_INFO, sc.build().toByteArray());
    }

    private void sendRet(PlayerSession session, int retType, int code, int p1) {
        byte[] bytes = Msgrune.PB_SCRuneRet.newBuilder()
                .setRetType(retType).addParam(code).addParam(p1).build().toByteArray();
        Emitters.emit(session, SC_RUNE_RET, bytes);
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

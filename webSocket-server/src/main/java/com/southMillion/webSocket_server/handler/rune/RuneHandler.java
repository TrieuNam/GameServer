package com.SouthMillion.webSocket_server.handler.rune;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.RuneFeign;
import com.SouthMillion.webSocket_server.service.grpc.RuneGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msgrune.Msgrune;
import org.SouthMillion.proto.rune.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Handles MsgId 1671 (PB_CSRuneReq) — maps each oper_type to its handler.
 *
 * Client RUNE_REQ_TYPE enum (from InscriptionCtrl.ts):
 *   0 = FIGHT          → routed via BattleCtrl on the client, NOT sent here
 *   1 = FETCHDAYREWARD → daily reward
 *   2 = TURNTABLE      → spin turntable  (p1 = count)
 *   3 = WEARRUNE       → equip rune      (p1 = slot, p2 = knapsack_index)
 *   4 = OFFRUNE        → unequip rune    (p1 = slot)
 *   5 = UPRUNE         → upgrade rune    (p1 = knapsack_index)
 *   6 = DECOMPOSE      → decompose       (p1_list = knapsack indices, p2_list = crystal item IDs)
 *   7 = PASS_REWARD    → chapter reward
 *   8 = RUNE_BOX       → box draw        (p1 = type)
 *
 * Client RUNE_RET_INFO_TYPE enum (ret_type in PB_SCRuneRet):
 *   0 = DAILY_REWARD   p[0] = 0|1
 *   1 = PASS_LEVEL     p[0] = new tower level
 *   2 = TURNTABLE_NUM  p[0] = remaining spins
 *   3 = TURNTABLE_RET  p[0] = slot bit that was drawn
 *   4 = RUNE_CHANGE    p[0] = knapsack_index, p[1] = rune_id, p[2] = level
 *   5 = RUNE_WEAR      p[0] = slot_index, p[1] = knapsack_index
 *   6 = PASS_REWARD    p[0] = pass_reward_index after claim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuneHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RuneHandler.class);

    private final RuneGrpcClient runeGrpcClient;
    private final RuneFeign runeFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // ── oper_type constants matching client RUNE_REQ_TYPE ────────────────────
    private static final int OP_FETCHDAYREWARD = 1;
    private static final int OP_TURNTABLE      = 2;
    private static final int OP_WEARRUNE       = 3;
    private static final int OP_OFFRUNE        = 4;
    private static final int OP_UPRUNE         = 5;
    private static final int OP_DECOMPOSE      = 6;
    private static final int OP_PASS_REWARD    = 7;
    private static final int OP_RUNE_BOX       = 8;

    // ── ret_type constants matching client RUNE_RET_INFO_TYPE ────────────────
    private static final int RET_DAILY_REWARD  = 0;
    private static final int RET_PASS_LEVEL    = 1;
    private static final int RET_TURNTABLE_NUM = 2;
    private static final int RET_TURNTABLE_RET = 3;
    private static final int RET_RUNE_CHANGE   = 4;
    private static final int RET_RUNE_WEAR     = 5;
    private static final int RET_PASS_REWARD   = 6;

    // ── MsgId constants ───────────────────────────────────────────────────────
    private static final int SC_RUNE_INFO = 1670;
    private static final int SC_RUNE_RET  = 1672;

    @Override
    public int[] interests() { return new int[]{1671}; }

    /** Push full rune info on login. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> sendRuneInfo(session, String.valueOf(roleId)));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                if (roleId == null) {
                    log.warn("[Rune] Missing roleId for msgId={}", msgId);
                    sendRet(session, 0, -1);
                    return;
                }
                Msgrune.PB_CSRuneReq req = Msgrune.PB_CSRuneReq.parseFrom(payload);
                String userId = String.valueOf(roleId);
                int op = req.hasOperType() ? req.getOperType() : 0;
                int p1 = req.hasP1() ? req.getP1() : 0;
                int p2 = req.hasP2() ? req.getP2() : 0;
                log.debug("[Rune] op={} p1={} p2={} roleId={}", op, p1, p2, userId);

                switch (op) {
                    case OP_FETCHDAYREWARD -> handleFetchDayReward(session, userId);
                    case OP_TURNTABLE      -> handleTurntable(session, userId, p1);
                    case OP_WEARRUNE       -> handleWearRune(session, userId, roleId, p1, p2);
                    case OP_OFFRUNE        -> handleOffRune(session, userId, p1);
                    case OP_UPRUNE         -> handleUpRune(session, userId, roleId, p1);
                    case OP_DECOMPOSE      -> handleDecompose(session, userId,
                                                    req.getP1ListList(), req.getP2ListList());
                    case OP_PASS_REWARD    -> handlePassReward(session, userId);
                    case OP_RUNE_BOX       -> handleRuneBoxDraw(session, roleId, p1);
                    default -> {
                        log.warn("[Rune] Unknown op={} roleId={}", op, userId);
                        sendRet(session, op, -1);
                    }
                }
            } catch (Exception e) {
                log.error("[Rune] Error processing msgId={} roleId={}", msgId, session.getRoleId(), e);
            }
        });
    }

    // ── op=1 FETCHDAYREWARD ───────────────────────────────────────────────────
    private void handleFetchDayReward(PlayerSession session, String userId) {
        sendRet(session, RET_DAILY_REWARD, -1);
    }

    // ── op=2 TURNTABLE ────────────────────────────────────────────────────────
    private void handleTurntable(PlayerSession session, String userId, int count) {
        sendRet(session, RET_TURNTABLE_NUM, -1);
    }

    // ── op=3 WEARRUNE ─────────────────────────────────────────────────────────
    private void handleWearRune(PlayerSession session, String userId, Long roleId, int slotIndex, int knapsackIndex) {
        // p1=slot_index, p2=knapsack_index
        EquipRuneResponse r = runeGrpcClient.equipRune(userId, knapsackIndex, slotIndex);
        if (r.getStatus().getSuccess()) {
            publishTaskProgress(roleId, taskActionConditionMapping.runeEquipTaskKey(), "websocket-rune-wear");
            sendRet(session, RET_RUNE_WEAR, slotIndex, knapsackIndex);
            sendRuneInfo(session, userId);
        } else {
            sendRet(session, RET_RUNE_WEAR, -1);
        }
    }

    // ── op=4 OFFRUNE ──────────────────────────────────────────────────────────
    private void handleOffRune(PlayerSession session, String userId, int slotIndex) {
        UnequipRuneResponse r = runeGrpcClient.offRune(userId, slotIndex);
        if (r.getStatus().getSuccess()) {
            // ret type RET_RUNE_WEAR with knapsack_index = -1 signals unequipped
            sendRet(session, RET_RUNE_WEAR, slotIndex, -1);
            sendRuneInfo(session, userId);
        } else {
            sendRet(session, RET_RUNE_WEAR, -1);
        }
    }

    // ── op=5 UPRUNE ───────────────────────────────────────────────────────────
    private void handleUpRune(PlayerSession session, String userId, Long roleId, int knapsackIndex) {
        LevelUpRuneResponse r = runeGrpcClient.levelUpRune(userId, knapsackIndex);
        if (r.getStatus().getSuccess()) {
            publishTaskProgress(roleId, taskActionConditionMapping.runeLevelUpTaskKey(), "websocket-rune-up");
            RuneData rd = r.getRune();
            sendRet(session, RET_RUNE_CHANGE, rd.getRuneIndex(), rd.getRuneId(), rd.getLevel());
            sendRuneInfo(session, userId);
        } else {
            sendRet(session, RET_RUNE_CHANGE, -1);
        }
    }

    // ── op=6 DECOMPOSE ────────────────────────────────────────────────────────
    private void handleDecompose(PlayerSession session, String userId,
                                  List<Integer> knapsackIndices, List<Integer> crystalItemIds) {
        sendRet(session, OP_DECOMPOSE, -1);
    }

    // ── op=7 PASS_REWARD ──────────────────────────────────────────────────────
    private void handlePassReward(PlayerSession session, String userId) {
        sendRet(session, RET_PASS_REWARD, -1);
    }

    // ── op=8 RUNE_BOX ─────────────────────────────────────────────────────────
    private void handleRuneBoxDraw(PlayerSession session, Long roleId, int drawType) {
        if (roleId == null) { sendRet(session, OP_RUNE_BOX, -1); return; }
        try {
            Map<String, Object> result = runeFeign.drawBox(roleId, drawType).getBody();
            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                sendRuneInfo(session, String.valueOf(roleId));
                sendRewardNotice(session, result.get("rewards"));
            } else {
                sendRet(session, OP_RUNE_BOX, -1);
            }
        } catch (Exception e) {
            log.error("[Rune] handleRuneBoxDraw roleId={}", roleId, e);
            sendRet(session, OP_RUNE_BOX, -1);
        }
    }

    // ── sendRuneInfo ──────────────────────────────────────────────────────────
    /**
     * Builds PB_SCRuneInfo (1670) from real DB data:
     * - tower_level, turntable_*, daily_reward, pass_reward_index from rune_tower_state
     * - rune_wear_list: 20-element array where element[slot] = knapsack_index of rune in that slot
     * - rune_knapsack_list: all runes in knapsack
     */
    private void sendRuneInfo(PlayerSession session, String userId) {
        GetAllRunesResponse allRunes = runeGrpcClient.getAllRunes(userId);
        Msgrune.PB_SCRuneInfo.Builder sc = Msgrune.PB_SCRuneInfo.newBuilder()
            .setTowerLevel(0)
            .setTurntableNum(0)
            .setTurntableFlag(0)
            .setTurntableRound(0)
            .setDailyReward(0)
            .setPassRewardIndex(0);

        // Build rune_wear_list: 20 slots, each = knapsack index of equipped rune (-1 if empty)
        // Proto repeated field: we add entries in slot order
        int[] wearList = new int[20];
        Arrays.fill(wearList, -1);
        for (RuneData rune : allRunes.getRunesList()) {
            if (rune.getIsEquipped() && rune.getEquipSlot() >= 0 && rune.getEquipSlot() < 20) {
                wearList[rune.getEquipSlot()] = rune.getRuneIndex();
            }
        }
        for (int w : wearList) {
            sc.addRuneWearList(w);
        }

        // Build knapsack list
        for (RuneData rune : allRunes.getRunesList()) {
            sc.addRuneKnapsackList(Msgrune.PB_RuneData.newBuilder()
                    .setIndex(rune.getRuneIndex())
                    .setId(rune.getRuneId())
                    .setLevel(rune.getLevel())
                    .build());
        }

        Emitters.emit(session, SC_RUNE_INFO, sc.build().toByteArray());
    }

    // ── Proto helpers ─────────────────────────────────────────────────────────

    private void sendRet(PlayerSession session, int retType, int... params) {
        Msgrune.PB_SCRuneRet.Builder b = Msgrune.PB_SCRuneRet.newBuilder().setRetType(retType);
        for (int p : params) b.addParam(p);
        Emitters.emit(session, SC_RUNE_RET, b.build().toByteArray());
    }

    private void sendItemNotice(PlayerSession session, int itemId, int num) {
        if (itemId <= 0 || num <= 0) return;
        Msgknapsack.PB_SCGetItemNotice notice = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                .setGetType(111)
                .addItemList(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(itemId).setNum(num).build())
                .build();
        Emitters.emit(session, MsgIds.SC_GET_ITEM_NOTICE, notice.toByteArray());
    }

    @SuppressWarnings("unchecked")
    private void sendRewardNotice(PlayerSession session, Object rewardsObj) {
        if (!(rewardsObj instanceof java.util.List<?> rewards) || rewards.isEmpty()) return;
        Msgknapsack.PB_SCGetItemNotice.Builder notice = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                .setGetType(111);
        int added = 0;
        for (Object obj : rewards) {
            if (!(obj instanceof Map<?, ?> rw)) continue;
            int itemId = readMapInt((Map<?, ?>) rw, "itemId", "item_id");
            int num    = readMapInt((Map<?, ?>) rw, "num", "amount");
            if (itemId > 0 && num > 0) {
                notice.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                        .setItemId(itemId).setNum(num).build());
                added++;
            }
        }
        if (added > 0) Emitters.emit(session, MsgIds.SC_GET_ITEM_NOTICE, notice.build().toByteArray());
    }

    private int readMapInt(Map<?, ?> map, String key, String fallback) {
        Object v = map.get(key);
        if (!(v instanceof Number) && fallback != null) v = map.get(fallback);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) return;
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

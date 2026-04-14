package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.rune.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RuneGrpcClient {

    @GrpcClient("rune-service")
    private RuneGrpcServiceGrpc.RuneGrpcServiceBlockingStub stub;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("[grpc-rune] userId '{}' is not numeric", userId);
            return -1L;
        }
    }

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    // ── Knapsack queries ──────────────────────────────────────────────────────

    public GetAllRunesResponse getAllRunes(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return GetAllRunesResponse.getDefaultInstance();
        try {
            return stub.getAllRunes(GetAllRunesRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "getAllRunes", userId);
            return GetAllRunesResponse.getDefaultInstance();
        }
    }

    public GetEquippedRunesResponse getEquippedRunes(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return GetEquippedRunesResponse.getDefaultInstance();
        try {
            return stub.getEquippedRunes(GetEquippedRunesRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "getEquippedRunes", userId);
            return GetEquippedRunesResponse.getDefaultInstance();
        }
    }

    // ── Tower state ───────────────────────────────────────────────────────────

    /** Get full tower state (tower_level, turntable_num, daily_reward, etc.) */
    public TowerStateResponse getTowerState(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return TowerStateResponse.getDefaultInstance();
        try {
            return stub.getTowerState(TowerStateRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "getTowerState", userId);
            return TowerStateResponse.getDefaultInstance();
        }
    }

    /** op=1 FETCHDAYREWARD: claim daily tower reward */
    public TowerStateResponse fetchDayReward(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return TowerStateResponse.getDefaultInstance();
        try {
            return stub.fetchDayReward(TowerStateRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "fetchDayReward", userId);
            return TowerStateResponse.getDefaultInstance();
        }
    }

    /** op=7 PASS_REWARD: claim chapter/pass reward */
    public TowerStateResponse passReward(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return TowerStateResponse.getDefaultInstance();
        try {
            return stub.passReward(TowerStateRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "passReward", userId);
            return TowerStateResponse.getDefaultInstance();
        }
    }

    /** Called by battle-service after winning INSCRIPTION_TOWER battle */
    public TowerStateResponse winTowerLevel(String userId) {
        long id = parseUserId(userId);
        if (id < 0) {
            log.warn("[grpc-rune] winTowerLevel: invalid userId '{}'", userId);
            return TowerStateResponse.getDefaultInstance();
        }
        try {
            return stub.winTowerLevel(TowerStateRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            log(e, "winTowerLevel", userId);
            return TowerStateResponse.getDefaultInstance();
        }
    }

    // ── Equipment ops ─────────────────────────────────────────────────────────

    /** op=3 WEARRUNE: p1=slotIndex, p2=knapsackIndex */
    public EquipRuneResponse equipRune(String userId, int runeIndex, int slotIndex) {
        long id = parseUserId(userId);
        if (id < 0) return EquipRuneResponse.getDefaultInstance();
        try {
            return stub.equipRune(EquipRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).setEquipSlot(slotIndex).build());
        } catch (Exception e) {
            log(e, "equipRune", userId);
            return EquipRuneResponse.getDefaultInstance();
        }
    }

    /** op=4 OFFRUNE: unequip rune by slot index (uses UnequipRuneBySlot RPC) */
    public UnequipBySlotResponse offRune(String userId, int slotIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UnequipBySlotResponse.getDefaultInstance();
        try {
            return stub.unequipRuneBySlot(UnequipBySlotRequest.newBuilder()
                    .setUserId(id).setEquipSlot(slotIndex).build());
        } catch (Exception e) {
            log(e, "offRune", userId);
            return UnequipBySlotResponse.getDefaultInstance();
        }
    }

    // ── Upgrade ops ───────────────────────────────────────────────────────────

    /**
     * op=5 UPRUNE: upgrade rune by consuming item 70100 (inscription crystal),
     * cost determined by config (color × level).
     */
    public UpRuneResponse upRune(String userId, int runeIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UpRuneResponse.getDefaultInstance();
        try {
            return stub.upRune(UpRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            log(e, "upRune", userId);
            return UpRuneResponse.getDefaultInstance();
        }
    }

    /** Legacy quality upgrade (internal / REST) */
    public UpgradeRuneQualityResponse upgradeQuality(String userId, int runeIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UpgradeRuneQualityResponse.getDefaultInstance();
        try {
            return stub.upgradeRuneQuality(UpgradeRuneQualityRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            log(e, "upgradeQuality", userId);
            return UpgradeRuneQualityResponse.getDefaultInstance();
        }
    }

    // ── Decompose (op=6) ──────────────────────────────────────────────────────

    /**
     * op=6 DECOMPOSE: decompose runes and/or exp-crystals.
     * Returns refund amount of item 70100 added back to bag.
     */
    public DecomposeRunesResponse decomposeRunes(String userId,
                                                  List<Integer> knapsackIndices,
                                                  List<Integer> crystalItemIds) {
        long id = parseUserId(userId);
        if (id < 0) return DecomposeRunesResponse.getDefaultInstance();
        try {
            DecomposeRunesRequest.Builder req = DecomposeRunesRequest.newBuilder().setUserId(id);
            if (knapsackIndices != null) req.addAllKnapsackIndices(knapsackIndices);
            if (crystalItemIds != null)  req.addAllCrystalItemIds(crystalItemIds);
            return stub.decomposeRunes(req.build());
        } catch (Exception e) {
            log(e, "decomposeRunes", userId);
            return DecomposeRunesResponse.getDefaultInstance();
        }
    }

    // ── Turntable (op=2) ──────────────────────────────────────────────────────

    /**
     * op=2 TURNTABLE: spin turntable `count` times.
     * Consumes spin cost, gives prizes, updates turntable_flag / turntable_round.
     */
    public TurntableResponse turntable(String userId, int count) {
        long id = parseUserId(userId);
        if (id < 0) return TurntableResponse.getDefaultInstance();
        try {
            return stub.turntable(TurntableRequest.newBuilder()
                    .setUserId(id).setCount(count).build());
        } catch (Exception e) {
            log(e, "turntable", userId);
            return TurntableResponse.getDefaultInstance();
        }
    }

    // ── Logging helper ────────────────────────────────────────────────────────

    private void log(Exception e, String method, String userId) {
        if (isUnavailable(e)) log.warn("[grpc-rune] {}: rune-service unavailable (userId={})", method, userId);
        else log.error("[grpc-rune] {} userId={} error: {}", method, userId, e.getMessage());
    }
}

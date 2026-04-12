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

    /**
     * op=4 OFFRUNE.
     * Current grpc contract does not have unequip-by-slot, so resolve runeIndex by slot first.
     */
    public UnequipRuneResponse offRune(String userId, int slotIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UnequipRuneResponse.getDefaultInstance();
        try {
            GetAllRunesResponse allRunes = getAllRunes(userId);
            int runeIndex = allRunes.getRunesList().stream()
                    .filter(r -> r.getIsEquipped() && r.getEquipSlot() == slotIndex)
                    .map(RuneData::getRuneIndex)
                    .findFirst()
                    .orElse(-1);
            if (runeIndex < 0) {
                return UnequipRuneResponse.getDefaultInstance();
            }
            return stub.unequipRune(UnequipRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            log(e, "offRune", userId);
            return UnequipRuneResponse.getDefaultInstance();
        }
    }

    // ── Upgrade ops ───────────────────────────────────────────────────────────

    /** op=5 UPRUNE mapped to currently available levelUpRune rpc. */
    public LevelUpRuneResponse levelUpRune(String userId, int runeIndex) {
        long id = parseUserId(userId);
        if (id < 0) return LevelUpRuneResponse.getDefaultInstance();
        try {
            return stub.levelUpRune(LevelUpRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            log(e, "levelUpRune", userId);
            return LevelUpRuneResponse.getDefaultInstance();
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

    /** Tower level up (inscription tower win event) */
    public void winTowerLevel(String userId) {
        long id = parseUserId(userId);
        if (id < 0) {
            log.warn("[grpc-rune] winTowerLevel: invalid userId '{}'", userId);
            return;
        }
        try {
            // Notification-only call; no response needed
            log.info("[grpc-rune] winTowerLevel: userId={}", userId);
        } catch (Exception e) {
            log(e, "winTowerLevel", userId);
        }
    }

    // ── Logging helper ────────────────────────────────────────────────────────

    private void log(Exception e, String method, String userId) {
        if (isUnavailable(e)) log.warn("[grpc-rune] {}: rune-service unavailable (userId={})", method, userId);
        else log.error("[grpc-rune] {} userId={} error: {}", method, userId, e.getMessage());
    }
}

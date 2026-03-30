package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.proto.rune.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RuneGrpcClient {

    @GrpcClient("rune-service")
    private RuneGrpcServiceGrpc.RuneGrpcServiceBlockingStub stub;

    /** Parse String userId to long; return -1 if invalid (ULID etc.). */
    private long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            log.warn("[grpc-rune] userId '{}' is not a numeric Long — rune-service uses Long IDs, skipping", userId);
            return -1L;
        }
    }

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetAllRunesResponse getAllRunes(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return GetAllRunesResponse.getDefaultInstance();
        try {
            return stub.getAllRunes(GetAllRunesRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] getAllRunes userId={}: rune-service unavailable", userId);
            else log.error("[grpc-rune] getAllRunes userId={} error: {}", userId, e.getMessage());
            return GetAllRunesResponse.getDefaultInstance();
        }
    }

    public LevelUpRuneResponse levelUpRune(String userId, int runeIndex) {
        long id = parseUserId(userId);
        if (id < 0) return LevelUpRuneResponse.getDefaultInstance();
        try {
            return stub.levelUpRune(LevelUpRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] levelUpRune: rune-service unavailable");
            else log.error("[grpc-rune] levelUpRune userId={} runeIndex={} error: {}", userId, runeIndex, e.getMessage());
            return LevelUpRuneResponse.getDefaultInstance();
        }
    }

    public EquipRuneResponse equipRune(String userId, int runeIndex, int slotIndex) {
        long id = parseUserId(userId);
        if (id < 0) return EquipRuneResponse.getDefaultInstance();
        try {
            return stub.equipRune(EquipRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).setEquipSlot(slotIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] equipRune: rune-service unavailable");
            else log.error("[grpc-rune] equipRune error: {}", e.getMessage());
            return EquipRuneResponse.getDefaultInstance();
        }
    }

    public UnequipRuneResponse unequipRune(String userId, int slotIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UnequipRuneResponse.getDefaultInstance();
        try {
            return stub.unequipRune(UnequipRuneRequest.newBuilder()
                    .setUserId(id).setRuneIndex(slotIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] unequipRune: rune-service unavailable");
            else log.error("[grpc-rune] unequipRune error: {}", e.getMessage());
            return UnequipRuneResponse.getDefaultInstance();
        }
    }

    public UpgradeRuneQualityResponse upgradeQuality(String userId, int runeIndex) {
        long id = parseUserId(userId);
        if (id < 0) return UpgradeRuneQualityResponse.getDefaultInstance();
        try {
            return stub.upgradeRuneQuality(UpgradeRuneQualityRequest.newBuilder()
                    .setUserId(id).setRuneIndex(runeIndex).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] upgradeQuality: rune-service unavailable");
            else log.error("[grpc-rune] upgradeQuality error: {}", e.getMessage());
            return UpgradeRuneQualityResponse.getDefaultInstance();
        }
    }

    public GetEquippedRunesResponse getEquippedRunes(String userId) {
        long id = parseUserId(userId);
        if (id < 0) return GetEquippedRunesResponse.getDefaultInstance();
        try {
            return stub.getEquippedRunes(GetEquippedRunesRequest.newBuilder().setUserId(id).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-rune] getEquippedRunes userId={}: rune-service unavailable", userId);
            else log.error("[grpc-rune] getEquippedRunes error: {}", e.getMessage());
            return GetEquippedRunesResponse.getDefaultInstance();
        }
    }
}

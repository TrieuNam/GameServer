package com.SouthMillion.rune_service.grpc;

import com.SouthMillion.rune_service.exception.RuneServiceException;
import com.SouthMillion.rune_service.model.entity.Rune;
import com.SouthMillion.rune_service.model.entity.RuneTowerState;
import com.SouthMillion.rune_service.service.RuneService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.rune.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RuneServiceGrpcImpl extends RuneGrpcServiceGrpc.RuneGrpcServiceImplBase {

    private final RuneService runeService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ResponseStatus ok() {
        return ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build();
    }

    private static ResponseStatus err(int code, String msg) {
        return ResponseStatus.newBuilder().setCode(code).setMessage(msg).setSuccess(false).build();
    }

    private RuneData toRuneData(Rune rune) {
        return RuneData.newBuilder()
                .setId(rune.getId())
                .setUserId(rune.getUserId())
                .setRuneIndex(rune.getRuneIndex())
                .setRuneId(rune.getRuneId())
                .setLevel(rune.getLevel())
                .setQuality(rune.getQuality())
                .setStarLevel(rune.getStar())
                .setExp(rune.getExp())
                .setIsEquipped(rune.getIsEquipped())
                .setEquipSlot(rune.getEquipSlot() != null ? rune.getEquipSlot() : 0)
                .setRefinementLevel(rune.getRefinementLevel())
                .setCombatPower(0L)
                .setCreatedAt(rune.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
                .setUpdatedAt(rune.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli())
                .build();
    }

    private TowerStateData toTowerStateData(RuneTowerState s) {
        return TowerStateData.newBuilder()
                .setTowerLevel(s.getTowerLevel())
                .setTurntableNum(s.getTurntableNum())
                .setTurntableFlag(s.getTurntableFlag())
                .setTurntableRound(s.getTurntableRound())
                .setDailyReward(s.getDailyReward())
                .setPassRewardIndex(s.getPassRewardIndex())
                .build();
    }

    // ── GetAllRunes ───────────────────────────────────────────────────────────
    @Override
    public void getAllRunes(GetAllRunesRequest req, StreamObserver<GetAllRunesResponse> obs) {
        try {
            List<Rune> runes = runeService.getAllRunes(req.getUserId());
            obs.onNext(GetAllRunesResponse.newBuilder()
                    .setStatus(ok())
                    .addAllRunes(runes.stream().map(this::toRuneData).collect(Collectors.toList()))
                    .build());
        } catch (Exception e) {
            log.error("getAllRunes", e);
            obs.onNext(GetAllRunesResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── CreateRune ────────────────────────────────────────────────────────────
    @Override
    public void createRune(CreateRuneRequest req, StreamObserver<CreateRuneResponse> obs) {
        try {
            Rune rune = runeService.createRune(req.getUserId(), req.getRuneId(), req.getQuality());
            obs.onNext(CreateRuneResponse.newBuilder().setStatus(ok()).setRune(toRuneData(rune)).build());
        } catch (Exception e) {
            log.error("createRune", e);
            obs.onNext(CreateRuneResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── GetEquippedRunes ──────────────────────────────────────────────────────
    @Override
    public void getEquippedRunes(GetEquippedRunesRequest req, StreamObserver<GetEquippedRunesResponse> obs) {
        try {
            List<Rune> runes = runeService.getEquippedRunes(req.getUserId());
            obs.onNext(GetEquippedRunesResponse.newBuilder()
                    .setStatus(ok())
                    .addAllRunes(runes.stream().map(this::toRuneData).collect(Collectors.toList()))
                    .build());
        } catch (Exception e) {
            log.error("getEquippedRunes", e);
            obs.onNext(GetEquippedRunesResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── EquipRune (op=3 WEARRUNE) ─────────────────────────────────────────────
    @Override
    public void equipRune(EquipRuneRequest req, StreamObserver<EquipRuneResponse> obs) {
        try {
            Rune rune = runeService.equipRune(req.getUserId(), req.getRuneIndex(), req.getEquipSlot());
            obs.onNext(EquipRuneResponse.newBuilder().setStatus(ok()).setRune(toRuneData(rune)).build());
        } catch (Exception e) {
            log.error("equipRune", e);
            obs.onNext(EquipRuneResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── UnequipRune (by knapsack index — legacy) ──────────────────────────────
    @Override
    public void unequipRune(UnequipRuneRequest req, StreamObserver<UnequipRuneResponse> obs) {
        try {
            runeService.unequipRune(req.getUserId(), req.getRuneIndex());
            obs.onNext(UnequipRuneResponse.newBuilder().setStatus(ok()).setMessage("Unequipped").build());
        } catch (Exception e) {
            log.error("unequipRune", e);
            obs.onNext(UnequipRuneResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── UnequipRuneBySlot (op=4 OFFRUNE) ─────────────────────────────────────
    @Override
    public void unequipRuneBySlot(UnequipBySlotRequest req, StreamObserver<UnequipBySlotResponse> obs) {
        try {
            int freedIndex = runeService.offRune(req.getUserId(), req.getEquipSlot());
            obs.onNext(UnequipBySlotResponse.newBuilder()
                    .setStatus(ok())
                    .setFreedRuneIndex(freedIndex)
                    .build());
        } catch (RuneServiceException e) {
            obs.onNext(UnequipBySlotResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("unequipRuneBySlot", e);
            obs.onNext(UnequipBySlotResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── UpRune (op=5 UPRUNE) ──────────────────────────────────────────────────
    @Override
    public void upRune(UpRuneRequest req, StreamObserver<UpRuneResponse> obs) {
        try {
            Rune rune = runeService.upRune(req.getUserId(), req.getRuneIndex());
            obs.onNext(UpRuneResponse.newBuilder()
                    .setStatus(ok())
                    .setRune(toRuneData(rune))
                    .setNewLevel(rune.getLevel())
                    .build());
        } catch (RuneServiceException e) {
            obs.onNext(UpRuneResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("upRune", e);
            obs.onNext(UpRuneResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── LevelUpRune (legacy / internal) ──────────────────────────────────────
    @Override
    public void levelUpRune(LevelUpRuneRequest req, StreamObserver<LevelUpRuneResponse> obs) {
        try {
            Rune rune = runeService.levelUpRune(req.getUserId(), req.getRuneIndex());
            obs.onNext(LevelUpRuneResponse.newBuilder().setStatus(ok()).setRune(toRuneData(rune)).build());
        } catch (Exception e) {
            log.error("levelUpRune", e);
            obs.onNext(LevelUpRuneResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── UpgradeRuneQuality (legacy / internal) ────────────────────────────────
    @Override
    public void upgradeRuneQuality(UpgradeRuneQualityRequest req, StreamObserver<UpgradeRuneQualityResponse> obs) {
        try {
            Rune rune = runeService.upgradeRuneQuality(req.getUserId(), req.getRuneIndex());
            obs.onNext(UpgradeRuneQualityResponse.newBuilder().setStatus(ok()).setRune(toRuneData(rune)).build());
        } catch (Exception e) {
            log.error("upgradeRuneQuality", e);
            obs.onNext(UpgradeRuneQualityResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── DecomposeRunes (op=6 DECOMPOSE) ──────────────────────────────────────
    @Override
    public void decomposeRunes(DecomposeRunesRequest req, StreamObserver<DecomposeRunesResponse> obs) {
        try {
            Map<String, Object> result = runeService.decomposeRunes(
                    req.getUserId(),
                    req.getKnapsackIndicesList(),
                    req.getCrystalItemIdsList());
            int refundAmount = result.get("refundAmount") instanceof Number n ? n.intValue() : 0;
            int refundItemId = result.get("refundItemId") instanceof Number n ? n.intValue() : 0;
            obs.onNext(DecomposeRunesResponse.newBuilder()
                    .setStatus(ok())
                    .setRefundItemId(refundItemId)
                    .setRefundAmount(refundAmount)
                    .build());
        } catch (RuneServiceException e) {
            obs.onNext(DecomposeRunesResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("decomposeRunes", e);
            obs.onNext(DecomposeRunesResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── GetTowerState ─────────────────────────────────────────────────────────
    @Override
    public void getTowerState(TowerStateRequest req, StreamObserver<TowerStateResponse> obs) {
        try {
            RuneTowerState state = runeService.getTowerState(req.getUserId());
            obs.onNext(TowerStateResponse.newBuilder().setStatus(ok()).setState(toTowerStateData(state)).build());
        } catch (Exception e) {
            log.error("getTowerState", e);
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── FetchDayReward (op=1) ─────────────────────────────────────────────────
    @Override
    public void fetchDayReward(TowerStateRequest req, StreamObserver<TowerStateResponse> obs) {
        try {
            RuneTowerState state = runeService.fetchDayReward(req.getUserId());
            obs.onNext(TowerStateResponse.newBuilder().setStatus(ok()).setState(toTowerStateData(state)).build());
        } catch (RuneServiceException e) {
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("fetchDayReward", e);
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── PassReward (op=7) ─────────────────────────────────────────────────────
    @Override
    public void passReward(TowerStateRequest req, StreamObserver<TowerStateResponse> obs) {
        try {
            RuneTowerState state = runeService.passReward(req.getUserId());
            obs.onNext(TowerStateResponse.newBuilder().setStatus(ok()).setState(toTowerStateData(state)).build());
        } catch (RuneServiceException e) {
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("passReward", e);
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── WinTowerLevel ─────────────────────────────────────────────────────────
    @Override
    public void winTowerLevel(TowerStateRequest req, StreamObserver<TowerStateResponse> obs) {
        try {
            RuneTowerState state = runeService.winTowerLevel(req.getUserId());
            obs.onNext(TowerStateResponse.newBuilder().setStatus(ok()).setState(toTowerStateData(state)).build());
        } catch (Exception e) {
            log.error("winTowerLevel", e);
            obs.onNext(TowerStateResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    // ── Turntable (op=2) ──────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    @Override
    public void turntable(TurntableRequest req, StreamObserver<TurntableResponse> obs) {
        try {
            Map<String, Object> result = runeService.turntable(req.getUserId(), req.getCount());
            TurntableResponse.Builder rb = TurntableResponse.newBuilder().setStatus(ok());

            // Rebuild state
            Map<String, Object> stateMap = (Map<String, Object>) result.get("state");
            if (stateMap != null) {
                rb.setState(TowerStateData.newBuilder()
                        .setTowerLevel(intOf(stateMap, "towerLevel"))
                        .setTurntableNum(intOf(stateMap, "turntableNum"))
                        .setTurntableFlag(intOf(stateMap, "turntableFlag"))
                        .setTurntableRound(intOf(stateMap, "turntableRound"))
                        .setDailyReward(intOf(stateMap, "dailyReward"))
                        .setPassRewardIndex(intOf(stateMap, "passRewardIndex"))
                        .build());
            }

            // Rebuild spin results
            List<Map<String, Object>> spinResults = (List<Map<String, Object>>) result.get("spinResults");
            if (spinResults != null) {
                for (Map<String, Object> spin : spinResults) {
                    TurntableSpinResult.Builder sb = TurntableSpinResult.newBuilder()
                            .setSlotBit(intOf(spin, "slotBit"));
                    List<Map<String, Object>> rewards = (List<Map<String, Object>>) spin.get("rewards");
                    if (rewards != null) {
                        for (Map<String, Object> rw : rewards) {
                            sb.addRewards(TurntableReward.newBuilder()
                                    .setItemId(intOf(rw, "itemId"))
                                    .setNum(intOf(rw, "num"))
                                    .build());
                        }
                    }
                    rb.addSpinResults(sb.build());
                }
            }
            obs.onNext(rb.build());
        } catch (RuneServiceException e) {
            obs.onNext(TurntableResponse.newBuilder().setStatus(err(400, e.getMessage())).build());
        } catch (Exception e) {
            log.error("turntable", e);
            obs.onNext(TurntableResponse.newBuilder().setStatus(err(500, e.getMessage())).build());
        }
        obs.onCompleted();
    }

    private int intOf(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }
}

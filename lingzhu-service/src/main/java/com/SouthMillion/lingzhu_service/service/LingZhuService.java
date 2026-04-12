package com.SouthMillion.lingzhu_service.service;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import com.SouthMillion.lingzhu_service.repository.LingZhuProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LingZhuService {

    private static final int TOTAL_STAGES = 3;
    private static final int MAX_DAILY_SWEEP = 10;
    /** Per-stage maximum pass level: stage 1 has 24 levels, stages 2-3 have 26. */
    private static final Map<Integer, Integer> MAX_LEVEL_BY_STAGE = Map.of(1, 24, 2, 26, 3, 26);

    private final LingZhuProgressRepository repository;

    public List<LingZhuProgress> getAll(Long roleId) {
        List<LingZhuProgress> dbList = repository.findByRoleId(roleId);
        Map<Integer, LingZhuProgress> byStage = new HashMap<>();
        for (LingZhuProgress progress : dbList) {
            if (progress.getStage() != null) {
                resetSweepIfNewDay(progress);
                byStage.put(progress.getStage(), progress);
            }
        }

        // Always return all stages in a deterministic 1..N order for client mapping.
        List<LingZhuProgress> list = new ArrayList<>(TOTAL_STAGES);
        for (int stage = 1; stage <= TOTAL_STAGES; stage++) {
            LingZhuProgress progress = byStage.get(stage);
            if (progress == null) {
                progress = LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build();
            }
            list.add(progress);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> challenge(Long roleId, int stage, int level) {
        if (stage < 1 || stage > TOTAL_STAGES) {
            return Map.of("success", false, "message", "invalid_stage");
        }

        LingZhuProgress progress = repository.findByRoleIdAndStage(roleId, stage)
                .orElseGet(() -> LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());

        int passLevel = progress.getPassLevel() != null ? progress.getPassLevel() : 0;
        // When client doesn't send level (level=0), auto-derive the next level from current progress.
        int requestedLevel = level > 0 ? level : passLevel + 1;

        if (requestedLevel < 1 || requestedLevel > maxLevelForStage(stage)) {
            return Map.of("success", false, "message", "invalid_level");
        }

        // Validate linear progression without writing to DB; passLevel update happens in finishChallenge.
        boolean canChallenge = requestedLevel == passLevel + 1;
        return Map.of("success", canChallenge, "pass_level", passLevel);
    }

    @Transactional
    public Map<String, Object> finishChallenge(Long roleId, int stage, int level) {
        if (stage < 1 || stage > TOTAL_STAGES) {
            return Map.of("success", false, "message", "invalid_stage");
        }

        if (level < 1 || level > maxLevelForStage(stage)) {
            return Map.of("success", false, "message", "invalid_level");
        }

        LingZhuProgress progress = repository.findByRoleIdAndStage(roleId, stage)
                .orElseGet(() -> LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());

        resetSweepIfNewDay(progress);
        int passLevel = progress.getPassLevel() != null ? progress.getPassLevel() : 0;
        boolean success = level == passLevel + 1;
        if (success) {
            progress.setPassLevel(level);
            repository.save(progress);
        }
        return Map.of("success", success, "pass_level", progress.getPassLevel() != null ? progress.getPassLevel() : passLevel);
    }

    @Transactional
    public Map<String, Object> sweep(Long roleId, int stage, int count) {
        if (stage < 1 || stage > TOTAL_STAGES) {
            return Map.of("success", false, "message", "invalid_stage");
        }

        int safeCount = Math.max(count, 1);

        LingZhuProgress progress = repository.findByRoleIdAndStage(roleId, stage)
                .orElseGet(() -> LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());

        resetSweepIfNewDay(progress);

        int passLevel = progress.getPassLevel() != null ? progress.getPassLevel() : 0;
        int sweepCount = progress.getSweepCount() != null ? progress.getSweepCount() : 0;

        boolean canSweep = passLevel > 0 && sweepCount + safeCount <= MAX_DAILY_SWEEP;
        if (canSweep) {
            progress.setSweepCount(sweepCount + safeCount);
            repository.save(progress);
        }
        return Map.of("success", canSweep, "sweep_count", progress.getSweepCount());
    }

    private int maxLevelForStage(int stage) {
        return MAX_LEVEL_BY_STAGE.getOrDefault(stage, 26);
    }

    private void resetSweepIfNewDay(LingZhuProgress progress) {
        if (progress == null || progress.getUpdatedAt() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (!progress.getUpdatedAt().toLocalDate().isEqual(today)) {
            progress.setSweepCount(0);
        }
    }
}

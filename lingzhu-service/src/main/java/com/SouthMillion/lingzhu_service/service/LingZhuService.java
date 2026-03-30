package com.SouthMillion.lingzhu_service.service;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import com.SouthMillion.lingzhu_service.repository.LingZhuProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LingZhuService {

    private static final int TOTAL_STAGES = 3;

    private final LingZhuProgressRepository repository;

    public List<LingZhuProgress> getAll(Long roleId) {
        List<LingZhuProgress> list = repository.findByRoleId(roleId);
        // New player has no records yet – return default progress for all stages
        // so the client never receives an empty lingzhuList
        if (list.isEmpty()) {
            list = new ArrayList<>();
            for (int stage = 1; stage <= TOTAL_STAGES; stage++) {
                list.add(LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());
            }
        }
        return list;
    }

    @Transactional
    public Map<String, Object> challenge(Long roleId, int stage, int p1) {
        LingZhuProgress progress = repository.findByRoleIdAndStage(roleId, stage)
                .orElseGet(() -> LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());

        // p1 = level being challenged
        boolean success = p1 > 0 && p1 <= progress.getPassLevel() + 1;
        if (success && p1 > progress.getPassLevel()) {
            progress.setPassLevel(p1);
            repository.save(progress);
        }
        return Map.of("success", success);
    }

    @Transactional
    public Map<String, Object> sweep(Long roleId, int stage, int count) {
        LingZhuProgress progress = repository.findByRoleIdAndStage(roleId, stage)
                .orElseGet(() -> LingZhuProgress.builder()
                        .roleId(roleId).stage(stage).passLevel(0).sweepCount(0).build());

        boolean canSweep = progress.getPassLevel() > 0 && progress.getSweepCount() + count <= 10;
        if (canSweep) {
            progress.setSweepCount(progress.getSweepCount() + count);
            repository.save(progress);
        }
        return Map.of("success", canSweep);
    }
}

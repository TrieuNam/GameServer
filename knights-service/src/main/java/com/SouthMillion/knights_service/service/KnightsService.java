package com.SouthMillion.knights_service.service;
import com.SouthMillion.knights_service.entity.KnightsHandbook;
import com.SouthMillion.knights_service.repository.KnightsHandbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Slf4j @Service @RequiredArgsConstructor
public class KnightsService {
    private final KnightsHandbookRepository repository;

    public KnightsHandbook getOrCreate(Long roleId) {
        return repository.findByRoleId(roleId)
                .orElseGet(() -> repository.save(
                        KnightsHandbook.builder().roleId(roleId).level(0).flag(0L).levelFlag(0L).build()));
    }

    @Transactional
    public KnightsHandbook claimSeqReward(Long roleId, int seqIndex) {
        KnightsHandbook handbook = getOrCreate(roleId);
        long bit = 1L << seqIndex;
        if ((handbook.getFlag() & bit) == 0) {
            handbook.setFlag(handbook.getFlag() | bit);
            repository.save(handbook);
        }
        return handbook;
    }

    @Transactional
    public KnightsHandbook claimLevelReward(Long roleId, int levelIndex) {
        KnightsHandbook handbook = getOrCreate(roleId);
        long bit = 1L << levelIndex;
        if ((handbook.getLevelFlag() & bit) == 0) {
            handbook.setLevelFlag(handbook.getLevelFlag() | bit);
            repository.save(handbook);
        }
        return handbook;
    }

    /** Returns condition counts — stubbed as empty list for now */
    public Map<String, Object> getConditions(Long roleId) {
        return Map.of("conditions", java.util.List.of());
    }
}

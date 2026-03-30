package com.SouthMillion.report_service.service;

import com.SouthMillion.report_service.entity.BossKillEntity;
import com.SouthMillion.report_service.repository.BossKillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BossService {
    private final BossKillRepository repository;

    public int getUserBossKill(Long userId) {
        return repository.findByUserId(userId)
                .map(BossKillEntity::getBossKillCount)
                .orElse(0);
    }
}
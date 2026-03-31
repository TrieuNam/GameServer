package com.SouthMillion.report_service.repository;

import com.SouthMillion.report_service.entity.BossKillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BossKillRepository extends JpaRepository<BossKillEntity, Long> {
    Optional<BossKillEntity> findByUserId(Long userId);
}
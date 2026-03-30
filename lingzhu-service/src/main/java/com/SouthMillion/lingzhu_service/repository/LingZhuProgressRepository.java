package com.SouthMillion.lingzhu_service.repository;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LingZhuProgressRepository extends JpaRepository<LingZhuProgress, Long> {
    List<LingZhuProgress> findByRoleId(Long roleId);
    Optional<LingZhuProgress> findByRoleIdAndStage(Long roleId, Integer stage);
}

package com.SouthMillion.main_fb_service.repository;

import com.SouthMillion.main_fb_service.entity.MainFbStageProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MainFbStageProgressRepository extends JpaRepository<MainFbStageProgressEntity, Long> {
    Optional<MainFbStageProgressEntity> findByPlayerIdAndStageAndLevel(String playerId, Integer stage, Integer level);
    List<MainFbStageProgressEntity> findByPlayerId(String playerId);
}
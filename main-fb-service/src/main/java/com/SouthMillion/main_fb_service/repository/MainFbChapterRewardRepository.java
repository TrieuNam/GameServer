package com.SouthMillion.main_fb_service.repository;

import com.SouthMillion.main_fb_service.entity.MainFbChapterRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MainFbChapterRewardRepository extends JpaRepository<MainFbChapterRewardEntity, Long> {
    Optional<MainFbChapterRewardEntity> findByPlayerIdAndStageAndChapterLabel(String playerId, Integer stage, String chapterLabel);
}
package com.SouthMillion.main_fb_service.repository;

import com.SouthMillion.main_fb_service.entity.MainFbTaskProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MainFbTaskProgressRepository extends JpaRepository<MainFbTaskProgressEntity, Long> {
    Optional<MainFbTaskProgressEntity> findByPlayerId(String playerId);
}
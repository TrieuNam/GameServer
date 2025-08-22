package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.model_clothes.PlayerClothesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerClothesRepository extends JpaRepository<PlayerClothesEntity, Long> {
    List<PlayerClothesEntity> findByPlayerId(String playerId);
    Optional<PlayerClothesEntity> findByPlayerIdAndClothesId(String playerId, Integer clothesId);
}
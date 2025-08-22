package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.PlayerAngelEquipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAngelEquipRepository extends JpaRepository<PlayerAngelEquipEntity, Long> {
    List<PlayerAngelEquipEntity> findByPlayerId(Long playerId);
    Optional<PlayerAngelEquipEntity> findByPlayerIdAndPosition(Long playerId, int position);
}
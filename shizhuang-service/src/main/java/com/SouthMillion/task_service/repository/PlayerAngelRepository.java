package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.PlayerAngelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerAngelRepository extends JpaRepository<PlayerAngelEntity, Long> {
    Optional<PlayerAngelEntity> findByPlayerId(Long playerId);
}
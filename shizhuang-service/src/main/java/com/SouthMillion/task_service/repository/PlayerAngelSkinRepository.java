package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.PlayerAngelSkinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAngelSkinRepository extends JpaRepository<PlayerAngelSkinEntity, Long> {
    List<PlayerAngelSkinEntity> findByPlayerId(Long playerId);
    Optional<PlayerAngelSkinEntity> findByPlayerIdAndSkinSeq(Long playerId, int skinSeq);
}
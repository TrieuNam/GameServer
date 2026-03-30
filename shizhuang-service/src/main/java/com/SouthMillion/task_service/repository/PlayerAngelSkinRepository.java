package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.PlayerAngelSkinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PlayerAngelSkinRepository extends JpaRepository<PlayerAngelSkinEntity, Long> {
    List<PlayerAngelSkinEntity> findByPlayerId(Long playerId);
    Optional<PlayerAngelSkinEntity> findByPlayerIdAndSkinSeq(Long playerId, int skinSeq);
}
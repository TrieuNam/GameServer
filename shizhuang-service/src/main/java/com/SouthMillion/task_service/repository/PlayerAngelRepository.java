package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.PlayerAngelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface PlayerAngelRepository extends JpaRepository<PlayerAngelEntity, Long> {
    Optional<PlayerAngelEntity> findByPlayerId(Long playerId);
}
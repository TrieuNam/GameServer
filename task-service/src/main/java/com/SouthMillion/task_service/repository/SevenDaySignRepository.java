package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.SevenDaySignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SevenDaySignRepository extends JpaRepository<SevenDaySignEntity, Long> {
    Optional<SevenDaySignEntity> findByPlayerId(String playerId);
}

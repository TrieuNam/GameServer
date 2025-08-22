package com.SouthMillion.pet_service.repository;


import com.SouthMillion.pet_service.entity.PlayerHarnessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerHarnessRepository extends JpaRepository<PlayerHarnessEntity, Long> {
    Optional<PlayerHarnessEntity> findByPlayerIdAndHarnessId(String playerId, Integer harnessId);
    List<PlayerHarnessEntity> findByPlayerId(String playerId);
}
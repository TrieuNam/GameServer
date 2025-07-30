package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.KnightsProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnightsProgressRepository extends JpaRepository<KnightsProgressEntity, Long> {
    Optional<KnightsProgressEntity> findByUserId(String userId);
}

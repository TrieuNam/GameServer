package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.model.LimitCoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LimitCoreRepository extends JpaRepository<LimitCoreEntity, LimitCoreEntity.LimitCoreKey> {
    List<LimitCoreEntity> findByIdUserId(Long userId);
}
package com.SouthMillion.task_service.repository;

import com.SouthMillion.task_service.entity.ShiZhuangEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiZhuangRepository extends JpaRepository<ShiZhuangEntity, Integer> {
    Optional<ShiZhuangEntity> findByUserIdAndId(String userId, int id);

    List<ShiZhuangEntity> findByUserId(String userId);
}
package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.GemDrawingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GemDrawingRepository extends JpaRepository<GemDrawingEntity, Long> {
    List<GemDrawingEntity> findAllByUserId(Long userId);
    GemDrawingEntity findByUserIdAndId(Long userId, Long drawingId);
}

package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<ShopEntity, Integer> {
    Optional<ShopEntity> findByUserIdAndItemIndex(Long userId, Integer itemIndex);
    List<ShopEntity> findByUserId(Long userId);
}
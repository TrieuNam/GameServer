package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ShopPurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchaseEntity, Long> {
    List<ShopPurchaseEntity> findByUserId(Long userId);
    Optional<ShopPurchaseEntity> findByUserIdAndShopIndex(Long userId, Integer shopIndex);
}
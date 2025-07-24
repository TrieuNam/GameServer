package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.MysteryShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MysteryShopRepository extends JpaRepository<MysteryShopEntity, Integer> {
    Optional<MysteryShopEntity> findByUserIdAndItemIndex(Long userId, Integer itemIndex);

    List<MysteryShopEntity> findByUserId(Long userId);
}

package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
    List<ItemEntity> findByUserId(String userId);

    Optional<ItemEntity> findByUserIdAndItemId(String userId, Integer itemId);
}

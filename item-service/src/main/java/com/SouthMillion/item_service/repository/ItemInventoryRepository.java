package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ItemInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemInventoryRepository extends JpaRepository<ItemInventory, Long> {
    Optional<ItemInventory> findByUserIdAndItemId(String userId, int itemId);
}

package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ItemRecycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRecycleRepository extends JpaRepository<ItemRecycleEntity, Long> {
}
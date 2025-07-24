package com.SouthMillion.item_service.repository;

import com.SouthMillion.item_service.entity.ItemEntity;
import org.SouthMillion.dto.item.ItemDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
    Optional<ItemEntity> findByUserIdAndItemId(String userId, int itemId);
    List<ItemEntity> findByUserId(String userId);
}
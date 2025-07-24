package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.ItemEntity;
import com.SouthMillion.item_service.repository.ItemRepository;
import com.SouthMillion.item_service.service.cache.ItemCacheService;
import com.SouthMillion.item_service.service.producer.ItemEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemCacheService itemCacheService;
    private final ItemEventProducer itemEventProducer;

    // Lấy 1 item
    public ItemDto getItemDto(String userId, int itemId) {
        ItemEntity entity = getItem(userId, itemId);
        return entity != null ? toDto(entity) : null;
    }

    // Lấy tất cả item
    public List<ItemDto> getAllItemsDto(String userId) {
        return getAllItems(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    // Thêm/tăng item
    public ItemDto addOrIncreaseItemDto(String userId, int itemId, int amount) {
        ItemEntity entity = addOrIncreaseItem(userId, itemId, amount);
        return toDto(entity);
    }

    // Mua/trừ item
    public void buyItem(String userId, int itemId, int amount) {
        ItemEntity item = itemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new RuntimeException("Not enough item"));
        if (item.getAmount() < amount) throw new RuntimeException("Not enough item");
        item.setAmount(item.getAmount() - amount);
        ItemEntity saved = itemRepository.save(item);
        itemCacheService.putItemToCache(saved);

        // Gửi event Kafka
        itemEventProducer.sendItemEvent(userId, "ITEM_BOUGHT",
                ItemDto.builder()
                        .id(saved.getId())
                        .userId(saved.getUserId())
                        .itemId(saved.getItemId())
                        .amount(saved.getAmount())
                        .build()
        );
    }

    // Xóa item
    public boolean deleteItem(String userId, int itemId) {
        Optional<ItemEntity> itemOpt = itemRepository.findByUserIdAndItemId(userId, itemId);
        if (itemOpt.isPresent()) {
            itemRepository.delete(itemOpt.get());
            itemCacheService.removeItemFromCache(userId, itemId);

            // Gửi event Kafka
            itemEventProducer.sendItemEvent(userId, "ITEM_DELETED", ItemDto.builder()
                    .id(itemOpt.get().getId())
                    .userId(userId)
                    .itemId(itemId)
                    .amount(0)
                    .build()
            );
            return true;
        }
        return false;
    }

    // --------- CÁC HÀM NỘI BỘ ENTITY --------

    // Lấy 1 item (entity)
    public ItemEntity getItem(String userId, int itemId) {
        ItemEntity item = itemCacheService.getItemFromCache(userId, itemId);
        if (item != null) return item;
        item = itemRepository.findByUserIdAndItemId(userId, itemId).orElse(null);
        if (item != null) itemCacheService.putItemToCache(item);
        return item;
    }

    // Lấy tất cả item (entity)
    public List<ItemEntity> getAllItems(String userId) {
        return itemRepository.findByUserId(userId);
    }

    // Thêm/tăng item (entity)
    public ItemEntity addOrIncreaseItem(String userId, int itemId, int amount) {
        ItemEntity item = itemRepository.findByUserIdAndItemId(userId, itemId)
                .orElse(ItemEntity.builder().userId(userId).itemId(itemId).amount(0).build());
        item.setAmount(item.getAmount() + amount);
        ItemEntity saved = itemRepository.save(item);
        itemCacheService.putItemToCache(saved);

        // Gửi event Kafka
        itemEventProducer.sendItemEvent(userId, "ITEM_ADDED_OR_INCREASED",
                ItemDto.builder()
                        .id(saved.getId())
                        .userId(saved.getUserId())
                        .itemId(saved.getItemId())
                        .amount(saved.getAmount())
                        .build()
        );
        return saved;
    }

    // ENTITY <-> DTO MAPPING
    public ItemDto toDto(ItemEntity entity) {
        return ItemDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .itemId(entity.getItemId())
                .amount(entity.getAmount())
                .build();
    }
}
package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.ItemInventory;
import com.SouthMillion.item_service.repository.ItemInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmItemService {
    @Autowired
    private ItemInventoryRepository itemRepository;

    public void addItem(String userId, int itemId, int count) {
        ItemInventory inv = itemRepository.findByUserIdAndItemId(userId, itemId)
                .orElse(new ItemInventory().builder().userId(userId).itemId(itemId).count(0).build());
        inv.setCount(inv.getCount() + count);
        itemRepository.save(inv);
    }

    public void removeItem(String userId, int itemId, int count) {
        ItemInventory inv = itemRepository.findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new RuntimeException("User does not have this item!"));
        int remain = inv.getCount() - count;
        if (remain <= 0) {
            itemRepository.delete(inv);
        } else {
            inv.setCount(remain);
            itemRepository.save(inv);
        }
    }
}
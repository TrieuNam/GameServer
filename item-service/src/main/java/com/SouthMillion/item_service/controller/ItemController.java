package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.ItemService;
import org.SouthMillion.dto.item.Knapsack.ItemDTO;
import org.SouthMillion.dto.item.Knapsack.ItemRetrieveConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {
    @Autowired
    private ItemService itemService;

    @GetMapping("/{userId}")
    public List<ItemDTO> getItems(@PathVariable String userId) {
        return itemService.getAllByUser(userId);
    }

    @PostMapping("/{userId}/add")
    public ResponseEntity<?> addItem(@PathVariable String userId, @RequestParam int itemId, @RequestParam int count) {
        try {
            itemService.addItem(userId, itemId, count);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/consume")
    public ResponseEntity<?> consumeItem(@PathVariable String userId, @RequestParam int itemId, @RequestParam int count) {
        try {
            boolean success = itemService.consumeItem(userId, itemId, count);
            if (!success) return ResponseEntity.badRequest().body("Not enough item or itemId invalid");
            return ResponseEntity.ok(true);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}/notenough/{itemId}")
    public ResponseEntity<Boolean> isNotEnough(
            @PathVariable String userId,
            @PathVariable int itemId,
            @RequestParam int count // số lượng cần kiểm tra
    ) {
        boolean enough = itemService.hasEnoughItem(userId, itemId, count);
        return ResponseEntity.ok(!enough); // true: không đủ, false: đủ
    }

    @GetMapping("/{userId}/single")
    public ItemDTO getSingle(@PathVariable String userId, @RequestParam int itemId) {
        return itemService.getSingle(userId, itemId);
    }

}
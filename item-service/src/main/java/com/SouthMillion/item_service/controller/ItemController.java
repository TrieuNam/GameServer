package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.entity.ItemEntity;
import com.SouthMillion.item_service.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.BuyItemRequest;
import org.SouthMillion.dto.item.ItemDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService service;

    @GetMapping("/get")
    public ResponseEntity<ItemDto> get(@RequestParam String userId, @RequestParam int itemId) {
        ItemDto item = service.getItemDto(userId, itemId);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ItemDto>> list(@RequestParam String userId) {
        return ResponseEntity.ok(service.getAllItemsDto(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<ItemDto> addOrIncrease(
            @RequestParam String userId,
            @RequestParam int itemId,
            @RequestParam int amount) {
        ItemDto result = service.addOrIncreaseItemDto(userId, itemId, amount);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buy(
            @RequestParam String userId,
            @RequestParam int itemId,
            @RequestParam int amount) {
        try {
            service.buyItem(userId, itemId, amount);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(
            @RequestParam String userId,
            @RequestParam int itemId) {
        boolean ok = service.deleteItem(userId, itemId);
        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }
}
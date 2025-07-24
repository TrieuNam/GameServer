package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.ItemRecycleService;
import org.SouthMillion.dto.item.ItemRecycleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itemrecycle")
public class ItemRecycleController {
    @Autowired
    private ItemRecycleService itemRecycleService;

    @GetMapping("/info/{userId}")
    public ItemRecycleDTO info(@PathVariable Long userId) {
        return itemRecycleService.getInfo(userId);
    }

    @PostMapping("/levelup/{userId}")
    public void levelUp(@PathVariable Long userId, @RequestBody List<Integer> itemIds) {
        itemRecycleService.levelUp(userId, itemIds);
    }
}

package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.GmItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gm")
public class GmItemController {
    @Autowired
    private GmItemService gmItemService;

    @PostMapping("/add")
    public void addItem(@RequestParam String userId, @RequestParam int itemId, @RequestParam int count) {
        gmItemService.addItem(userId, itemId, count);
    }

    @PostMapping("/remove")
    public void removeItem(@RequestParam String userId, @RequestParam int itemId, @RequestParam int count) {
        gmItemService.removeItem(userId, itemId, count);
    }
}

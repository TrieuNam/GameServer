package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.MysteryShopService;
import org.SouthMillion.dto.item.MysteryShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mysteryshop")
public class MysteryShopController {
    @Autowired
    private MysteryShopService shopService;

    // Lấy thông tin shop user
    @GetMapping("/info/{userId}")
    public MysteryShopDTO info(@PathVariable Long userId) {
        return shopService.getShopInfo(userId);
    }

    // Mua item
    @PostMapping("/buy/{userId}/{index}")
    public void buy(@PathVariable Long userId, @PathVariable Integer index) {
        shopService.buy(userId, index);
    }

    // Đổi shop (random lại hàng shop cho user, ví dụ truyền từ config)
    @PostMapping("/refresh/{userId}")
    public void refresh(@PathVariable Long userId, @RequestBody List<Integer> newIndexes) {
        shopService.refreshShop(userId, newIndexes);
    }
}
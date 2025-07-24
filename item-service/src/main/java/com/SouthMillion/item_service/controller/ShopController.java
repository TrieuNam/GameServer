package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.ShopService;
import org.SouthMillion.dto.item.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {
    @Autowired
    private ShopService shopService;

    @GetMapping("/limit/{userId}")
    public List<ShopDTO> limit(@PathVariable Long userId) {
        return shopService.getLimit(userId);
    }

    @PostMapping("/buy/{userId}/{index}/{num}")
    public void buy(@PathVariable Long userId, @PathVariable Integer index, @PathVariable Integer num) {
        shopService.buy(userId, index, num);
    }
}
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
    private MysteryShopService mysteryShopService;

    @GetMapping("/limit/{userId}")
    public List<MysteryShopDTO> limit(@PathVariable Long userId) {
        return mysteryShopService.getLimit(userId);
    }

    @PostMapping("/buy/{userId}/{index}/{num}")
    public void buy(@PathVariable Long userId, @PathVariable Integer index, @PathVariable Integer num) {
        mysteryShopService.buy(userId, index, num);
    }
}

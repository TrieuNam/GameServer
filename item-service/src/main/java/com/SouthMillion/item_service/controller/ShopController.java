package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.ShopService;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
public class ShopController {
    @Autowired
    private ShopService shopService;

    // Mua item thường trong shop
    @PostMapping("/shop/buy")
    public void buyItem(@RequestParam("userId") Long userId,
                        @RequestParam("index") int index,
                        @RequestParam("num") int num) {
        shopService.buyItem(userId, index, num);
    }

    // Lấy thông tin giới hạn shop
    @GetMapping("/shop/info")
    public Msgother.PB_SCShopInfo getShopInfo(@RequestParam("userId") Long userId) {
        return shopService.getShopInfo(userId);
    }

    // Mua item từ cloth shop
    @PostMapping("/clothshop/buy")
    public void buyCloth(@RequestParam("userId") Long userId,
                         @RequestParam("seq") int seq,
                         @RequestParam("num") int num) {
        shopService.buyClothItem(userId, seq, num);
    }

    // Thao tác mystery shop (0: làm mới, 1: mua)
    @PostMapping("/mysteryshop/operate")
    public void operateMystery(@RequestParam("userId") Long userId,
                               @RequestParam("opType") int opType,
                               @RequestParam("param") int param) {
        shopService.operateMystery(userId, opType, param);
    }

    // Lấy thông tin mystery shop
    @GetMapping("/mysteryshop/info")
    public Msgother.PB_SCMysteryShopInfo getMysteryInfo(@RequestParam("userId") Long userId) {
        return shopService.getMysteryShopInfo(userId);
    }
}
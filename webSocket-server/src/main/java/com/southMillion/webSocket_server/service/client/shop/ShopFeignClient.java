package com.southMillion.webSocket_server.service.client.shop;

import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-service", contextId = "shopFeignClient")
public interface ShopFeignClient {
    @PostMapping("/api/shop/buy")
    void buyItem(@RequestParam("userId") Long userId, @RequestParam("index") int index, @RequestParam("num") int num);

    @GetMapping("/api/shop/info")
    Msgother.PB_SCShopInfo getShopInfo(@RequestParam("userId") Long userId);

    @PostMapping("/api/clothshop/buy")
    void buyCloth(@RequestParam("userId") Long userId, @RequestParam("seq") int seq, @RequestParam("num") int num);

    @PostMapping("/api/mysteryshop/operate")
    void operateMystery(@RequestParam("userId") Long userId, @RequestParam("opType") int opType, @RequestParam("param") int param);

    @GetMapping("/api/mysteryshop/info")
    Msgother.PB_SCMysteryShopInfo getMysteryInfo(@RequestParam("userId") Long userId);
}
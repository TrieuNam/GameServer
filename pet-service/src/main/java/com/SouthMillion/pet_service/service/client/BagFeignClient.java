package com.SouthMillion.pet_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-service", contextId = "bagFeignClient")
public interface BagFeignClient {
    @GetMapping("/api/bag/has-enough")
    boolean hasEnough(@RequestParam("playerId") String playerId,
                      @RequestParam("itemId") Integer itemId,
                      @RequestParam("count") Integer count);

    @GetMapping("/api/bag/has-enough-gold")
    boolean hasEnoughGold(@RequestParam("playerId") String playerId,
                          @RequestParam("gold") Integer gold);

    @PostMapping("/api/bag/consume-item")
    String consumeItem(@RequestParam("playerId") String playerId,
                       @RequestParam("itemId") Integer itemId,
                       @RequestParam("count") Integer count);

    @PostMapping("/api/bag/consume-gold")
    String consumeGold(@RequestParam("playerId") String playerId,
                       @RequestParam("gold") Integer gold);

    @PostMapping("/api/bag/add-item")
    String addItem(@RequestParam("playerId") String playerId,
                   @RequestParam("itemId") Integer itemId,
                   @RequestParam("count") Integer count);

    @PostMapping("/api/bag/add-gold")
    String addGold(@RequestParam("playerId") String playerId,
                   @RequestParam("gold") Integer gold);
}
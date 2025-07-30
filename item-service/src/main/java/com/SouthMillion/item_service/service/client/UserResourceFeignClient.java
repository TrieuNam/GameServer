package com.SouthMillion.item_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserResourceFeignClient {
    @PostMapping("/api/user/deduct")
    void deductResource(@RequestParam("userId") Long userId,
                        @RequestParam("itemId") Integer itemId,
                        @RequestParam("amount") Integer amount);

    @PostMapping("/api/user/add")
    void addItem(@RequestParam("userId") Long userId,
                 @RequestParam("itemId") Integer itemId,
                 @RequestParam("amount") Integer amount);

    @GetMapping("/api/user/has")
    boolean hasEnough(@RequestParam("userId") Long userId,
                      @RequestParam("itemId") Integer itemId,
                      @RequestParam("amount") Integer amount);


    @GetMapping("/api/user/level")
     Integer getUserLevel(@RequestParam("userId") String userId);
}
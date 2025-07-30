package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.UserResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserResourceController {

    @Autowired
    private UserResourceService service;

    @PostMapping("/deduct")
    public void deductResource(@RequestParam("userId") Long userId,
                               @RequestParam("itemId") Integer itemId,
                               @RequestParam("amount") Integer amount) {
        service.deductResource(userId, itemId, amount);
    }

    @PostMapping("/add")
    public void addItem(@RequestParam("userId") Long userId,
                        @RequestParam("itemId") Integer itemId,
                        @RequestParam("amount") Integer amount) {
        service.addItem(userId, itemId, amount);
    }

    @GetMapping("/has")
    public boolean hasEnough(@RequestParam("userId") Long userId,
                             @RequestParam("itemId") Integer itemId,
                             @RequestParam("amount") Integer amount) {
        return service.hasEnough(userId, itemId, amount);
    }
}
package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.LimitCoreService;
import org.SouthMillion.dto.item.LimitCoreDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/limitcore")
public class LimitCoreController {
    @Autowired
    private LimitCoreService limitCoreService;

    @GetMapping("/info/{userId}")
    public LimitCoreDTO info(@PathVariable Long userId) {
        return limitCoreService.getInfo(userId);
    }

    @PostMapping("/operate/{userId}/{opType}/{p1}")
    public void operate(@PathVariable Long userId,
                        @PathVariable Integer opType,
                        @PathVariable Integer p1) {
        limitCoreService.operate(userId, opType, p1);
    }
}
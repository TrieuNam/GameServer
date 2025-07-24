package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.GemService;
import org.SouthMillion.dto.item.GemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gem")
public class GemController {
    @Autowired
    private GemService gemService;

    @GetMapping("/info/{userId}")
    public GemDTO info(@PathVariable Long userId) {
        return gemService.getInfo(userId);
    }

    @PostMapping("/operate/{userId}/{opType}/{param1}/{param2}")
    public void operate(@PathVariable Long userId,
                        @PathVariable Integer opType,
                        @PathVariable Integer param1,
                        @PathVariable Integer param2) {
        gemService.operate(userId, opType, param1, param2);
    }
}

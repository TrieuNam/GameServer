package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.KnightsService;
import org.SouthMillion.dto.globalserver.KnightsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knights")
public class KnightsController {
    @Autowired
    private KnightsService knightsService;

    @GetMapping("/info/{userId}")
    public KnightsDTO getInfo(@PathVariable Long userId) {
        return knightsService.getKnightsInfo(userId);
    }

    @PostMapping("/operate/{userId}/{opType}/{param1}")
    public void operate(@PathVariable Long userId, @PathVariable Integer opType, @PathVariable Integer param1) {
        knightsService.operate(userId, opType, param1);
    }
}

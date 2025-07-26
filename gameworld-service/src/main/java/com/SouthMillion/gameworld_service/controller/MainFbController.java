package com.SouthMillion.gameworld_service.controller;

import com.SouthMillion.gameworld_service.service.MainFbService;
import org.SouthMillion.dto.gameworld.MainFbDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mainfb")
public class MainFbController {
    @Autowired
    private MainFbService mainFbService;

    @GetMapping("/{userId}")
    public MainFbDTO get(@PathVariable Long userId) {
        return mainFbService.getOrInit(userId);
    }

    @PostMapping("/{userId}/challenge")
    public MainFbDTO challenge(@PathVariable Long userId) {
        return mainFbService.update(userId, entity -> {
            entity.setLevel(entity.getLevel() + 1);
            // update time, v.v nếu cần
        });
    }

    @PostMapping("/{userId}/reward")
    public MainFbDTO reward(@PathVariable Long userId) {
        return mainFbService.update(userId, entity -> {
            entity.setStage(entity.getStage() + 1);
            // update time, v.v nếu cần
        });
    }
}

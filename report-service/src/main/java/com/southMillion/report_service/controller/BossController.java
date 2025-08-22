package com.southMillion.report_service.controller;

import com.southMillion.report_service.service.BossService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boss")
@RequiredArgsConstructor
public class BossController {
    private final BossService bossService;

    @GetMapping("/{userId}/kill-count")
    public int getUserBossKill(@PathVariable("userId") Long userId) {
        return bossService.getUserBossKill(userId);
    }
}
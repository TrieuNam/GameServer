package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.AdService;
import org.SouthMillion.dto.globalserver.AdDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ad")
public class AdController {
    @Autowired
    private AdService adService;

    @GetMapping("/info/{userId}")
    public AdDTO info(@PathVariable Long userId) {
        return adService.getAdInfo(userId);
    }

    @PostMapping("/fetch/{userId}")
    public void fetch(@PathVariable Long userId) {
        adService.fetchAd(userId);
    }
}

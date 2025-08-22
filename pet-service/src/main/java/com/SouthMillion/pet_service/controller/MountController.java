package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.MountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mount")
@RequiredArgsConstructor
public class MountController {

    private final MountService mountService;

    @GetMapping("/info")
    public byte[] getMountInfo(@RequestParam("playerId") String playerId) {
        return mountService.getMountInfo(playerId);
    }

    @GetMapping("/harness-list")
    public byte[] getHarnessListInfo(@RequestParam("playerId") String playerId) {
        return mountService.getHarnessListInfo(playerId);
    }
}
package com.SouthMillion.lingzhu_service.controller;

import com.SouthMillion.lingzhu_service.entity.LingZhuProgress;
import com.SouthMillion.lingzhu_service.service.LingZhuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lingzhu")
@RequiredArgsConstructor
public class LingZhuController {

    private final LingZhuService lingZhuService;

    /** GET all lingzhu progress for a role */
    @GetMapping("/{roleId}")
    public List<LingZhuProgress> getAll(@PathVariable Long roleId) {
        return lingZhuService.getAll(roleId);
    }

    /** POST challenge a dungeon stage */
    @PostMapping("/{roleId}/challenge")
    public Map<String, Object> challenge(
            @PathVariable Long roleId,
            @RequestParam int stage,
            @RequestParam(defaultValue = "0") int p1) {
        return lingZhuService.challenge(roleId, stage, p1);
    }

    /** POST sweep (auto-clear) a dungeon stage */
    @PostMapping("/{roleId}/sweep")
    public Map<String, Object> sweep(
            @PathVariable Long roleId,
            @RequestParam int stage,
            @RequestParam(defaultValue = "1") int count) {
        return lingZhuService.sweep(roleId, stage, count);
    }
}

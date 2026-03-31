package com.SouthMillion.gem_service.controller;
import com.SouthMillion.gem_service.service.GemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/gem") @RequiredArgsConstructor
public class GemController {
    private final GemService gemService;

    @GetMapping("/{roleId}")
    public Map<String, Object> getInfo(@PathVariable Long roleId) {
        return gemService.getGemInfo(roleId);
    }

    @PostMapping("/{roleId}/inlay")
    public Map<String, Object> inlay(@PathVariable Long roleId,
            @RequestParam Integer gemId, @RequestParam(required = false) Integer slotType) {
        return gemService.inlay(roleId, gemId, slotType);
    }

    @PostMapping("/{roleId}/remove")
    public Map<String, Object> remove(@PathVariable Long roleId, @RequestParam Integer gemId) {
        return gemService.remove(roleId, gemId);
    }

    @PostMapping("/{roleId}/compose")
    public Map<String, Object> compose(@PathVariable Long roleId, @RequestBody List<Integer> sourceIds) {
        return gemService.compose(roleId, sourceIds);
    }

    @PostMapping("/{roleId}/upgrade-all")
    public Map<String, Object> upgradeAll(@PathVariable Long roleId, @RequestBody List<Integer> itemIds) {
        return gemService.upgradeAll(roleId, itemIds);
    }

    @PostMapping("/{roleId}/buy")
    public Map<String, Object> buy(@PathVariable Long roleId, @RequestBody List<Integer> itemIds) {
        return gemService.buy(roleId, itemIds);
    }
}

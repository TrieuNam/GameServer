package com.SouthMillion.knights_service.controller;
import com.SouthMillion.knights_service.entity.KnightsHandbook;
import com.SouthMillion.knights_service.service.KnightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/knights") @RequiredArgsConstructor
public class KnightsController {
    private final KnightsService knightsService;

    @GetMapping("/{roleId}")
    public KnightsHandbook getInfo(@PathVariable Long roleId) {
        return knightsService.getOrCreate(roleId);
    }

    @GetMapping("/{roleId}/conditions")
    public Map<String, Object> getConditions(@PathVariable Long roleId) {
        return knightsService.getConditions(roleId);
    }

    @PostMapping("/{roleId}/claim-seq")
    public KnightsHandbook claimSeq(@PathVariable Long roleId, @RequestParam int seqIndex) {
        return knightsService.claimSeqReward(roleId, seqIndex);
    }

    @PostMapping("/{roleId}/claim-level")
    public KnightsHandbook claimLevel(@PathVariable Long roleId, @RequestParam int levelIndex) {
        return knightsService.claimLevelReward(roleId, levelIndex);
    }
}

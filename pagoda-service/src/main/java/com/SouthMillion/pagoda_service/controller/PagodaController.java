package com.SouthMillion.pagoda_service.controller;

import com.SouthMillion.pagoda_service.entity.ShiLianProgress;
import com.SouthMillion.pagoda_service.service.PagodaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/pagoda")
@RequiredArgsConstructor
public class PagodaController {

    private final PagodaService pagodaService;

    @GetMapping("/{roleId}/shilian")
    public ShiLianProgress getShiLian(@PathVariable Long roleId) {
        return pagodaService.getShiLian(roleId);
    }

    @PostMapping("/{roleId}/shilian/challenge")
    public ShiLianProgress challengeShiLian(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int p1) {
        return pagodaService.challengeShiLian(roleId, p1);
    }

    @PostMapping("/{roleId}/shilian/claim")
    public boolean claimShiLian(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int p1) {
        return pagodaService.claimShiLian(roleId, p1);
    }

    @GetMapping("/{roleId}/gumo")
    public Map<String, Object> getGuMo(@PathVariable Long roleId) {
        return pagodaService.getGuMo(roleId);
    }

    @PostMapping("/{roleId}/gumo/challenge")
    public Map<String, Object> challengeGuMo(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int p1) {
        return pagodaService.challengeGuMo(roleId, p1);
    }

    @PostMapping("/{roleId}/gumo/claim")
    public boolean claimGuMo(
            @PathVariable Long roleId,
            @RequestParam(defaultValue = "0") int p1) {
        return pagodaService.claimGuMo(roleId, p1);
    }
}

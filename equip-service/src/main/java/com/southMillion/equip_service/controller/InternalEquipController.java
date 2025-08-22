package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.EquipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/equip")
@RequiredArgsConstructor
public class InternalEquipController {

    private final EquipService svc;

    @GetMapping("/{roleId}")
    public EquipDTOs.ListResp list(@PathVariable String roleId) {
        return svc.list(roleId);
    }

    @PostMapping("/equip")
    public EquipDTOs.OkResp equip(@Valid @RequestBody EquipDTOs.EquipReq req) {
        return svc.equip(req);
    }

    @PostMapping("/unequip")
    public EquipDTOs.OkResp unequip(@Valid @RequestBody EquipDTOs.UnequipReq req) {
        return svc.unequip(req);
    }
}
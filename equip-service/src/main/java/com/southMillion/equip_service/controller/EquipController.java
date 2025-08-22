package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.EquipFumoService;
import com.southMillion.equip_service.service.EquipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/equip")
public class EquipController {

    private final EquipService equipService;
    private final EquipFumoService fumoService;

    // ======= Equip basic =======

    @GetMapping("/{roleId}")
    public EquipDTOs.ListResp list(@PathVariable String roleId) {
        return equipService.list(roleId);
    }

    @PostMapping("/equip")
    @ResponseStatus(HttpStatus.OK)
    public EquipDTOs.OkResp equip(@Valid @RequestBody EquipDTOs.EquipReq req) {
        return equipService.equip(req);
    }

    @PostMapping("/unequip")
    @ResponseStatus(HttpStatus.OK)
    public EquipDTOs.OkResp unequip(@Valid @RequestBody EquipDTOs.UnequipReq req) {
        return equipService.unequip(req);
    }

    // ======= Fumo =======

    @GetMapping("/fumo/{roleId}")
    public EquipFumoDTOs.FumoListResp fumoList(@PathVariable("roleId") String roleId) {
        return fumoService.list(roleId);
    }

    @GetMapping("/fumo/{roleId}/{equipType}")
    public EquipFumoDTOs.FumoOneResp fumoOne(@PathVariable("roleId") String roleId,@PathVariable("equipType") int equipType) {
        return fumoService.one(roleId, equipType);
    }

    @PostMapping("/fumo/add-exp")
    public EquipFumoDTOs.FumoOneResp addExp(@Valid @RequestBody EquipFumoDTOs.AddExpReq req) {
        return fumoService.addExp(req);
    }

    @PostMapping("/fumo/activate")
    public EquipFumoDTOs.FumoOneResp activate(@Valid @RequestBody EquipFumoDTOs.ActivateReq req) {
        return fumoService.activate(req);
    }

    @PostMapping("/fumo/reset")
    public EquipFumoDTOs.OkResp reset(@Valid @RequestBody EquipFumoDTOs.ResetReq req) {
        return fumoService.reset(req);
    }
}
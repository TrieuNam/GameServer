package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.EquipFumoService;
import com.southMillion.equip_service.service.EquipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/equip", produces = MediaType.APPLICATION_JSON_VALUE)
public class EquipController {

    private final EquipService equipService;
    private final EquipFumoService fumoService;

    // ======= Equip basic =======

    @GetMapping("/{roleId}")
    public EquipDTOs.ListResp list(@PathVariable("roleId") String roleId) {
        return equipService.list(roleId);
    }

    @PostMapping(value = "/equip", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public EquipDTOs.OkResp equip(@Valid @RequestBody EquipDTOs.EquipReq req) {
        return equipService.equip(req);
    }

    @PostMapping(value = "/unequip", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public EquipDTOs.OkResp unequip(@Valid @RequestBody EquipDTOs.UnequipReq req) {
        return equipService.unequip(req);
    }

    // NEW: wear theo itemId trong túi. Optional: ?bagType=1 (mặc định lấy từ props)
    @PostMapping("/wear/{roleId}/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public EquipDTOs.OkResp wear(@PathVariable("roleId") String roleId,
                                 @PathVariable("itemId") int itemId,
                                 @RequestParam(value = "bagType", required = false) Integer bagType) {
        return equipService.wear(roleId, itemId, bagType);
    }

    // ======= Fumo =======

    @GetMapping("/fumo/{roleId}")
    public EquipFumoDTOs.FumoListResp fumoList(@PathVariable("roleId") String roleId) {
        return fumoService.list(roleId);
    }

    @GetMapping("/fumo/{roleId}/{equipType}")
    public EquipFumoDTOs.FumoOneResp fumoOne(@PathVariable("roleId") String roleId,
                                             @PathVariable("equipType") int equipType) {
        return fumoService.one(roleId, equipType);
    }

    @PostMapping(value = "/fumo/add-exp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipFumoDTOs.FumoOneResp addExp(@Valid @RequestBody EquipFumoDTOs.AddExpReq req) {
        return fumoService.addExp(req);
    }

    @PostMapping(value = "/fumo/activate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipFumoDTOs.FumoOneResp activate(@Valid @RequestBody EquipFumoDTOs.ActivateReq req) {
        return fumoService.activate(req);
    }

    @PostMapping(value = "/fumo/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EquipFumoDTOs.OkResp reset(@Valid @RequestBody EquipFumoDTOs.ResetReq req) {
        return fumoService.reset(req);
    }
}
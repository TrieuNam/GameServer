package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="equip-service", path="/api/equip", contextId = "EquipFeign")
public interface EquipHttpClient {

    @GetMapping("/{roleId}")
    EquipDTOs.ListResp list(@PathVariable("roleId") String roleId);

    @PostMapping("/equip")
    EquipDTOs.OkResp equip(@RequestBody EquipDTOs.EquipReq req);

    @PostMapping("/unequip")
    EquipDTOs.OkResp unequip(@RequestBody EquipDTOs.UnequipReq req);

    @PostMapping("/wear/{roleId}/{itemId}")
    EquipDTOs.OkResp wear(@PathVariable("roleId") String roleId,
                          @PathVariable("itemId") int itemId);
}
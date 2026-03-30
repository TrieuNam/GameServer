package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name="equip-service", path="/api/equip", contextId = "EquipFeign")
public interface EquipHttpClient {

    @GetMapping("/{roleId}")
    EquipDTOs.ListResp list(@PathVariable("roleId") String roleId);

    @GetMapping("/{roleId}/wearable-items")
    Map<String, Object> wearableItems(@PathVariable("roleId") String roleId);

    @PostMapping("/equip")
    EquipDTOs.OkResp equip(@RequestBody EquipDTOs.EquipReq req);

    @PostMapping("/unequip")
    EquipDTOs.OkResp unequip(@RequestBody EquipDTOs.UnequipReq req);

    @PostMapping("/wear/{roleId}/{itemId}")
    EquipDTOs.OkResp wear(@PathVariable("roleId") String roleId,
                          @PathVariable("itemId") int itemId);

    @GetMapping("/fumo/{roleId}")
    EquipFumoDTOs.FumoListResp fumoList(@PathVariable("roleId") String roleId);

    @GetMapping("/fumo/{roleId}/{equipType}")
    EquipFumoDTOs.FumoOneResp fumoOne(@PathVariable("roleId") String roleId,
                                      @PathVariable("equipType") int equipType);

    @PostMapping("/fumo/add-exp")
    EquipFumoDTOs.FumoOneResp fumoAddExp(@RequestBody EquipFumoDTOs.AddExpReq req);

    @PostMapping("/fumo/activate")
    EquipFumoDTOs.FumoOneResp fumoActivate(@RequestBody EquipFumoDTOs.ActivateReq req);

    @PostMapping("/bag-sell")
    Map<String, Object> bagSell(@RequestBody Map<String, Object> body);

    @PostMapping("/transform")
    Map<String, Object> transform(@RequestBody Map<String, Object> body);
}
package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="equip-service", path="/api/equip", contextId = "EquipFumoFeign")
public interface EquipFumoFeign {
    @GetMapping("/fumo/{roleId}")
    EquipFumoDTOs.FumoListResp list(@PathVariable("roleId") String roleId);

    @GetMapping("/fumo/{roleId}/{equipType}")
    EquipFumoDTOs.FumoOneResp one(@PathVariable("roleId") String roleId,
                                  @PathVariable("equipType") int equipType);

    @PostMapping("/fumo/add-exp")
    EquipFumoDTOs.FumoOneResp addExp(@RequestBody EquipFumoDTOs.AddExpReq req);

    @PostMapping("/fumo/activate")
    EquipFumoDTOs.FumoOneResp activate(@RequestBody EquipFumoDTOs.ActivateReq req);

    @PostMapping("/fumo/reset")
    EquipFumoDTOs.OkResp reset(@RequestBody EquipFumoDTOs.ResetReq req);
}
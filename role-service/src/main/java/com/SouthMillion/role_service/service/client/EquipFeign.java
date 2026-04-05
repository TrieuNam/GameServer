package com.SouthMillion.role_service.service.client;

import org.SouthMillion.dto.equip.EquipDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "equip-service", path = "/api/equip/internal", contextId = "RoleEquipFeign")
public interface EquipFeign {

    @GetMapping("/{roleId}")
    EquipDTOs.ListResp list(@PathVariable("roleId") Long roleId);
}

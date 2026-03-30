package com.SouthMillion.equip_service.service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "role-service")
public interface RoleFeign {

    @PostMapping("/api/role/internal/{roleId}/stats/delta")
    RoleDTOs.RoleResp applyStatDelta(@PathVariable("roleId") Long roleId,
                                     @RequestBody StatDeltaReq req);

    record StatDeltaReq(long hp, long attack, long defense, int speed) {}
}

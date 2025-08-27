package com.SouthMillion.box_service.service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "role-service", path = "/api/role")
public interface RoleFeign {
    @GetMapping("/{roleId}")
    RoleDTOs.RoleResp detail(@PathVariable("roleId") String roleId);


    @PostMapping("/exp/add")
    void addExp(@RequestBody RoleDTOs.AddExpReq req);
}
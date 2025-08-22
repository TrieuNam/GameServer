package com.southMillion.webSocket_server.service.client;

import jakarta.validation.Valid;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="role-service", path="/api/role")
public interface RoleHttpClient {
    @GetMapping("/list")
    org.SouthMillion.dto.role.RoleDTOs.ListResp list(@RequestParam("userId") String userId);

    @PostMapping
    org.SouthMillion.dto.role.RoleDTOs.RoleResp create(@RequestBody org.SouthMillion.dto.role.RoleDTOs.CreateRoleReq req);

    @GetMapping("/{roleId}")
    org.SouthMillion.dto.role.RoleDTOs.RoleResp detail(@PathVariable("roleId") String roleId);
}
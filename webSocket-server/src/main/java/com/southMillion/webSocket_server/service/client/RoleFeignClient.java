package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "role-service", contextId = "RoleFeignClient", path = "/api/role")
public interface RoleFeignClient {

    @PostMapping
    RoleDTOs.RoleResp createRole(@RequestBody RoleDTOs.CreateRoleReq req);

    @GetMapping("/by-user/{userId}")
    List<RoleDTOs.RoleResp> getRolesByUserId(@PathVariable("userId") String userId);
}

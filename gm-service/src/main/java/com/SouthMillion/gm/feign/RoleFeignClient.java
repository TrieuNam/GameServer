package com.SouthMillion.gm.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "role-service", path = "/api/role")
public interface RoleFeignClient {

    /** GET /api/role/by-user/{userId} → List of roles for a user */
    @GetMapping("/by-user/{userId}")
    List<Map<String, Object>> getRolesByUserId(@PathVariable String userId);

    /** PUT /api/role/{roleId}/vip — NOTE: endpoint does not exist in role-service yet.
     *  Requires role-service to implement VIP update before this call works. */
    @PutMapping("/{roleId}/vip")
    Map<String, Object> updateVipLevel(@PathVariable Long roleId, @RequestBody Map<String, Integer> request);

    /** GET /api/role/{roleId} */
    @GetMapping("/{roleId}")
    Map<String, Object> getRoleDetails(@PathVariable Long roleId);
}

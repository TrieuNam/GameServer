package com.SouthMillion.pet_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "role-service")
public interface RoleClient {

    @GetMapping("/api/role/{roleId}/level")
    ResponseEntity<Integer> getPlayerLevel(@PathVariable("roleId") String roleId);
}
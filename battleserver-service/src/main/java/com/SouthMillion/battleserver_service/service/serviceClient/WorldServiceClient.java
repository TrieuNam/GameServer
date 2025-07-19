package com.SouthMillion.battleserver_service.service.serviceClient;

import org.SouthMillion.dto.MonsterKilledRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "world-service")
public interface WorldServiceClient {
    @PostMapping("/api/world/monster-killed")
    void monsterKilled(@RequestBody MonsterKilledRequestDTO req);
}

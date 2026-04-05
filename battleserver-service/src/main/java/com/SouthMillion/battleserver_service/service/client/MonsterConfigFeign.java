package com.SouthMillion.battleserver_service.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "config-service", path = "/api/config", contextId = "BattleMonsterConfigFeign")
public interface MonsterConfigFeign {

    @GetMapping("/file")
    JsonNode getFile(@RequestParam("path") String path);
}

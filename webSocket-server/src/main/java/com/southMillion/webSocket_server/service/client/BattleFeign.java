package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@FeignClient(name = "battle-service")
public interface BattleFeign {
    @PostMapping("/api/battle/petfb/start")
    Map<String, Object> startPetFbBattle(@RequestParam("roleId") Long roleId, @RequestParam("level") int level);
}

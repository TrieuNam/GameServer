package com.SouthMillion.main_fb_service.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "gameworld-service")
public interface GameworldFeignClient {

    record CreateInstanceReq(String playerId, int stage, int level) {}
    record CreateInstanceResp(String battleId, long seed, long expireAt) {}

    @PostMapping("/api/gameworld/pve/instance")
    CreateInstanceResp createInstance(@RequestBody CreateInstanceReq req);
}
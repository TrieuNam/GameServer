package com.SouthMillion.world_service.service.client;

import org.SouthMillion.dto.battle.BattleRequestDTO;
import org.SouthMillion.dto.battle.BattleResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "battle-service", url = "http://localhost:8082")
public interface BattleServiceClient {
    @PostMapping("/api/battle/fight")
    BattleResultDTO fight(@RequestBody BattleRequestDTO request);
}

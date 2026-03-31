package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for leaderboard-service
 * Endpoint: GET /api/leaderboard/{rankingType}?roleId=
 *
 * rankingType values (phải khớp với LeaderboardController):
 *   1 = POWER, 2 = LEVEL, 3 = ARENA, 4 = GUILD, 5 = PET
 */
@FeignClient(name = "leaderboard-service")
public interface LeaderboardFeign {

    /**
     * Get leaderboard by type.
     * Controller: GET /api/leaderboard/{rankingType}?roleId=
     * Response: LeaderboardDTO.Response<LeaderboardDTO.LeaderboardResponse>
     *
     * @param rankingType integer type (1=power,2=level,3=arena,4=guild,5=pet,...)
     * @param roleId      optional – if provided, includes the player's own rank info
     */
    @GetMapping("/api/leaderboard/{rankingType}")
    Map<String, Object> getLeaderboard(
            @PathVariable("rankingType") int rankingType,
            @RequestParam(value = "roleId", required = false) String roleId);
}

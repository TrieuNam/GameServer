package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for gm-service (Game Master tool)
 * All calls are server-to-server (internal); no player authentication header needed.
 */
@FeignClient(name = "gm-service", path = "/api/gm")
public interface GmFeign {

    /** Give items to a player */
    @PostMapping("/item/give")
    Map<String, Object> giveItem(@RequestBody Map<String, Object> request);

    /** Remove items from a player */
    @PostMapping("/item/remove")
    Map<String, Object> removeItem(@RequestBody Map<String, Object> request);

    /** Add currency (gold, gem, etc.) to a player */
    @PostMapping("/currency/add")
    Map<String, Object> addCurrency(@RequestBody Map<String, Object> request);

    /** Deduct currency from a player */
    @PostMapping("/currency/deduct")
    Map<String, Object> deductCurrency(@RequestBody Map<String, Object> request);

    /** Update a player's VIP level */
    @PostMapping("/vip/update")
    Map<String, Object> updateVipLevel(@RequestBody Map<String, Object> request);

    /** Ban a user */
    @PostMapping("/user/{userId}/ban")
    Map<String, Object> banUser(@PathVariable("userId") Long userId,
                                @RequestParam String reason,
                                @RequestParam(required = false) Integer durationDays);

    /** Unban a user */
    @PostMapping("/user/{userId}/unban")
    Map<String, Object> unbanUser(@PathVariable("userId") Long userId,
                                  @RequestParam(required = false) String reason);

    /** Broadcast a system message to all players */
    @PostMapping("/broadcast")
    Map<String, Object> broadcastMessage(@RequestBody Map<String, Object> request);

    /** Get player details (used to verify GM can access the target) */
    @GetMapping("/player/{playerId}")
    Map<String, Object> getPlayerDetails(@PathVariable("playerId") String playerId);
}

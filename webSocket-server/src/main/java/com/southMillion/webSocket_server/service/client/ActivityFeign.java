package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for activity-service (新服活动 — Open-Server Activity system).
 * Covers 4 subsystems: SevenDaySign, LuckUnpacking, NewAreaPreferential, MarketShop.
 */
@FeignClient(name = "activity-service")
public interface ActivityFeign {

    // === SevenDaySign (七日签到) ===

    @GetMapping("/api/activity/{roleId}/sevenday")
    Map<String, Object> getSevenDay(@PathVariable("roleId") String roleId);

    @PostMapping("/api/activity/{roleId}/sevenday/claim")
    Map<String, Object> claimSevenDay(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "day", defaultValue = "1") int day);

    // === LuckUnpacking (开箱大吉) ===

    @GetMapping("/api/activity/{roleId}/luck")
    Map<String, Object> getLuck(@PathVariable("roleId") String roleId);

    @PostMapping("/api/activity/{roleId}/luck/claim")
    Map<String, Object> claimLuck(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "seq", defaultValue = "0") int seq);

    // === NewAreaPreferential (新服特惠) ===

    @GetMapping("/api/activity/{roleId}/newarea")
    Map<String, Object> getNewArea(@PathVariable("roleId") String roleId);

    @PostMapping("/api/activity/{roleId}/newarea/buy")
    Map<String, Object> buyNewArea(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "itemIndex", defaultValue = "0") int itemIndex);

    // === MarketShop (集市商店) ===

    @GetMapping("/api/activity/{roleId}/market")
    Map<String, Object> getMarket(@PathVariable("roleId") String roleId);

    @PostMapping("/api/activity/{roleId}/market/buy")
    Map<String, Object> buyMarket(
            @PathVariable("roleId") String roleId,
            @RequestParam(value = "goodsSeq", defaultValue = "0") int goodsSeq);

    @PostMapping("/api/activity/{roleId}/market/refresh")
    Map<String, Object> refreshMarket(@PathVariable("roleId") String roleId);

    // === DuoBao (夺宝) ===

    @PostMapping("/api/activity/{roleId}/duobao")
    Map<String, Object> duobaoOperation(
            @PathVariable("roleId") String roleId,
            @RequestBody Map<String, Object> request);

    // === RandActivity generic dispatch (handles all 42 activity types from msg 3000) ===

    @PostMapping("/api/activity/{roleId}/rand")
    Map<String, Object> randActivity(
            @PathVariable("roleId") String roleId,
            @RequestBody Map<String, Object> request);

    // === Advertisement Reward ===

    @PostMapping("/api/activity/{roleId}/ad-reward")
    Map<String, Object> claimAdReward(
            @PathVariable("roleId") String roleId,
            @RequestParam("adSeq") Integer adSeq,
            @RequestParam(value = "isDiamond", defaultValue = "false") Boolean isDiamond);

    // === Recharge Config (CS:3004 → SC:3005) ===

    @GetMapping("/api/activity/recharge-config")
    Map<String, Object> getRechargeConfig(
            @RequestParam(value = "currency", defaultValue = "0") int currency,
            @RequestParam(value = "spid", required = false) String spid);
}

package com.SouthMillion.activity_service.controller;

import com.SouthMillion.activity_service.entity.*;
import com.SouthMillion.activity_service.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/activity") @RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // === List active activities ===
    @GetMapping
    public java.util.Map<String, Object> listActivities() {
        return activityService.listActiveActivities();
    }

    // === SevenDaySign ===
    @GetMapping("/{roleId}/sevenday")
    public SevenDaySign getSevenDay(@PathVariable Long roleId) {
        return activityService.getSevenDay(roleId);
    }
    @PostMapping("/{roleId}/sevenday/claim")
    public SevenDaySign claimSevenDay(@PathVariable Long roleId, @RequestParam(defaultValue = "1") int day) {
        return activityService.claimSevenDay(roleId, day);
    }

    // === LuckUnpacking ===
    @GetMapping("/{roleId}/luck")
    public LuckUnpacking getLuck(@PathVariable Long roleId) {
        return activityService.getLuck(roleId);
    }
    @PostMapping("/{roleId}/luck/claim")
    public LuckUnpacking claimLuck(@PathVariable Long roleId, @RequestParam(defaultValue = "0") int seq) {
        return activityService.claimLuck(roleId, seq);
    }

    // === NewAreaPreferential ===
    @GetMapping("/{roleId}/newarea")
    public NewAreaPreferential getNewArea(@PathVariable Long roleId) {
        return activityService.getNewArea(roleId);
    }
    @PostMapping("/{roleId}/newarea/buy")
    public NewAreaPreferential buyNewArea(@PathVariable Long roleId, @RequestParam(defaultValue = "0") int itemIndex) {
        return activityService.buyNewArea(roleId, itemIndex);
    }

    // === MarketShop ===
    @GetMapping("/{roleId}/market")
    public MarketShop getMarket(@PathVariable Long roleId) {
        return activityService.getMarket(roleId);
    }
    @PostMapping("/{roleId}/market/buy")
    public MarketShop buyMarket(@PathVariable Long roleId, @RequestParam(defaultValue = "0") int goodsSeq) {
        return activityService.buyMarket(roleId, goodsSeq);
    }
    @PostMapping("/{roleId}/market/refresh")
    public MarketShop refreshMarket(@PathVariable Long roleId) {
        return activityService.refreshMarket(roleId);
    }

    // === DuoBao (夺宝) ===
    @PostMapping("/{roleId}/duobao")
    public java.util.Map<String, Object> duobaoOperation(
            @PathVariable Long roleId,
            @RequestBody java.util.Map<String, Object> request) {
        int opType = (Integer) request.getOrDefault("opType", 1);
        int param1 = (Integer) request.getOrDefault("param1", 0);
        int param2 = (Integer) request.getOrDefault("param2", 0);
        return activityService.handleDuoBao(roleId, opType, param1, param2);
    }

    // === RandActivity generic dispatch (3000) ===
    @PostMapping("/{roleId}/rand")
    public java.util.Map<String, Object> randActivity(
            @PathVariable Long roleId,
            @RequestBody java.util.Map<String, Object> request) {
        int activityType = ((Number) request.getOrDefault("activityType", 0)).intValue();
        int operaType    = ((Number) request.getOrDefault("operaType",    0)).intValue();
        int param1       = ((Number) request.getOrDefault("param1",       0)).intValue();
        int param2       = ((Number) request.getOrDefault("param2",       0)).intValue();
        int param3       = ((Number) request.getOrDefault("param3",       0)).intValue();
        return activityService.handleRandActivity(roleId, activityType, operaType, param1, param2, param3);
    }

    @PostMapping("/internal/friend-invite/share-play")
    public java.util.Map<String, Object> recordFriendInviteShare(
            @RequestBody java.util.Map<String, Object> request) {
        Long roleId = request.get("roleId") instanceof Number n ? n.longValue() : null;
        Long userId = request.get("userId") instanceof Number n ? n.longValue() : null;
        Long shareRoleId = request.get("shareRoleId") instanceof Number n ? n.longValue() : null;
        Long shareUserId = request.get("shareUserId") instanceof Number n ? n.longValue() : null;
        Integer shareServerId = request.get("shareServerId") instanceof Number n ? n.intValue() : null;
        return activityService.recordFriendInviteShare(roleId, userId, shareRoleId, shareUserId, shareServerId);
    }

    // === Advertisement Reward ===
    @PostMapping("/{roleId}/ad-reward")
    public java.util.Map<String, Object> claimAdReward(
            @PathVariable Long roleId,
            @RequestParam Integer adSeq,
            @RequestParam(defaultValue = "false") Boolean isDiamond) {
        return activityService.claimAdReward(roleId, adSeq, isDiamond);
    }

    // === Recharge Config (CS:3004 → SC:3005) ===
    @GetMapping("/recharge-config")
    public java.util.Map<String, Object> getRechargeConfig(
            @RequestParam(defaultValue = "0") int currency,
            @RequestParam(required = false) String spid) {
        return activityService.getRechargeConfig(currency, spid);
    }
}

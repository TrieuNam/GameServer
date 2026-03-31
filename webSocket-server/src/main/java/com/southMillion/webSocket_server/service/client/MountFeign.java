package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mount Service Feign Client
 * 
 * Backend Service: mount-service
 * Manages mount system operations:
 * - Mount level/grade upgrades, explore/adventure
 * - Appearance/skin management (pifu system)
 * - Harness equipment (wear, decompose, unlock)
 * - Shop system (buy, refresh)
 */
@FeignClient(name = "mount-service")
public interface MountFeign {

    /**
     * Get mount data for role
     * @param roleId Role ID
     * @return Mount data: mounts[], harness[], active_mount_id, shop_entries[]
     */
    @GetMapping("/api/mount/{roleId}")
    Map<String, Object> getMountData(@PathVariable("roleId") String roleId);

    /**
     * OP0: Level up mount (consume exp items)
     * @param roleId Role ID
     * @param request {mountId, materials[]}
     * @return {success, newLevel, newExp, consumedItems[]}
     */
    @PostMapping("/api/mount/{roleId}/levelup")
    Map<String, Object> levelUp(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP1: Grade up mount
     * @param roleId Role ID
     * @param mountId Mount ID
     * @return {success, newGrade, newStats{}}
     */
    @PostMapping("/api/mount/{roleId}/gradeup/{mountId}")
    Map<String, Object> gradeUp(@PathVariable("roleId") String roleId, @PathVariable("mountId") Integer mountId);

    /**
     * OP2: Explore/Adventure with mount
     * @param roleId Role ID
     * @param request {mountId, exploreType}
     * @return {success, rewards[], exploreEndTime}
     */
    @PostMapping("/api/mount/{roleId}/explore")
    Map<String, Object> explore(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP3: Set mount appearance (activate skin)
     * @param roleId Role ID
     * @param request {mountId, appearanceId}
     * @return {success, activeAppearance}
     */
    @PostMapping("/api/mount/{roleId}/appearance")
    Map<String, Object> setAppearance(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP4: Upgrade mount skin (pifu upgrade)
     * @param roleId Role ID
     * @param request {mountId, pifuId}
     * @return {success, newPifuLevel, newStats{}}
     */
    @PostMapping("/api/mount/{roleId}/pifu/upgrade")
    Map<String, Object> upgradePifu(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP5: Set active skin (switch pifu)
     * @param roleId Role ID
     * @param request {mountId, pifuId}
     * @return {success, activePifuId}
     */
    @PostMapping("/api/mount/{roleId}/pifu/set")
    Map<String, Object> setPifu(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP6: Wear harness equipment
     * @param roleId Role ID
     * @param request {mountId, harnessId, slot}
     * @return {success, equippedHarness{}}
     */
    @PostMapping("/api/mount/{roleId}/harness/wear")
    Map<String, Object> wearHarness(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP7: Decompose harness
     * @param roleId Role ID
     * @param request {harnessIds[]}
     * @return {success, rewards[]}
     */
    @PostMapping("/api/mount/{roleId}/harness/decompose")
    Map<String, Object> decomposeHarness(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP8: Unlock harness slot
     * @param roleId Role ID
     * @param request {mountId, slotIndex}
     * @return {success, unlockedSlot}
     */
    @PostMapping("/api/mount/{roleId}/harness/unlock")
    Map<String, Object> unlockHarnessSlot(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP9: Refresh shop entries (free/paid)
     * @param roleId Role ID
     * @param request {refreshType}
     * @return {success, newEntries[], refreshCost}
     */
    @PostMapping("/api/mount/{roleId}/shop/refresh")
    Map<String, Object> refreshShop(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP10: Buy item from shop
     * @param roleId Role ID
     * @param request {entryId, quantity}
     * @return {success, boughtItem{}, cost}
     */
    @PostMapping("/api/mount/{roleId}/shop/buy")
    Map<String, Object> buyFromShop(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP11: Refresh and buy (combo operation)
     * @param roleId Role ID
     * @param request {entryId}
     * @return {success, boughtItem{}, totalCost}
     */
    @PostMapping("/api/mount/{roleId}/shop/refreshbuy")
    Map<String, Object> refreshAndBuy(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * OP12: Open shop (unlock shop access)
     * @param roleId Role ID
     * @return {success, shopUnlocked, entries[]}
     */
    @PostMapping("/api/mount/{roleId}/shop/open")
    Map<String, Object> openShop(@PathVariable("roleId") String roleId);
}

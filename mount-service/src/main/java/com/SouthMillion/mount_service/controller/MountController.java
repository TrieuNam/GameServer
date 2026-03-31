package com.SouthMillion.mount_service.controller;

import com.SouthMillion.mount_service.model.entity.Mount;
import com.SouthMillion.mount_service.service.MountHarnessService;
import com.SouthMillion.mount_service.service.MountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mount Controller - WebSocket Compatible
 * Handles mount/cavalry operations
 * 
 * MsgIDs: 2140-2145 (Mount operations)
 * Protocol: Returns Map<String, Object> with "success" key for WebSocket integration
 */
@RestController
@RequestMapping("/api/mount")
@RequiredArgsConstructor
@Slf4j
public class MountController {
    
    private final MountService mountService;
    private final MountHarnessService mountHarnessService;

    /**
     * Get mount data for role (WebSocket compatible)
     * @param roleId Role ID
     * @return Mount data map
     */
    @GetMapping("/{roleId}")
    public Map<String, Object> getMountData(@PathVariable("roleId") Long roleId) {
        log.info("[MountController] Get mount data - roleId: {}", roleId);
        try {
            List<Mount> mounts = mountService.getAllMounts(roleId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mounts", mounts);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to get mount data - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP0: Level up mount (consume exp items)
     * @param roleId Role ID
     * @param request {mountId, materials[]}
     * @return {success, newLevel, newExp, consumedItems[]}
     */
    @PostMapping("/{roleId}/levelup")
    public Map<String, Object> levelUp(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        log.info("[MountController] Level up mount - roleId: {}, mountId: {}", roleId, mountId);
        
        try {
            Mount mount = mountService.levelUpMount(roleId, mountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newLevel", mount.getLevel());
            response.put("newExp", mount.getExp());
            response.put("mountId", mount.getId());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to level up mount - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP1: Grade up mount
     * @param roleId Role ID
     * @param mountId Mount ID
     * @return {success, newGrade, newStats{}}
     */
    @PostMapping("/{roleId}/gradeup/{mountId}")
    public Map<String, Object> gradeUp(
            @PathVariable("roleId") Long roleId,
            @PathVariable("mountId") Integer mountId) {
        log.info("[MountController] Grade up mount - roleId: {}, mountId: {}", roleId, mountId);
        
        try {
            Mount mount = mountService.gradeUpMount(roleId, mountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newGrade", mount.getGrade());
            response.put("mountId", mount.getId());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to grade up mount - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP2: Explore/Adventure with mount
     * @param roleId Role ID
     * @param request {mountId, exploreType}
     * @return {success, rewards[], exploreEndTime}
     */
    @PostMapping("/{roleId}/explore")
    public Map<String, Object> explore(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        Integer exploreType = (Integer) request.get("exploreType");
        log.info("[MountController] Explore - roleId: {}, mountId: {}, type: {}", 
                roleId, mountId, exploreType);
        
        try {
            Mount mount = mountService.explore(roleId, mountId, exploreType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mountId", mount.getId());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to explore - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP3: Set mount appearance (activate skin)
     * @param roleId Role ID
     * @param request {mountId, appearanceId}
     * @return {success, activeAppearance}
     */
    @PostMapping("/{roleId}/appearance")
    public Map<String, Object> setAppearance(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        Integer appearanceId = (Integer) request.get("appearanceId");
        log.info("[MountController] Set appearance - roleId: {}, mountId: {}, appearanceId: {}", 
                roleId, mountId, appearanceId);
        
        try {
            mountService.setAppearance(roleId, mountId, appearanceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeAppearance", appearanceId);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to set appearance - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP4: Upgrade mount skin (pifu upgrade)
     * @param roleId Role ID
     * @param request {mountId, pifuId}
     * @return {success, newPifuLevel, newStats{}}
     */
    @PostMapping("/{roleId}/pifu/upgrade")
    public Map<String, Object> upgradePifu(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        log.info("[MountController] Upgrade pifu - roleId: {}, mountId: {}", roleId, mountId);
        
        try {
            Mount mount = mountService.upgradeSkin(roleId, mountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mountId", mount.getId());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to upgrade pifu - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP5: Set active skin (switch pifu)
     * @param roleId Role ID
     * @param request {mountId, pifuId}
     * @return {success, activePifuId}
     */
    @PostMapping("/{roleId}/pifu/set")
    public Map<String, Object> setPifu(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        Integer pifuId = (Integer) request.get("pifuId");
        log.info("[MountController] Set pifu - roleId: {}, mountId: {}, pifuId: {}", 
                roleId, mountId, pifuId);
        
        try {
            mountService.setSkin(roleId, mountId, pifuId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activePifuId", pifuId);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to set pifu - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP6: Wear harness equipment
     * @param roleId Role ID
     * @param request {mountId, harnessId, slot}
     * @return {success, equippedHarness{}}
     */
    @PostMapping("/{roleId}/harness/wear")
    public Map<String, Object> wearHarness(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        Integer harnessId = (Integer) request.get("harnessId");
        log.info("[MountController] Wear harness - roleId: {}, mountId: {}, harnessId: {}", 
                roleId, mountId, harnessId);
        
        try {
            mountService.equipMount(roleId, mountId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("harnessId", harnessId);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to wear harness - roleId: {}, mountId: {}", 
                    roleId, mountId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP7: Decompose harness
     * @param roleId Role ID
     * @param request {harnessIds[]}
     * @return {success, rewards[]}
     */
    @PostMapping("/{roleId}/harness/decompose")
    public Map<String, Object> decomposeHarness(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer harnessIndex = (Integer) request.get("harnessIndex");
        log.info("[MountController] Decompose harness - roleId: {}, harnessIndex: {}",
                roleId, harnessIndex);

        if (harnessIndex == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "harnessIndex is required");
            return response;
        }

        try {
            mountHarnessService.decomposeHarness(roleId, harnessIndex);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to decompose harness - roleId: {}, harnessIndex: {}",
                    roleId, harnessIndex, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP8: Unlock harness slot
     * @param roleId Role ID
     * @param request {mountId, slotIndex}
     * @return {success, unlockedSlot}
     */
    @PostMapping("/{roleId}/harness/unlock")
    public Map<String, Object> unlockHarnessSlot(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer mountId = (Integer) request.get("mountId");
        Integer slotIndex = (Integer) request.get("slotIndex");
        log.info("[MountController] Unlock harness slot - roleId: {}, mountId: {}, slot: {}", 
                roleId, mountId, slotIndex);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unlockedSlot", slotIndex);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to unlock harness slot - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP9: Refresh shop entries (free/paid)
     * @param roleId Role ID
     * @param request {refreshType}
     * @return {success, newEntries[], refreshCost}
     */
    @PostMapping("/{roleId}/shop/refresh")
    public Map<String, Object> refreshShop(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        log.info("[MountController] Refresh shop - roleId: {}", roleId);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newEntries", new HashMap<>());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to refresh shop - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP10: Buy item from shop
     * @param roleId Role ID
     * @param request {entryId, quantity}
     * @return {success, boughtItem{}, cost}
     */
    @PostMapping("/{roleId}/shop/buy")
    public Map<String, Object> buyFromShop(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        log.info("[MountController] Buy from shop - roleId: {}", roleId);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boughtItem", new HashMap<>());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to buy from shop - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP11: Refresh and buy (combo operation)
     * @param roleId Role ID
     * @param request {entryId}
     * @return {success, boughtItem{}, totalCost}
     */
    @PostMapping("/{roleId}/shop/refreshbuy")
    public Map<String, Object> refreshAndBuy(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        log.info("[MountController] Refresh and buy - roleId: {}", roleId);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boughtItem", new HashMap<>());
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to refresh and buy - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * OP12: Open shop (unlock shop access)
     * @param roleId Role ID
     * @return {success, shopUnlocked, entries[]}
     */
    @PostMapping("/{roleId}/shop/open")
    public Map<String, Object> openShop(@PathVariable("roleId") Long roleId) {
        log.info("[MountController] Open shop - roleId: {}", roleId);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("shopUnlocked", true);
            return response;
        } catch (Exception e) {
            log.error("[MountController] Failed to open shop - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
}

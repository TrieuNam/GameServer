package com.SouthMillion.angel_service.controller;

import com.SouthMillion.angel_service.model.entity.Angel;
import com.SouthMillion.angel_service.service.AngelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Angel Controller - WebSocket Compatible
 * Handles angel/wing companion operations
 * 
 * MsgIDs: 2130-2132 (Angel operations)
 * Protocol: Returns Map<String, Object> with "success" key for WebSocket integration
 */
@RestController
@RequestMapping("/api/angel")
@RequiredArgsConstructor
@Slf4j
public class AngelController {
    
    private final AngelService angelService;
    
    /**
     * Get angel data for role (WebSocket compatible)
     * @param roleId Role ID
     * @return Angel data map
     */
    @GetMapping("/{roleId}")
    public Map<String, Object> getAngelData(@PathVariable("roleId") Long roleId) {
        log.info("[AngelController] Get angel data - roleId: {}", roleId);
        try {
            List<Angel> angels = angelService.getAllAngels(roleId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("angels", angels);
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to get angel data - roleId: {}", roleId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Level up angel (consume exp items)
     * @param roleId Role ID
     * @param request {angelId, materialIds[]}
     * @return {success, newLevel, newExp, consumedItems[]}
     */
    @PostMapping("/{roleId}/levelup")
    public Map<String, Object> levelUp(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer angelId = (Integer) request.get("angelId");
        log.info("[AngelController] Level up angel - roleId: {}, angelId: {}", roleId, angelId);
        
        try {
            Angel angel = angelService.levelUpAngel(roleId, angelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newLevel", angel.getLevel());
            response.put("newExp", angel.getExp());
            response.put("angelId", angel.getId());
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to level up angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Grade up angel
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, newGrade, newStats{}}
     */
    @PostMapping("/{roleId}/gradeup/{angelId}")
    public Map<String, Object> gradeUp(
            @PathVariable("roleId") Long roleId,
            @PathVariable("angelId") Integer angelId) {
        log.info("[AngelController] Grade up angel - roleId: {}, angelId: {}", roleId, angelId);
        
        try {
            Angel angel = angelService.gradeUpAngel(roleId, angelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newGrade", angel.getGrade());
            response.put("angelId", angel.getId());
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to grade up angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Upgrade angel skill
     * @param roleId Role ID
     * @param request {angelId, skillId}
     * @return {success, newSkillLevel, skillEffect{}}
     */
    @PostMapping("/{roleId}/skill/upgrade")
    public Map<String, Object> upgradeSkill(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer angelId = (Integer) request.get("angelId");
        Integer skillId = (Integer) request.get("skillId");
        log.info("[AngelController] Upgrade skill - roleId: {}, angelId: {}, skillId: {}", 
                roleId, angelId, skillId);
        
        try {
            Angel angel = angelService.upgradeSkill(roleId, angelId, skillId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("angelId", angel.getId());
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to upgrade skill - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Activate new angel
     * @param roleId Role ID
     * @param angelId Angel template ID
     * @return {success, angelData{}}
     */
    @PostMapping("/{roleId}/activate/{angelId}")
    public Map<String, Object> activate(
            @PathVariable("roleId") Long roleId,
            @PathVariable("angelId") Integer angelId) {
        log.info("[AngelController] Activate angel - roleId: {}, angelId: {}", roleId, angelId);
        
        try {
            Angel angel = angelService.unlockAngel(roleId, angelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("angelData", angel);
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to activate angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Switch active angel
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, activeAngelId}
     */
    @PostMapping("/{roleId}/switch/{angelId}")
    public Map<String, Object> switchAngel(
            @PathVariable("roleId") Long roleId,
            @PathVariable("angelId") Integer angelId) {
        log.info("[AngelController] Switch angel - roleId: {}, angelId: {}", roleId, angelId);
        
        try {
            angelService.equipAngel(roleId, angelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeAngelId", angelId);
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to switch angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Rename angel
     * @param roleId Role ID
     * @param request {angelId, newName}
     * @return {success, newName, cost}
     */
    @PostMapping("/{roleId}/rename")
    public Map<String, Object> rename(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer angelId = (Integer) request.get("angelId");
        String newName = (String) request.get("newName");
        log.info("[AngelController] Rename angel - roleId: {}, angelId: {}, newName: {}", 
                roleId, angelId, newName);
        
        try {
            Angel angel = angelService.renameAngel(roleId, angelId, newName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newName", angel.getName());
            response.put("cost", 10000);
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to rename angel - roleId: {}, angelId: {}",
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Bless angel (exp boost event)
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, expGained, blessingEndTime}
     */
    @PostMapping("/{roleId}/blessing/{angelId}")
    public Map<String, Object> blessing(
            @PathVariable("roleId") Long roleId,
            @PathVariable("angelId") Integer angelId) {
        log.info("[AngelController] Blessing - roleId: {}, angelId: {}", roleId, angelId);
        
        try {
            Angel angel = angelService.addBlessing(roleId, angelId, 0L);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("angelId", angel.getId());
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to bless angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Transform angel appearance (skin/fashion)
     * @param roleId Role ID
     * @param request {angelId, transformId}
     * @return {success, transformId, transformEffects{}}
     */
    @PostMapping("/{roleId}/transform")
    public Map<String, Object> transform(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer angelId = (Integer) request.get("angelId");
        Integer transformId = (Integer) request.get("transformId");
        log.info("[AngelController] Transform - roleId: {}, angelId: {}, transformId: {}", 
                roleId, angelId, transformId);
        
        try {
            angelService.setAppearance(roleId, angelId, transformId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transformId", transformId);
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to transform angel - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
    
    /**
     * Upgrade angel appearance level
     * @param roleId Role ID
     * @param request {angelId, targetLevel}
     * @return {success, newLevel, bonusStats}
     */
    @PostMapping("/{roleId}/appearance-upgrade")
    public Map<String, Object> upgradeAppearance(
            @PathVariable("roleId") Long roleId,
            @RequestBody Map<String, Object> request) {
        Integer angelId = (Integer) request.get("angelId");
        Integer targetLevel = (Integer) request.getOrDefault("targetLevel", 1);
        
        log.info("[AngelController] AppearanceUpgrade - roleId: {}, angelId: {}, targetLevel: {}", 
                roleId, angelId, targetLevel);
        
        try {
            Angel angel = angelService.upgradeAppearance(roleId, angelId, targetLevel);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("angelId", angelId);
            response.put("newLevel", angel.getAppearanceLevel());
            return response;
        } catch (Exception e) {
            log.error("[AngelController] Failed to upgrade appearance - roleId: {}, angelId: {}", 
                    roleId, angelId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
}

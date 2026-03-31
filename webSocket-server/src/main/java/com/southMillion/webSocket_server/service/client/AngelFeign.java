package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Angel Service Feign Client
 * 
 * Backend Service: angel-service
 * Manages angel system operations:
 * - Level up, grade up, skill upgrades
 * - Activation, switching, renaming
 * - Blessing (exp boost), transformations
 */
@FeignClient(name = "angel-service")
public interface AngelFeign {

    /**
     * Get angel data for role
     * @param roleId Role ID
     * @return Angel data: angels[], active_angel_id, blessing_time
     */
    @GetMapping("/api/angel/{roleId}")
    Map<String, Object> getAngelData(@PathVariable("roleId") String roleId);

    /**
     * Level up angel (consume exp items)
     * @param roleId Role ID
     * @param request {angelId, materialIds[]}
     * @return {success, newLevel, newExp, consumedItems[]}
     */
    @PostMapping("/api/angel/{roleId}/levelup")
    Map<String, Object> levelUp(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * Grade up angel
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, newGrade, newStats{}}
     */
    @PostMapping("/api/angel/{roleId}/gradeup/{angelId}")
    Map<String, Object> gradeUp(@PathVariable("roleId") String roleId, @PathVariable("angelId") Integer angelId);

    /**
     * Upgrade angel skill
     * @param roleId Role ID
     * @param request {angelId, skillId}
     * @return {success, newSkillLevel, skillEffect{}}
     */
    @PostMapping("/api/angel/{roleId}/skill/upgrade")
    Map<String, Object> upgradeSkill(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * Activate new angel
     * @param roleId Role ID
     * @param angelId Angel template ID
     * @return {success, angelData{}}
     */
    @PostMapping("/api/angel/{roleId}/activate/{angelId}")
    Map<String, Object> activate(@PathVariable("roleId") String roleId, @PathVariable("angelId") Integer angelId);

    /**
     * Switch active angel
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, activeAngelId}
     */
    @PostMapping("/api/angel/{roleId}/switch/{angelId}")
    Map<String, Object> switchAngel(@PathVariable("roleId") String roleId, @PathVariable("angelId") Integer angelId);

    /**
     * Rename angel
     * @param roleId Role ID
     * @param request {angelId, newName}
     * @return {success, newName, cost}
     */
    @PostMapping("/api/angel/{roleId}/rename")
    Map<String, Object> rename(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);

    /**
     * Bless angel (exp boost event)
     * @param roleId Role ID
     * @param angelId Angel ID
     * @return {success, expGained, blessingEndTime}
     */
    @PostMapping("/api/angel/{roleId}/blessing/{angelId}")
    Map<String, Object> blessing(@PathVariable("roleId") String roleId, @PathVariable("angelId") Integer angelId);

    /**
     * Transform angel appearance (skin/fashion)
     * @param roleId Role ID
     * @param request {angelId, transformId}
     * @return {success, transformId, transformEffects{}}
     */
    @PostMapping("/api/angel/{roleId}/transform")
    Map<String, Object> transform(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);
    
    /**
     * Upgrade angel appearance level
     * @param roleId Role ID
     * @param request {angelId, targetLevel}
     * @return {success, newLevel}
     */
    @PostMapping("/api/angel/{roleId}/appearance-upgrade")
    Map<String, Object> upgradeAppearance(@PathVariable("roleId") String roleId, @RequestBody Map<String, Object> request);
}

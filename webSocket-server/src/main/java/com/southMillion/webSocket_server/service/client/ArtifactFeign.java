package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for artifact-service (ShenQi 神器 system).
 *
 * Base: /api/artifact — maps to ArtifactController endpoints.
 * Note: upgradeSkill has no controller endpoint (stub in ShenQiHandler).
 */
@FeignClient(name = "artifact-service", path = "/api/artifact")
public interface ArtifactFeign {

    /** GET /{userId} — list all artifacts for role */
    @GetMapping("/{roleId}")
    List<Map<String, Object>> getRoleArtifacts(@PathVariable("roleId") Long roleId);

    /** POST /{userId}/unlock?artifactId= — activate/unlock artifact by template id */
    @PostMapping("/{roleId}/unlock")
    Map<String, Object> activateArtifact(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactId") Integer artifactId);

    /** POST /{userId}/levelup?artifactIndex= — level up artifact */
    @PostMapping("/{roleId}/levelup")
    Map<String, Object> upgradeArtifact(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactIndex") Integer artifactIndex);

    /** POST /{userId}/equip?artifactIndex= — equip/set artifact as active */
    @PostMapping("/{roleId}/equip")
    void setActiveArtifact(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactIndex") Integer artifactIndex);

    /** POST /{userId}/gradeup?artifactIndex= — grade up / evolve artifact */
    @PostMapping("/{roleId}/gradeup")
    Map<String, Object> evolveArtifact(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactIndex") Integer artifactIndex);

    /** POST /{userId}/refine?artifactIndex= — refine (reroll) artifact attributes */
    @PostMapping("/{roleId}/refine")
    Map<String, Object> refineArtifact(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactIndex") Integer artifactIndex);
    
    /** POST /{userId}/upgrade-skill — upgrade artifact skill level */
    @PostMapping("/{roleId}/upgrade-skill")
    Map<String, Object> upgradeSkill(
            @PathVariable("roleId") Long roleId,
            @RequestParam("artifactIndex") Integer artifactIndex,
            @RequestParam("skillIndex") Integer skillIndex);
    
    /** POST /{userId}/draw — draw artifacts from gacha (1=single, 10=10-pull) */
    @PostMapping("/{roleId}/draw")
    List<Map<String, Object>> drawArtifacts(
            @PathVariable("roleId") Long roleId,
            @RequestParam("drawType") Integer drawType);
    
    /** GET /{userId}/draw-records — get recent draw history */
    @GetMapping("/{roleId}/draw-records")
    List<Map<String, Object>> getDrawRecords(@PathVariable("roleId") Long roleId);
}

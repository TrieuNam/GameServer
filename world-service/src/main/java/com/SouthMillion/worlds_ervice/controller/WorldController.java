package com.SouthMillion.worlds_ervice.controller;

import com.SouthMillion.worlds_ervice.dto.WorldBossDTO;
import com.SouthMillion.worlds_ervice.dto.WorldEventDTO;
import com.SouthMillion.worlds_ervice.dto.WorldStateDTO;
import com.SouthMillion.worlds_ervice.entity.WorldBoss;
import com.SouthMillion.worlds_ervice.entity.WorldEvent;
import com.SouthMillion.worlds_ervice.service.WorldService;
import com.SouthMillion.worlds_ervice.service.SceneManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/world")
@RequiredArgsConstructor
public class WorldController {

    private final WorldService worldService;
    private final SceneManagementService sceneManagementService;

    @GetMapping("/state")
    public ResponseEntity<WorldStateDTO> getWorldState() {
        WorldStateDTO state = worldService.getGlobalState();
        return ResponseEntity.ok(state);
    }

    @GetMapping("/time")
    public ResponseEntity<Map<String, Object>> getServerTime() {
        return ResponseEntity.ok(Map.of(
                "serverTime", Instant.now(),
                "timestamp", Instant.now().toEpochMilli()
        ));
    }

    @PutMapping("/state")
    public ResponseEntity<?> updateWorldState(@RequestBody Map<String, Object> stateData) {
        try {
            worldService.updateGlobalState(stateData);
            return ResponseEntity.ok(Map.of("message", "World state updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<List<WorldEventDTO>> getActiveEvents() {
        List<WorldEventDTO> events = worldService.getActiveEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/upcoming")
    public ResponseEntity<List<WorldEventDTO>> getUpcomingEvents() {
        List<WorldEventDTO> events = worldService.getUpcomingEvents();
        return ResponseEntity.ok(events);
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody WorldEvent event) {
        try {
            WorldEvent created = worldService.createEvent(event);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/events/{eventId}/activate")
    public ResponseEntity<?> activateEvent(@PathVariable Long eventId) {
        try {
            worldService.activateEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "Event activated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/events/{eventId}/deactivate")
    public ResponseEntity<?> deactivateEvent(@PathVariable Long eventId) {
        try {
            worldService.deactivateEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "Event deactivated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/bosses")
    public ResponseEntity<List<WorldBossDTO>> getActiveBosses() {
        List<WorldBossDTO> bosses = worldService.getActiveBosses();
        return ResponseEntity.ok(bosses);
    }

    @GetMapping("/bosses/recent")
    public ResponseEntity<List<WorldBossDTO>> getRecentBosses() {
        List<WorldBossDTO> bosses = worldService.getRecentBosses();
        return ResponseEntity.ok(bosses);
    }

    @PostMapping("/bosses/spawn")
    public ResponseEntity<?> spawnBoss(@RequestBody Map<String, Object> request) {
        try {
            String bossName = request.get("bossName").toString();
            Integer level = Integer.parseInt(request.get("level").toString());
            Long maxHp = Long.parseLong(request.get("maxHp").toString());
            String location = request.get("location").toString();

            WorldBoss boss = worldService.spawnBoss(bossName, level, maxHp, location);
            return ResponseEntity.ok(boss);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bosses/{bossId}/damage")
    public ResponseEntity<?> damageBoss(
            @PathVariable Long bossId,
            @RequestBody Map<String, Object> request) {
        try {
            Long damage = Long.parseLong(request.get("damage").toString());
            String attackerId = request.get("attackerId").toString();

            WorldBoss boss = worldService.updateBossHp(bossId, damage, attackerId);
            return ResponseEntity.ok(boss);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SCENE MANAGEMENT ENDPOINTS ====================

    /**
     * Enter scene
     * Called by WebSocket server when player enters a scene
     */
    @PostMapping("/enter")
    public String enterScene(
            @RequestParam("roleId") Long roleId,
            @RequestParam("sceneId") Integer sceneId,
            @RequestParam("enterType") Integer enterType,
            @RequestParam(value = "x", required = false) Float x,
            @RequestParam(value = "y", required = false) Float y,
            @RequestParam(value = "z", required = false) Float z) {
        
        log.info("[WorldController] Enter scene - roleId: {}, sceneId: {}, enterType: {}", 
                roleId, sceneId, enterType);

        try {
            Map<String, Object> result = sceneManagementService.enterScene(
                    roleId, sceneId, enterType, x, y, z);
            
            // Return simple string format for Feign client
            return "SUCCESS:sceneId=" + sceneId + ",roleId=" + roleId;
        } catch (Exception e) {
            log.error("[WorldController] Enter scene failed - roleId: {}, sceneId: {}", 
                    roleId, sceneId, e);
            return "ERROR:" + e.getMessage();
        }
    }

    /**
     * Leave scene
     * Called by WebSocket server when player leaves a scene
     */
    @PostMapping("/leave")
    public Boolean leaveScene(
            @RequestParam("roleId") Long roleId,
            @RequestParam("sceneId") Integer sceneId,
            @RequestParam("leaveType") Integer leaveType) {
        
        log.info("[WorldController] Leave scene - roleId: {}, sceneId: {}, leaveType: {}", 
                roleId, sceneId, leaveType);

        try {
            Map<String, Object> result = sceneManagementService.leaveScene(
                    roleId, sceneId, leaveType);
            return (Boolean) result.get("success");
        } catch (Exception e) {
            log.error("[WorldController] Leave scene failed - roleId: {}, sceneId: {}", 
                    roleId, sceneId, e);
            return false;
        }
    }

    /**
     * Update player position
     * Called by WebSocket server when player moves
     */
    @PostMapping("/move")
    public Boolean updatePosition(
            @RequestParam("roleId") Long roleId,
            @RequestParam("x") Float x,
            @RequestParam("y") Float y,
            @RequestParam("z") Float z,
            @RequestParam(value = "dirX", required = false) Float dirX,
            @RequestParam(value = "dirY", required = false) Float dirY,
            @RequestParam(value = "dirZ", required = false) Float dirZ) {
        
        log.debug("[WorldController] Update position - roleId: {}, pos: ({},{},{})", 
                roleId, x, y, z);

        try {
            Map<String, Object> result = sceneManagementService.updatePosition(
                    roleId, x, y, z, dirX, dirY, dirZ);
            return (Boolean) result.get("success");
        } catch (Exception e) {
            log.error("[WorldController] Update position failed - roleId: {}", roleId, e);
            return false;
        }
    }

    /**
     * Pickup item from scene
     * Called by WebSocket server when player picks up an item
     */
    @PostMapping("/pickup")
    public String pickupItem(
            @RequestParam("roleId") Long roleId,
            @RequestParam("itemUid") Long itemUid) {
        
        log.info("[WorldController] Pickup item - roleId: {}, itemUid: {}", roleId, itemUid);

        try {
            Map<String, Object> result = sceneManagementService.pickupItem(roleId, itemUid);
            
            if ((Boolean) result.get("success")) {
                // Return format: "itemId:count"
                return result.get("itemId") + ":" + result.get("count");
            } else {
                return "ERROR:" + result.get("error");
            }
        } catch (Exception e) {
            log.error("[WorldController] Pickup item failed - roleId: {}, itemUid: {}", 
                    roleId, itemUid, e);
            return "ERROR:" + e.getMessage();
        }
    }

    /**
     * Interact with NPC
     * Called by WebSocket server when player interacts with NPC
     */
    @PostMapping("/interact")
    public Boolean interactNpc(
            @RequestParam("roleId") Long roleId,
            @RequestParam("npcId") Integer npcId,
            @RequestParam("interactType") Integer interactType) {
        
        log.info("[WorldController] Interact NPC - roleId: {}, npcId: {}, type: {}", 
                roleId, npcId, interactType);

        try {
            Map<String, Object> result = sceneManagementService.interactNpc(
                    roleId, npcId, interactType);
            return (Boolean) result.get("success");
        } catch (Exception e) {
            log.error("[WorldController] Interact NPC failed - roleId: {}, npcId: {}", 
                    roleId, npcId, e);
            return false;
        }
    }

    /**
     * Get scene information
     * Returns JSON string with scene details
     */
    @GetMapping("/scene/{sceneId}")
    public String getSceneInfo(@PathVariable("sceneId") Integer sceneId) {
        log.debug("[WorldController] Get scene info - sceneId: {}", sceneId);

        try {
            Map<String, Object> info = sceneManagementService.getSceneInfo(sceneId);
            // Simple JSON format
            return String.format("{\"sceneId\":%d,\"playerCount\":%d,\"sceneName\":\"%s\"}", 
                    sceneId, 
                    info.get("playerCount"), 
                    info.get("sceneName"));
        } catch (Exception e) {
            log.error("[WorldController] Get scene info failed - sceneId: {}", sceneId, e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}

package com.SouthMillion.gameworld_service.controller;

import com.SouthMillion.gameworld_service.dto.PlayerPosition;
import com.SouthMillion.gameworld_service.service.GameWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * GameWorld REST Controller
 * Provides scene management and player position tracking endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/world")
@RequiredArgsConstructor
public class GameWorldController {

    private final GameWorldService gameWorldService;

    /**
     * Player enters a scene.
     * POST /api/world/enter
     */
    @PostMapping("/enter")
    public ResponseEntity<Map<String, Object>> enterScene(
            @RequestParam Long roleId,
            @RequestParam Integer sceneId,
            @RequestParam(defaultValue = "1") Integer enterType,
            @RequestParam(required = false) Float x,
            @RequestParam(required = false) Float y,
            @RequestParam(required = false) Float z) {

        log.info("[GameWorldController] Player {} entering scene {}", roleId, sceneId);

        PlayerPosition position = PlayerPosition.builder()
                .playerId(roleId)
                .mapId(sceneId)
                .x(x != null ? x : 0f)
                .y(y != null ? y : 0f)
                .z(z != null ? z : 0f)
                .state("IDLE")
                .build();

        PlayerPosition updated = gameWorldService.updatePosition(position);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "roleId", roleId,
                "sceneId", sceneId,
                "position", updated
        ));
    }

    /**
     * Player leaves a scene.
     * POST /api/world/leave
     */
    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveScene(
            @RequestParam Long roleId,
            @RequestParam Integer sceneId,
            @RequestParam(defaultValue = "0") Integer leaveType) {

        log.info("[GameWorldController] Player {} leaving scene {}", roleId, sceneId);
        boolean removed = gameWorldService.removePlayer(roleId);

        return ResponseEntity.ok(Map.of(
                "success", removed,
                "roleId", roleId,
                "sceneId", sceneId
        ));
    }

    /**
     * Update player position.
     * POST /api/world/move
     */
    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> updatePosition(
            @RequestParam Long roleId,
            @RequestParam Float x,
            @RequestParam Float y,
            @RequestParam(required = false, defaultValue = "0") Float z,
            @RequestParam(required = false) Integer mapId) {

        PlayerPosition position = PlayerPosition.builder()
                .playerId(roleId)
                .mapId(mapId != null ? mapId : 1)
                .x(x)
                .y(y)
                .z(z)
                .state("MOVING")
                .build();

        if (!gameWorldService.validatePosition(position)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid position coordinates"
            ));
        }

        PlayerPosition updated = gameWorldService.updatePosition(position);
        return ResponseEntity.ok(Map.of("success", true, "position", updated));
    }

    /**
     * Player picks up an item from the scene.
     * POST /api/world/pickup
     */
    @PostMapping("/pickup")
    public ResponseEntity<Map<String, Object>> pickupItem(
            @RequestParam Long roleId,
            @RequestParam Long itemUid) {

        log.info("[GameWorldController] Player {} picking up item {}", roleId, itemUid);

        // Retrieve player position to confirm they're in the scene
        Optional<PlayerPosition> posOpt = gameWorldService.getPosition(roleId);
        if (posOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Player not in world"
            ));
        }

        // Return success - actual item grant handled by bag-service via event
        return ResponseEntity.ok(Map.of(
                "success", true,
                "roleId", roleId,
                "itemUid", itemUid
        ));
    }

    /**
     * Player interacts with an NPC.
     * POST /api/world/interact
     */
    @PostMapping("/interact")
    public ResponseEntity<Map<String, Object>> interactNpc(
            @RequestParam Long roleId,
            @RequestParam Integer npcId,
            @RequestParam(defaultValue = "0") Integer interactType) {

        log.info("[GameWorldController] Player {} interacting with NPC {} type={}", roleId, npcId, interactType);

        Optional<PlayerPosition> posOpt = gameWorldService.getPosition(roleId);
        if (posOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Player not in world"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "roleId", roleId,
                "npcId", npcId,
                "interactType", interactType
        ));
    }

    /**
     * Get scene information.
     * GET /api/world/scene/{sceneId}
     */
    @GetMapping("/scene/{sceneId}")
    public ResponseEntity<Map<String, Object>> getSceneInfo(@PathVariable Integer sceneId) {
        log.debug("[GameWorldController] Getting scene info for sceneId={}", sceneId);

        var players = gameWorldService.getPlayersOnMap(sceneId);

        return ResponseEntity.ok(Map.of(
                "sceneId", sceneId,
                "playerCount", players.size(),
                "players", players
        ));
    }
}


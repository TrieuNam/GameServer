package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * World Service Feign Client
 * 
 * Service: world-service
 * Port: 8390
 * Base Path: /world
 * 
 * Endpoints:
 * - POST /world/enter - Enter scene
 * - POST /world/leave - Leave scene
 * - POST /world/move - Update position
 * - POST /world/pickup - Pickup item
 * - POST /world/interact - Interact with NPC
 * - GET /world/scene/{sceneId} - Get scene info
 */
@FeignClient(name = "world-service", path = "/api/world")
public interface WorldFeign {

    /**
     * Enter scene
     * 
     * @param roleId Role ID
     * @param sceneId Scene ID
     * @param enterType Enter type (1-teleport, 2-dungeon, 3-jump)
     * @param x Target X position
     * @param y Target Y position
     * @param z Target Z position
     * @return Result message
     */
    @PostMapping("/enter")
    String enterScene(
        @RequestParam("roleId") Long roleId,
        @RequestParam("sceneId") Integer sceneId,
        @RequestParam("enterType") Integer enterType,
        @RequestParam("x") Float x,
        @RequestParam("y") Float y,
        @RequestParam("z") Float z
    );

    /**
     * Leave scene
     * 
     * @param roleId Role ID
     * @param sceneId Scene ID
     * @param leaveType Leave type (1-normal, 2-force)
     * @return Success status
     */
    @PostMapping("/leave")
    Boolean leaveScene(
        @RequestParam("roleId") Long roleId,
        @RequestParam("sceneId") Integer sceneId,
        @RequestParam("leaveType") Integer leaveType
    );

    /**
     * Update player position
     * 
     * @param roleId Role ID
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param dirX Direction X
     * @param dirY Direction Y
     * @param dirZ Direction Z
     * @return Success status
     */
    @PostMapping("/move")
    Boolean updatePosition(
        @RequestParam("roleId") Long roleId,
        @RequestParam("x") Float x,
        @RequestParam("y") Float y,
        @RequestParam("z") Float z,
        @RequestParam("dirX") Float dirX,
        @RequestParam("dirY") Float dirY,
        @RequestParam("dirZ") Float dirZ
    );

    /**
     * Pickup item from scene
     * 
     * @param roleId Role ID
     * @param itemUid Item unique ID
     * @return Result format: "itemId:count" or error message
     */
    @PostMapping("/pickup")
    String pickupItem(
        @RequestParam("roleId") Long roleId,
        @RequestParam("itemUid") Long itemUid
    );

    /**
     * Interact with NPC
     * 
     * @param roleId Role ID
     * @param npcId NPC ID
     * @param interactType Interact type (1-dialog, 2-shop, 3-task)
     * @return Success status
     */
    @PostMapping("/interact")
    Boolean interactNpc(
        @RequestParam("roleId") Long roleId,
        @RequestParam("npcId") Integer npcId,
        @RequestParam("interactType") Integer interactType
    );

    /**
     * Get scene information
     * 
     * @param sceneId Scene ID
     * @return Scene data (JSON)
     */
    @GetMapping("/scene/{sceneId}")
    String getSceneInfo(@PathVariable("sceneId") Integer sceneId);
}

package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for friend-service
 * Handles friend operations: add, remove, block
 *
 * ⚠ Các endpoint sau đã bị xoá khỏi FriendController mới, cần bổ sung lại:
 *   GET  /api/friend/search       – tìm kiếm người chơi (FriendHandler.handleSearchPlayers)
 *   GET  /api/friend/online/{roleId} – trạng thái online (FriendHandler.handleGetOnlineStatus)
 *
 * Endpoints mới trong FriendController (chưa được dùng ở Feign):
 *   GET    /api/friend/blocked/{roleId}  – danh sách bị chặn
 *   PUT    /api/friend/status            – cập nhật trạng thái
 *   POST   /api/friend/gift              – gửi quà
 */
@FeignClient(name = "friend-service", path = "/api/friend")
public interface FriendFeign {
    
    /**
     * Get friend list
     */
    @GetMapping("/list/{roleId}")
    Map<String, Object> getFriendList(@PathVariable("roleId") String roleId);
    
    /**
     * Send friend request
     */
    @PostMapping("/request/send")
    Map<String, Object> sendFriendRequest(@RequestBody Map<String, Object> request);
    
    /**
     * Get received requests
     */
    @GetMapping("/request/received/{roleId}")
    Map<String, Object> getReceivedRequests(@PathVariable("roleId") String roleId);
    
    /**
     * Handle friend request (accept/reject)
     */
    @PostMapping("/request/handle")
    Map<String, Object> handleFriendRequest(@RequestBody Map<String, Object> request);
    
    /**
     * Remove friend
     */
    @DeleteMapping("/remove")
    Map<String, Object> removeFriend(@RequestParam("roleId") String roleId,
                                      @RequestParam("friendId") String friendId);
    
    /**
     * Block player
     */
    @PostMapping("/block")
    Map<String, Object> blockPlayer(@RequestBody Map<String, Object> request);
    
    /**
     * Unblock player
     */
    @DeleteMapping("/unblock")
    Map<String, Object> unblockPlayer(@RequestParam("blockerId") String blockerId,
                                       @RequestParam("blockedId") String blockedId);

    /**
     * Search players by name keyword
     */
    @GetMapping("/search")
    Map<String, Object> searchPlayers(@RequestParam("roleId") String roleId,
                                       @RequestParam("keyword") String keyword);

    /**
     * Get online status of friends
     */
    @GetMapping("/online/{roleId}")
    Map<String, Object> getOnlineStatus(@PathVariable("roleId") String roleId);
}

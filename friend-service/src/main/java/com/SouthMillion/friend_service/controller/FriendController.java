package com.SouthMillion.friend_service.controller;

import com.SouthMillion.friend_service.dto.FriendDTO;
import com.SouthMillion.friend_service.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Friend REST Controller
 * 
 * Provides RESTful API for friend management
 */
@Slf4j
@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
@Validated
public class FriendController {

    private final FriendService friendService;

    /**
     * Get friend list
     * GET /api/friend/list/{roleId}
     */
    @GetMapping("/list/{roleId}")
    public ResponseEntity<FriendDTO.Response<List<FriendDTO.FriendInfo>>> getFriendList(
            @PathVariable @NotBlank String roleId) {
        log.info("REST API: Get friend list - roleId={}", roleId);
        FriendDTO.Response<List<FriendDTO.FriendInfo>> response = friendService.getFriendList(roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Send friend request
     * POST /api/friend/request/send
     */
    @PostMapping("/request/send")
    public ResponseEntity<FriendDTO.Response<Void>> sendFriendRequest(
            @Valid @RequestBody FriendDTO.AddFriendRequest request) {
        log.info("REST API: Send friend request - from={}, to={}", request.getSenderId(), request.getReceiverId());
        FriendDTO.Response<Void> response = friendService.sendFriendRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get received requests
     * GET /api/friend/request/received/{roleId}
     */
    @GetMapping("/request/received/{roleId}")
    public ResponseEntity<FriendDTO.Response<List<FriendDTO.RequestInfo>>> getReceivedRequests(
            @PathVariable @NotBlank String roleId) {
        log.info("REST API: Get received requests - roleId={}", roleId);
        FriendDTO.Response<List<FriendDTO.RequestInfo>> response = friendService.getReceivedRequests(roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle friend request
     * POST /api/friend/request/handle
     */
    @PostMapping("/request/handle")
    public ResponseEntity<FriendDTO.Response<Void>> handleFriendRequest(
            @Valid @RequestBody FriendDTO.HandleRequest request) {
        log.info("REST API: Handle friend request - requestId={}, approve={}", 
                 request.getRequestId(), request.getApprove());
        FriendDTO.Response<Void> response = friendService.handleFriendRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove friend
     * DELETE /api/friend/remove
     */
    @DeleteMapping("/remove")
    public ResponseEntity<FriendDTO.Response<Void>> removeFriend(
            @RequestParam @NotBlank String roleId,
            @RequestParam @NotBlank String friendId) {
        log.info("REST API: Remove friend - roleId={}, friendId={}", roleId, friendId);
        FriendDTO.Response<Void> response = friendService.removeFriend(roleId, friendId);
        return ResponseEntity.ok(response);
    }

    /**
     * Block player
     * POST /api/friend/block
     */
    @PostMapping("/block")
    public ResponseEntity<FriendDTO.Response<Void>> blockPlayer(
            @Valid @RequestBody FriendDTO.BlockRequest request) {
        log.info("REST API: Block player - blockerId={}, blockedId={}", 
                 request.getBlockerId(), request.getBlockedId());
        FriendDTO.Response<Void> response = friendService.blockPlayer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Unblock player
     * DELETE /api/friend/unblock
     */
    @DeleteMapping("/unblock")
    public ResponseEntity<FriendDTO.Response<Void>> unblockPlayer(
            @RequestParam @NotBlank String blockerId,
            @RequestParam @NotBlank String blockedId) {
        log.info("REST API: Unblock player - blockerId={}, blockedId={}", blockerId, blockedId);
        FriendDTO.Response<Void> response = friendService.unblockPlayer(blockerId, blockedId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get blocked list
     * GET /api/friend/blocked/{roleId}
     */
    @GetMapping("/blocked/{roleId}")
    public ResponseEntity<FriendDTO.Response<List<FriendDTO.BlockedInfo>>> getBlockedList(
            @PathVariable @NotBlank String roleId) {
        log.info("REST API: Get blocked list - roleId={}", roleId);
        FriendDTO.Response<List<FriendDTO.BlockedInfo>> response = friendService.getBlockedList(roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update online status
     * PUT /api/friend/status
     */
    @PutMapping("/status")
    public ResponseEntity<FriendDTO.Response<Void>> updateOnlineStatus(
            @RequestParam @NotBlank String roleId,
            @RequestParam @NotBlank String roleName,
            @RequestParam Integer level,
            @RequestParam Boolean online) {
        log.info("REST API: Update online status - roleId={}, online={}", roleId, online);
        FriendDTO.Response<Void> response = friendService.updateOnlineStatus(roleId, roleName, level, online);
        return ResponseEntity.ok(response);
    }

    /**
     * Give gift
     * POST /api/friend/gift
     */
    @PostMapping("/gift")
    public ResponseEntity<FriendDTO.Response<Void>> giveGift(
            @Valid @RequestBody FriendDTO.GiveGiftRequest request) {
        log.info("REST API: Give gift - from={}, to={}", request.getGiverId(), request.getReceiverId());
        FriendDTO.Response<Void> response = friendService.giveGift(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     * GET /api/friend/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Friend Service is running!");
    }

    /**
     * Search players by name keyword
     * GET /api/friend/search?roleId=&keyword=
     */
    @GetMapping("/search")
    public ResponseEntity<FriendDTO.Response<List<FriendDTO.PlayerInfo>>> search(
            @RequestParam String roleId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(friendService.searchPlayers(roleId, keyword));
    }

    /**
     * Get online status of friends
     * GET /api/friend/online/{roleId}
     */
    @GetMapping("/online/{roleId}")
    public ResponseEntity<FriendDTO.Response<List<FriendDTO.OnlineStatusInfo>>> onlineFriends(
            @PathVariable String roleId) {
        return ResponseEntity.ok(friendService.getOnlineFriends(roleId));
    }
}

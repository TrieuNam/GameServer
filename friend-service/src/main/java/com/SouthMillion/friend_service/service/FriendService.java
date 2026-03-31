package com.SouthMillion.friend_service.service;

import com.SouthMillion.friend_service.client.BagClient;
import com.SouthMillion.friend_service.client.RoleClient;
import com.SouthMillion.friend_service.dto.FriendDTO;
import com.SouthMillion.friend_service.entity.*;
import com.SouthMillion.friend_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.GrantReq;
import org.SouthMillion.dto.bag.ItemInfo;
import org.SouthMillion.dto.bag.UseItemReq;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Friend Service
 * 
 * Core business logic for friend management system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository requestRepository;
    private final BlockedPlayerRepository blockedRepository;
    private final OnlineStatusRepository statusRepository;
    private final BagClient bagClient;
    private final RoleClient roleClient;

    private static final int MAX_FRIENDS = 100;
    private static final int MAX_BLOCKED = 50;

    /** Parse numeric String roleId to Long */
    private static Long toId(String s) { return Long.parseLong(s); }

    /**
     * Get friend list
     */
    @Transactional(readOnly = true)
    public FriendDTO.Response<List<FriendDTO.FriendInfo>> getFriendList(String roleId) {
        log.info("Getting friend list: roleId={}", roleId);

        List<Friend> friends = friendRepository.findAllFriends(toId(roleId));
        
        List<FriendDTO.FriendInfo> friendInfos = friends.stream()
                .map(f -> buildFriendInfo(f, roleId))
                .collect(Collectors.toList());

        log.info("Friend list retrieved: roleId={}, count={}", roleId, friendInfos.size());
        return FriendDTO.Response.success(friendInfos);
    }

    /**
     * Send friend request
     */
    @Transactional
    public FriendDTO.Response<Void> sendFriendRequest(FriendDTO.AddFriendRequest request) {
        log.info("Sending friend request: from={}, to={}", request.getSenderId(), request.getReceiverId());

        // Cannot add self
        if (request.getSenderId().equals(request.getReceiverId())) {
            return FriendDTO.Response.error(-1, "Cannot add yourself as friend");
        }

        Long senderIdL   = toId(request.getSenderId());
        Long receiverIdL = toId(request.getReceiverId());

        // Check if already friends
        if (friendRepository.areFriends(senderIdL, receiverIdL)) {
            return FriendDTO.Response.error(-2, "Already friends");
        }

        // Check if sender is blocked by receiver
        if (blockedRepository.existsByBlockerIdAndBlockedId(receiverIdL, senderIdL)) {
            return FriendDTO.Response.error(-3, "You are blocked by this player");
        }

        // Check if pending request exists
        if (requestRepository.existsBySenderIdAndReceiverIdAndStatus(senderIdL, receiverIdL, 0)) {
            return FriendDTO.Response.error(-4, "Friend request already sent");
        }

        // Check if reverse request exists (receiver already sent request to sender)
        Optional<FriendRequest> reverseRequest = requestRepository.findBySenderIdAndReceiverIdAndStatus(
                receiverIdL, senderIdL, 0);
        
        if (reverseRequest.isPresent()) {
            // Auto-accept: both players want to be friends
            return acceptFriendRequest(reverseRequest.get().getId(), request.getSenderId());
        }

        // Check friend limit for sender
        long friendCount = friendRepository.countFriends(senderIdL);
        if (friendCount >= MAX_FRIENDS) {
            return FriendDTO.Response.error(-5, String.format("Friend limit reached (%d/%d)", friendCount, MAX_FRIENDS));
        }

        // Create request
        FriendRequest friendRequest = FriendRequest.builder()
                .senderId(senderIdL)
                .senderName(request.getSenderName())
                .senderLevel(request.getSenderLevel())
                .receiverId(receiverIdL)
                .receiverName(request.getReceiverName())
                .message(request.getMessage())
                .status(0) // Pending
                .build();

        requestRepository.save(friendRequest);
        log.info("Friend request sent: from={}, to={}", request.getSenderId(), request.getReceiverId());

        return FriendDTO.Response.success(null);
    }

    /**
     * Get received friend requests
     */
    @Transactional(readOnly = true)
    public FriendDTO.Response<List<FriendDTO.RequestInfo>> getReceivedRequests(String roleId) {
        log.info("Getting received requests: roleId={}", roleId);

        List<FriendRequest> requests = requestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(toId(roleId), 0);
        
        List<FriendDTO.RequestInfo> requestInfos = requests.stream()
                .map(this::buildRequestInfo)
                .collect(Collectors.toList());

        return FriendDTO.Response.success(requestInfos);
    }

    /**
     * Handle friend request (approve/reject)
     */
    @Transactional
    public FriendDTO.Response<Void> handleFriendRequest(FriendDTO.HandleRequest request) {
        log.info("Handling friend request: requestId={}, receiverId={}, approve={}", 
                 request.getRequestId(), request.getReceiverId(), request.getApprove());

        Optional<FriendRequest> reqOpt = requestRepository.findById(request.getRequestId());
        if (reqOpt.isEmpty()) {
            return FriendDTO.Response.error(-1, "Friend request not found");
        }

        FriendRequest friendRequest = reqOpt.get();

        // Verify receiver
        if (!friendRequest.getReceiverId().equals(toId(request.getReceiverId()))) {
            return FriendDTO.Response.error(-2, "Not the receiver of this request");
        }

        // Check if still pending
        if (!friendRequest.isPending()) {
            return FriendDTO.Response.error(-3, "Request already processed");
        }

        if (request.getApprove()) {
            return acceptFriendRequest(request.getRequestId(), request.getReceiverId());
        } else {
            // Reject
            friendRequest.reject();
            requestRepository.save(friendRequest);
            log.info("Friend request rejected: requestId={}", request.getRequestId());
            return FriendDTO.Response.success(null);
        }
    }

    /**
     * Accept friend request (internal helper)
     */
    private FriendDTO.Response<Void> acceptFriendRequest(Long requestId, String receiverId) {
        Optional<FriendRequest> reqOpt = requestRepository.findById(requestId);
        if (reqOpt.isEmpty()) {
            return FriendDTO.Response.error(-1, "Friend request not found");
        }

        FriendRequest friendRequest = reqOpt.get();

        // Check friend limits for both players
        long senderFriendCount   = friendRepository.countFriends(friendRequest.getSenderId());
        long receiverFriendCount = friendRepository.countFriends(friendRequest.getReceiverId());

        if (senderFriendCount >= MAX_FRIENDS) {
            return FriendDTO.Response.error(-4, "Sender's friend list is full");
        }
        if (receiverFriendCount >= MAX_FRIENDS) {
            return FriendDTO.Response.error(-5, "Your friend list is full");
        }

        // Create friendship (store with smaller ID first for consistency)
        Long roleId1, roleId2;
        String roleName1, roleName2;
        if (friendRequest.getSenderId().compareTo(friendRequest.getReceiverId()) < 0) {
            roleId1 = friendRequest.getSenderId();   roleName1 = friendRequest.getSenderName();
            roleId2 = friendRequest.getReceiverId(); roleName2 = friendRequest.getReceiverName();
        } else {
            roleId1 = friendRequest.getReceiverId(); roleName1 = friendRequest.getReceiverName();
            roleId2 = friendRequest.getSenderId();   roleName2 = friendRequest.getSenderName();
        }

        Friend friend = Friend.builder()
                .roleId1(roleId1).roleName1(roleName1)
                .roleId2(roleId2).roleName2(roleName2)
                .friendshipLevel(1)
                .friendshipPoints(0L)
                .build();

        friendRepository.save(friend);
        friendRequest.accept();
        requestRepository.save(friendRequest);

        log.info("Friend request accepted: from={}, to={}", friendRequest.getSenderId(), friendRequest.getReceiverId());
        return FriendDTO.Response.success(null);
    }

    /**
     * Remove friend
     */
    @Transactional
    public FriendDTO.Response<Void> removeFriend(String roleId, String friendId) {
        log.info("Removing friend: roleId={}, friendId={}", roleId, friendId);

        Optional<Friend> friendOpt = friendRepository.findByRoleIds(toId(roleId), toId(friendId));
        if (friendOpt.isEmpty()) {
            return FriendDTO.Response.error(-1, "Not friends");
        }

        friendRepository.delete(friendOpt.get());
        log.info("Friend removed: roleId={}, friendId={}", roleId, friendId);

        return FriendDTO.Response.success(null);
    }

    /**
     * Block player
     */
    @Transactional
    public FriendDTO.Response<Void> blockPlayer(FriendDTO.BlockRequest request) {
        log.info("Blocking player: blockerId={}, blockedId={}", request.getBlockerId(), request.getBlockedId());

        // Cannot block self
        if (request.getBlockerId().equals(request.getBlockedId())) {
            return FriendDTO.Response.error(-1, "Cannot block yourself");
        }

        Long blockerIdL = toId(request.getBlockerId());
        Long blockedIdL = toId(request.getBlockedId());

        // Check if already blocked
        if (blockedRepository.existsByBlockerIdAndBlockedId(blockerIdL, blockedIdL)) {
            return FriendDTO.Response.error(-2, "Player already blocked");
        }

        // Check block limit
        long blockCount = blockedRepository.countByBlockerId(blockerIdL);
        if (blockCount >= MAX_BLOCKED) {
            return FriendDTO.Response.error(-3, String.format("Block list full (%d/%d)", blockCount, MAX_BLOCKED));
        }

        // Remove friendship if exists
        Optional<Friend> friendOpt = friendRepository.findByRoleIds(blockerIdL, blockedIdL);
        friendOpt.ifPresent(friendRepository::delete);

        // Create block
        BlockedPlayer blocked = BlockedPlayer.builder()
                .blockerId(blockerIdL)
                .blockedId(blockedIdL)
                .blockedName(request.getBlockedName())
                .reason(request.getReason())
                .build();

        blockedRepository.save(blocked);
        log.info("Player blocked: blockerId={}, blockedId={}", request.getBlockerId(), request.getBlockedId());

        return FriendDTO.Response.success(null);
    }

    /**
     * Unblock player
     */
    @Transactional
    public FriendDTO.Response<Void> unblockPlayer(String blockerId, String blockedId) {
        log.info("Unblocking player: blockerId={}, blockedId={}", blockerId, blockedId);

        Optional<BlockedPlayer> blockedOpt = blockedRepository.findByBlockerIdAndBlockedId(toId(blockerId), toId(blockedId));
        if (blockedOpt.isEmpty()) {
            return FriendDTO.Response.error(-1, "Player not blocked");
        }

        blockedRepository.delete(blockedOpt.get());
        log.info("Player unblocked: blockerId={}, blockedId={}", blockerId, blockedId);

        return FriendDTO.Response.success(null);
    }

    /**
     * Get blocked list
     */
    @Transactional(readOnly = true)
    public FriendDTO.Response<List<FriendDTO.BlockedInfo>> getBlockedList(String roleId) {
        log.info("Getting blocked list: roleId={}", roleId);

        List<BlockedPlayer> blocked = blockedRepository.findByBlockerIdOrderByBlockedAtDesc(toId(roleId));
        
        List<FriendDTO.BlockedInfo> blockedInfos = blocked.stream()
                .map(this::buildBlockedInfo)
                .collect(Collectors.toList());

        return FriendDTO.Response.success(blockedInfos);
    }

    /**
     * Update online status
     */
    @Transactional
    public FriendDTO.Response<Void> updateOnlineStatus(String roleId, String roleName, Integer level, Boolean online) {
        log.info("Updating online status: roleId={}, online={}", roleId, online);

        Long roleIdL = toId(roleId);
        Optional<OnlineStatus> statusOpt = statusRepository.findByRoleId(roleIdL);
        
        OnlineStatus status;
        if (statusOpt.isPresent()) {
            status = statusOpt.get();
            status.setRoleName(roleName);
            status.setLevel(level);
        } else {
            status = OnlineStatus.builder()
                    .roleId(roleIdL)
                    .roleName(roleName)
                    .level(level)
                    .build();
        }

        if (online) {
            status.setOnline();
        } else {
            status.setOffline();
        }

        statusRepository.save(status);
        log.info("Online status updated: roleId={}, online={}", roleId, online);

        return FriendDTO.Response.success(null);
    }

    /**
     * Give gift to friend
     */
    @Transactional
    public FriendDTO.Response<Void> giveGift(FriendDTO.GiveGiftRequest request) {
        log.info("Giving gift: from={}, to={}, itemId={}, quantity={}", 
                 request.getGiverId(), request.getReceiverId(), request.getItemId(), request.getQuantity());

        Long giverIdL    = toId(request.getGiverId());
        Long receiverIdL = toId(request.getReceiverId());

        // Check if friends
        if (!friendRepository.areFriends(giverIdL, receiverIdL)) {
            return FriendDTO.Response.error(-1, "Not friends");
        }

        // Consume item from giver
        try {
            UseItemReq useReq = UseItemReq.builder()
                    .itemId(request.getItemId())
                    .quantity(request.getQuantity())
                    .source("FRIEND_GIFT")
                    .build();
            bagClient.useItem(request.getGiverId(), useReq);
            log.info("Item consumed from giver: roleId={}, itemId={}", request.getGiverId(), request.getItemId());
        } catch (Exception e) {
            log.error("Failed to consume item: {}", e.getMessage());
            return FriendDTO.Response.error(-2, "Insufficient items");
        }
        
        // Grant item to receiver
        try {
            GrantReq grantReq = GrantReq.builder()
                    .userId(1L) // audit field
                    .roleId(request.getReceiverId())
                    .items(Collections.singletonList(
                        ItemInfo.builder()
                                .itemId(request.getItemId())
                                .quantity(request.getQuantity())
                                .build()
                    ))
                    .eventId("FRIEND_GIFT_" + System.currentTimeMillis())
                    .source("Gift from " + request.getGiverId())
                    .build();
            bagClient.grantItems(grantReq);
            log.info("Item granted to receiver: roleId={}, itemId={}", request.getReceiverId(), request.getItemId());
        } catch (Exception e) {
            log.error("Failed to grant item: {}", e.getMessage());
            return FriendDTO.Response.error(-3, "Failed to send gift");
        }

        // Add friendship points (gifting increases friendship)
        Optional<Friend> friendOpt = friendRepository.findByRoleIds(giverIdL, receiverIdL);
        if (friendOpt.isPresent()) {
            Friend friend = friendOpt.get();
            friend.addFriendshipPoints(10); // 10 points per gift
            friendRepository.save(friend);
            log.info("Friendship points added: +10, newLevel={}", friend.getFriendshipLevel());
        }

        log.info("Gift given successfully: from={}, to={}", request.getGiverId(), request.getReceiverId());
        return FriendDTO.Response.success(null);
    }

    // ==================== Helper Methods ====================

    private FriendDTO.FriendInfo buildFriendInfo(Friend friend, String roleId) {
        Long roleIdL   = toId(roleId);
        Long friendIdL = friend.getOtherRoleId(roleIdL);
        String friendId   = String.valueOf(friendIdL);
        String friendName = friend.getOtherRoleName(roleIdL);
        
        // Get online status
        Boolean online = statusRepository.existsByRoleIdAndOnlineTrue(friendIdL);
        LocalDateTime lastOnlineTime = null;
        
        Optional<OnlineStatus> statusOpt = statusRepository.findByRoleId(friendIdL);
        if (statusOpt.isPresent()) {
            OnlineStatus status = statusOpt.get();
            lastOnlineTime = status.getOnline() ? status.getLastLoginTime() : status.getLastLogoutTime();
        }

        // Fetch role power from role-service
        Long power = 0L;
        try {
            Optional<RoleDTOs.RoleResp> roleOpt = roleClient.getRoleInfo(friendIdL);
            if (roleOpt.isPresent() && roleOpt.get().getCap() != null) {
                power = roleOpt.get().getCap();
            }
        } catch (Exception e) {
            log.debug("Failed to fetch power for friend {}: {}", friendId, e.getMessage());
        }

        return FriendDTO.FriendInfo.builder()
                .roleId(friendId)
                .roleName(friendName)
                .level(statusOpt.map(OnlineStatus::getLevel).orElse(1))
                .power(power)
                .online(online)
                .friendshipLevel(friend.getFriendshipLevel())
                .friendshipPoints(friend.getFriendshipPoints())
                .lastOnlineTime(lastOnlineTime)
                .friendSince(friend.getCreatedAt())
                .build();
    }

    private FriendDTO.RequestInfo buildRequestInfo(FriendRequest request) {
        return FriendDTO.RequestInfo.builder()
                .id(request.getId())
                .senderId(String.valueOf(request.getSenderId()))
                .senderName(request.getSenderName())
                .senderLevel(request.getSenderLevel())
                .receiverId(String.valueOf(request.getReceiverId()))
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private FriendDTO.BlockedInfo buildBlockedInfo(BlockedPlayer blocked) {
        return FriendDTO.BlockedInfo.builder()
                .id(blocked.getId())
                .blockedId(String.valueOf(blocked.getBlockedId()))
                .blockedName(blocked.getBlockedName())
                .reason(blocked.getReason())
                .blockedAt(blocked.getBlockedAt())
                .build();
    }

    public FriendDTO.Response<List<FriendDTO.PlayerInfo>> searchPlayers(String roleId, String keyword) {
        Long roleIdL = toId(roleId);
        if (keyword == null || keyword.isBlank()) return FriendDTO.Response.success(List.of());
        List<OnlineStatus> found = statusRepository.findByRoleNameContainingIgnoreCase(keyword);
        List<FriendDTO.PlayerInfo> results = found.stream()
            .filter(s -> !s.getRoleId().equals(roleIdL))
            .limit(20)
            .map(s -> FriendDTO.PlayerInfo.builder()
                .roleId(String.valueOf(s.getRoleId())).roleName(s.getRoleName())
                .level(s.getLevel()).online(s.getOnline())
                .isFriend(friendRepository.areFriends(roleIdL, s.getRoleId()))
                .isBlocked(blockedRepository.existsByBlockerIdAndBlockedId(roleIdL, s.getRoleId()))
                .build())
            .collect(Collectors.toList());
        return FriendDTO.Response.success(results);
    }

    public FriendDTO.Response<List<FriendDTO.OnlineStatusInfo>> getOnlineFriends(String roleId) {
        Long roleIdL = toId(roleId);
        List<Friend> friends = friendRepository.findAllFriends(roleIdL);
        List<FriendDTO.OnlineStatusInfo> statuses = friends.stream()
            .map(f -> {
                Long fId = f.getOtherRoleId(roleIdL);
                Optional<OnlineStatus> status = statusRepository.findByRoleId(fId);
                return FriendDTO.OnlineStatusInfo.builder()
                    .roleId(String.valueOf(fId))
                    .online(status.map(OnlineStatus::getOnline).orElse(false))
                    .timestamp(status.map(s -> s.getOnline()
                        ? (s.getLastLoginTime() != null ? s.getLastLoginTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L)
                        : (s.getLastLogoutTime() != null ? s.getLastLogoutTime().toInstant(ZoneOffset.UTC).toEpochMilli() : 0L))
                        .orElse(0L))
                    .build();
            })
            .collect(Collectors.toList());
        return FriendDTO.Response.success(statuses);
    }
}

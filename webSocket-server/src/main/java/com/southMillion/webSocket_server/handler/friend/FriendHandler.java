package com.SouthMillion.webSocket_server.handler.friend;

import com.google.protobuf.InvalidProtocolBufferException;
import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.FriendFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgfriend.Msgfriend;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Friend Handler - Friend System
 * 
 * Messages:
 * - C→S 1431 (CS_FRIEND_REQ) - Client friend request
 * - S→C 1432 (SC_FRIEND_LIST) - Friend list response
 * - S→C 1433 (SC_FRIEND_REQUEST) - Friend request notification
 * - S→C 1434 (SC_FRIEND_ONLINE) - Friend online status update
 * - S→C 1435 (SC_FRIEND_OPERATION) - Operation result
 * 
 * Friend Operations:
 * - 1: Get friend list
 * - 2: Send friend request
 * - 3: Get received requests
 * - 4: Handle request (accept/reject)
 * - 5: Remove friend
 * - 6: Block player
 * - 7: Unblock player
 * - 8: Search players
 * - 9: Get online status
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendHandler implements MessageHandler {

    private final FriendFeign friendFeign;

    // Friend operation types from Frontend
    private static final int FRIEND_OP_GET_LIST = 1;           // Get friend list
    private static final int FRIEND_OP_SEND_REQUEST = 2;       // Send friend request
    private static final int FRIEND_OP_GET_REQUESTS = 3;       // Get received requests
    private static final int FRIEND_OP_HANDLE_REQUEST = 4;     // Accept/Reject request
    private static final int FRIEND_OP_REMOVE = 5;             // Remove friend
    private static final int FRIEND_OP_BLOCK = 6;              // Block player
    private static final int FRIEND_OP_UNBLOCK = 7;            // Unblock player
    private static final int FRIEND_OP_SEARCH = 8;             // Search players
    private static final int FRIEND_OP_GET_ONLINE = 9;         // Get online status

    @Override
    public int[] interests() {
        return new int[]{MessageIds.CS_FRIEND_REQ}; // 1431
    }

    /** Gọi sau login: đẩy danh sách bạn bè (1432) và lời mời chờ (1433) về client. */
    public Mono<Void> pushAll(PlayerSession ps) {
        if (ps.getRoleId() == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            handleGetFriendList(ps);
            handleGetRequests(ps);
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        if (msgId != MessageIds.CS_FRIEND_REQ) {
            log.warn("[friend] Unknown msgId: {}", msgId);
            return;
        }

        try {
            Msgfriend.CS_FRIEND_REQ req = Msgfriend.CS_FRIEND_REQ.parseFrom(payload);
            int op = req.getOp();
            Long roleId = ps.getRoleId();

            log.debug("[friend] op={}, roleId={}", op, roleId);

            switch (op) {
                case FRIEND_OP_GET_LIST:
                    handleGetFriendList(ps);
                    break;
                case FRIEND_OP_SEND_REQUEST:
                    handleSendRequest(ps, req);
                    break;
                case FRIEND_OP_GET_REQUESTS:
                    handleGetRequests(ps);
                    break;
                case FRIEND_OP_HANDLE_REQUEST:
                    handleHandleRequest(ps, req);
                    break;
                case FRIEND_OP_REMOVE:
                    handleRemoveFriend(ps, req);
                    break;
                case FRIEND_OP_BLOCK:
                    handleBlockPlayer(ps, req);
                    break;
                case FRIEND_OP_UNBLOCK:
                    handleUnblockPlayer(ps, req);
                    break;
                case FRIEND_OP_SEARCH:
                    handleSearchPlayers(ps, req);
                    break;
                case FRIEND_OP_GET_ONLINE:
                    handleGetOnlineStatus(ps);
                    break;
                default:
                    log.warn("[friend] Unknown operation: {}", op);
                    sendErrorResponse(ps, "Unknown operation");
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("[friend] Failed to parse CS_FRIEND_REQ", e);
            sendErrorResponse(ps, "Invalid request format");
        } catch (Exception e) {
            log.error("[friend] Error handling friend request", e);
            sendErrorResponse(ps, "Internal error");
        }
        });
    }

    /**
     * Handle get friend list
     */
    private void handleGetFriendList(PlayerSession ps) {
        try {
            Map<String, Object> result = friendFeign.getFriendList(String.valueOf(ps.getRoleId()));

            // Build response
            Msgfriend.SC_FRIEND_LIST.Builder builder = Msgfriend.SC_FRIEND_LIST.newBuilder()
                    .setCode(0);

            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> friends = (java.util.List<Map<String, Object>>) result.get("data");
                
                for (Map<String, Object> friend : friends) {
                    Msgfriend.FriendInfo.Builder friendInfo = Msgfriend.FriendInfo.newBuilder()
                            .setRoleId(String.valueOf(friend.get("roleId")))
                            .setRoleName(String.valueOf(friend.get("roleName")))
                            .setLevel(((Number) friend.get("level")).intValue())
                            .setOnline((Boolean) friend.getOrDefault("online", false))
                            .setLastOnlineTime(((Number) friend.getOrDefault("lastLoginTime", 0L)).longValue());

                    builder.addFriends(friendInfo);
                }
            }

            Emitters.emit(ps, MessageIds.SC_FRIEND_LIST, builder.build().toByteArray());

            log.info("[friend] Friend list sent - roleId={}, count={}", ps.getRoleId(), builder.getFriendsCount());

        } catch (Exception e) {
            log.error("[friend] Failed to get friend list", e);
            sendErrorResponse(ps, "Failed to get friend list");
        }
    }

    /**
     * Handle send friend request
     */
    private void handleSendRequest(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("senderId", ps.getRoleId());
            request.put("receiverId", req.getTargetRoleId());
            if (req.hasMessage()) {
                request.put("message", req.getMessage());
            }

            friendFeign.sendFriendRequest(request);

            // Send success response
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(0)
                    .setMessage("Friend request sent")
                    .setOpType(FRIEND_OP_SEND_REQUEST);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());

            log.info("[friend] Request sent - from={}, to={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[friend] Failed to send request", e);
            sendErrorResponse(ps, "Failed to send request");
        }
    }

    /**
     * Handle get received requests
     */
    private void handleGetRequests(PlayerSession ps) {
        try {
            Map<String, Object> result = friendFeign.getReceivedRequests(String.valueOf(ps.getRoleId()));

            // Build response
            Msgfriend.SC_FRIEND_REQUEST.Builder builder = Msgfriend.SC_FRIEND_REQUEST.newBuilder()
                    .setCode(0);

            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> requests = (java.util.List<Map<String, Object>>) result.get("data");
                
                for (Map<String, Object> req : requests) {
                    Msgfriend.RequestInfo.Builder reqInfo = Msgfriend.RequestInfo.newBuilder()
                            .setRequestId(((Number) req.get("requestId")).intValue())
                            .setFromRoleId(String.valueOf(req.get("senderId")))
                            .setFromRoleName(String.valueOf(req.get("senderName")))
                            .setRequestTime(((Number) req.get("timestamp")).longValue());

                    builder.addRequests(reqInfo);
                }
            }

            Emitters.emit(ps, MessageIds.SC_FRIEND_REQUEST, builder.build().toByteArray());

            log.info("[friend] Requests sent - roleId={}, count={}", ps.getRoleId(), builder.getRequestsCount());

        } catch (Exception e) {
            log.error("[friend] Failed to get requests", e);
            sendErrorResponse(ps, "Failed to get requests");
        }
    }

    /**
     * Handle accept/reject friend request
     */
    private void handleHandleRequest(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("requestId", req.getRequestId());
            request.put("approve", req.getApprove());

            friendFeign.handleFriendRequest(request);

            // Send success response
            String msg = req.getApprove() ? "Request accepted" : "Request rejected";
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(0)
                    .setMessage(msg)
                    .setOpType(FRIEND_OP_HANDLE_REQUEST);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());

            log.info("[friend] Request handled - roleId={}, approve={}", ps.getRoleId(), req.getApprove());

        } catch (Exception e) {
            log.error("[friend] Failed to handle request", e);
            sendErrorResponse(ps, "Failed to handle request");
        }
    }

    /**
     * Handle remove friend
     */
    private void handleRemoveFriend(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            friendFeign.removeFriend(String.valueOf(ps.getRoleId()), req.getTargetRoleId());

            // Send success response
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(0)
                    .setMessage("Friend removed")
                    .setOpType(FRIEND_OP_REMOVE);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());

            log.info("[friend] Friend removed - roleId={}, friendId={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[friend] Failed to remove friend", e);
            sendErrorResponse(ps, "Failed to remove friend");
        }
    }

    /**
     * Handle block player
     */
    private void handleBlockPlayer(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("blockerId", ps.getRoleId());
            request.put("blockedId", req.getTargetRoleId());

            friendFeign.blockPlayer(request);

            // Send success response
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(0)
                    .setMessage("Player blocked")
                    .setOpType(FRIEND_OP_BLOCK);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());

            log.info("[friend] Player blocked - blocker={}, blocked={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[friend] Failed to block player", e);
            sendErrorResponse(ps, "Failed to block player");
        }
    }

    /**
     * Handle unblock player
     */
    private void handleUnblockPlayer(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            friendFeign.unblockPlayer(String.valueOf(ps.getRoleId()), req.getTargetRoleId());

            // Send success response
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(0)
                    .setMessage("Player unblocked")
                    .setOpType(FRIEND_OP_UNBLOCK);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());

            log.info("[friend] Player unblocked - blocker={}, blocked={}", ps.getRoleId(), req.getTargetRoleId());

        } catch (Exception e) {
            log.error("[friend] Failed to unblock player", e);
            sendErrorResponse(ps, "Failed to unblock player");
        }
    }

    private void handleSearchPlayers(PlayerSession ps, Msgfriend.CS_FRIEND_REQ req) {
        try {
            String keyword = req.hasKeyword() ? req.getKeyword() : "";
            Map<String, Object> result = friendFeign.searchPlayers(String.valueOf(ps.getRoleId()), keyword);
            Msgfriend.SC_FRIEND_SEARCH.Builder builder = Msgfriend.SC_FRIEND_SEARCH.newBuilder().setCode(0);
            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> players = (java.util.List<Map<String, Object>>) result.get("data");
                for (Map<String, Object> p : players) {
                    Msgfriend.PlayerInfo.Builder pi = Msgfriend.PlayerInfo.newBuilder()
                            .setRoleId(String.valueOf(p.get("roleId")))
                            .setRoleName(String.valueOf(p.get("roleName")))
                            .setLevel(((Number) p.getOrDefault("level", 1)).intValue())
                            .setIsFriend((Boolean) p.getOrDefault("isFriend", false));
                    builder.addPlayers(pi);
                }
            }
            Emitters.emit(ps, MessageIds.SC_FRIEND_SEARCH, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[friend] Failed to search players", e);
            sendErrorResponse(ps, "Failed to search players");
        }
    }

    private void handleGetOnlineStatus(PlayerSession ps) {
        try {
            Map<String, Object> result = friendFeign.getOnlineStatus(String.valueOf(ps.getRoleId()));
            Msgfriend.SC_FRIEND_ONLINE.Builder builder = Msgfriend.SC_FRIEND_ONLINE.newBuilder().setCode(0);
            if (result.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> statuses = (java.util.List<Map<String, Object>>) result.get("data");
                for (Map<String, Object> s : statuses) {
                    Msgfriend.OnlineStatus.Builder os = Msgfriend.OnlineStatus.newBuilder()
                            .setRoleId(String.valueOf(s.get("roleId")))
                            .setOnline((Boolean) s.getOrDefault("online", false))
                            .setTimestamp(((Number) s.getOrDefault("timestamp", 0L)).longValue());
                    builder.addStatuses(os);
                }
            }
            Emitters.emit(ps, MessageIds.SC_FRIEND_ONLINE, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[friend] Failed to get online status", e);
            sendErrorResponse(ps, "Failed to get online status");
        }
    }

    /**
     * Send error response to client
     */
    private void sendErrorResponse(PlayerSession ps, String errorMsg) {
        try {
            Msgfriend.SC_FRIEND_OPERATION.Builder builder = Msgfriend.SC_FRIEND_OPERATION.newBuilder()
                    .setCode(1)
                    .setMessage(errorMsg)
                    .setOpType(0);

            Emitters.emit(ps, MessageIds.SC_FRIEND_OPERATION, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[friend] Failed to send error response", e);
        }
    }
}

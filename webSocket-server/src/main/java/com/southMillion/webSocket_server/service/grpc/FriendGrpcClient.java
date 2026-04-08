package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.friend.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FriendGrpcClient {

    @GrpcClient("friend-service")
    private FriendServiceGrpc.FriendServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public FriendListResponse getFriendList(String roleId) {
        try {
            return stub.getFriendList(GetFriendListRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] getFriendList: friend-service unavailable");
            else log.error("[grpc-friend] getFriendList error: {}", e.getMessage());
            return FriendListResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus sendFriendRequest(String requesterId, String requesterName, String targetId) {
        try {
            return stub.sendFriendRequest(SendFriendReq.newBuilder()
                    .setRequesterId(requesterId)
                    .setRequesterName(requesterName != null ? requesterName : "")
                    .setTargetId(targetId)
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] sendFriendRequest: friend-service unavailable");
            else log.error("[grpc-friend] sendFriendRequest error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus handleFriendRequest(String roleId, String requesterId, boolean accept) {
        try {
            return stub.handleFriendRequest(HandleFriendReq.newBuilder()
                    .setRoleId(roleId)
                    .setRequesterId(requesterId)
                    .setAccept(accept)
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] handleFriendRequest: friend-service unavailable");
            else log.error("[grpc-friend] handleFriendRequest error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus removeFriend(String roleId, String friendId) {
        try {
            return stub.removeFriend(RemoveFriendRequest.newBuilder().setRoleId(roleId).setFriendId(friendId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] removeFriend: friend-service unavailable");
            else log.error("[grpc-friend] removeFriend error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus blockPlayer(String blockerId, String blockedId, String blockerName) {
        try {
            return stub.blockPlayer(BlockPlayerRequest.newBuilder()
                    .setBlockerId(blockerId)
                    .setBlockedId(blockedId)
                    .setBlockerName(blockerName != null ? blockerName : "")
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] blockPlayer: friend-service unavailable");
            else log.error("[grpc-friend] blockPlayer error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public OnlineFriendsResponse getOnlineFriends(String roleId) {
        try {
            return stub.getOnlineFriends(GetOnlineFriendsRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] getOnlineFriends: friend-service unavailable");
            else log.error("[grpc-friend] getOnlineFriends error: {}", e.getMessage());
            return OnlineFriendsResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus updateOnlineStatus(String roleId, String roleName, int level, boolean online) {
        try {
            return stub.updateOnlineStatus(UpdateOnlineStatusRequest.newBuilder()
                    .setRoleId(roleId)
                    .setRoleName(roleName != null ? roleName : "")
                    .setLevel(level)
                    .setOnline(online)
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] updateOnlineStatus: friend-service unavailable");
            else log.error("[grpc-friend] updateOnlineStatus error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus giveGift(String senderId, String receiverId) {
        try {
            return stub.giveGift(GiveGiftRequest.newBuilder().setSenderId(senderId).setReceiverId(receiverId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-friend] giveGift: friend-service unavailable");
            else log.error("[grpc-friend] giveGift error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }
}

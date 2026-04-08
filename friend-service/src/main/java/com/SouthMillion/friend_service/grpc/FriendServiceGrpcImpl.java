package com.SouthMillion.friend_service.grpc;

import com.SouthMillion.friend_service.dto.FriendDTO;
import com.SouthMillion.friend_service.repository.FriendRequestRepository;
import com.SouthMillion.friend_service.service.FriendService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.friend.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * gRPC Server Implementation for Friend Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FriendServiceGrpcImpl extends FriendServiceGrpc.FriendServiceImplBase {

    private final FriendService friendService;
    private final FriendRequestRepository friendRequestRepository;

    @Override
    public void getFriendList(GetFriendListRequest request, StreamObserver<FriendListResponse> responseObserver) {
        try {
            FriendDTO.Response<List<FriendDTO.FriendInfo>> result = friendService.getFriendList(request.getRoleId());

            List<FriendData> friendDataList = result.getData() == null
                    ? List.of()
                    : result.getData().stream()
                            .map(this::toFriendData)
                            .collect(Collectors.toList());

            FriendListResponse response = FriendListResponse.newBuilder()
                    .setSuccess(result.getCode() == 0)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .addAllFriends(friendDataList)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getFriendList error: roleId={}", request.getRoleId(), e);
            responseObserver.onNext(FriendListResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendFriendRequest(SendFriendReq request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            FriendDTO.AddFriendRequest dto = FriendDTO.AddFriendRequest.builder()
                    .senderId(request.getRequesterId())
                    .senderName(request.getRequesterName())
                    .senderLevel(1)
                    .receiverId(request.getTargetId())
                    .receiverName("")
                    .build();

            FriendDTO.Response<Void> result = friendService.sendFriendRequest(dto);

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("sendFriendRequest error: requesterId={}, targetId={}", request.getRequesterId(), request.getTargetId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void handleFriendRequest(HandleFriendReq request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            // Resolve request ID by looking up pending request from requester to role
            Long requesterId = Long.parseLong(request.getRequesterId());
            Long roleId = Long.parseLong(request.getRoleId());

            Optional<com.SouthMillion.friend_service.entity.FriendRequest> reqOpt =
                    friendRequestRepository.findBySenderIdAndReceiverIdAndStatus(requesterId, roleId, 0);

            if (reqOpt.isEmpty()) {
                responseObserver.onNext(ResponseStatus.newBuilder()
                        .setCode(-1)
                        .setMessage("Friend request not found")
                        .setSuccess(false)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            FriendDTO.HandleRequest dto = FriendDTO.HandleRequest.builder()
                    .requestId(reqOpt.get().getId())
                    .receiverId(request.getRoleId())
                    .approve(request.getAccept())
                    .build();

            FriendDTO.Response<Void> result = friendService.handleFriendRequest(dto);

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("handleFriendRequest error: roleId={}, requesterId={}", request.getRoleId(), request.getRequesterId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void removeFriend(RemoveFriendRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            FriendDTO.Response<Void> result = friendService.removeFriend(request.getRoleId(), request.getFriendId());

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("removeFriend error: roleId={}, friendId={}", request.getRoleId(), request.getFriendId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void blockPlayer(BlockPlayerRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            FriendDTO.BlockRequest dto = FriendDTO.BlockRequest.builder()
                    .blockerId(request.getBlockerId())
                    .blockedId(request.getBlockedId())
                    .blockedName(request.getBlockerName())
                    .build();

            FriendDTO.Response<Void> result = friendService.blockPlayer(dto);

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("blockPlayer error: blockerId={}, blockedId={}", request.getBlockerId(), request.getBlockedId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getOnlineFriends(GetOnlineFriendsRequest request, StreamObserver<OnlineFriendsResponse> responseObserver) {
        try {
            FriendDTO.Response<List<FriendDTO.OnlineStatusInfo>> result = friendService.getOnlineFriends(request.getRoleId());

            List<OnlineStatusData> statusList = result.getData() == null
                    ? List.of()
                    : result.getData().stream()
                            .map(info -> OnlineStatusData.newBuilder()
                                    .setRoleId(info.getRoleId() != null ? info.getRoleId() : "")
                                    .setRoleName("")
                                    .setLevel(0)
                                    .setOnline(Boolean.TRUE.equals(info.getOnline()))
                                    .build())
                            .collect(Collectors.toList());

            responseObserver.onNext(OnlineFriendsResponse.newBuilder()
                    .setSuccess(result.getCode() == 0)
                    .addAllFriends(statusList)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getOnlineFriends error: roleId={}", request.getRoleId(), e);
            responseObserver.onNext(OnlineFriendsResponse.newBuilder()
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateOnlineStatus(UpdateOnlineStatusRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            FriendDTO.Response<Void> result = friendService.updateOnlineStatus(
                    request.getRoleId(),
                    request.getRoleName(),
                    request.getLevel(),
                    request.getOnline());

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("updateOnlineStatus error: roleId={}", request.getRoleId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void giveGift(GiveGiftRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            FriendDTO.GiveGiftRequest dto = FriendDTO.GiveGiftRequest.builder()
                    .giverId(request.getSenderId())
                    .receiverId(request.getReceiverId())
                    .itemId(1)
                    .quantity(1)
                    .build();

            FriendDTO.Response<Void> result = friendService.giveGift(dto);

            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSuccess(result.getCode() == 0)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("giveGift error: senderId={}, receiverId={}", request.getSenderId(), request.getReceiverId(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ==================== Mapping Helpers ====================

    private FriendData toFriendData(FriendDTO.FriendInfo info) {
        long friendSinceMs = 0L;
        if (info.getFriendSince() != null) {
            friendSinceMs = info.getFriendSince().toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        return FriendData.newBuilder()
                .setRoleId(info.getRoleId() != null ? info.getRoleId() : "")
                .setRoleName(info.getRoleName() != null ? info.getRoleName() : "")
                .setLevel(info.getLevel() != null ? info.getLevel() : 0)
                .setOnline(Boolean.TRUE.equals(info.getOnline()))
                .setFriendSinceMs(friendSinceMs)
                .build();
    }
}

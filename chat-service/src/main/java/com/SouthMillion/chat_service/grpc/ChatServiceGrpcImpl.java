package com.SouthMillion.chat_service.grpc;

import com.SouthMillion.chat_service.dto.ChatDTO;
import com.SouthMillion.chat_service.service.ChatService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.chat.*;

import java.time.ZoneOffset;
import java.util.List;

/**
 * gRPC Server Implementation for Chat Service
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ChatServiceGrpcImpl extends ChatServiceGrpc.ChatServiceImplBase {

    private final ChatService chatService;

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
        try {
            ChatDTO.SendMessageRequest dto = ChatDTO.SendMessageRequest.builder()
                    .channel(request.getChannel())
                    .senderId(request.getSenderId())
                    .senderName(request.getSenderName())
                    .receiverId(request.getReceiverId().isEmpty() ? null : request.getReceiverId())
                    .receiverName(request.getReceiverName().isEmpty() ? null : request.getReceiverName())
                    .channelId(request.getChannelId().isEmpty() ? null : request.getChannelId())
                    .content(request.getContent())
                    .build();

            ChatDTO.Response<ChatDTO.MessageInfo> result = chatService.sendMessage(dto);

            SendMessageResponse.Builder builder = SendMessageResponse.newBuilder()
                    .setSuccess(result.getCode() == 0)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "");

            if (result.getCode() == 0 && result.getData() != null) {
                builder.setMsg(toProtoMessage(result.getData()));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

            log.info("[grpc-chat] SendMessage success: channel={}, sender={}", request.getChannel(), request.getSenderId());

        } catch (Exception e) {
            log.error("[grpc-chat] SendMessage failed: {}", e.getMessage(), e);
            responseObserver.onNext(SendMessageResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getHistory(GetHistoryRequest request, StreamObserver<GetHistoryResponse> responseObserver) {
        try {
            int count = request.getCount() > 0 ? request.getCount() : 50;

            ChatDTO.GetHistoryRequest dto = ChatDTO.GetHistoryRequest.builder()
                    .channel(request.getChannel())
                    .roleId1(request.getRoleId1().isEmpty() ? null : request.getRoleId1())
                    .roleId2(request.getRoleId2().isEmpty() ? null : request.getRoleId2())
                    .channelId(request.getChannelId().isEmpty() ? null : request.getChannelId())
                    .count(count)
                    .build();

            ChatDTO.Response<List<ChatDTO.MessageInfo>> result = chatService.getHistory(dto);

            GetHistoryResponse.Builder builder = GetHistoryResponse.newBuilder()
                    .setSuccess(result.getCode() == 0);

            if (result.getCode() == 0 && result.getData() != null) {
                for (ChatDTO.MessageInfo msg : result.getData()) {
                    builder.addMessages(toProtoMessage(msg));
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

            log.info("[grpc-chat] GetHistory success: channel={}", request.getChannel());

        } catch (Exception e) {
            log.error("[grpc-chat] GetHistory failed: {}", e.getMessage(), e);
            responseObserver.onNext(GetHistoryResponse.newBuilder()
                    .setSuccess(false)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void mutePlayer(MutePlayerRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            ChatDTO.MuteRequest dto = ChatDTO.MuteRequest.builder()
                    .roleId(request.getRoleId())
                    .roleName(request.getRoleName())
                    .reason(request.getReason().isEmpty() ? null : request.getReason())
                    .durationMinutes(request.getDurationMinutes())
                    .build();

            ChatDTO.Response<Void> result = chatService.mutePlayer(dto);

            boolean success = result.getCode() == 0;
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(success)
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .build());
            responseObserver.onCompleted();

            log.info("[grpc-chat] MutePlayer success: roleId={}", request.getRoleId());

        } catch (Exception e) {
            log.error("[grpc-chat] MutePlayer failed: {}", e.getMessage(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void unmutePlayer(UnmutePlayerRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            ChatDTO.Response<Void> result = chatService.unmutePlayer(request.getRoleId());

            boolean success = result.getCode() == 0;
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(success)
                    .setCode(result.getCode())
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .build());
            responseObserver.onCompleted();

            log.info("[grpc-chat] UnmutePlayer success: roleId={}", request.getRoleId());

        } catch (Exception e) {
            log.error("[grpc-chat] UnmutePlayer failed: {}", e.getMessage(), e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                    .setSuccess(false)
                    .setCode(500)
                    .setMessage("Internal error: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ==================== Helper Methods ====================

    private ChatMessageData toProtoMessage(ChatDTO.MessageInfo msg) {
        return ChatMessageData.newBuilder()
                .setId(msg.getId() != null ? msg.getId() : 0L)
                .setChannel(msg.getChannel() != null ? msg.getChannel() : 0)
                .setSenderId(msg.getSenderId() != null ? msg.getSenderId() : "")
                .setSenderName(msg.getSenderName() != null ? msg.getSenderName() : "")
                .setReceiverId(msg.getReceiverId() != null ? msg.getReceiverId() : "")
                .setContent(msg.getContent() != null ? msg.getContent() : "")
                .setCreatedAtMs(msg.getCreatedAt() != null
                        ? msg.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : 0L)
                .build();
    }
}

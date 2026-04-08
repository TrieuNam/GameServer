package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.chat.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatGrpcClient {

    @GrpcClient("chat-service")
    private ChatServiceGrpc.ChatServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public SendMessageResponse sendMessage(int channel, String senderId, String senderName,
                                           String receiverId, String content) {
        try {
            SendMessageRequest req = SendMessageRequest.newBuilder()
                    .setChannel(channel)
                    .setSenderId(senderId)
                    .setSenderName(senderName != null ? senderName : "")
                    .setReceiverId(receiverId != null ? receiverId : "")
                    .setContent(content)
                    .build();
            return stub.sendMessage(req);
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-chat] sendMessage: chat-service unavailable");
            else log.error("[grpc-chat] sendMessage error: {}", e.getMessage());
            return SendMessageResponse.newBuilder().setSuccess(false).build();
        }
    }

    public GetHistoryResponse getHistory(int channel, String roleId1, String roleId2, int count) {
        try {
            GetHistoryRequest req = GetHistoryRequest.newBuilder()
                    .setChannel(channel)
                    .setRoleId1(roleId1 != null ? roleId1 : "")
                    .setRoleId2(roleId2 != null ? roleId2 : "")
                    .setCount(count > 0 ? count : 50)
                    .build();
            return stub.getHistory(req);
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-chat] getHistory: chat-service unavailable");
            else log.error("[grpc-chat] getHistory error: {}", e.getMessage());
            return GetHistoryResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus mutePlayer(String roleId, String roleName, String reason, int durationMinutes) {
        try {
            return stub.mutePlayer(MutePlayerRequest.newBuilder()
                    .setRoleId(roleId)
                    .setRoleName(roleName != null ? roleName : "")
                    .setReason(reason != null ? reason : "")
                    .setDurationMinutes(durationMinutes)
                    .build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-chat] mutePlayer: chat-service unavailable");
            else log.error("[grpc-chat] mutePlayer error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ResponseStatus unmutePlayer(String roleId) {
        try {
            return stub.unmutePlayer(UnmutePlayerRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-chat] unmutePlayer: chat-service unavailable");
            else log.error("[grpc-chat] unmutePlayer error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }
}

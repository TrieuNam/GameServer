package com.SouthMillion.webSocket_server.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat DTOs for Feign Client Integration
 */
public class ChatDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private Integer channel; // 1=World, 2=Guild, 3=Team, 4=Private, 5=System
        private String senderId;
        private String senderName;
        private String receiverId; // For private chat
        private String receiverName;
        private String channelId; // Guild/Team ID
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageInfo {
        private Long id;
        private Integer channel;
        private String senderId;
        private String senderName;
        private String receiverId;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetHistoryRequest {
        private Integer channel;
        private String channelId;
        private String roleId1; // For private chat
        private String roleId2;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MuteRequest {
        private String roleId;
        private String roleName;
        private Integer durationMinutes;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response<T> {
        private Integer code;
        private String message;
        private T data;
    }
}

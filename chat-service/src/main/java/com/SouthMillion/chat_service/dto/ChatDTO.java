package com.SouthMillion.chat_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class ChatDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotNull
        @Min(1) @Max(5)
        private Integer channel; // 1=World, 2=Guild, 3=Team, 4=Private, 5=System
        
        @NotBlank
        private String senderId;
        
        @NotBlank
        private String senderName;
        
        private String receiverId; // For private chat
        
        private String receiverName;
        
        private String channelId; // Guild/Team ID
        
        @NotBlank
        @Size(min = 1, max = 500)
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
        @NotNull
        private Integer channel;
        
        private String channelId;
        
        private String roleId1; // For private chat
        
        private String roleId2;
        
        @Min(1) @Max(100)
        private Integer count = 50;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MuteRequest {
        @NotBlank
        private String roleId;
        
        @NotBlank
        private String roleName;
        
        @NotNull
        @Min(1) @Max(10080) // Max 7 days in minutes
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
        
        public static <T> Response<T> success(T data) {
            return Response.<T>builder().code(0).message("Success").data(data).build();
        }
        
        public static <T> Response<T> error(Integer code, String message) {
            return Response.<T>builder().code(code).message(message).build();
        }
    }
}

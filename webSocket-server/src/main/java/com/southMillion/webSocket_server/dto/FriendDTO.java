package com.SouthMillion.webSocket_server.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Friend DTOs for Feign Client Integration
 */
public class FriendDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddFriendRequest {
        private String senderId;
        private String senderName;
        private Integer senderLevel;
        private String receiverId;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendListResponse {
        private Integer totalCount;
        private List<FriendInfo> friends;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendInfo {
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private Integer friendshipLevel;
        private Integer friendshipPoints;
        private Boolean online;
        private LocalDateTime lastLoginTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestListResponse {
        private Integer totalCount;
        private List<RequestInfo> requests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInfo {
        private Long id;
        private String senderId;
        private String senderName;
        private Integer senderLevel;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandleRequestRequest {
        private Long requestId;
        private String receiverId;
        private Boolean accept;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockRequest {
        private String blockerId;
        private String blockedId;
        private String blockedName;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockedListResponse {
        private Integer totalCount;
        private List<BlockedInfo> blockedPlayers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockedInfo {
        private String blockedId;
        private String blockedName;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineStatusUpdate {
        private String roleId;
        private Boolean online;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiveGiftRequest {
        private String senderId;
        private String receiverId;
        private String giftId;
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

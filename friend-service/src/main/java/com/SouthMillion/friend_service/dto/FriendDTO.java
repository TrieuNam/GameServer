package com.SouthMillion.friend_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Friend DTOs for API requests and responses
 */
public class FriendDTO {

    /**
     * Friend Info Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendInfo {
        private String roleId;
        private String roleName;
        private Integer level;
        private Long power;
        private Boolean online;
        private Integer friendshipLevel;
        private Long friendshipPoints;
        private LocalDateTime lastOnlineTime;
        private LocalDateTime friendSince;
    }

    /**
     * Add Friend Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddFriendRequest {
        @NotBlank(message = "Sender ID cannot be blank")
        private String senderId;
        
        @NotBlank(message = "Sender name cannot be blank")
        private String senderName;
        
        @Min(1)
        private Integer senderLevel;
        
        @NotBlank(message = "Receiver ID cannot be blank")
        private String receiverId;
        
        @NotBlank(message = "Receiver name cannot be blank")
        private String receiverName;
        
        @Size(max = 200)
        private String message;
    }

    /**
     * Friend Request Info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInfo {
        private Long id;
        private String senderId;
        private String senderName;
        private Integer senderLevel;
        private String receiverId;
        private String message;
        private Integer status;
        private LocalDateTime createdAt;
    }

    /**
     * Handle Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandleRequest {
        @NotNull(message = "Request ID cannot be null")
        private Long requestId;
        
        @NotBlank(message = "Receiver ID cannot be blank")
        private String receiverId;
        
        @NotNull(message = "Approve flag cannot be null")
        private Boolean approve;
    }

    /**
     * Search Player Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        @NotBlank(message = "Keyword cannot be blank")
        @Size(min = 2, max = 50)
        private String keyword;
        
        @Min(0)
        private Integer page = 0;
        
        @Min(1)
        @Max(50)
        private Integer size = 20;
    }

    /**
     * Player Info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo {
        private String roleId;
        private String roleName;
        private Integer level;
        private Long power;
        private Boolean online;
        private Boolean isFriend;
        private Boolean isBlocked;
    }

    /**
     * Block Player Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockRequest {
        @NotBlank(message = "Blocker ID cannot be blank")
        private String blockerId;
        
        @NotBlank(message = "Blocked ID cannot be blank")
        private String blockedId;
        
        @NotBlank(message = "Blocked name cannot be blank")
        private String blockedName;
        
        @Size(max = 200)
        private String reason;
    }

    /**
     * Blocked Player Info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockedInfo {
        private Long id;
        private String blockedId;
        private String blockedName;
        private String reason;
        private LocalDateTime blockedAt;
    }

    /**
     * Give Gift Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiveGiftRequest {
        @NotBlank(message = "Giver ID cannot be blank")
        private String giverId;
        
        @NotBlank(message = "Receiver ID cannot be blank")
        private String receiverId;
        
        @NotNull(message = "Gift item ID cannot be null")
        private Integer itemId;
        
        @Min(1)
        private Integer quantity = 1;
    }

    /**
     * Generic Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response<T> {
        private Integer code;
        private String message;
        private T data;
        
        public static <T> Response<T> success(T data) {
            return Response.<T>builder()
                    .code(0)
                    .message("Success")
                    .data(data)
                    .build();
        }
        
        public static <T> Response<T> error(Integer code, String message) {
            return Response.<T>builder()
                    .code(code)
                    .message(message)
                    .build();
        }
    }

    /**
     * Page Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse<T> {
        private List<T> content;
        private Integer page;
        private Integer size;
        private Long total;
        private Integer totalPages;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnlineStatusInfo {
        private String roleId;
        private Boolean online;
        private Long timestamp;
    }
}

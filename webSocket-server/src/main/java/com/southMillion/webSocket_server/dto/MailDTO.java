package com.SouthMillion.webSocket_server.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mail DTOs for Feign Client Integration
 */
public class MailDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMailRequest {
        private Integer type; // 1=System, 2=Player, 3=Reward, 4=Notice
        private String senderId;
        private String senderName;
        private String receiverId;
        private String title;
        private String content;
        private Integer expirationDays;
        private List<AttachmentInfo> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Integer attachmentType; // 1=Gold, 2=Gems, 3=Item, 4=Equipment, 5=EXP
        private String itemId;
        private String itemName;
        private Integer quantity;
        private Integer quality;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailInfo {
        private Long id;
        private Integer type;
        private String senderId;
        private String senderName;
        private String receiverId;
        private String title;
        private String content;
        private Boolean isRead;
        private Boolean isClaimedAttachment;
        private Boolean hasAttachments;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private List<AttachmentInfo> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MailListResponse {
        private Integer totalCount;
        private Integer unreadCount;
        private List<MailInfo> mails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimAttachmentResponse {
        private Long mailId;
        private List<AttachmentInfo> claimedAttachments;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkMailRequest {
        private Integer type;
        private String senderId;
        private String senderName;
        private List<String> receiverIds;
        private String title;
        private String content;
        private Integer expirationDays;
        private List<AttachmentInfo> attachments;
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

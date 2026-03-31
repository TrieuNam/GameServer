package com.SouthMillion.mail_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class MailDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMailRequest {
        @NotNull
        @Min(1) @Max(4)
        private Integer type; // 1=System, 2=Player, 3=Reward, 4=Notice
        
        private String senderId;
        
        private String senderName;
        
        @NotBlank
        private String receiverId;
        
        @NotBlank
        @Size(min = 1, max = 100)
        private String title;
        
        @Size(max = 1000)
        private String content;
        
        @Min(1) @Max(30)
        @Builder.Default
        private Integer expirationDays = 7; // Default 7 days
        
        private List<AttachmentInfo> attachments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        @NotNull
        @Min(1) @Max(5)
        private Integer attachmentType; // 1=Gold, 2=Gems, 3=Item, 4=Equipment, 5=EXP
        
        private String itemId;
        
        private String itemName;
        
        @Min(1)
        @Builder.Default
        private Integer quantity = 1;
        
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
        @NotNull
        @Min(1) @Max(4)
        private Integer type;
        
        private String senderId;
        
        private String senderName;
        
        @NotEmpty
        private List<String> receiverIds; // Multiple receivers
        
        @NotBlank
        @Size(min = 1, max = 100)
        private String title;
        
        @Size(max = 1000)
        private String content;
        
        @Min(1) @Max(30)
        @Builder.Default
        private Integer expirationDays = 7;
        
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
        
        public static <T> Response<T> success(T data) {
            return Response.<T>builder().code(0).message("Success").data(data).build();
        }
        
        public static <T> Response<T> error(Integer code, String message) {
            return Response.<T>builder().code(code).message(message).build();
        }
    }
}

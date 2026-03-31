package com.SouthMillion.guild_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Guild DTOs for API requests and responses
 */
public class GuildDTO {

    /**
     * Create Guild Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Guild name cannot be blank")
        @Size(min = 2, max = 20, message = "Guild name must be 2-20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$", message = "Guild name can only contain letters, numbers, and Chinese characters")
        private String name;

        @NotBlank(message = "Leader ID cannot be blank")
        private String leaderId;

        @Size(max = 500, message = "Notice cannot exceed 500 characters")
        private String notice;
    }

    /**
     * Guild Info Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InfoResponse {
        private Long id;
        private String name;
        private String leaderId;
        private String leaderName;
        private Integer level;
        private Long exp;
        private Long expForNextLevel;
        private Integer memberCount;
        private Integer maxMembers;
        private String notice;
        private Long funds;
        
        // Technologies
        private Integer techAttack;
        private Integer techDefense;
        private Integer techHp;
        private Integer techCrit;
        private Integer techSpeed;
        
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Optional: rank info
        private Long rank;
    }

    /**
     * Guild List Item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String name;
        private String leaderName;
        private Integer level;
        private Integer memberCount;
        private Integer maxMembers;
        private Long power; // Total guild power
        private Boolean recruiting; // Is accepting members
    }

    /**
     * Search Guild Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String keyword;
        
        @Min(0)
        private Integer page = 0;
        
        @Min(1)
        @Max(100)
        private Integer size = 20;
    }

    /**
     * Join Guild Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        @NotNull(message = "Guild ID cannot be null")
        private Long guildId;
        
        @NotBlank(message = "Role ID cannot be blank")
        private String roleId;
        
        @NotBlank(message = "Role name cannot be blank")
        private String roleName;
        
        @Min(1)
        private Integer roleLevel;
        
        @Min(0)
        private Long power;
        
        @Size(max = 200, message = "Message cannot exceed 200 characters")
        private String message;
    }

    /**
     * Member Info Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private Integer rank; // 1=Member, 2=Officer, 3=Leader
        private String rankName; // "Leader", "Officer", "Member"
        private Long contribution;
        private Boolean online;
        private LocalDateTime lastOnlineTime;
        private LocalDateTime joinedAt;
    }

    /**
     * Donate Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonateRequest {
        @NotNull(message = "Guild ID cannot be null")
        private Long guildId;
        
        @NotBlank(message = "Role ID cannot be blank")
        private String roleId;
        
        @Min(1000)
        @Max(100000)
        private Long amount; // Gold amount to donate
    }

    /**
     * Upgrade Tech Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpgradeTechRequest {
        @NotNull(message = "Guild ID cannot be null")
        private Long guildId;
        
        @NotBlank(message = "Role ID cannot be blank")
        private String roleId;
        
        @NotBlank(message = "Tech type cannot be blank")
        @Pattern(regexp = "^(ATTACK|DEFENSE|HP|CRITICAL|SPEED|ATK|DEF|CRT|SPD)$", 
                 message = "Invalid tech type")
        private String techType;
    }

    /**
     * Transfer Leadership Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferLeaderRequest {
        @NotNull(message = "Guild ID cannot be null")
        private Long guildId;
        
        @NotBlank(message = "Current leader ID cannot be blank")
        private String currentLeaderId;
        
        @NotBlank(message = "New leader ID cannot be blank")
        private String newLeaderId;
    }

    /**
     * Edit Notice Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditNoticeRequest {
        @NotNull(message = "Guild ID cannot be null")
        private Long guildId;
        
        @NotBlank(message = "Role ID cannot be blank")
        private String roleId;
        
        @Size(max = 500, message = "Notice cannot exceed 500 characters")
        private String notice;
    }

    /**
     * Application Info Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        private Long id;
        private Long guildId;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private String message;
        private Integer status; // 0=Pending, 1=Approved, 2=Rejected
        private LocalDateTime appliedAt;
    }

    /**
     * Process Application Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessApplicationRequest {
        @NotNull(message = "Application ID cannot be null")
        private Long applicationId;
        
        @NotBlank(message = "Processor ID cannot be blank")
        private String processorId;
        
        @NotNull(message = "Approve flag cannot be null")
        private Boolean approve; // true=approve, false=reject
    }

    /**
     * Generic Guild Response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response<T> {
        private Integer code; // 0=success, negative=error
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
     * Paginated Response
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
}

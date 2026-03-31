package com.SouthMillion.webSocket_server.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Guild DTOs for Feign Client Integration
 * Matches guild-service DTOs
 */
public class GuildDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGuildRequest {
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private String guildName;
        private String notice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuildInfo {
        private Long id;
        private String name;
        private String leaderId;
        private String leaderName;
        private Integer level;
        private Long exp;
        private Integer memberCount;
        private Integer maxMembers;
        private String notice;
        private Integer techAttack;
        private Integer techDefense;
        private Integer techHp;
        private Integer techCrit;
        private Integer techSpeed;
        private Long funds;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String keyword;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse {
        private Integer totalCount;
        private List<GuildInfo> guilds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinApplicationRequest {
        private Long guildId;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessApplicationRequest {
        private Long guildId;
        private String roleId;
        private Long applicationId;
        private Boolean approve;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonateRequest {
        private Long guildId;
        private String roleId;
        private Long goldAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonateResponse {
        private Long contribution;
        private Integer dailyDonationCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechUpgradeRequest {
        private Long guildId;
        private String roleId;
        private Integer techType; // 1=ATK, 2=DEF, 3=HP, 4=CRT, 5=SPD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechUpgradeResponse {
        private Integer newLevel;
        private Long cost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditNoticeRequest {
        private Long guildId;
        private String roleId;
        private String notice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferLeaderRequest {
        private Long guildId;
        private String currentLeaderId;
        private String newLeaderId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberListResponse {
        private Integer totalMembers;
        private List<MemberInfo> members;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private Integer rank; // 1=Member, 2=Officer, 3=Leader
        private Long contribution;
        private Boolean online;
        private LocalDateTime joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationListResponse {
        private Integer totalApplications;
        private List<ApplicationInfo> applications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        private Long id;
        private String roleId;
        private String roleName;
        private Integer roleLevel;
        private Long power;
        private String message;
        private LocalDateTime createdAt;
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

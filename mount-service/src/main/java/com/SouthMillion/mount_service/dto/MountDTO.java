package com.SouthMillion.mount_service.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Mount Data Transfer Object
 * Maps to MountData proto message
 */
@Data
public class MountDTO {
    
    private Long id;
    private String userId;
    private Integer mountIndex;
    private Integer mountId;
    private Integer level;
    private Integer grade;
    private Long exp;
    private Boolean isActive;
    private Boolean isEquipped;
    private Integer appearanceId;
    private Integer skinId;
    private Integer skinLevel;
    private Long exploreProgress;
    private Integer harnessSlot1;
    private Integer harnessSlot2;
    private Integer harnessSlot3;
    private Integer harnessSlot4;
    private Long combatPower;
    private Instant createdAt;
    private Instant updatedAt;
}

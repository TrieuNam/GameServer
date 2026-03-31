package com.SouthMillion.pet_service.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Pet Data Transfer Object
 * Maps to PB_PetInfo proto message
 */
@Data
public class PetDTO {
    
    /**
     * Pet unique ID
     */
    private Long petId;
    
    /**
     * Owner role ID
     */
    private String roleId;
    
    /**
     * Pet template ID (species/type)
     */
    private Integer petTemplateId;
    
    /**
     * Pet level
     */
    private Integer level;
    
    /**
     * Pet experience
     */
    private Integer petExp;
    
    /**
     * Pet star rating
     */
    private Integer petStar;
    
    /**
     * Pet status (0=inactive, 1=active, 2=locked)
     */
    private Integer petStatus;
    
    /**
     * Pet name (optional)
     */
    private String petName;
    
    /**
     * HP attribute
     */
    private Integer hp;
    
    /**
     * Attack attribute
     */
    private Integer attack;
    
    /**
     * Defense attribute
     */
    private Integer defense;
    
    /**
     * Speed attribute
     */
    private Integer speed;
    
    /**
     * Creation timestamp
     */
    private Instant createdAt;
    
    /**
     * Last update timestamp
     */
    private Instant updatedAt;
}

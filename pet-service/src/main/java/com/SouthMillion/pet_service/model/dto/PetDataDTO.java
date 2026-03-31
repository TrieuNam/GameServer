package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pet Data DTO
 * Maps to PB_SCRolePetData protocol message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetDataDTO {
    private Integer petIndex;
    private Integer petId;
    private Integer petLevel;
    private Long petExp;
    private Integer petOrder;
    private List<Integer> skillList;
    private List<Integer> gemItemId;
    private List<Integer> tsGemIndex;
    private List<Integer> attrList;
    private Long capability;
    private Integer skillLockFlag;
}

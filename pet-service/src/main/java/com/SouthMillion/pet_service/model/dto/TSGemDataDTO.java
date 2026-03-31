package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Special Gem Data DTO
 * Maps to PB_SCRoleTSGemData protocol message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TSGemDataDTO {
    private Integer gemIndex;
    private Integer gemLevel;
    private Integer petIndex;
    private List<Integer> attrType;
    private List<Integer> attrValue;
}

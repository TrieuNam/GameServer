package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cloth Data DTO
 * Maps to PB_SCRoleClothData protocol message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothDataDTO {
    private Integer itemId;
    private Integer level;
    private Integer petIndex;
}

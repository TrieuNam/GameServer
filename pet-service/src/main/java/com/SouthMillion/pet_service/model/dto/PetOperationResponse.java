package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pet Operation Response DTO
 * Maps to PB_SCRolePetRetInfo protocol message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetOperationResponse {
    private Integer retType;    // Return type (PetRetType)
    private Integer retP1;      // Return parameter 1
    private Integer retP2;      // Return parameter 2
}

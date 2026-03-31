package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pet Operation Request DTO
 * Maps to PB_CSRolePetReq protocol message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetOperationRequest {
    private Integer reqType;        // Operation type (PetOpType)
    private Integer param1;         // Parameter 1
    private Integer param2;         // Parameter 2
    private Integer param3;         // Parameter 3
    private List<Integer> paramList; // Parameter list
}

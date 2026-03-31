package com.SouthMillion.pet_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pet All Info Response DTO
 * Maps to PB_SCRolePetAllInfo protocol message
 * Sent on login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetAllInfoResponse {
    private List<Integer> fightPetIndex;      // Active fighting pets
    private List<PetDataDTO> petList;         // All pets
    private List<TSGemDataDTO> tsGemList;     // All special gems
    private List<ClothDataDTO> clothList;     // All clothing
}

package com.SouthMillion.gm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiveItemRequest {
    
    @NotBlank(message = "Player ID is required")
    private String playerId;
    
    @NotNull(message = "Item ID is required")
    private Long itemId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    
    private String reason;
}

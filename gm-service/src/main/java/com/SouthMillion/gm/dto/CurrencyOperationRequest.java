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
public class CurrencyOperationRequest {
    
    @NotBlank(message = "Player ID is required")
    private String playerId;
    
    @NotBlank(message = "Currency type is required")
    private String currencyType; // GOLD, DIAMOND, COIN, etc.
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Long amount;
    
    private String reason;
}

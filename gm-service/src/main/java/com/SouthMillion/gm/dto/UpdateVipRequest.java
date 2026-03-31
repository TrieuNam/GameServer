package com.SouthMillion.gm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVipRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Min(value = 0, message = "VIP level must be at least 0")
    @Max(value = 15, message = "VIP level must not exceed 15")
    private int vipLevel;
    
    private String reason;
}

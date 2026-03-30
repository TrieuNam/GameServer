package com.SouthMillion.gm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String type; // SYSTEM, NOTICE, EVENT, MAINTENANCE
    
    @Min(value = 1, message = "Duration must be at least 1 second")
    private int durationSeconds = 30;
}

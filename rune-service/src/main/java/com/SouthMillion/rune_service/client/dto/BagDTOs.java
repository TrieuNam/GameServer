package com.SouthMillion.rune_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BagDTOs {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseItemReq {
        private Integer itemId;
        private Integer amount;
    }
}

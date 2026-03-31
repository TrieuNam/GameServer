package com.SouthMillion.equip_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WearableItemsResponse {
    private String roleId;
    private List<WearableItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WearableItem {
        private Long bagItemId;
        private Integer itemId;
        private Integer num;
        private Integer quality;
        private Integer equipType;
    }
}


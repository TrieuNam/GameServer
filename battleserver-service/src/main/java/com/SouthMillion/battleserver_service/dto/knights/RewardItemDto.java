package com.SouthMillion.battleserver_service.dto.knights;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardItemDto {
    @JsonProperty("item_id")
    private String itemId;
    private String num;
}

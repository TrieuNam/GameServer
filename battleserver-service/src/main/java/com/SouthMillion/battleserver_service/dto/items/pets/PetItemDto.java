package com.SouthMillion.battleserver_service.dto.items.pets;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetItemDto {
    private String id;
    private String name;
    @JsonProperty("item_type")
    private String itemType;
    @JsonProperty("is_virtual")
    private String isVirtual;
    private String sellprice;
    @JsonProperty("pile_limit")
    private String pileLimit;
    private String isdroprecord;
    @JsonProperty("invalid_time")
    private String invalidTime;
    private String param;
    @JsonProperty("get_the_source")
    private String getTheSource;
}

package org.SouthMillion.dto.pet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PetWeaponDTO {
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("item_type")
    private int itemType;
    @JsonProperty("is_virtual")
    private int isVirtual;
    @JsonProperty("sellprice")
    private int sellPrice;
    @JsonProperty("pile_limit")
    private int pileLimit;
    @JsonProperty("isdroprecord")
    private int isDropRecord;
    @JsonProperty("invalid_time")
    private int invalidTime;
    @JsonProperty("param")
    private int param;
    @JsonProperty("get_the_source")
    private int getTheSource;
}

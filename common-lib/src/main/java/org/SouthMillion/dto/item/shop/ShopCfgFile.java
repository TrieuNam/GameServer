package org.SouthMillion.dto.item.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ShopCfgFile {
    private List<ShopCfgDTO> shop;

    @JsonProperty("shop_label")
    private List<List<Object>> shopLabel; // hoặc List<List<ShopLabelDTO>>

    @JsonProperty("shop_label_1")
    private List<List<Object>> shopLabel1;

    private List<List<Object>> skip;
}
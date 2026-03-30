package org.SouthMillion.dto.ShiZhuang;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.dto.item.shop.ClothShopItemDTO;

import java.util.List;

/**
 * Root wrapper DTO for cloth_shop.json
 * <pre>
 * {
 *   "shop": [ { "id": 1, "clothes_id": 101, ... }, ... ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothShopConfigDTO {

    @JsonProperty("shop")
    private List<ClothShopItemDTO> shop;
}

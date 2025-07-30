package org.SouthMillion.dto.item.gem;

import lombok.Data;

@Data
public class GemCompoundDTO {
    private Integer gemType;
    private Integer level;
    private Integer needGemType;
    private Integer compoundNum;
    private Integer price;
    private Integer priceCount;
}

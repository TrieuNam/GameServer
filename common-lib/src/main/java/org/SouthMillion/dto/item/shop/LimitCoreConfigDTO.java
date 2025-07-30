package org.SouthMillion.dto.item.shop;

import lombok.Data;

import java.util.List;

@Data
public class LimitCoreConfigDTO {
    private List<LimitCoreDTO> core;
    private List<CoreBoxDTO> corebox;
    private List<LimitCoreOtherDTO> other;
    private List<ThreeBoxDTO> threebox;
}

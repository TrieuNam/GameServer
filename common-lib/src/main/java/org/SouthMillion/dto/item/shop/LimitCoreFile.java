package org.SouthMillion.dto.item.shop;

import lombok.Data;

import java.util.List;

@Data
public class LimitCoreFile {
    private List<LimitCoreDTO> core;

    private List<CoreBoxDTO> corebox;

    private List<OtherDTO> other;

    private List<ThreeBoxDTO> threebox;
}
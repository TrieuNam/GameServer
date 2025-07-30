package org.SouthMillion.dto.item.gem;

import lombok.Data;

import java.util.List;

@Data
public class GemDrawingDTO {
    private Integer level;
    private List<GemInlayDTO> gemList;
}
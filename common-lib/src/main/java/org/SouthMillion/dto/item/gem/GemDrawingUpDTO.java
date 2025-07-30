package org.SouthMillion.dto.item.gem;

import lombok.Data;

import java.util.List;

@Data
public class GemDrawingUpDTO {
    private Integer gemDrawingId;
    private Integer gemDrawingLevel;
    private Integer isTsDrawing;
    private List<GemDrawingUpAttrDTO> gemDrawing;
    private Integer gemLevel;
}
